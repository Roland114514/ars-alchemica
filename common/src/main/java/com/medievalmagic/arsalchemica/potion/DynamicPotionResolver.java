package com.medievalmagic.arsalchemica.potion;

import com.medievalmagic.arsalchemica.api.aspect.AlchemyAspect;
import com.medievalmagic.arsalchemica.api.vector.AspectVector;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.List;

/**
 * 动态复合药水数学解算引擎。
 * <p>
 * 基于连续以太流形空间 (AspectVector) 动态推导复合药效、持续时间、纯度等级加成、毒性负载与光子混合色彩。
 * 彻底颠覆原版静态 Potion 枚举绑定模式。
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public final class DynamicPotionResolver {

    public static final String TAG_POTION_DATA = "AlchemicalPotion";
    public static final String TAG_VECTOR = "AspectVector";
    public static final String TAG_PURITY = "Purity";
    public static final String TAG_MASS = "Mass";
    public static final String TAG_VOLATILITY = "Volatility";

    private DynamicPotionResolver() {}

    /**
     * 根据物品 NBT 解析其中的 AspectVector 与热力学参数，推导动态效果列表。
     */
    public static List<MobEffectInstance> resolveEffects(CompoundTag rootTag) {
        List<MobEffectInstance> results = new ArrayList<>();
        if (rootTag == null || !rootTag.contains(TAG_POTION_DATA)) {
            return results;
        }

        CompoundTag data = rootTag.getCompound(TAG_POTION_DATA);
        AspectVector vector = data.contains(TAG_VECTOR)
                ? AspectVector.deserializeNBT(data.getCompound(TAG_VECTOR))
                : AspectVector.ZERO;

        double purity = data.contains(TAG_PURITY) ? data.getDouble(TAG_PURITY) : vector.calculatePurity();
        double mass = data.contains(TAG_MASS) ? data.getDouble(TAG_MASS) : vector.getTotalAspectMass();
        double volatility = data.contains(TAG_VOLATILITY) ? data.getDouble(TAG_VOLATILITY) : 1.0;

        return resolveEffects(vector, mass, purity, volatility);
    }

    /**
     * 核心算法：以太向量映射为药水效果。
     */
    public static List<MobEffectInstance> resolveEffects(AspectVector vector, double mass, double purity, double volatility) {
        List<MobEffectInstance> results = new ArrayList<>();
        double totalMass = vector.getTotalAspectMass();
        if (totalMass <= 1e-4) return results;

        purity = Mth.clamp(purity, 0.05, 2.0);
        mass = Math.max(1.0, mass);
        volatility = Mth.clamp(volatility, 0.1, 3.0);

        // 纯度决定基础等级 (Amplifier: 0 ~ 3, 对应 I ~ IV 级)
        int baseAmplifier = Mth.clamp((int) (purity * 2.0), 0, 3);
        if (volatility > 1.8 && purity > 1.0) {
            baseAmplifier = Math.min(4, baseAmplifier + 1); // 极高挥发度激发额外爆发
        }

        // 质量决定持续时间基数，挥发性越高释放越快（持续时间越短）
        int baseDurationTicks = Math.max(40, (int) (mass * 20.0 * (1.25 - (volatility * 0.25))));

        for (AlchemyAspect aspect : AlchemyAspect.VALUES) {
            double amount = vector.getAmount(aspect);
            double ratio = amount / totalMass;
            if (ratio < 0.15) continue; // 摩尔比低于 15% 的微量杂质不产生主导药效

            int scaledDuration = Math.max(20, (int) (baseDurationTicks * ratio));
            int scaledAmplifier = ratio >= 0.45 ? baseAmplifier : Math.max(0, baseAmplifier - 1);

            MobEffect effect = mapAspectToEffect(aspect, volatility > 1.5);
            results.add(new MobEffectInstance(effect, scaledDuration, scaledAmplifier));
        }

        return results;
    }

    /**
     * 解算饮用该灵药所产生的毒性负荷值 [5.0, 90.0]。
     * <p>
     * 公式：Tox = (Base + Mass * 0.25) * (1.65 - Purity * 0.90) * (1.0 + MortisRatio * 0.6)
     * </p>
     */
    public static float calculateToxicityLoad(CompoundTag rootTag) {
        if (rootTag == null || !rootTag.contains(TAG_POTION_DATA)) {
            return 15.0f;
        }

        CompoundTag data = rootTag.getCompound(TAG_POTION_DATA);
        AspectVector vector = data.contains(TAG_VECTOR)
                ? AspectVector.deserializeNBT(data.getCompound(TAG_VECTOR))
                : AspectVector.ZERO;

        double purity = data.contains(TAG_PURITY) ? data.getDouble(TAG_PURITY) : vector.calculatePurity();
        double mass = data.contains(TAG_MASS) ? data.getDouble(TAG_MASS) : vector.getTotalAspectMass();

        return calculateToxicityLoad(vector, mass, purity);
    }

    public static float calculateToxicityLoad(AspectVector vector, double mass, double purity) {
        double total = vector.getTotalAspectMass();
        double mortisRatio = total > 1e-4 ? (vector.getAmount(AlchemyAspect.MORTIS) / total) : 0.0;

        purity = Mth.clamp(purity, 0.0, 2.0);
        mass = Math.max(1.0, mass);

        double rawTox = (12.0 + mass * 0.25) * (1.65 - Math.min(1.0, purity) * 0.90) * (1.0 + mortisRatio * 0.6);
        return (float) Mth.clamp(rawTox, 5.0, 90.0);
    }

    /**
     * 实时获取药水流体颜色（RGB）。
     */
    public static int getBlendedColor(CompoundTag rootTag) {
        if (rootTag == null || !rootTag.contains(TAG_POTION_DATA)) {
            return 0x385DC6; // 默认深蓝以太色
        }
        CompoundTag data = rootTag.getCompound(TAG_POTION_DATA);
        if (data.contains(TAG_VECTOR)) {
            AspectVector vector = AspectVector.deserializeNBT(data.getCompound(TAG_VECTOR));
            return vector.calculateBlendedColor();
        }
        return 0x385DC6;
    }

    private static MobEffect mapAspectToEffect(AlchemyAspect aspect, boolean highVolatility) {
        return switch (aspect) {
            case IGNIS -> highVolatility ? MobEffects.DAMAGE_BOOST : MobEffects.FIRE_RESISTANCE;
            case AQUA -> highVolatility ? MobEffects.MOVEMENT_SPEED : MobEffects.WATER_BREATHING;
            case TERRA -> highVolatility ? MobEffects.DAMAGE_RESISTANCE : MobEffects.DAMAGE_RESISTANCE;
            case AER -> highVolatility ? MobEffects.MOVEMENT_SPEED : MobEffects.MOVEMENT_SPEED;
            case VITA -> highVolatility ? MobEffects.REGENERATION : MobEffects.REGENERATION;
            case MORTIS -> highVolatility ? MobEffects.POISON : MobEffects.POISON;
            case AETHER -> MobEffects.NIGHT_VISION;
            case SULFUR -> MobEffects.DAMAGE_BOOST;
            case MERCURIUS -> MobEffects.MOVEMENT_SPEED;
            case SAL -> MobEffects.DAMAGE_RESISTANCE;
            case ORDO -> MobEffects.REGENERATION;
            case CHAOS -> MobEffects.CONFUSION;
        };
    }
}
