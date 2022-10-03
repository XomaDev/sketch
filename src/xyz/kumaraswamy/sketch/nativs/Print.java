package xyz.kumaraswamy.sketch.nativs;

import xyz.kumaraswamy.sketch.processor.Evaluator;
import xyz.kumaraswamy.sketch.processor.Expression;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class Print extends Native {

    private static OutputStream stream;

    public static void setOutputStream(OutputStream stream) {
        Print.stream = stream;
    }

    public Print(Evaluator eval) {
        super(eval);
        if (  stream  == null) {
            stream = System.out;
        }
    }

    @Override
    public Object accept(List<Expression> exprs) {
        for (Expression expr : exprs) {
            Object eval = expr == null ? null : this.eval.evaluate(expr);
            try {
                stream.write(String.valueOf(eval).getBytes());
                stream.write('\n');
            } catch (IOException e) {
                e.printStackTrace();;
                throw new RuntimeException("Cannot write to stream!");
            }
        }
        return null;
    }
}
