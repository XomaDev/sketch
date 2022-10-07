package xyz.kumaraswamy.sketch.nativs;

import xyz.kumaraswamy.sketch.processor.Evaluator;
import xyz.kumaraswamy.sketch.processor.Expression;
import xyz.kumaraswamy.sketch.processor.RuntimeError;

import java.util.List;

public class Len extends Native {
    public Len(Evaluator eval) {
        super(eval);
    }

    @Override
    public Object accept(List<Expression> exprs) {
        if (exprs.size() == 1) {
            Object val = eval.evaluate(exprs.get(0));
            if (val == null)
                return 0;
            if (val instanceof Object[] array)
                return (double) array.length;
            if (val instanceof java.lang.String string)
                return (double) string.length();
            if (val instanceof Boolean bool)
                return bool ? 1 : 0;
            if (val instanceof Double number)
                // return itself
                return number;
            throw new RuntimeError("Unknown convert: " + val);
        }
        throw new RuntimeError("Expected one argument for len()");
    }
}
