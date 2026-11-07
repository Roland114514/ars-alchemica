package com.medievalmagic.arsalchemica.client;

import com.medievalmagic.arsalchemica.api.toxicity.IToxicityData;
import com.medievalmagic.arsalchemica.api.toxicity.ToxicityThreshold;
import com.medievalmagic.arsalchemica.potion.AlchemicalCorrosionEffect;
import com.medievalmagic.arsalchemica.potion.PlayerToxicityManager;
import com.medievalmagic.arsalchemica.registry.ModEffects;
import net.minecraft.world.entity.player.Player;

/**
 * 炼金毒蚀生命心形渲染器 (Common-First 客户端渲染表现模型)。
 * <p>
 * 效仿并超越原版剧毒心形渲染：
 * <ul>
 *   <li><b>暗夜以太血心 (Corrosion Hearts)：</b>当玩家受到 {@link AlchemicalCorrosionEffect} 侵蚀或处于高危以太毒性状态时，
 *       血心渲染为暗夜幽紫与死寂幽绿交织的枯萎经脉纹理；</li>
 *   <li><b>休克震颤 (Cardiovascular Tremor)：</b>在 {@link ToxicityThreshold#OVERDOSE} 状态下，
 *       心形图标触发心律失常式剧烈抖动与脉冲抽搐动画。</li>
 * </ul>
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public final class AlchemicalHeartRenderer {

    private AlchemicalHeartRenderer() {}

    public enum HeartVisualState {
        VANILLA,              // 原版正常/剧毒/凋零心
        ALCHEMICAL_CORROSION, // 炼金毒蚀（暗紫侵蚀心）
        OVERDOSE_COLLAPSE     // 毒性过载休克（紫绿脉动血心 + 剧烈心律不齐震颤）
    }

    /**
     * 判断当前玩家生命条的心形渲染模式。
     */
    public static HeartVisualState getVisualState(Player player) {
        if (player == null) return HeartVisualState.VANILLA;

        boolean hasCorrosion = player.hasEffect(ModEffects.getCorrosion());
        ToxicityThreshold threshold = PlayerToxicityManager.getData(player)
                .map(IToxicityData::getThreshold)
                .orElse(ToxicityThreshold.SAFE);

        if (threshold == ToxicityThreshold.OVERDOSE) {
            return HeartVisualState.OVERDOSE_COLLAPSE;
        }
        if (hasCorrosion || threshold == ToxicityThreshold.DANGER) {
            return HeartVisualState.ALCHEMICAL_CORROSION;
        }

        return HeartVisualState.VANILLA;
    }

    /**
     * 获取心形图标混合染色值 (ARGB)。
     */
    public static int getHeartTintRgb(HeartVisualState state, long gameTime) {
        return switch (state) {
            case VANILLA -> 0xFFFFFFFF; // 无额外着色
            case ALCHEMICAL_CORROSION -> 0xFF4A1259; // 深暗以太腐蚀紫
            case OVERDOSE_COLLAPSE -> {
                // 紫绿脉动呼吸波形
                double wave = (Math.sin(gameTime * 0.25) + 1.0) * 0.5;
                int r = (int) (0x4A * (1.0 - wave) + 0x1A * wave);
                int g = (int) (0x12 * (1.0 - wave) + 0x5E * wave);
                int b = (int) (0x59 * (1.0 - wave) + 0x2A * wave);
                yield (0xFF << 24) | (r << 16) | (g << 8) | b;
            }
        };
    }

    /**
     * 计算过载状态下的心形图标 Y 轴垂直抽搐抖动位移 (心律失常模拟)。
     *
     * @param heartIndex 心形序号 (0 ~ 9)
     * @param gameTime 游戏世界时间
     * @return 垂直像素位移 (-2 ~ +2)
     */
    public static int getHeartTremorOffsetY(HeartVisualState state, int heartIndex, long gameTime) {
        if (state != HeartVisualState.OVERDOSE_COLLAPSE) {
            return 0;
        }
        // 基于伪随机噪声和正弦波叠加，模拟心房纤颤般的杂乱抽搐
        long seed = gameTime * 31L + heartIndex * 17L;
        int phase = (int) ((seed ^ (seed >>> 16)) % 5);
        return phase - 2; // -2 ~ +2 像素抖动
    }
}
