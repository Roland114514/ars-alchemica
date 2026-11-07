package com.medievalmagic.arsalchemica.client;

import com.medievalmagic.arsalchemica.grimoire.ScholarIntuitionData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 双持学者直觉大世界视界 HUD 渲染器 (Common-First 客户端表现层)。
 * <p>
 * 提供悬浮羊皮纸微观热力学与药理指标渲染计算。
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public final class ScholarIntuitionHudRenderer {

    private ScholarIntuitionHudRenderer() {}

    /**
     * 根据温度推导渲染颜色代码 (ARGB)。
     */
    public static int getTemperatureColor(double temp) {
        if (temp < 40.0) return 0xFF55FFFF;  // 冰凉水蓝
        if (temp < 85.0) return 0xFF55FF55;  // 温热嫩绿
        if (temp < 160.0) return 0xFFFFAA00; // 沸腾烈橙
        return 0xFFAA00FF;                   // 星界极炎深紫
    }

    /**
     * 生成供 HUD 浮动面板渲染的格式化文本行列表。
     */
    public static List<Component> buildDisplayLines(ScholarIntuitionData data) {
        List<Component> lines = new ArrayList<>();
        if (data == null || data == ScholarIntuitionData.EMPTY) {
            lines.add(Component.translatable("hud.ars_alchemica.intuition.empty")
                    .withStyle(ChatFormatting.GRAY));
            return lines;
        }

        // 1. 标题与水深刻度
        lines.add(Component.translatable("hud.ars_alchemica.intuition.title")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        lines.add(Component.translatable("hud.ars_alchemica.intuition.water_level", data.waterLevel(), 3)
                .withStyle(ChatFormatting.AQUA));

        // 2. 温度与热量阶级
        ChatFormatting tempStyle = data.isBoiling() ? ChatFormatting.RED : ChatFormatting.GREEN;
        lines.add(Component.translatable("hud.ars_alchemica.intuition.temperature",
                String.format(Locale.ROOT, "%.1f°C", data.temperature()))
                .withStyle(tempStyle));

        // 3. 药理指标：纯度、失稳度、浑浊度
        lines.add(Component.translatable("hud.ars_alchemica.intuition.purity",
                String.format(Locale.ROOT, "%.1f%%", data.purity() * 100.0))
                .withStyle(ChatFormatting.LIGHT_PURPLE));

        ChatFormatting instabStyle = data.instability() >= 0.60 ? ChatFormatting.DARK_RED : ChatFormatting.GRAY;
        lines.add(Component.translatable("hud.ars_alchemica.intuition.instability",
                String.format(Locale.ROOT, "%.2f", data.instability()))
                .withStyle(instabStyle));

        // 4. 水动力学搅拌记录
        lines.add(Component.translatable("hud.ars_alchemica.intuition.stirring",
                data.clockwiseTurns(), data.counterClockwiseTurns())
                .withStyle(ChatFormatting.YELLOW));

        // 5. 当前工序指引
        lines.add(Component.translatable("hud.ars_alchemica.intuition.guidance",
                Component.translatable(data.recipeStepGuidance()))
                .withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC));

        return lines;
    }
}
