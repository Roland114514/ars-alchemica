package com.medievalmagic.arsalchemica.potion;

import com.medievalmagic.arsalchemica.api.toxicity.ToxicityThreshold;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 专属炼金 Debuff：以太毒蚀 (Alchemical Corrosion)。
 * <p>
 * <b>核心机制（模仿并进化原版中毒效果）：</b>
 * <ol>
 *   <li><b>频率脉冲：</b>类似于原版中毒，衰减周期随药效等级提升加速 (Interval = 25 >> amplifier Ticks)。</li>
 *   <li><b>生理破防与致死判定：</b>
 *       <ul>
 *         <li>在安全期/警觉期/中毒期（常规状态下），效仿原版剧毒机制：伤害不会使目标生命值降至 1.0 点 (半颗心) 以下；</li>
 *         <li>一旦玩家进入<b>毒性过载 (OVERDOSE, >90%)</b> 或药效等级达到 II 级 (Amplifier >= 2)，以太毒素彻底击穿血液灵质屏障，
 *             生命保底失效，造成<b>真实致死炼金崩解伤害</b>！</li>
 *       </ul>
 *   </li>
 * </ol>
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public class AlchemicalCorrosionEffect extends MobEffect {

    public static final int CORROSION_COLOR = 0x3A0E4C; // 暗夜以太暗紫带幽绿偏光

    public AlchemicalCorrosionEffect() {
        super(EffectCategory.HARMFUL, CORROSION_COLOR);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level() != null && entity.level().isClientSide()) return;

        // 判定是否击穿生命保底屏障（进入致死阶段）
        boolean isLethal = (amplifier >= 2);
        if (!isLethal && entity instanceof Player player) {
            isLethal = PlayerToxicityManager.getData(player)
                    .map(data -> data.getThreshold() == ToxicityThreshold.OVERDOSE)
                    .orElse(false);
        }

        // 伤害梯度：基础 1.0f (半颗心)，每级增幅 +0.5f
        float damage = 1.0f + (amplifier * 0.5f);

        if (!isLethal) {
            // 常规中毒模仿：保留最后 1.0 点生命值（半颗心）
            if (entity.getHealth() > 1.0f) {
                float safeDamage = Math.min(damage, entity.getHealth() - 1.0f);
                entity.hurt(AlchemicalDamageSources.corrosion(entity.level()), safeDamage);
            }
        } else {
            // 毒性过载 / 高阶毒蚀：直击本源，致死反噬
            entity.hurt(AlchemicalDamageSources.corrosion(entity.level()), damage);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        int interval = 25 >> amplifier;
        if (interval > 0) {
            return duration % interval == 0;
        }
        return true;
    }
}
