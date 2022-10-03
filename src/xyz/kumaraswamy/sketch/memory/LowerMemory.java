package xyz.kumaraswamy.sketch.memory;

import lombok.Getter;
import xyz.kumaraswamy.sketch.processor.Expression;

import java.util.HashMap;

public class LowerMemory extends GlobalMemory {

    private final String name;
    // sMemory - > super memory
    @Getter
    private final GlobalMemory sMemory;

    public LowerMemory(String name, GlobalMemory sMemory) {
        this.name = name;
        this.sMemory = sMemory;
    }

    private final HashMap<String, Object> values = new HashMap<>();

    public void defineVal(String name, Object value) {
        if (values.containsKey(name)) {
            throw new IllegalArgumentException("[" + this.name + "] Variable name already defined \"" + name + "\"");
        }
        values.put(name, value);
    }

    public void deleteVal(String name) {
        values.remove(name);
    }

    public void push(String name, Object value) {
        if (!values.containsKey(name)) {
            sMemory.push(name, value);
        } else {
            values.put(name, value);
        }
    }

    public Object getVal(String name) {
        if (!values.containsKey(name)) {
            return sMemory.getVal(name);
        }
        return values.get(name);
    }

    private final HashMap<String, Expression.Fun> functions = new HashMap<>();

    public void defineFun(String name, Expression.Fun fun) {
        if (values.containsKey(name)) {
            throw new IllegalArgumentException("[" + this.name + "]Function name already defined \"" + name + "\"");
        }
        functions.put(name, fun);
    }

    public Expression.Fun getFun(String name) {
        if (!functions.containsKey(name)) {
            return sMemory.getFun(name);
        }
        return functions.get(name);
    }

    public void delete() {
        values.clear();
    }
}
