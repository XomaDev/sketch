package xyz.kumaraswamy.sketch.processor;

import xyz.kumaraswamy.sketch.Visit;
import xyz.kumaraswamy.sketch.lex.TokenType;
import xyz.kumaraswamy.sketch.lex.Token;

import java.util.List;

public abstract class Expression implements Visit {

    interface Visitor<R> {
        R visitBinaryExpr(Binary expr);
        R visitBinaryUnaryExpr(BinaryUnary expr);
        R visitUnaryExpr(Unary expr);
        R visitLogicalExpr(Logical expr);
        R visitGroupingExpr(Grouping expr);
        R visitLiteralExpr(Literal<?> expr);
        R visitSharedExpr(Shared expr);
        R visitValEpr(Val expr);
        R visitTernary(Ternary expr);
        R visitIfExpr(If expr);
        R visitRangeExpr(Range expr);
        R visitForExpr(For expr);
        R visitWhileExpr(While expr);
        R visitFunExpr(Fun expr);
        R visitReturnExpr(Return expr);
        R visitBreakExpr(Break expr);
        R visitContinueExpr(Continue expr);
        R visitForwardExpr(Forward expr);
        R visitFunCallExpr(FunCall expr);
        R visitIdentifierExpr(Identifier expr);
        R visitPropertyAccessExpr(PropertyIdentifier expr);
    }

    public static class Binary extends Expression {
        public Binary(Expression left, Token operator, Expression right) {
            if (right == null || left == null) {
                throw new RuntimeException();
            }
            this.left = left;
            this.operator = operator;
            this.right = right;
        }
        final Expression left;
        final Token operator;
        final Expression right;

        @Override
        public String visit() {
            return "(" + operator.lexeme + " " +
                    left.visit() + " " + right.visit() + ")";
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitBinaryExpr(this);
        }
    }

    // for increment and decrement operators
    public static class BinaryUnary extends Expression {

        public BinaryUnary(boolean left, Token valId, Token operator) {
            this.left = left;
            this.valId = valId;
            this.operator = operator;
        }

        final boolean left;
        final Token valId;
        final Token operator;

        @Override
        public String visit() {
            return "direct operator (" + left + " " +
                    valId.lexeme + " " + operator.lexeme + ")";
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitBinaryUnaryExpr(this);
        }
    }

    static class Logical extends Expression {

        public Logical(Expression left, Token operator, Expression right) {
            this.left = left;
            this.right = right;
            this.operator = operator;
        }

        Expression left;
        Token operator;
        Expression right;

        @Override
        public String visit() {
            return "(logical " + operator.lexeme + " " +
                    left.visit() + " " + right.visit() + ")";
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitLogicalExpr(this);
        }
    }

    public static class Grouping extends Expression {
        public Grouping(Expression expression) {
            this.expression = expression;
        }

        final Expression expression;

        @Override
        public String visit() {
            return expression.visit();
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitGroupingExpr(this);
        }
    }


    public static class Literal<T> extends Expression {
        public Literal(T value) {
            if (value instanceof Literal<?>) {
                throw new RuntimeException();
            }
            this.value = value;
        }
        T value;

        @Override
        public String visit() {
            return "(literal " + value + ")";
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitLiteralExpr(this);
        }
    }

    public static class Identifier extends Expression {
        public Identifier(Token token) {
            this.token = token;
        }
        Token token;

        @Override
        public String visit() {
            return String.valueOf(token);
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitIdentifierExpr(this);
        }
    }

    public static class PropertyIdentifier extends Expression {
        public PropertyIdentifier(Token name, Token property) {
            this.name = name;
            this.property = property;
        }

        final Token name;
        final Token property;

        @Override
        public String visit() {
            return "(" + name.lexeme + "." + property.lexeme + ")";
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitPropertyAccessExpr(this);
        }
    }

    public static class Unary extends Expression {
        public Unary(Token operator, Expression expression) {
            this.operator = operator;
            this.expression = expression;
        }
        Token operator;
        Expression expression;

        @Override
        public String visit() {
            return operator.lexeme + "(unary " + expression.visit() + ") ";
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitUnaryExpr(this);
        }
    }

    public static class Shared extends Expression {
        public Shared(Val val) {
            this.val = val;
        }
        Val val;

        @Override
        public String visit() {
            return "(shared " + val.visit() + ") ";
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitSharedExpr(this);
        }
    }

    public static class Val extends Expression {
        // if assignment is true, it means no
        // val keyword was used

        public static class ValId {

            private PropertyIdentifier property;
            private Token token;

            public ValId(Token token) {
                this.token = token;
            }

