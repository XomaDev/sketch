package xyz.kumaraswamy.sketch.processor;

import xyz.kumaraswamy.sketch.Slime;
import xyz.kumaraswamy.sketch.lex.TokenType;
import xyz.kumaraswamy.sketch.memory.GlobalMemory;
import xyz.kumaraswamy.sketch.memory.LowerMemory;
import xyz.kumaraswamy.sketch.nativs.Native;
import xyz.kumaraswamy.sketch.lex.Token;

import java.util.List;

public class Evaluator implements Expression.Visitor<Object> {

    GlobalMemory fMemory;
    GlobalMemory memory;

    public Evaluator(GlobalMemory memory) {
        this.fMemory = memory;
        this.memory = memory;
    }

    public Object evaluate(Expression expr) {
        return expr.accept(this);
    }

    private void lowerMemory(String name) {
        memory = new LowerMemory(name, memory);
    }

    private void upperMemory() {
        if (memory instanceof LowerMemory lMemory) {
            memory.delete();
            memory = lMemory.getSMemory();
            return;
        }
        throw new RuntimeError("Already a super memory!");
    }

    @Override
    public Object visitLiteralExpr(Expression.Literal<?> expr) {
        return expr.value;
    }

    @Override
    public Object visitIdentifierExpr(Expression.Identifier expr) {
        return memory.getVal(expr.token.lexeme);
    }

    @Override
    public Object visitPropertyAccessExpr(Expression.PropertyIdentifier expr) {
        if (expr.name.type == TokenType.THIS) {
            return fMemory.getVal(expr.property.lexeme);
        }
        return null;
    }

    @Override
    public Object visitBinaryExpr(Expression.Binary expr) {
        TokenType type = expr.operator.type;

        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        if (  type == TokenType.PLUS  ) {
            if (  left == null && right == null  ) {
                cannotApplyOperator(expr.operator);
                return null; // not reached
            }
            if (  left == null  ) {
                left = "null";
            } else if (  right == null  ) {
                right = "null";
            }
            if (left instanceof Double
                    && right instanceof Double) {
                return (double) left + (double) right;
            }
            if (left instanceof String
                    || right instanceof String) {
                return String.valueOf(left) + right;
            }
            cannotApplyOperator(expr.operator);
        } else if (  type == TokenType.MINUS  ) {
            if (left instanceof Double
                    && right instanceof Double) {
                return (double) left - (double) right;
            }
            cannotApplyOperator(expr.operator);
        } else if (  type == TokenType.STAR  ) {
            if (left instanceof Double
                    && right instanceof Double) {
                return (double) left * (double) right;
            }
            cannotApplyOperator(expr.operator);
        } else if (  type == TokenType.SLASH  ) {
            if (left instanceof Double
                    && right instanceof Double) {
                return (double) left / (double) right;
            }
            cannotApplyOperator(expr.operator);
        } else if (  type == TokenType.PERCENTAGE  ) {
            if (left instanceof Double
                    && right instanceof Double) {
                return (double) left % (double) right;
            }
            cannotApplyOperator(expr.operator);
        }
        return null;
    }

    @Override
    public Object visitBinaryUnaryExpr(Expression.BinaryUnary expr) {
        String valId = expr.valId.lexeme;
        Object val = memory.getVal(valId);
        if (  val instanceof Double x  ) {
            if (expr.operator.matches(  TokenType.INCREMENT,
                    TokenType.DECREMENT  )) {
                int n = expr.operator.type ==
                        TokenType.INCREMENT ? 1 : -1;
                memory.push(valId, x + n);
                return expr.left ? x + n : x;
            }
        }
        cannotApplyOperator(expr.operator);
        return null;
    }

    @Override
    public Object visitUnaryExpr(Expression.Unary expr) {
        TokenType type = expr.operator.type;
        Object value = evaluate(expr.expression);

        if (  type == TokenType.MINUS  ) {
            if (  value instanceof Double  ) {
                return -((double) value);
            }
            cannotApplyOperator(expr.operator);
        } else if (  type == TokenType.EXCLAMATION  ) {
            if (  value instanceof Boolean bool  ) {
                return !bool;
            }
        }
        return null;
    }

