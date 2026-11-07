package net.minecraft.world.entity;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LivingEntity extends Entity {
    private final List<MobEffectInstance> activeEffects = new ArrayList<>();
    private MobType mobType = MobType.UNDEFINED;
    private boolean canChangeDimensions = true;

    public boolean addEffect(MobEffectInstance effect) {
        if (effect != null) {
            activeEffects.add(effect);
        }
        return true;
    }

    public boolean removeEffect(MobEffect effect) {
        return activeEffects.removeIf(inst -> inst.getEffect() == effect);
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
