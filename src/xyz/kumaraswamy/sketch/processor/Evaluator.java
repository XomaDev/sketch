package xyz.kumaraswamy.sketch.processor;

import lombok.SneakyThrows;
import xyz.kumaraswamy.sketch.Sketch;
import xyz.kumaraswamy.sketch.lex.TokenType;
import xyz.kumaraswamy.sketch.memory.Memory;
import xyz.kumaraswamy.sketch.nativs.Native;
import xyz.kumaraswamy.sketch.lex.Token;
import xyz.kumaraswamy.sketch.nativs.sketch.Import;

import java.lang.reflect.Method;
import java.util.List;

public class Evaluator implements Expression.Visitor<Object> {

    private final Import anImport = new Import(this);

    public Memory headMemory;
    public Memory memory;

    public Evaluator(Memory memory) {
        headMemory = memory;
        this.memory = memory;
    }

    public Object evaluate(Expression expr) {
        return expr.accept(this);
    }

    public void lowerMemory(String name) {
        // use memory.next instead of creating
        // new objects, this will make the language
        // faster, memory.next is a cleared memory (its like new)
        memory = memory.next != null ? memory.next
                : new Memory(name, memory);
    }

    public void upperMemory() {
        if (memory.lower()) {
            memory = memory.upwards();
            return;
        }
        throw new RuntimeError("Already a super memory!");
    }

    @Override
    public Object visitLiteralExpr(Expression.Literal<?> expr) {
        return expr.value;
    }

    @Override
    public Object visitArrayAccessExpr(Expression.ArrayAccess expr) {
        Object valArray = expr.array;
        Object val = evaluate(expr.array);

        if (val instanceof Object[] array) {
            return array[getArrayIndex(expr)];
        } else if (val instanceof String string) {
            return string.charAt(getArrayIndex(expr));
        }
        throw new RuntimeError("\"" + valArray + "\"" + " is not an valArray");
    }

    private int getArrayIndex(Expression.ArrayAccess expr) {
        Object nNumber = evaluate(expr.access);
        if (nNumber instanceof Double n) {
            return n.intValue();
        }
        throw new RuntimeError("Needs a number for array access");
    }

    @Override
    public Object visitIdentifierExpr(Expression.Identifier expr) {
        return memory.getVal(expr.token.lexeme);
    }

    @Override
    public Object visitPropertyAccessExpr(Expression.PropertyIdentifier expr) {
        if (expr.name.type == TokenType.THIS) {
            return headMemory.getVal(expr.property.lexeme);
        }
        return null;
    }

    @Override
    public Object visitWithExpr(Expression.With expr) {
        anImport.doImport(expr);
        return null;
    }

