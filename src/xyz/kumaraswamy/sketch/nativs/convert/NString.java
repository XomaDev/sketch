package xyz.kumaraswamy.sketch.nativs.convert;

import xyz.kumaraswamy.sketch.nativs.Native;
import xyz.kumaraswamy.sketch.processor.Evaluator;
import xyz.kumaraswamy.sketch.processor.Expression;

import java.util.List;

public class NString extends Native {
    public NString(Evaluator eval) {
        super(eval);
    }

    @Override
    public Object accept(List<Expression> exprs) {
        StringBuilder builder = new StringBuilder();
        for (Expression expr : exprs) {
            Object val = eval.evaluate(expr);
            builder.append(val);
        }
        return builder.toString();
    }
}