    @Override
    public Object visitLogicalExpr(Expression.Logical expr) {
        TokenType type = expr.operator.type;

        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        if (  type == TokenType.BANG_EQUAL  ) {
            if (left == null && right == null) {
                return true;
            }
            // if only one of them is null, return
            // false
            if (left == null || right == null) {
                return false;
            }
            return left.equals(right);
        } else if (  type == TokenType.NOT_EQUAL  ) {
            if (left == null && right == null) {
                return false;
            }
            if (left == null || right == null) {
                return true;
            }
            return !left.equals(right);
        } else if (  type == TokenType.LOGICAL_AND  ) {
            if (left instanceof Boolean && right instanceof Boolean) {
                return (boolean) left && (boolean) right;
            }
            cannotApplyOperator(expr.operator);
        } else if (  type == TokenType.ABOVE  ) {
            // > operator
            if (left instanceof Double first
                    && right instanceof Double second) {
                return first > second;
            }
            cannotApplyOperator(expr.operator, "non numbers.");
        } else if (  type == TokenType.BELOW  ) {
            // < operator
            if (left instanceof Double first
                    && right instanceof Double second) {
                return first < second;
            }
            cannotApplyOperator(expr.operator, "non numbers.");
        } else if (  type == TokenType.ABOVE_EQUAL  ) {
            // < operator
            if (left instanceof Double first
                    && right instanceof Double second) {
                return first >= second;
            }
            cannotApplyOperator(expr.operator, "non numbers.");
        } else if (  type == TokenType.BELOW_EQUAL  ) {
            // < operator
            if (left instanceof Double first
                    && right instanceof Double second) {
                return first <= second;
            }
            cannotApplyOperator(expr.operator, "non numbers.");
        }
        return null;
    }

    @Override
    public Object visitGroupingExpr(Expression.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitSharedExpr(Expression.Shared expr) {
        // todo do think of another way to do it
        return null;
    }

    @Override
    public Object visitValEpr(Expression.Val expr) {
        Object val = evaluate(expr.expression);

        Expression.Val.ValId vId = expr.valId;
        if (vId.get() instanceof Token token) {
            assignVal(expr, val, token);
        } else {
            Expression.PropertyIdentifier property =
                    (Expression.PropertyIdentifier) vId.get();
            if (property.name.type == TokenType.THIS) {
                // from Executor.class
                Executor.onSuperMemory(this,
                        () -> assignVal(expr, val, property.property));
            }
        }
        return val;
    }

    private void assignVal(Expression.Val expr, Object val, Token valId) {
        String name = valId.lexeme;
        if (  expr.assignment  ) {
            memory.push(name, val);
        } else {
            memory.defineVal(name, val);
        }
    }

    @Override
    public Object visitTernary(Expression.Ternary expr) {
        Object cond = evaluate(expr.expr);
        if (  cond instanceof Boolean  bool) {
            return bool ? evaluate(expr.then) : evaluate(expr.or);
        }
        throw new RuntimeError("Condition should be logical expression." +
                " [logical expr] then [expr] or [expr] required");
    }

    @Override
    public Object visitIfExpr(Expression.If expr) {
        Object cond = evaluate(expr.expr);
        if (!(  cond instanceof Boolean  )) {
            // todo
            //  fix this!, use normal token
            throw new RuntimeError("Condition should be logical expression");
        }
        lowerMemory("if");
        Object result = null;
        if ((boolean) cond) {
            result =  evaluate(expr.body);
        } else if (expr.orElse != null) {
            result =  evaluate(expr.orElse);
        }
        upperMemory();
        return result;
    }

    private Interrupt evaluate(List<Expression> exprs) {
        for (Expression expr : exprs) {
            Object result = evaluate(expr);
            if (result instanceof Interrupt interrupt) {
                return interrupt;
            }
        }
        return null;
    }

    @Override
    public Object visitForExpr(Expression.For expr) {
        String valId = expr.valId.lexeme;

        List<Expression> loop = expr.block;
        Object[] range = (Object[]) evaluate(expr.range);

        TokenType type = (TokenType) range[0];
        boolean reverse = type == TokenType.RIGHT_LEFT;

        double from = (double) range[1];
        double to = (double) range[2];

        if (reverse) {
            double f = from;
            from = to;
            to = f;
        }
        System.out.println(from);
        System.out.println(to);

        // for ->
        memory.defineVal(valId, from);

        lowerMemory("for loop");

        Object result = null;
        double x;
        loop:
        for(
                x = reverse ? from - 1 : from;
                reverse ? x >= to : x <= to;
        ) {
            Interrupt interrupt = evaluate(loop);
            Object val = memory.getVal(valId);
            if (  val instanceof Double parallel) {
                x = parallel;
                memory.push(valId, parallel = (double) val + (reverse ? -1 : 1));
            } else {
                // todo move this
                //  error and detection to somewhere else
                throw new RuntimeError("variable [" + valId + "] modified to a non number!");
            }
            if (interrupt != null) {
                boolean handled = true;
                switch (interrupt.type()) {
                    case "break":
                        break loop;
                    case "continue":
                        // the, evaluate(List) function has
                        // already stopped execution
                        break;
                    case "forward":
                        double by = (double) interrupt.value();
                        if (reverse) by = -by;
                        x += by;
                        memory.push(valId, parallel + by);
                        break;
                    default:
                        handled = false;
                }
                if (!handled) {
                    result =  interrupt;
                    break;
                }
            }
            memory.delete();
            if (reverse) {
                x--;
            } else {
                x++;
            }
        }
        upperMemory();
        return result;
    }

    @Override
    public Object visitRangeExpr(Expression.Range expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);
        if (  left instanceof Double && right instanceof Double) {
            return new Object[] {
                        expr.type.type,
                        left, right  };
        }
        cannotApplyOperator(expr.type);
        return null;
    }

