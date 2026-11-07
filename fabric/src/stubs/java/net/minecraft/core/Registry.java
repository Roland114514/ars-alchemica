package net.minecraft.core;

import net.minecraft.resources.ResourceLocation;

public interface Registry<T> {
    static <V, T extends V> T register(Registry<V> registry, ResourceLocation id, T entry) {
        return entry;
    }
}
