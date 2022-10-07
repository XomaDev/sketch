package xyz.kumaraswamy.sketch.nativs.sketch;

import xyz.kumaraswamy.sketch.processor.Evaluator;
import xyz.kumaraswamy.sketch.processor.Expression;
import xyz.kumaraswamy.sketch.processor.RuntimeError;

import java.lang.reflect.Method;

public class Import {

    private static final String BASE_PACKAGE = "xyz.kumaraswamy.sketch.nativs.sketch.";

    private final Evaluator eval;

    public Import(Evaluator eval) {
        this.eval = eval;
    }

    public void doImport(Expression.With with) {
        String from = with.from.lexeme;

        String clazzName = BASE_PACKAGE + from;

        Class<?> clazz;
        try {
            clazz = Class.forName(clazzName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeError("[with] did not find \"" + from + "\"");
        }
        Method[] methods = clazz.getMethods();

        String funcName = with.func.lexeme;
        Method func = null;
        for (Method method : methods) {
            if (method.getName().equals(funcName)) {
                func = method;
                break;
            }
        }
        if (func == null) {
            throw new RuntimeError("[with] did not find func \"" +
                    funcName + "\" from \"" + from + "\"");
        }

        String declare = with.as == null ? funcName : with.as.lexeme;
        eval.memory.defineFun(declare, func);
    }
}
