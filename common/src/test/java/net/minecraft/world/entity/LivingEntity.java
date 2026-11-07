package net.minecraft.world.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LivingEntity extends Entity {
    private final List<MobEffectInstance> activeEffects = new ArrayList<>();
    private MobType mobType = MobType.UNDEFINED;
    private boolean canChangeDimensions = true;
    private float health = 20.0f;
    private float maxHealth = 20.0f;

    public float getHealth() { return health; }
    public void setHealth(float health) { this.health = Math.max(0.0f, Math.min(health, maxHealth)); }
    public float getMaxHealth() { return maxHealth; }
    public void setMaxHealth(float maxHealth) { this.maxHealth = Math.max(1.0f, maxHealth); }
    public boolean isDeadOrDying() { return health <= 0.0f; }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        super.hurt(source, amount);
        this.health = Math.max(0.0f, this.health - amount);
        return true;
    }

    public boolean addEffect(MobEffectInstance effect) {
        if (effect != null) {
            activeEffects.add(effect);
        }
        return true;
    }

    public boolean removeEffect(MobEffect effect) {
        return activeEffects.removeIf(inst -> inst.getEffect() == effect);
    }

    public boolean hasEffect(MobEffect effect) {
        return activeEffects.stream().anyMatch(inst -> inst.getEffect() == effect);
    }

    public Collection<MobEffectInstance> getActiveEffects() {
        return activeEffects;
    }

    public MobType getMobType() {
        return mobType;
    }

    public void setMobType(MobType mobType) {
        this.mobType = mobType;
    }

    public boolean canChangeDimensions() {
        return canChangeDimensions;
    }

    public void setCanChangeDimensions(boolean canChangeDimensions) {
        this.canChangeDimensions = canChangeDimensions;
    }
}
