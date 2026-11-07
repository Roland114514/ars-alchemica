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
}
