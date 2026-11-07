package com.medievalmagic.arsalchemica.potion;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;

/**
 * 炼金特殊伤害源封装（Common-First 跨平台解耦）。
 * <p>
 * 提供致死以太毒蚀、过载反噬与奥术腐蚀伤害源。
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public final class AlchemicalDamageSources {

    private AlchemicalDamageSources() {}

    /**
     * 以太毒蚀与反噬伤害源（穿透常规物理护甲）。
     */
    public static DamageSource corrosion(Level level) {
        if (level != null && level.damageSources() != null) {
            return level.damageSources().magic();
        }
        return new DamageSource();
    }
}
