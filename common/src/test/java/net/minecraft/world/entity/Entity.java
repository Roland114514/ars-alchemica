package net.minecraft.world.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.level.Level;

public class Entity {
    public int tickCount = 0;
    public Level level;
    public int secondsOnFire = 0;
    public float lastDamageTaken = 0.0f;

    public Level level() { return level; }
    public boolean fireImmune() { return false; }
    public void setSecondsOnFire(int seconds) { this.secondsOnFire = seconds; }
    public boolean hurt(DamageSource source, float amount) {
        this.lastDamageTaken += amount;
        return true;
    }

    public BlockPos blockPosition() { return BlockPos.ZERO; }
    public double getX() { return 0.0; }
    public double getY() { return 0.0; }
    public double getZ() { return 0.0; }

    public DamageSources damageSources() {
        return level != null ? level.damageSources() : new DamageSources();
    }
}
