package net.minecraft.util;

import java.util.Random;

public interface RandomSource {
    int nextInt(int bound);
    float nextFloat();

    static RandomSource create() {
        Random r = new Random();
        return new RandomSource() {
            @Override public int nextInt(int bound) { return r.nextInt(bound); }
            @Override public float nextFloat() { return r.nextFloat(); }
        };
    }
}
