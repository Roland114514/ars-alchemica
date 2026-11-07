package com.medievalmagic.arsalchemica.grimoire;

import com.medievalmagic.arsalchemica.cauldron.AlchemicalCauldronBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 《炼金手记》/ 秘术之典 (Alchemical Journal / Grimoire)。
 * <p>
 * <b>双持学者直觉 (Dual-Wield Scholar's Intuition)：</b>
 * 当玩家将手记装备于副手 (OFF_HAND) 时，视线对准炼金坩埚即可透视其中溶液的微观热力学刻度、
 * 元素以太向量流向、失稳度以及当前工序向导。
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public class AlchemicalJournalItem extends Item {

    public AlchemicalJournalItem(Properties properties) {
        super(properties);
    }

    /**
     * 检测玩家是否正处于双持学者直觉激活状态。
     */
    public static boolean isScholarIntuitionActive(Player player) {
        if (player == null) return false;
        ItemStack offhand = player.getItemInHand(InteractionHand.OFF_HAND);
        return !offhand.isEmpty() && offhand.getItem() instanceof AlchemicalJournalItem;
    }

    /**
     * 凝视分析坩埚：生成瞬时学者直觉数据快照。
     */
    public static ScholarIntuitionData inspectCauldron(Level level, BlockPos targetPos, Player player) {
        if (level == null || targetPos == null || !isScholarIntuitionActive(player)) {
            return ScholarIntuitionData.EMPTY;
        }

        BlockEntity be = level.getBlockEntity(targetPos);
        if (be instanceof AlchemicalCauldronBlockEntity cauldron) {
            return cauldron.createIntuitionSnapshot();
        }

        return ScholarIntuitionData.EMPTY;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.ars_alchemica.journal.desc")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(" ✦ ")
                .append(Component.translatable("tooltip.ars_alchemica.journal.dual_wield"))
                .withStyle(ChatFormatting.AQUA));
    }
}
