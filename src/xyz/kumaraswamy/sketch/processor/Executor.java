package xyz.kumaraswamy.sketch.processor;

import xyz.kumaraswamy.sketch.memory.Memory;

public class Executor {

    static void onSuperMemory(Evaluator evaluator, Runnable runnable) {
        Memory current = evaluator.memory;
        evaluator.memory = evaluator.headMemory;
        runnable.run();
        evaluator.memory = current;
    }
}
