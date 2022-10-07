package xyz.kumaraswamy.sketch.memory;

import java.util.HashMap;

public class Memory {

    private final String name;
    // sMemory - > super memory
    private final Memory sMemory;

    public Memory(String name, Memory sMemory) {
        this.name = name;
        this.sMemory = sMemory;
    }

    public Memory upwards() {
        values.clear();
        // reuse memory objects
        // for lower memory system
        sMemory.next = this;
        return sMemory;
    }

    public Memory next;

    public boolean lower() {
        return sMemory != null;
    }

    private final HashMap<String, Object> values = new HashMap<>();

    public void defineVal(String name, Object value) {
        if (values.containsKey(name)) {
            throw new IllegalArgumentException("[" + this.name + "] Variable name already defined \"" + name + "\"");
        }
        values.put(name, value);
    }

    public void define(String name, Object value) {
        values.put(name, value);
    }

    public void push(String name, Object value) {
        if (sMemory != null && !values.containsKey(name)) {
            sMemory.push(name, value);
        } else {
            values.put(name, value);
        }
    }

    public Object getVal(String name) {
        if (sMemory != null && !values.containsKey(name)) {
            return sMemory.getVal(name);
        }
        return values.get(name);
    }

    private final HashMap<String, Object> functions = new HashMap<>();

    public void defineFun(String name, Object fun) {
        if (values.containsKey(name)) {
            throw new IllegalArgumentException("[" + this.name + "]Function name already defined \"" + name + "\"");
        }
        functions.put(name, fun);
    }

    public Object getFun(String name) {
        if (sMemory != null && !functions.containsKey(name)) {
            return sMemory.getFun(name);
        }
        return functions.get(name);
    }

    public void delete() {
        values.clear();
    }
}
