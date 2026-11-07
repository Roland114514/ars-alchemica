package net.minecraft.network.chat;

import net.minecraft.ChatFormatting;

public class MutableComponent implements Component {
    private final StringBuilder text;
    private Style style = Style.EMPTY;

    public MutableComponent(String initial) {
        this.text = new StringBuilder(initial);
    }

    public MutableComponent append(Component other) {
        this.text.append(other.getString());
        return this;
    }

    public MutableComponent withStyle(ChatFormatting formatting) {
        return this;
    }

    public MutableComponent withStyle(ChatFormatting... formattings) {
        return this;
    }

    public MutableComponent withStyle(Style style) {
        this.style = style;
        return this;
    }

    public MutableComponent setStyle(Style style) {
        this.style = style;
        return this;
    }

    @Override
    public String getString() {
        return text.toString();
    }
}
