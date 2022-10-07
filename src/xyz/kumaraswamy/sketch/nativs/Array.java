package xyz.kumaraswamy.sketch.nativs;

import xyz.kumaraswamy.sketch.processor.Evaluator;
import xyz.kumaraswamy.sketch.processor.Expression;
import xyz.kumaraswamy.sketch.processor.RuntimeError;

import java.util.List;

public class Array extends Native {
    public Array(Evaluator eval) {
        super(eval);
    }

    @Override
    public Object accept(List<Expression> exprs) {
        if (exprs.size() == 1) {
            Object val = eval.evaluate(exprs.get(0));
            if (val instanceof Double number) {
                return new Object[number.intValue()];
            }
            throw new RuntimeError("Expected a number for array()");
        }
        throw new RuntimeError("Expected one argument for array()");
    }
}
