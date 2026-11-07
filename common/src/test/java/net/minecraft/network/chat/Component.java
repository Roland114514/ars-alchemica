package net.minecraft.network.chat;

import net.minecraft.ChatFormatting;

public interface Component {
    static MutableComponent empty() {
        return new MutableComponent("");
    }

    static MutableComponent literal(String text) {
        return new MutableComponent(text);
    }

    static MutableComponent translatable(String key) {
        return new MutableComponent(key);
    }

    static MutableComponent translatable(String key, Object... args) {
        StringBuilder sb = new StringBuilder(key);
        for (Object arg : args) {
            sb.append("[").append(arg instanceof Component c ? c.getString() : String.valueOf(arg)).append("]");
        }
        return new MutableComponent(sb.toString());
    }

    String getString();
}
