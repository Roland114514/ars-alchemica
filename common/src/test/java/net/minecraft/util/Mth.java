package net.minecraft.util;

public class Mth {
    public static final float PI = (float) Math.PI;
    public static final float TWO_PI = (float) (Math.PI * 2.0);

    public static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    public static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    public static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }
}
