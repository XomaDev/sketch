package xyz.kumaraswamy.sketch.processor;

import xyz.kumaraswamy.sketch.Sketch;
import xyz.kumaraswamy.sketch.lex.TokenType;
import xyz.kumaraswamy.sketch.lex.Token;

import java.util.ArrayList;
import java.util.List;

import static xyz.kumaraswamy.sketch.lex.TokenType.*;

public class Parser {

    public static class ParseException extends RuntimeException {
    }

    private static final TokenType[] BINARY_OPERATORS = {
            EQUAL, PLUS, MINUS,
            SLASH, STAR, PERCENTAGE
    };

    private static final TokenType[] LOGICAL_OPERATORS = {
            EQUAL_EQUAL, NOT_EQUAL,
            ABOVE, BELOW,
            ABOVE_EQUAL, BELOW_EQUAL,
            LOGICAL_OR, LOGICAL_AND,
            BITWISE_OR, BITWISE_AND
    };
    // ==, >, <, >=, <=, ||, &&, |, &

    private final List<Token> tokens;

    private int current = 0;

    private int posCache = -1;
    private Token cache = null;
    private boolean skipExpr = false;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public List<Expression> parseTokens() {
        if (peek().type == TokenType.EOF) {
            return new ArrayList<>();
        }
        return parse();
    }

    private List<Expression> parse() {
        final ArrayList<Expression> expressions = new ArrayList<>();

        Expression expr = primary(next());
        if (skipExpr) {
            skipExpr = false;
            return expressions;
        }
        expressions.add(expr);

        while (peek().matches(TokenType.SEMICOLON)) {
            next(); // eat ';'
            while (peek().matches(SEMICOLON)) {
                next(); // eat extra ';'
            }
            if (peek().matches(TokenType.EOF, RIGHT_BRACE)) {
                return expressions;
            }
            expr = primary(next());
            if (skipExpr) {
                skipExpr = false;
                continue;
            }
            expressions.add(expr);
        }
        if (!peek().matches(TokenType.EOF, RIGHT_BRACE)) {
            Sketch.error(peek(), "Unexpected token");
            throw new ParseException();
        }
        return expressions;
    }

    private Expression primary(Token token) {
        return switch (token.type) {
            case FUN -> fun();
            case IF -> ifIdentifier();
            case FOR -> forIdentifier(token);
            case WHILE -> whileIdentifier();
            case EACH -> eachIdentifier();

            case BREAK -> Expression.Break.BREAK;
            case CONTINUE -> Expression.Continue.CONTINUE;
            case RETURN -> new Expression.Return(expr());
            case FORWARD -> forwardIdentifier();

            case LEV, VAL -> datatype(token);
            case WITH -> withIdentifier();
            default -> value(token);
        };
    }

    private Expression withIdentifier() {
        // with sketch.random : random
        // with [from [dot] function] : [as identifier]
        Token from = consume(IDENTIFIER,
                "Expected [from] identifier");
        consume(DOT, "Expected '.'");
        Token function = consume(IDENTIFIER,
                "Expected [function] identifier");
        Token as = peek().type == SEMICOLON
                ? null
                : consume(IDENTIFIER,
                "Expected [as] identifier");
        return new Expression.With(from, function, as);
    }

    private Expression eachIdentifier() {
        Token valName = consume(IDENTIFIER,
                "Expected val identifier");
        consume(TokenType.LEFT_RIGHT, "Expected '->'");
        Token elementName = consume(IDENTIFIER,
                "Expected val element identifier");
        List<Expression> block = blockBody();
        return new Expression.Each(valName, elementName, block);
    }

    private Expression forIdentifier(Token token) {
        Token valName = consume(IDENTIFIER,
                "Expected variable identifier");
        consume(TokenType.LEFT_PAREN, "Expected '('");
        Expression range = expr();
        if (!(range instanceof Expression.Range)) {
            Sketch.error(token, "Expected a valid range -> or <-");
            // never reached
            return null;
        }
        consume(TokenType.RIGHT_PAREN, "Expected ')'");
        return new Expression.For(valName,
                (Expression.Range) range, blockBody());
    }

