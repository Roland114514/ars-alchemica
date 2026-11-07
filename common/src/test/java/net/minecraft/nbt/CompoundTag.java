package net.minecraft.nbt;

import java.util.HashMap;
import java.util.Map;

public class CompoundTag {
    private final Map<String, Object> tags = new HashMap<>();

    public void putDouble(String key, double value) { tags.put(key, value); }
    public double getDouble(String key) {
        Object val = tags.get(key);
        return val instanceof Number n ? n.doubleValue() : 0.0;
    }

    public void putInt(String key, int value) { tags.put(key, value); }
    public int getInt(String key) {
        Object val = tags.get(key);
        return val instanceof Number n ? n.intValue() : 0;
    }

    public void putString(String key, String value) { tags.put(key, value); }
    public String getString(String key) {
        Object val = tags.get(key);
        return val != null ? val.toString() : "";
    }

    public void putBoolean(String key, boolean value) { tags.put(key, value); }
    public boolean getBoolean(String key) {
        Object val = tags.get(key);
        return val instanceof Boolean b && b;
    }

    public void put(String key, CompoundTag tag) { tags.put(key, tag); }
    public CompoundTag getCompound(String key) {
        Object val = tags.get(key);
        return val instanceof CompoundTag ct ? ct : new CompoundTag();
    }

    public boolean contains(String key) { return tags.containsKey(key); }
    public boolean isEmpty() { return tags.isEmpty(); }
}
