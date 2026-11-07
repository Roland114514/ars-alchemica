package net.minecraft.world.level.block.state;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class BlockState {
    private final Block block;
    private final Map<Property<?>, Comparable<?>> values = new HashMap<>();

    public BlockState(Block block) {
        this.block = block;
    }

    public Block getBlock() { return block; }
    public boolean is(Block other) { return this.block == other; }

    @SuppressWarnings("unchecked")
    public <T extends Comparable<T>> T getValue(Property<T> property) {
        Object val = values.get(property);
        if (val == null) {
            // Default fallbacks
            if (property.getName().equals("age")) return (T) Integer.valueOf(0);
            if (property.getName().equals("blooming") || property.getName().equals("charged") || property.getName().equals("ceiling") || property.getName().equals("pressed")) return (T) Boolean.FALSE;
            if (property.getName().equals("facing")) return (T) net.minecraft.core.Direction.NORTH;
        }
        return (T) val;
    }

    public <T extends Comparable<T>> Optional<T> getOptionalValue(Property<T> property) {
        T val = getValue(property);
        return Optional.ofNullable(val);
    }

    public <T extends Comparable<T>, V extends T> BlockState setValue(Property<T> property, V value) {
        BlockState copy = new BlockState(this.block);
        copy.values.putAll(this.values);
        copy.values.put(property, value);
        return copy;
    }
}
