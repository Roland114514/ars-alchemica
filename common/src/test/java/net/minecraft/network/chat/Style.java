package net.minecraft.network.chat;

import net.minecraft.ChatFormatting;

public class Style {
    public static final Style EMPTY = new Style(null);

    private final TextColor color;

    private Style(TextColor color) {
        this.color = color;
    }

    public Style withColor(TextColor color) {
        return new Style(color);
    }

    public Style withColor(ChatFormatting formatting) {
        return this;
    }

    public TextColor getColor() {
        return color;
    }
}
