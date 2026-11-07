package net.minecraft.world.effect;

public class MobEffects {
    public static final MobEffect CONFUSION = new MobEffect(false, 0x551D4A, "effect.minecraft.nausea");
    public static final MobEffect BLINDNESS = new MobEffect(false, 0x1F1F23, "effect.minecraft.blindness");
    public static final MobEffect POISON = new MobEffect(false, 0x4E9331, "effect.minecraft.poison");
    public static final MobEffect DARKNESS = new MobEffect(false, 0x222222, "effect.minecraft.darkness");
    public static final MobEffect MOVEMENT_SLOWDOWN = new MobEffect(false, 0x5A6C81, "effect.minecraft.slowness");
    public static final MobEffect WITHER = new MobEffect(false, 0x352A27, "effect.minecraft.wither");
    public static final MobEffect WEAKNESS = new MobEffect(false, 0x484D48, "effect.minecraft.weakness");

    public static final MobEffect FIRE_RESISTANCE = new MobEffect(true, 0xE49A3A, "effect.minecraft.fire_resistance");
    public static final MobEffect WATER_BREATHING = new MobEffect(true, 0x2E5299, "effect.minecraft.water_breathing");
    public static final MobEffect DAMAGE_RESISTANCE = new MobEffect(true, 0x99453A, "effect.minecraft.resistance");
    public static final MobEffect MOVEMENT_SPEED = new MobEffect(true, 0x7CAFC6, "effect.minecraft.speed");
    public static final MobEffect REGENERATION = new MobEffect(true, 0xCD5CAB, "effect.minecraft.regeneration");
    public static final MobEffect NIGHT_VISION = new MobEffect(true, 0x1F1FA1, "effect.minecraft.night_vision");
    public static final MobEffect DAMAGE_BOOST = new MobEffect(true, 0x932423, "effect.minecraft.strength");
    public static final MobEffect HEAL = new MobEffect(true, 0xF82423, "effect.minecraft.instant_health") {
        @Override public boolean isInstantenous() { return true; }
    };
    public static final MobEffect HARM = new MobEffect(false, 0x430A09, "effect.minecraft.instant_damage") {
        @Override public boolean isInstantenous() { return true; }
    };
    public static final MobEffect ABSORPTION = new MobEffect(true, 0x2552A5, "effect.minecraft.absorption");
    public static final MobEffect JUMP = new MobEffect(true, 0x22FF4C, "effect.minecraft.jump_boost");
    public static final MobEffect SLOW_FALLING = new MobEffect(true, 0xFCE8ED, "effect.minecraft.slow_falling");
    public static final MobEffect DIG_SPEED = new MobEffect(true, 0xD9C043, "effect.minecraft.haste");
}
