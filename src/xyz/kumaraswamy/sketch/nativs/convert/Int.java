package xyz.kumaraswamy.sketch.nativs.convert;

import xyz.kumaraswamy.sketch.nativs.Native;
import xyz.kumaraswamy.sketch.processor.Evaluator;
import xyz.kumaraswamy.sketch.processor.Expression;
import xyz.kumaraswamy.sketch.processor.RuntimeError;

import java.util.List;

public class Int extends Native {
    public Int(Evaluator eval) {
        super(eval);
    }

    @Override
    public Object accept(List<Expression> exprs) {
        if (exprs.size() == 1) {
            Object val = eval.evaluate(exprs.get(0));
            if (val instanceof Double) {
                return val;
            }
            return Double.parseDouble(String.valueOf(val));
        }
        throw new RuntimeError("number() argument accepts only one value");
    }
}