    private Expression value(Token token) {
        switch (token.type) {
            case NULL:
            case TRUE:
            case FALSE:
            case NUMBER:
            case STRING:
                return new Expression.Literal<>(token.literal);
            case IDENTIFIER:
                Token peek = peek();
                switch (peek.type) {
                    case LEFT_PAREN:
                        // function call
                        // exprId -> functionName
                        return new Expression.FunCall(token, arguments());
                    case EQUAL:
                    case LEFT_SQUARE:
                        // because assignment/array_access can be a normal statement
                        // like z = z = 1;
                        // like array[z] = x;
                        return expression(literal(token));
                    case INCREMENT:
                    case DECREMENT:
                        next(); // eat '++' or '--'
                        return new Expression.BinaryUnary(false, token, peek);
                    case DOT:
                        next(); // eat '.'
                        return getPropertyIdentifier(token);
                    default:
                        return literal(token);
                }
            case INCREMENT:
            case DECREMENT:
                // parse from the left
                Token id = consume(IDENTIFIER,
                        "Expected identifier after " +
                                token.type.name() + " operator");
                return new Expression.BinaryUnary(true, id, token);
            case LEFT_PAREN:
                Expression expression = expr();
                consume(TokenType.RIGHT_PAREN, "Expected ')'");
                return expression;
            case LEFT_SQUARE:
                List<Expression> array = array();
                consume(TokenType.RIGHT_SQUARE, "Expected ']'");
                return new Expression.Array(array);
            case MINUS:
            case EXCLAMATION:
                // negate operator, exclamation operator
                // negate var -> -(*negate*)
                // exclamation var -> !(*bool*)
                return new Expression.Unary(token, expr());
            case THIS:
                consume(TokenType.DOT, "Invalid value access after [.]");
                return getPropertyIdentifier(token);
        }
        return null;
    }

    private List<Expression> array() {
        List<Expression> args = new ArrayList<>();
        if (peek().type == TokenType.RIGHT_SQUARE) {
            next(); // eat ')'
            return args;
        }
        args.add(expr());

        while (peek().type == TokenType.COMMA) {
            next(); // eat ','
            args.add(expr());
        }
        return args;
    }

    private Expression getPropertyIdentifier(Token token) {
        Token id = consume(IDENTIFIER,
                "Expected identifier after [.] property access");
        Expression expr = new Expression.PropertyIdentifier(token, id);
        if (peek().type == TokenType.EQUAL) return expression(expr);
        return expr;
    }

    private Expression datatype(Token type) {
        Token valId = consume(IDENTIFIER,
                "Variable identifier expected");
        Expression expr;
        if (peek().type == TokenType.SEMICOLON) {
            // empty initialization
            // val z;, default value = null
            expr = new Expression.Literal<>(null);
        } else {
            consume(TokenType.EQUAL, "Expected '='");
            expr = expr();
        }
        // arg false, because we are
        // creating a new variable and not
        // just changing a value of variable that
        // already exists
        return new Expression.Val(type.type,
                false, new Expression.Val.ValId(valId), expr);
    }

    private Expression forwardIdentifier() {
        Token peek = peek();
        Expression val;
        if (peek.type == TokenType.SEMICOLON) {
            val = new Expression.Literal<>(1);
        } else {
            val = expr();
        }
        return new Expression.Forward(val);
    }

    private Expression ifIdentifier() {
        Expression condition = consumeExpression();
        List<Expression> then = blockBody();

        if (peek(1).type == ELSE) {
            next(); // eat ';' added by parser
            next(); // eat else
            return new Expression.If(condition, then, blockBody());
        }

        if (!then.isEmpty() && then.get(0)
                instanceof Expression.Interruption interrupt) {
            // label after ';'
            if (!peek(1).matches(  RETURN, FORWARD, BREAK, CONTINUE  )) {
                return new Expression.If(condition, then);
            }
            next(); // eat current ';' after if expr
            Token type = next(); // eat interruption label like 'return'
            // expression of interruption
            // ex;
            // return 2 + 2;
            // return <expression>
            Expression expression = interrupt.expr();

            if (expression != null) {
                Expression.Ternary ternary =
                        new Expression.Ternary(
                                condition, expression, expr());
                switch (type.type) {
                    case RETURN:
                        return new Expression.Return(ternary);
                    case FORWARD:
                        return new Expression.Forward(ternary);
                }
            }

        }

        // eat semicolons after } if there
        // are any
        return new Expression.If(condition, then);
    }

    private Expression whileIdentifier() {
        Expression cond = consumeExpression();
        List<Expression> body = blockBody();
        Expression wh = new Expression.While(cond, body);
        return wh;
    }

    private Expression consumeExpression() {
        consume(TokenType.LEFT_PAREN, "Expected '(");
        Expression expr = expr();
        consume(TokenType.RIGHT_PAREN, "Expected ')'");
        return expr;
    }

    private Expression literal(Token token) {
        return token.type == IDENTIFIER
                ? new Expression.Identifier(token)
                : new Expression.Literal.Literal<>(token.literal);
    }

    private List<Expression> arguments() {
        consume(TokenType.LEFT_PAREN, "Expected '('");
        List<Expression> args = new ArrayList<>();
        if (peek().type == TokenType.RIGHT_PAREN) {
            next(); // eat ')'
            return args;
        }
        args.add(expr());

        while (peek().type == TokenType.COMMA) {
            next(); // eat ','
            args.add(expr());
        }
        consume(TokenType.RIGHT_PAREN, "Expected ')'");
        return args;
    }

    private Expression fun() {
        // fun <funId,(,args), {body}
        Token funId = consume(IDENTIFIER,
                "Expect identifier after 'val' keyword");
        List<Token> args = funArgIdentifiers();
        List<Expression> fun = blockBody();
        return new Expression.Fun(funId, args, fun);
    }

