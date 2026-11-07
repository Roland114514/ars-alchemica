package com.medievalmagic.arsalchemica.cauldron.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 炼金铁渣 / 废渣残片 (Alchemical Slag Item)。
 * <p>
 * 坩埚遭遇以太裂解爆炸或干烧熔毁后崩落的扭曲金属残块。
 * 以太流形已崩溃，可熔炼回收微量铁锭。
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public class AlchemicalSlagItem extends Item {

    public AlchemicalSlagItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.ars_alchemica.slag.desc")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.ars_alchemica.slag.hint")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
