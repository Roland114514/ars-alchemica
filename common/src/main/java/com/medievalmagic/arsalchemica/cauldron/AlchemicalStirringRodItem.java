package com.medievalmagic.arsalchemica.cauldron;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 浸入式硬木炼金搅拌棒 (Alchemical Stirring Rod)。
 * <p>
 * 具备双向流体水动力学操控物理特性：
 * <ul>
 *   <li><b>顺时针搅拌 (直接右键)：</b>激发向心旋流，平息以太紊乱，消散瞬时失稳度 (Instability)；</li>
 *   <li><b>逆时针搅拌 (Shift + 右键)：</b>激发离心剪切力，沉淀排杂，降低溶液浑浊度 (Turbidity)。</li>
 * </ul>
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public class AlchemicalStirringRodItem extends Item {

    public AlchemicalStirringRodItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.ars_alchemica.stirring_rod.desc")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(" ↻ ")
                .append(Component.translatable("tooltip.ars_alchemica.stirring_rod.clockwise"))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal(" ↺ ")
                .append(Component.translatable("tooltip.ars_alchemica.stirring_rod.counter_clockwise"))
                .withStyle(ChatFormatting.GOLD));
    }
}
