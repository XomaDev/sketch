package xyz.kumaraswamy.sketch.processor;

import xyz.kumaraswamy.sketch.Slime;
import xyz.kumaraswamy.sketch.lex.TokenType;
import xyz.kumaraswamy.sketch.lex.Token;

import java.util.ArrayList;
import java.util.List;

public class Parser {

    public static class ParseException extends RuntimeException {}

    private static final TokenType[]
            BINARY_OPERATORS = {    TokenType.EQUAL, TokenType.PLUS, TokenType.MINUS,
            TokenType.SLASH, TokenType.STAR, TokenType.PERCENTAGE    };

    private static final TokenType[]
            LOGICAL_OPERATORS = {    TokenType.BANG_EQUAL, TokenType.NOT_EQUAL, TokenType.ABOVE, TokenType.BELOW, TokenType.ABOVE_EQUAL, TokenType.BELOW_EQUAL,
                                     TokenType.LOGICAL_OR, TokenType.LOGICAL_AND, TokenType.BITWISE_OR, TokenType.BITWISE_AND
                                }; // ==, >, <, >=, <=, ||, &&, |, &

    private static final TokenType[]
            INLINE_OPERATORS = {    TokenType.THEN    };

    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public List<Expression> parse() {
        if (peek().type == TokenType.EOF) {
            return new ArrayList<>();
        }
        return parse(TokenType.EOF);
    }

    private List<Expression> parse(TokenType endAt) {
        List<Expression> expressions = new ArrayList<>();

        Token token = next();
        expressions.add(primary(token));

        while (peek().matches(  TokenType.SEMICOLON   )) {
            next(); // eat ';'
            if (peek().matches(TokenType.EOF, endAt)) {
                return expressions;
            }
            Token next = next();
            expressions.add(primary(next));
        }
        if (!peek().matches(TokenType.EOF, endAt)) {
            Slime.error(peek(), "Unexpected token");
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

            case BREAK -> breakIdentifier();
            case CONTINUE -> continueIdentifier();
            case RETURN -> new Expression.Return(expr());
            case FORWARD -> forwardIdentifier();

            case LEV, VAL -> datatype(token);
            default -> value(token);
        };
    }

    private Expression forIdentifier(Token token) {
        Token valName = consume(TokenType.IDENTIFIER,
                "Expected variable identifier");
        consume(TokenType.LEFT_PAREN, "Expected '('");
        Expression range = expr();
        System.out.println(range);
        if (!(range instanceof Expression.Range)) {
            Slime.error(token, "Expected a valid range -> or <-");
            // never reached
            return null;
        }
        consume(TokenType.RIGHT_PAREN, "Expected ')'");
        return new Expression.For(valName,
                (Expression.Range) range, blockBody());
    }

    private Expression value(Token token) {
        switch (token.type) {
            case NULL: case TRUE: case FALSE:
            case NUMBER: case STRING:
                return new Expression.Literal<>(token.literal);
            case IDENTIFIER:
                Token peek = peek();
                if (peek.type == TokenType.LEFT_PAREN) {
                    // function call
                    // exprId -> functionName
                    return new Expression.FunCall(token, arguments());
                } else if (peek.type == TokenType.EQUAL) {
                    // because assignment can be a normal statement
                    // like z = z = 1;
                    return expression(literal(token));
                } else if (peek.matches(  TokenType.INCREMENT, TokenType.DECREMENT  )) {
                    next(); // eat '++' or '--'
                    return new Expression.BinaryUnary(false, token, peek);
                } else if (peek.matches(  TokenType.DOT  )) {
                    next(); // eat '.'
                    return getPropertyIdentifier(token);
                }
                return literal(token);
            case INCREMENT:
            case DECREMENT:
                // parse from the left
                Token id = consume(TokenType.IDENTIFIER,
                        "Expected identifier after " +
                                token.type.name() + " operator");
                return new Expression.BinaryUnary(true, id, token);
            case LEFT_PAREN:
                Expression expression = expr();
                consume(TokenType.RIGHT_PAREN, "Expected ')'");
                return new Expression.Grouping(expression);
            case MINUS:
            case EXCLAMATION:
                // negate operator, exclamation operator
                // negate var -> -(*negate*)
                // exclamation var -> !(*bool*)
                Expression expr = value(next());
                return new Expression.Unary(token, expr);
            case THIS:
                consume(TokenType.DOT, "Invalid value access after [.]");
                return getPropertyIdentifier(token);
        }
        return null;
    }

    private Expression getPropertyIdentifier(Token token) {
        Token id = consume(TokenType.IDENTIFIER,
                "Expected identifier after [.] property access");
        Expression expr = new Expression.PropertyIdentifier(token, id);
        if (peek().type == TokenType.EQUAL) {
            return expression(expr);
        }
        return expr;
    }

