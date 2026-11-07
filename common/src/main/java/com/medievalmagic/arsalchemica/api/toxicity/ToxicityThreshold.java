package com.medievalmagic.arsalchemica.api.toxicity;

import net.minecraft.ChatFormatting;

/**
 * 猎魔人风格毒性生理阈值状态机。
 * <p>
 * 根据当前毒性占最大毒性容量的比例 (Ratio = Current / Max) 划分生理反应区间：
 * <ul>
 *   <li><b>SAFE (< 50%)</b>：安全代谢区，药效正常发挥；</li>
 *   <li><b>CAUTION (50% ~ 75%)</b>：以太饱和警戒区，视界暗角微弱毒晕；</li>
 *   <li><b>DANGER (75% ~ 90%)</b>：毒性淤积期，视界剧烈畸变、恶心晃动、神经迟缓；</li>
 *   <li><b>OVERDOSE (> 90%)</b>：灵质崩解休克期，剥夺全部增益，反转为凋零/剧毒并造成持续真实炼金反噬伤害！</li>
 * </ul>
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public enum ToxicityThreshold {
    SAFE(0.0f, 0.50f, ChatFormatting.GREEN, "toxicity.ars_alchemica.state.safe"),
    CAUTION(0.50f, 0.75f, ChatFormatting.YELLOW, "toxicity.ars_alchemica.state.caution"),
    DANGER(0.75f, 0.90f, ChatFormatting.GOLD, "toxicity.ars_alchemica.state.danger"),
    OVERDOSE(0.90f, Float.MAX_VALUE, ChatFormatting.DARK_RED, "toxicity.ars_alchemica.state.overdose");

    private final float minRatio;
    private final float maxRatio;
    private final ChatFormatting color;
    private final String translationKey;

    ToxicityThreshold(float minRatio, float maxRatio, ChatFormatting color, String translationKey) {
        this.minRatio = minRatio;
        this.maxRatio = maxRatio;
        this.color = color;
        this.translationKey = translationKey;
    }

    public static ToxicityThreshold fromRatio(float ratio) {
        if (!Float.isFinite(ratio) || ratio < 0.50f) return SAFE;
        if (ratio < 0.75f) return CAUTION;
        if (ratio < 0.90f) return DANGER;
        return OVERDOSE;
    }

    public float getMinRatio() { return minRatio; }
    public float getMaxRatio() { return maxRatio; }
    public ChatFormatting getColor() { return color; }
    public String getTranslationKey() { return translationKey; }
}
