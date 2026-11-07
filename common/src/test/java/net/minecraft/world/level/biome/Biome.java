package net.minecraft.world.level.biome;

public class Biome {
    private final float baseTemperature;

    public Biome(float baseTemperature) {
        this.baseTemperature = baseTemperature;
    }

    public float getBaseTemperature() {
        return baseTemperature;
    }
}