    @Override
    public Object visitWhileExpr(Expression.While expr) {
        for(;;) {
            Object cond = evaluate(expr.expr);
            if (!(  cond instanceof Boolean bool  )) {
                // todo handle it more correctly!
                throw new RuntimeError("Needed an logical expression for while loop!");
            }
            if (bool) {
                Object val = evaluate(expr.body);
                if (val != null) {
                    return val;
                }
            } else {
                break;
            }
        }
        return null;
    }

    @Override
    public Object visitFunCallExpr(Expression.FunCall expr) {
        String funId = expr.funId.lexeme;
        Native aNative = Native.create(this, funId);

        if (aNative != null) {
            // a native method call like print()
            aNative.accept(expr.args);
        } else {
            Expression.Fun fun = memory.getFun(funId);

            List<Expression> callArgs = expr.args;
            List<Token> signature = fun.args;

            int expected = signature.size();
            int got = callArgs.size();
            if (expected != got) {
                throw new RuntimeError("fun " + funId + "() " +
                        expected + " arguments, but got " + got);
            }
            lowerMemory("fun " + funId);
            for (int i = 0; i < signature.size(); i++) {
                Token token = signature.get(i);
                Expression arg = callArgs.get(i);

                memory.defineVal(token.lexeme, evaluate(arg));
            }
            Object invokeResult = null;
            Interrupt interrupt = evaluate(fun.expressions);
            if (interrupt != null) {
                invokeResult = interrupt.value();
            }
            // just do this so that the memory
            // gets cleaned
            for (Token token : signature)
                memory.deleteVal(token.lexeme);
            upperMemory();
            return invokeResult;
        }
        return null;
    }


    @Override
    public Object visitFunExpr(Expression.Fun expr) {
        memory.defineFun(expr.funId.lexeme, expr);
        return null;
    }

    @Override
    public Object visitReturnExpr(Expression.Return expr) {
        // yeah!, return self
        return new Interrupt.Return(
                evaluate(expr.expression));
    }

    private static void cannotApplyOperator(Token operator) {
        cannotApplyOperator(operator, "non values(s).");
    }

    private static void cannotApplyOperator(Token operator, String type) {
        Slime.error(operator,
                "Operator cannot be applied on " + type);
    }

    @Override
    public Object visitBreakExpr(Expression.Break expr) {
        return new Interrupt.Break();
    }

    @Override
    public Object visitContinueExpr(Expression.Continue expr) {
        return new Interrupt.Continue();
    }

    @Override
    public Object visitForwardExpr(Expression.Forward expr) {
        Object times = evaluate(expr.expression);
        if (  times instanceof Double  ) {
            return new Interrupt.Forward((double) times);
        }
        throw new RuntimeError("Expected number for \"forward;\"");
    }

    static abstract class Interrupt {

        abstract Object value();
        abstract String type();

        public static class Return extends Interrupt {
            public Return(Object val) {
                this.val = val;
            }

            Object val;

            @Override
            Object value() {  return val;  }

            @Override
            String type() {  return "return";  }
        }

        public static class Continue extends Interrupt {
            @Override
            Object value() {  return null;  }

            @Override
            String type() {  return "continue";  }
        }

        public static class Break extends Interrupt {
            @Override
            Object value() {  return null;  }

            @Override
            String type() {  return "break";  }
        }

        public static class Forward extends Interrupt {

            public Forward(double val) {
                this.val = val;
            }

            double val;

            @Override
            Object value() {  return val;  }

            @Override
            String type() {  return "forward";  }
        }
    }
}