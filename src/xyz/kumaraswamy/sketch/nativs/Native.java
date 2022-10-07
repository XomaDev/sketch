package xyz.kumaraswamy.sketch.nativs;

import xyz.kumaraswamy.sketch.nativs.convert.NString;
import xyz.kumaraswamy.sketch.nativs.convert.Int;
import xyz.kumaraswamy.sketch.processor.Evaluator;
import xyz.kumaraswamy.sketch.processor.Expression;

import java.util.List;

public abstract class Native {
    public static Native create(Evaluator eval, String funId) {
        return switch (funId) {
            case "print" -> new Print(eval);
            case "printf" -> new Printf(eval);
            case "len" -> new Len(eval);

            // casting
            case "string" -> new NString(eval);
            case "int" -> new Int(eval);

            case "array" -> new Array(eval);
            default -> null;
        };
    }

    public final Evaluator eval;

    public Native(Evaluator eval) {
        this.eval = eval;
    }

    public abstract Object accept(List<Expression> exprs);
}
