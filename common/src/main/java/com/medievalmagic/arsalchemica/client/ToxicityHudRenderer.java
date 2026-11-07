package com.medievalmagic.arsalchemica.client;

import com.medievalmagic.arsalchemica.api.toxicity.IToxicityData;
import com.medievalmagic.arsalchemica.api.toxicity.ToxicityThreshold;
import com.medievalmagic.arsalchemica.potion.PlayerToxicityManager;
import net.minecraft.world.entity.player.Player;

/**
 * 猎魔人式炼金毒性 HUD 仪表盘与屏幕滤镜计算器 (Common-First 客户端表现层)。
 * <p>
 * 提供跨平台 GUI 事件 (Forge/Fabric RenderGuiOverlay) 所需的纯数学计算与色彩混合方案：
 * <ol>
 *   <li><b>毒性刻度条：</b>动态平滑渐变色彩渲染与过载警告闪烁；</li>
 *   <li><b>暗夜毒晕视效 (Vignette Overlay)：</b>随体内以太沉积加剧在屏幕边缘蔓延的毒脉暗角透明度与色调。</li>
 * </ol>
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public final class ToxicityHudRenderer {

    private ToxicityHudRenderer() {}

    /**
     * 获取毒性刻度条填充颜色 (ARGB)。
     */
    public static int getBarColor(ToxicityThreshold threshold, long gameTime) {
        return switch (threshold) {
            case SAFE -> 0xFF2ECC71;       // 翡翠健康绿
            case CAUTION -> 0xFFF1C40F;    // 琥珀警戒黄
            case DANGER -> 0xFFE67E22;     // 灼热中毒橙
            case OVERDOSE -> {
                // 毒性过载：急促心跳频率红紫交替频闪
                boolean flash = (gameTime / 6) % 2 == 0;
                yield flash ? 0xFF8E44AD : 0xFFC0392B;
            }
        };
    }

    /**
     * 获取屏幕四周暗角毒晕 (Screen Vignette) 的渲染不透明度 Alpha [0.0, 1.0]。
     */
    public static float getScreenVignetteAlpha(float toxicityRatio, long gameTime) {
        if (toxicityRatio < 0.50f) {
            return 0.0f; // 50% 以下完全透明
        }
        if (toxicityRatio < 0.75f) {
            // 警觉期：平滑渐入 (0.0 ~ 0.35)
            float t = (toxicityRatio - 0.50f) / 0.25f;
            return t * 0.35f;
        }
        if (toxicityRatio < 0.90f) {
            // 中毒期：经脉显现 (0.35 ~ 0.65)
            float t = (toxicityRatio - 0.75f) / 0.15f;
            return 0.35f + t * 0.30f;
        }

        // 过载崩解期：剧烈暗角脉冲 (0.65 ~ 0.95)
        double pulse = (Math.sin(gameTime * 0.3) + 1.0) * 0.5;
        return (float) (0.65f + pulse * 0.30f);
    }

    /**
     * 获取当前屏幕暗角渲染的色调。
     */
    public static int getVignetteTintColor(ToxicityThreshold threshold) {
        return switch (threshold) {
            case SAFE -> 0x000000;
            case CAUTION -> 0x1A2B15;    // 暗绿微晕
            case DANGER -> 0x2A1035;     // 腐蚀暗紫
            case OVERDOSE -> 0x450A1A;   // 充血死寂深红
        };
    }

    /**
     * 辅助工具：获取玩家当前归一化毒性百分比字符串。
     */
    public static String getFormattedToxicityString(Player player) {
        return PlayerToxicityManager.getData(player)
                .map(data -> String.format("%.0f%%", data.getToxicityRatio() * 100.0f))
                .orElse("0%");
    }
}
