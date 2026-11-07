package net.minecraft.world.level.block.state;

import net.minecraft.world.level.block.state.properties.Property;

public abstract class StateHolder<O, S> {
    public abstract <T extends Comparable<T>, V extends T> S setValue(Property<T> property, V value);
}