            public ValId(Expression valId) {
                if (valId instanceof Identifier id) {
                    this.token = id.token;
                } else if (valId instanceof PropertyIdentifier) {
                    this.property = (PropertyIdentifier) valId;
                }
            }

            public Object get() {
                if (token == null) return property;
                return token;
            }

            @Override
            public String toString() {
                return String.valueOf(get());
            }
        }

        public Val(TokenType datatype, boolean assignment, ValId valId, Expression expression) {
            this.datatype = datatype;
            this.assignment = assignment;
            this.valId = valId;
            this.expression = expression;
        }

        final TokenType datatype;
        boolean assignment;
        ValId valId;
        Expression expression;

        @Override
        public String visit() {
            return "(val " + datatype.name() + " " + (!assignment ? "new " : "") + valId + ") " + expression.visit();
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitValEpr(this);
        }
    }

    public static class Ternary extends Expression {

        public Ternary(Expression expr, Expression then, Expression or) {
            this.expr = expr;
            this.then = then;
            this.or = or;
        }

        final Expression expr;
        final Expression then;
        final Expression or;

        @Override
        public String visit() {
            return "(ternary " + expr.visit() + " then " +
                    then.visit() + " else " + or.visit() + ")";
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitTernary(this);
        }
    }

    public static class If extends Expression {

        public If(Expression expr, List<Expression> body, List<Expression> orElse) {
            this(expr, body);
            this.orElse = orElse;
        }

        public If(Expression expr, List<Expression> body) {
            this.expr = expr;
            this.body = body;
        }

        Expression expr;
        List<Expression> body;
        List<Expression> orElse = null;

        @Override
        public String visit() {
            return "(if " + expr.visit() + " then " + body + ")";
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitIfExpr(this);
        }
    }

    public static class Range extends Expression {

        public Range(Token type, Expression left, Expression right) {
            this.type = type;
            this.left = left;
            this.right = right;
        }

        final Token type;
        final Expression left;
        final Expression right;

        @Override
        public String visit() {
            return "range(" + left.visit() + " " +
                    type.lexeme + " " + right.visit() + ")";
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitRangeExpr(this);
        }
    }

    public static class For extends Expression {

        // if shared is true, the value
        // identifier for the looper will
        // be accessible outside the loop

        public For(Token valId,
                   Range range,
                   List<Expression> block) {
            this.valId = valId;
            this.range = range;
            this.block = block;
        }

        Token valId;
        Range range;
        List<Expression> block;

        @Override
        public String visit() {
            return "for(" + valId.lexeme + ", " + range.visit() + " " + block + ")";
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitForExpr(this);
        }
    }

    public static class While extends Expression {
        public While(Expression expr, List<Expression> body) {
            this.expr = expr;
            this.body = body;
        }

        Expression expr;
        List<Expression> body;

        @Override
        public String visit() {
            return "(while " + expr.visit() + " do " + body + ")";
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitWhileExpr(this);
        }
    }

    public static class Fun extends Expression {

        public Fun(Token funId, List<Token> args, List<Expression> expressions) {
            this.funId = funId;
            this.args = args;
            this.expressions = expressions;
        }

        Token funId;
        List<Token> args;
        List<Expression> expressions;

        @Override
        public String visit() {
            return "(fun " + args + ", " + expressions + ")";
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitFunExpr(this);
        }
    }

    public static class Return extends Expression {
        public Return(Expression expression) {
            this.expression = expression;
        }

        final Expression expression;

        @Override
        public String visit() {
            return "(return " + expression.visit() + ")";
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitReturnExpr(this);
        }
    }

    public static class Break extends Expression {

        public static final Break BREAK = new Break();

        private Break() {}

        @Override
        public String visit() {
            return "(break)";
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitBreakExpr(this);
        }
    }

    public static class Continue extends Expression {

        public static final Continue CONTINUE = new Continue();

        private Continue() {}

        @Override
        public String visit() {
            return "(continue)";
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitContinueExpr(this);
        }
    }

    public static class Forward extends Expression {

        public Forward(Expression expression) {
            this.expression = expression;
        }

        Expression expression;

        @Override
        public String visit() {
            return "forward(" + expression.visit() + ")";
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitForwardExpr(this);
        }
    }

    public static class FunCall extends Expression {

        public FunCall(Token funId, List<Expression> args) {
            this.funId = funId;
            this.args = args;
        }

        final Token funId;
        final List<Expression> args;

        @Override
        public String visit() {
            return "(funcall " + funId.lexeme + " " + args + ")";
        }

        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visitFunCallExpr(this);
        }
    }

    public abstract <R> R accept(Visitor<R> visitor);

    @Override
    public String toString() {
        return visit();
    }
}
