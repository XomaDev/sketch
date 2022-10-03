package xyz.kumaraswamy.sketch.nativs;

import xyz.kumaraswamy.sketch.processor.Evaluator;
import xyz.kumaraswamy.sketch.processor.Expression;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;

public abstract class Native {

    public static final HashMap<String, Class<? extends Native>> calls;

    static {
        calls = new HashMap<>();
        calls.put("print", Print.class);
    }

    public static Native create(Evaluator eval, String funId) {
        Class<? extends Native> clazz = calls.get(funId);
        if (clazz == null) return null;
        try {
            return (Native) clazz.getConstructors()[0].newInstance(eval);
        } catch (InstantiationException
                 | IllegalAccessException
                 | InvocationTargetException e) {
            // should never each this point
            throw new RuntimeException(e);
        }
    }

    public final Evaluator eval;

    public Native(Evaluator eval) {
        this.eval = eval;
    }

    public abstract Object accept(List<Expression> exprs);
}
