package net.minecraft.world.effect;

import net.minecraft.ChatFormatting;

public class MobEffect {
    private final boolean beneficial;
    private final int color;
    private final String descriptionId;

    public MobEffect(boolean beneficial, int color, String descriptionId) {
        this.beneficial = beneficial;
        this.color = color;
        this.descriptionId = descriptionId;
    }

    public MobEffect() {
        this(false, 0, "effect.minecraft.unknown");
    }

    public boolean isBeneficial() {
        return beneficial;
    }

    public boolean isInstantenous() {
        return false;
    }

    public String getDescriptionId() {
        return descriptionId;
    }

    public EffectCategory getCategory() {
        return beneficial ? EffectCategory.BENEFICIAL : EffectCategory.HARMFUL;
    }

    public enum EffectCategory {
        BENEFICIAL(ChatFormatting.BLUE),
        HARMFUL(ChatFormatting.RED),
        NEUTRAL(ChatFormatting.WHITE);

        private final ChatFormatting tooltipFormatting;

        EffectCategory(ChatFormatting formatting) {
            this.tooltipFormatting = formatting;
        }

        public ChatFormatting getTooltipFormatting() {
            return tooltipFormatting;
        }
    }
}
