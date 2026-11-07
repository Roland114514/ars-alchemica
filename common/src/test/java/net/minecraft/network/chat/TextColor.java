package net.minecraft.network.chat;

public final class TextColor {
    private final int value;

    private TextColor(int value) {
        this.value = value;
    }

    public static TextColor fromRgb(int rgb) {
        return new TextColor(rgb);
    }

    public int getValue() {
        return value;
    }
}
