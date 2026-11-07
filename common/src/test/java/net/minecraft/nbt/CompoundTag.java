package net.minecraft.nbt;

import java.util.HashMap;
import java.util.Map;

public class CompoundTag {
    private final Map<String, Object> tags = new HashMap<>();

    public void putFloat(String key, float value) { tags.put(key, value); }
    public float getFloat(String key) {
        Object val = tags.get(key);
        return val instanceof Number n ? n.floatValue() : 0.0f;
    }

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

    public void putIntArray(String key, int[] array) { tags.put(key, array); }
    public int[] getIntArray(String key) {
        Object val = tags.get(key);
        return val instanceof int[] arr ? arr : new int[0];
    }

    public void put(String key, CompoundTag tag) { tags.put(key, tag); }
    public CompoundTag getCompound(String key) {
        Object val = tags.get(key);
        return val instanceof CompoundTag ct ? ct : new CompoundTag();
    }

    public boolean contains(String key) { return tags.containsKey(key); }
    public boolean contains(String key, int tagType) { return tags.containsKey(key); }
    public void remove(String key) { tags.remove(key); }
    public boolean isEmpty() { return tags.isEmpty(); }
    public java.util.Set<String> getAllKeys() { return tags.keySet(); }

    public CompoundTag copy() {
        CompoundTag copy = new CompoundTag();
        for (Map.Entry<String, Object> entry : tags.entrySet()) {
            if (entry.getValue() instanceof CompoundTag ct) {
                copy.tags.put(entry.getKey(), ct.copy());
            } else {
                copy.tags.put(entry.getKey(), entry.getValue());
            }
        }
        return copy;
    }
}