    private List<Expression> blockBody() {
        consume(TokenType.LEFT_BRACE, "Expected '{'");
        // parse until, we find the right brace '}'
        if (peek().type == TokenType.RIGHT_BRACE) {
            // empty body
            next(); // eat '}';
            addSemicolon();
            return new ArrayList<>();
        }
        List<Expression> expressions = parse();
        consume(RIGHT_BRACE, "Expected '}'");
        addSemicolon();
        return expressions;
    }

    private void addSemicolon() {
        tokens.add(current, new Token(SEMICOLON,
                ";", ";", -1));
    }

    private List<Token> funArgIdentifiers() {
        consume(TokenType.LEFT_PAREN, "Expected '('");
        List<Token> ids = new ArrayList<>();
        if (peek().type == TokenType.RIGHT_PAREN) {
            next(); // eat '('
            return ids;
        }

        String msg = "Expected arg identifier";
        // eat identifier
        ids.add(consume(IDENTIFIER, msg));

        while (peek().type == TokenType.COMMA) {
            next(); // eat ','
            ids.add(consume(IDENTIFIER, msg));
        }
        consume(TokenType.RIGHT_PAREN, "Expected ')'");
        return ids;
    }

    private Expression expr() {
        // read next expression
        // value -> (left)
        return expression(value(next()));
    }

    private Expression expression(Expression left) {
        Token peek = peek();

        switch (peek.type) {
            case LEFT_RIGHT:
            case RIGHT_LEFT:
                next(); // eat '->' or '<-'
                // right side expression left -> right
                return new Expression.Range(peek, left, expr());
            case LEFT_SQUARE:
                next(); // eat '['
                Expression.ArrayAccess access =
                        new Expression.ArrayAccess(left, expr());
                consume(RIGHT_SQUARE, "Expected ']'");
                return expression(access);
            case THEN:
                next(); // consume 'then'
                Expression then = expr();
                consume(TokenType.OR, "Expected OR expression for inline expression");
                Expression or = expr();
                return new Expression.Ternary(left, then, or);
            case EQUAL:
                return assignmentExpression(left);
            default:
                if (peek.matches(BINARY_OPERATORS)) {
                    return binaryOperator(left);
                } else if (peek.matches(LOGICAL_OPERATORS)) {
                    return logicalOperator(left, peek);
                }
                return left;
        }
    }

    private Expression assignmentExpression(Expression left) {
        Token operator = next(); // eat '='
        // the operator, set
        // val z;
        // z = 7;
        if (!(left instanceof Expression.Identifier
                || left instanceof Expression.PropertyIdentifier
                || left instanceof Expression.ArrayAccess)) {
            Sketch.error(operator,
                    "Invalid assignment operation");
            return null;
        }
        return new Expression.Val(TokenType.VAL,
                true, new Expression.Val.ValId(left), expr());
    }

    private Expression logicalOperator(Expression left, Token peek) {
        Token logicalOp = next();

        Expression right;
        if (peek.matches(TokenType.LOGICAL_AND, TokenType.LOGICAL_OR)) {
            right = expr();
        } else {
            right = value(next());
            while (peek().matches(BINARY_OPERATORS)) {
                right = parseExpressionBind(right);
            }
        }
        return expression(
                new Expression.Logical(left, logicalOp, right));
    }

    private Expression binaryOperator(Expression left) {
        Token operator = next();
        Expression right = rightExpression();
        return expression(
                new Expression.Binary(
                        left, operator, right));
    }

    private Expression parseExpressionBind(Expression left) {
        if (!peek().matches(BINARY_OPERATORS)) {
            return left;
        }
        Token operator = next();
        Expression right = rightExpression();
        return new Expression.Binary(left, operator, right);
    }

    private Expression rightExpression() {
        Expression right = value(next());
        while (peek().matches(SLASH, STAR, PERCENTAGE)) {
            // bin(left, right)
            // expr(bin) - > continue
            right = parseExpression(right);
        }
        return right;
    }

    private Expression parseExpression(Expression left) {
        // consume current
        if (!peek().matches(Parser.BINARY_OPERATORS)) {
            return left;
        }
        Token operator = next();
        Expression right = value(next());
        return new Expression.Binary(left, operator, right);
    }

    private Token consume(TokenType type, String message) {
        Token token = next();
        if (token.type != type) {
            Sketch.error(token, message);
            throw new ParseException();
        }
        return token;
    }

    public Token next() {
        return tokens.get(current++);
    }

    public Token peek() {
        if (current == posCache)
            return cache;
        if (current == tokens.size())
            return new Token(TokenType.EOF, "", null, 0);
        Token token = tokens.get(current);
        posCache = current;
        return cache = token;
    }

    public Token peek(int n) {
        return tokens.get(current + n);
    }
}
