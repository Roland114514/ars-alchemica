package net.minecraft.world.entity;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class AreaEffectCloud extends Entity {
    private LivingEntity owner;
    private float radius = 3.0f;
    private float radiusOnUse = 0.0f;
    private int waitTime = 0;
    private int duration = 600;
    private float radiusPerTick = 0.0f;
    private int color = 0;
    private final List<MobEffectInstance> effects = new ArrayList<>();

    public AreaEffectCloud(Level level, double x, double y, double z) {
        this.level = level;
    }

    public void setOwner(LivingEntity owner) { this.owner = owner; }
    public LivingEntity getOwner() { return owner; }
    public void setRadius(float radius) { this.radius = radius; }
    public float getRadius() { return radius; }
    public void setRadiusOnUse(float radiusOnUse) { this.radiusOnUse = radiusOnUse; }
    public void setWaitTime(int waitTime) { this.waitTime = waitTime; }
    public void setDuration(int duration) { this.duration = duration; }
    public int getDuration() { return duration; }
    public void setRadiusPerTick(float radiusPerTick) { this.radiusPerTick = radiusPerTick; }
    public void setFixedColor(int color) { this.color = color; }
    public int getColor() { return color; }
    public void addEffect(MobEffectInstance effect) { this.effects.add(effect); }
    public List<MobEffectInstance> getEffects() { return effects; }
}
