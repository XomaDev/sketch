package xyz.kumaraswamy.sketch.nativs;

import xyz.kumaraswamy.sketch.memory.Memory;
import xyz.kumaraswamy.sketch.processor.Evaluator;
import xyz.kumaraswamy.sketch.processor.Expression;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

public class Printf extends Native{
    static OutputStream stream;

    public static void setOutputStream(OutputStream stream) {
        Printf.stream = stream;
    }


    public Printf(Evaluator eval) {
        super(eval);
        if (  stream  == null) {
            stream = System.out;
        }
    }

    StringBuilder val;

    @Override
    public Object accept(List<Expression> exprs) {
        if (exprs.size() != 1) {
            throw new RuntimeException("Expected one argument for printf()");
        }
        Object object = eval.evaluate(exprs.get(0));
        if (object instanceof java.lang.String fText) {
            val = new StringBuilder(fText);
            Memory memory = eval.memory;

            StringBuilder name = new StringBuilder();
            for (int i = 0; i < val.length(); i++) {
                char ch = val.charAt(i);
                if (ch == '$') {
                    int start = i;
                    i++; // eat '$'
                    while (i < val.length()) {
                        char nch = val.charAt(i);
                        if (!isAlpha(nch)) {
                            reValNameRegion(memory, name, i, start);
                            i--;
                            break;
                        }
                        name.append(nch);
                        if (++i >= val.length() ) {
                            // no more characters ahead
                            reValNameRegion(memory, name, i, start);
                            break;
                        }
                    }
                }
            }
            write(val.toString().getBytes());
        } else {
            if (object instanceof Object[] array) object = Arrays.toString(array);
            write(java.lang.String.valueOf(object).getBytes());
        }
        write(new byte[] {'\n'});
        return null;
    }

    private void reValNameRegion(Memory memory, StringBuilder name, int i, int start) {
        val.replace(start, i,
                java.lang.String.valueOf(
                        memory.getVal(name.toString())));
        name.setLength(0);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    private void write(byte[] write) {
        try {
            stream.write(write);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
