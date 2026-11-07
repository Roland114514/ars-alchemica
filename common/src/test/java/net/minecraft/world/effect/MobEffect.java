package net.minecraft.world.effect;

import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.LivingEntity;

public class MobEffect {
    private final boolean beneficial;
    private final int color;
    private final String descriptionId;

    public MobEffect(boolean beneficial, int color, String descriptionId) {
        this.beneficial = beneficial;
        this.color = color;
        this.descriptionId = descriptionId;
    }

    public MobEffect(MobEffectCategory category, int color) {
        this.beneficial = (category == MobEffectCategory.BENEFICIAL);
        this.color = color;
        this.descriptionId = "effect.ars_alchemica.custom";
    }

    public MobEffect() {
        this(false, 0, "effect.minecraft.unknown");
    }

    public void applyEffectTick(LivingEntity entity, int amplifier) {
    }

    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }

    public int getColor() {
        return color;
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