    @Override
    public Object visitBinaryExpr(Expression.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case PLUS -> {
                if (left instanceof Double
                        && right instanceof Double) {
                    return (double) left + (double) right;
                }
                if (left instanceof String
                        || right instanceof String) {
                    return String.valueOf(left) + right;
                }
                cannotApplyOperator(expr.operator);
            }
            case MINUS -> {
                if (left instanceof Double
                        && right instanceof Double) {
                    return (double) left - (double) right;
                }
                cannotApplyOperator(expr.operator);
            }
            case STAR -> {
                if (left instanceof Double
                        && right instanceof Double) {
                    return (double) left * (double) right;
                }
                cannotApplyOperator(expr.operator);
            }
            case SLASH -> {
                if (left instanceof Double
                        && right instanceof Double) {
                    return (double) left / (double) right;
                }
                cannotApplyOperator(expr.operator);
            }
            case PERCENTAGE -> {
                if (left instanceof Double
                        && right instanceof Double) {
                    return (double) left % (double) right;
                }
                cannotApplyOperator(expr.operator);
            }
        }
        return null;
    }

    @Override
    public Object visitBinaryUnaryExpr(Expression.BinaryUnary expr) {
        String valId = expr.valId.lexeme;
        Object val = memory.getVal(valId);
        if (val instanceof Double x) {
            switch (expr.operator.type) {
                case INCREMENT, DECREMENT -> {
                    int n = expr.operator.type ==
                            TokenType.INCREMENT ? 1 : -1;
                    memory.push(valId, x + n);
                    return expr.left ? x + n : x;
                }
            }
        }
        cannotApplyOperator(expr.operator);
        return null;
    }

    @Override
    public Object visitUnaryExpr(Expression.Unary expr) {
        TokenType type = expr.operator.type;
        Object value = evaluate(expr.expression);

        if (type == TokenType.MINUS) {
            if (value instanceof Double) {
                return -((double) value);
            }
            cannotApplyOperator(expr.operator);
        } else if (type == TokenType.EXCLAMATION) {
            return !truthy(value);
        }
        return null;
    }

    @Override
    public Object visitLogicalExpr(Expression.Logical expr) {
        TokenType type = expr.operator.type;

        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (type) {
            case EQUAL_EQUAL:
                return equal(left, right);
            case NOT_EQUAL:
                return !equal(left, right);
            case LOGICAL_AND:
                if (left instanceof Boolean && right instanceof Boolean) {
                    return (boolean) left && (boolean) right;
                }
                cannotApplyOperator(expr.operator);
                break;
            case ABOVE:
                // > operator
                if (left instanceof Double first
                        && right instanceof Double second) {
                    return first > second;
                }
                cannotApplyOperator(expr.operator, "Operation of non numbers.");
                break;
            case BELOW:
                // < operator
                if (left instanceof Double first
                        && right instanceof Double second) {
                    return first < second;
                }
                cannotApplyOperator(expr.operator, "Operation of non numbers.");
                break;
            case ABOVE_EQUAL:
                // < operator
                if (left instanceof Double first
                        && right instanceof Double second) {
                    return first >= second;
                }
                cannotApplyOperator(expr.operator, "Operation of non numbers.");
                break;
            case BELOW_EQUAL:
                // < operator
                if (left instanceof Double first
                        && right instanceof Double second) {
                    return first <= second;
                }
                cannotApplyOperator(expr.operator, "Operation of non numbers.");
                break;
        }
        return null;
    }

    @Override
    public Object visitArrayExpr(Expression.Array expr) {
        List<Expression> list = expr.exprs;
        int size = list.size();

        Object[] vals = new Object[size];

        for (int i = 0; i < size; i++) {
            Expression expression = list.get(i);
            vals[i] = evaluate(expression);
        }
        return vals;
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
        Object get = vId.get();

        if (vId.get() instanceof Token token) {
            assignVal(expr, val, token);
        } else if (get instanceof Expression.ArrayAccess access) {
            // array assignment, like names[0] = something
            setArrayElement(val, access);
        } else {
            Expression.PropertyIdentifier property =
                    (Expression.PropertyIdentifier) get;
            if (property.name.type == TokenType.THIS) {
                // from Executor.class
                Executor.onSuperMemory(this,
                        () -> assignVal(expr, val, property.property));
            }
        }
        return val;
    }

    private void setArrayElement(Object val, Expression.ArrayAccess access) {
        Object aVal = evaluate(access.array);

        if (aVal instanceof Object[] array) {
            Object nPosition = evaluate(access.access);
            if (nPosition instanceof Double nPos) {
                int index = nPos.intValue();
                array[index] = val;
                return;
            }
            throw new RuntimeError("Needs a number for array access");
        }
        throw new RuntimeError("\"" + aVal + "\"" + " is not an array");
    }

    private void assignVal(Expression.Val expr, Object val, Token valId) {
        String name = valId.lexeme;
        if (expr.assignment) {
            memory.push(name, val);
        } else {
            memory.defineVal(name, val);
        }
    }

    @Override
    public Object visitTernary(Expression.Ternary expr) {
        return truthy(
                evaluate(expr.expr))
                ? evaluate(expr.then) : evaluate(expr.or);
    }

    @Override
    public Object visitIfExpr(Expression.If expr) {
        Object cond = evaluate(expr.expr);
        lowerMemory("if");
        Object result = null;
        if (truthy(cond)) {
            result = evaluate(expr.body);
        } else if (expr.orElse != null) {
            result = evaluate(expr.orElse);
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

        // for ->
        memory.defineVal(valId, from);

        lowerMemory("for loop");

        Object result = null;
        double x;
        loop:
        for (
                x = reverse ? from - 1 : from;
                reverse ? x >= to : x <= to;
        ) {
            Interrupt interrupt = evaluate(loop);
            Object val = memory.getVal(valId);
            if (val instanceof Double parallel) {
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
                    result = interrupt;
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
        if (left instanceof Double && right instanceof Double) {
            return new Object[]{
                    expr.type.type,
                    left, right};
        }
        cannotApplyOperator(expr.type);
        return null;
    }

    @Override
    public Object visitWhileExpr(Expression.While expr) {
        for (; ; ) {
            if (truthy(evaluate(expr.expr))) {
                Interrupt val = evaluate(expr.body);
                if (val != null) {
                    if ("break".equals(val.type())) {
                        break;
                    }
                    return val;
                }
            } else {
                break;
            }
        }
        return null;
    }

    @Override
    public Object visitEachExpr(Expression.Each expr) {
        String targetName = expr.targetName.lexeme;
        String elementName = expr.elementName.lexeme;

        lowerMemory("each");
        Object val = memory.getVal(targetName);

        Interrupt result = null;
        // guys, any ideas, how to reuse code multiple
        // times?
        if (val instanceof List<?> exprs) {
            loop:
            for (Object elementVal : exprs) {
                result = untilInterrupt(expr, elementName,
                        evaluate((Expression) elementVal));
                if (result != null) {
                    switch (result.type()) {
                        case "break":
                            break loop;
                        case "continue":
                            break;
                    }
                }
            }
        } else if (val instanceof String vVal) {
            loop:
            for (char aChar : vVal.toCharArray()) {
                result = untilInterrupt(expr,
                        elementName, String.valueOf(aChar));
                if (result != null) {
                    switch (result.type()) {
                        case "break":
                            break loop;
                        case "continue":
                            break;
                    }
                }
            }
        } else {
            throw new RuntimeException("Needs an array to iterate elements");
        }
        upperMemory();
        return result;
    }

    // used for visitEachExpr() to iterate on multiple
    // types of elements (Array, String)
    private Interrupt untilInterrupt(Expression.Each expr,
                                     String elementName, Object elementVal) {
        Interrupt result;
        memory.defineVal(elementName, elementVal);

        // delete memory
        result = evaluate(expr.body);
        memory.delete();
        if (result != null) {
            switch (result.type()) {
                case "break":
                    break;
                case "continue":
                    // the, evaluate(List) function has
                    // already stopped execution
                    return null;
            }
        }
        return result;
    }

    @SneakyThrows
    @Override
    public Object visitFunCallExpr(Expression.FunCall expr) {
        String funId = expr.funId.lexeme;
        Native aNative = Native.create(this, funId);

        if (aNative != null) {
            // a native method call like print()
            return aNative.accept(expr.args);
        } else {
            Object function = memory.getFun(funId);
            if (function instanceof Method method) {
                List<Expression> args = expr.args;

                int size = args.size();
                Object[] _args = new Object[size];
                for (int i = 0; i < size; i++) {
                    _args[i] = evaluate(args.get(i));
                }
                // null because, they are static
                // methods
                return method.invoke(null, _args);
            }
            Expression.Fun fun = (Expression.Fun) function;
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
                memory.define(
                        signature.get(i).lexeme,
                        evaluate(callArgs.get(i)));
            }
            Object invokeResult = null;
            Interrupt interrupt = evaluate(fun.expressions);
            if (interrupt != null) {
                invokeResult = interrupt.value();
            }
            upperMemory();
            return invokeResult;
        }
    }

    @Override
    public Object visitFunExpr(Expression.Fun expr) {
        memory.defineFun(expr.funId.lexeme, expr);
        return null;
    }

    @Override
    public Object visitReturnExpr(Expression.Return expr) {
        return new Interrupt(
                "return", evaluate(expr.expression));
    }

    private static void cannotApplyOperator(Token operator) {
        cannotApplyOperator(operator, "non values(s).");
    }

    private static void cannotApplyOperator(Token operator, String type) {
        Sketch.error(operator,
                "Operator cannot be applied on " + type);
    }

    @Override
    public Object visitBreakExpr(Expression.Break expr) {
        return new Interrupt("break", null);
    }

    @Override
    public Object visitContinueExpr(Expression.Continue expr) {
        return new Interrupt("continue", null);
    }

    @Override
    public Object visitForwardExpr(Expression.Forward expr) {
        Object times = evaluate(expr.expression);
        if (times instanceof Double) {
            return new Interrupt("forward", times);
        }
        throw new RuntimeError("Expected number for \"forward;\"");
    }

    private static boolean equal(Object left, Object right) {
        if (left == null && right == null)
            return false;
        if (left == null)
            return false;
        return left.equals(right);
    }

    private static boolean truthy(Object object) {
        if (object instanceof Boolean bool)
            return bool;
        throw new RuntimeError("Expected truthy, got \"" + object + "\"");
    }

    record Interrupt(String type, Object value) {

    }
}