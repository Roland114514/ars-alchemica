package net.minecraft.world.effect;

public class MobEffectInstance {
    private final MobEffect effect;
    private final int duration;
    private final int amplifier;

    public MobEffectInstance(MobEffect effect, int duration, int amplifier) {
        this.effect = effect;
        this.duration = duration;
        this.amplifier = amplifier;
    }

    public MobEffectInstance(MobEffect effect, int duration, int amplifier, boolean ambient, boolean visible, boolean showIcon) {
        this(effect, duration, amplifier);
    }

    public MobEffectInstance(MobEffectInstance other) {
        this(other.effect, other.duration, other.amplifier);
    }

    public MobEffect getEffect() { return effect; }
    public int getDuration() { return duration; }
    public int getAmplifier() { return amplifier; }
    public String getDescriptionId() { return effect != null ? effect.getDescriptionId() : "effect.unknown"; }
}