    private Expression datatype(Token type) {
        Token valId = consume(TokenType.IDENTIFIER,
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

    private Expression breakIdentifier() {
        Token peek = peek();
        if (!peek.matches(  TokenType.SEMICOLON  )) {
            Slime.error(peek, "Unexpected token after 'break'");
            return null;
        }
        // break constant to reuse
        return Expression.Break.BREAK;
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

    private Expression continueIdentifier() {
        Token peek = peek();
        if (peek.type != TokenType.SEMICOLON) {
            Slime.error(peek, "Unexpected token after 'continue'");
            return null;
        }
        // break constant to reuse
        return Expression.Continue.CONTINUE;
    }

    private Expression ifIdentifier() {
        Expression expr = consumeExpression();
        List<Expression> then = blockBody();

        if (peek().matches(TokenType.ELSE)) {
            next(); // eat 'else'
            List<Expression> elseExpr = blockBody();
            return new Expression.If(expr, then, elseExpr);
        }
        return new Expression.If(expr, then);
    }

    private Expression whileIdentifier() {
        return new Expression.While(
                consumeExpression(), blockBody());
    }


    private Expression consumeExpression() {
        consume(TokenType.LEFT_PAREN, "Expected '(");
        Expression expr = expr();
        consume(TokenType.RIGHT_PAREN, "Expected ')'");
        return expr;
    }

    private Expression literal(Token token) {
        return token.type == TokenType.IDENTIFIER
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

        while ( peek().type == TokenType.COMMA ) {
            next(); // eat ','
            args.add(expr());
        }
        consume(TokenType.RIGHT_PAREN, "Expected ')'");
        return args;
    }

    private Expression fun() {
        // fun <funId,(,args), {body}
        Token funId = consume(TokenType.IDENTIFIER,
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
            return new ArrayList<>();
        }
        List<Expression> expressions = parse(TokenType.RIGHT_BRACE);
        consume(TokenType.RIGHT_BRACE, "Expected '}'");
        return expressions;
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
        ids.add(consume(TokenType.IDENTIFIER, msg));

        while ( peek().type == TokenType.COMMA ) {
            next(); // eat ','
            ids.add(consume(TokenType.IDENTIFIER, msg));
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
        if (peek.matches(  TokenType.LEFT_RIGHT, TokenType.RIGHT_LEFT  )) {
            next(); // eat '->' or '<-'
            // right side expression left -> right
            Expression right = expr();
            return new Expression.Range(peek, left, right);
        } else if (peek.matches(  INLINE_OPERATORS  )) {
            next(); // consume 'then'
            Expression then = expr();
            consume(TokenType.OR, "Expected OR expression for inline expression");
            Expression or = expr();
            return new Expression.Ternary(left, then, or);
        } else if (peek.matches(BINARY_OPERATORS)) {
            // consume current
            Token operator = next();
            if (operator.type == TokenType.EQUAL) {
                // the operator, set
                // val z;
                // z = 7;
                if (!(left instanceof Expression.Identifier
                        || left instanceof Expression.PropertyIdentifier)) {
                    Slime.error(operator,
                            "Requires val id to be an identifier");
                    return null; // never reached
                }
                return new Expression.Val(TokenType.VAL, true, new Expression.Val.ValId(left), expr());
            }
            Expression right = parseRightExpression();
            return expression(new Expression.Binary(
                    left, operator, right));
        } else if (peek.matches(LOGICAL_OPERATORS)) {
            Token logicalOp = next();

            Expression right;
            if (peek.matches(  TokenType.LOGICAL_AND, TokenType.LOGICAL_OR  )) {
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
        return left;
    }

    private Expression parseExpressionBind(Expression left) {
        if (!peek().matches(BINARY_OPERATORS)) {
            return left;
        }
        Token operator = next();
        Expression right = parseRightExpression();
        return new Expression.Binary(left, operator, right);
    }

    private Expression parseRightExpression() {
        Expression right = value(next());
        while (peek().matches(TokenType.SLASH, TokenType.STAR, TokenType.PERCENTAGE)) {
            // bin(left, right)
            // expr(bin) - > continue
            right = parseExpression(right);
        }
        return right;
    }

    private Expression parseExpression(Expression left) {
        // consume current
        if (!peek().matches( Parser.BINARY_OPERATORS  )) {
            return left;
        }
        Token operator = next();
        Expression right = value(next());
        return new Expression.Binary(left, operator, right);
    }

    private Token consume(TokenType type, String message) {
        Token token = next();
        if (token.type != type) {
            Slime.error(token, message);
            throw new ParseException();
        }
        return token;
    }

    public Token next() {
        return tokens.get(current++);
    }

    public Token peek() {
        if (current == tokens.size()) {
            return new Token(TokenType.EOF, "", null, 0);
        }
        return tokens.get(current);
    }
}
