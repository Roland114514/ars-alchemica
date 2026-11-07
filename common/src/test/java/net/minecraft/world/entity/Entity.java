package net.minecraft.world.entity;

import net.minecraft.world.damagesource.DamageSource;

public class Entity {
    public boolean fireImmune() { return false; }
    public void setSecondsOnFire(int seconds) {}
    public boolean hurt(DamageSource source, float amount) { return true; }
}
