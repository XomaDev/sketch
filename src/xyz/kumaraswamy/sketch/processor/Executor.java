package xyz.kumaraswamy.sketch.processor;

import xyz.kumaraswamy.sketch.memory.GlobalMemory;

public class Executor {

    static void onSuperMemory(Evaluator evaluator, Runnable runnable) {
        GlobalMemory current = evaluator.memory;
        evaluator.memory = evaluator.fMemory;
        runnable.run();
        evaluator.memory = current;
    }
}
