package xyz.kumaraswamy.sketch.memory;

import xyz.kumaraswamy.sketch.processor.Expression;

import java.util.HashMap;

public class GlobalMemory {

    private final HashMap<String, Object> values = new HashMap<>();

    public void defineVal(String name, Object value) {
        if (values.containsKey(name)) {
            throw new IllegalArgumentException("Variable name already defined \"" + name + "\"");
        }
        values.put(name, value);
    }

    public void deleteVal(String name) {
        values.remove(name);
    }

    public void push(String name, Object value) {
        if (!values.containsKey(name)) {
            throw new IllegalArgumentException("Unable to find memory location \"" + name + "\"");
        }
        values.put(name, value);
    }

    public Object getVal(String name) {
        if (!values.containsKey(name)) {
            throw new IllegalArgumentException("Unable to find memory location \"" + name + "\"");
        }
        return values.get(name);
    }

    private final HashMap<String, Expression.Fun> functions = new HashMap<>();

    public void defineFun(String name, Expression.Fun fun) {
        if (values.containsKey(name)) {
            throw new IllegalArgumentException("Function name already defined \"" + name + "\"");
        }
        functions.put(name, fun);
    }

    public Expression.Fun getFun(String name) {
        if (!functions.containsKey(name)) {
            throw new IllegalArgumentException("Unable to find fun memory location \"" + name + "\"");
        }
        return functions.get(name);
    }

    public void delete() {
        values.clear();
    }
}
