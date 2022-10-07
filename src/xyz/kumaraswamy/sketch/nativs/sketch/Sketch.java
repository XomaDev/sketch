package xyz.kumaraswamy.sketch.nativs.sketch;

// sketch object, invoked dynamically
// by the use of reflection

// rules:
// no duplicate method names
// one method name can be used one time

import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings("unused")
public class Sketch {
    public static double random(Double start, Double end) {
        int min = start.intValue();
        int max = end.intValue();
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    public static double systemTime() {
        return System.currentTimeMillis();
    }
}
