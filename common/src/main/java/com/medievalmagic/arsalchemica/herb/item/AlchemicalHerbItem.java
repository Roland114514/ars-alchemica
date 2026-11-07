package com.medievalmagic.arsalchemica.herb.item;

import com.medievalmagic.arsalchemica.api.aspect.AlchemyAspect;
import com.medievalmagic.arsalchemica.api.vector.AspectVector;
import com.medievalmagic.arsalchemica.herb.HerbRegistry;
import com.medievalmagic.arsalchemica.herb.IAlchemicalHerb;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 炼金草药植株原貌物品载体 (BlockItem)。
 * <p>
 * 既可作为建筑/种植方块放置于适生基底，亦可作为初级原料投入研磨钵、压榨机、阴干架或大坩埚中。
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public class AlchemicalHerbItem extends BlockItem implements IAlchemicalHerb {

    private final HerbRegistry herb;

    public AlchemicalHerbItem(Block block, Properties properties, HerbRegistry herb) {
        super(block, properties);
        this.herb = Objects.requireNonNull(herb, "HerbRegistry cannot be null");
    }

    public HerbRegistry getHerb() {
        return this.herb;
    }

    @Override
    public String getId() {
        return herb.getId();
    }

    @Override
    public AspectVector getAspectVector(ProcessingForm form) {
        return herb.getAspectVector(form);
    }

    @Override
    public double getBaseQuality(ProcessingForm form) {
        return herb.getBaseQuality(form);
    }

    @Override
    public double getVolatilityModifier(ProcessingForm form) {
        return herb.getVolatilityModifier(form);
    }

    @Override
    public double getBaseToxicity(ProcessingForm form) {
        return herb.getBaseToxicity(form);
    }

    @Override
    public boolean canSurviveInEnvironment(Level level, BlockPos pos, BlockState state) {
        return herb.canSurviveInEnvironment(level, pos, state);
    }

    @Override
    public boolean isSafeToHarvest(Level level, BlockPos pos, BlockState state, Player player, ItemStack tool) {
        return herb.isSafeToHarvest(level, pos, state, player, tool);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        // 1. 生态与形态描述
        tooltip.add(Component.translatable("tooltip.ars_alchemica.herb." + herb.getId() + ".desc")
                .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));

        // 2. 原生植物形态标签
        tooltip.add(Component.translatable("tooltip.ars_alchemica.form.raw")
                .withStyle(ChatFormatting.GOLD));

        // 3. 动力学指标
        double vol = herb.getVolatilityModifier(ProcessingForm.RAW);
        double tox = herb.getBaseToxicity(ProcessingForm.RAW);
        MutableComponent kinetics = Component.literal("❖ 挥发率: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.format(Locale.ROOT, "%.0f%%", vol * 100.0)).withStyle(ChatFormatting.AQUA))
                .append(Component.literal("  ☠ 毒性: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.format(Locale.ROOT, "%.1f", tox))
                        .withStyle(tox > 30.0 ? ChatFormatting.RED : ChatFormatting.GREEN));
        tooltip.add(kinetics);

        // 4. 分割线
        tooltip.add(Component.literal("━━━━━━━━━━━━━━━━━━━━━").withStyle(ChatFormatting.DARK_GRAY));

        // 5. 固有元素向量
        AspectVector vec = herb.getAspectVector(ProcessingForm.RAW);
        for (AlchemyAspect aspect : AlchemyAspect.VALUES) {
            double val = vec.getAmount(aspect);
            if (val > 0.05) {
                String icon = switch (aspect) {
                    case IGNIS -> "🜂";
                    case AQUA -> "🜄";
                    case TERRA -> "🜃";
                    case AER -> "🜁";
                    case SULFUR -> "🜍";
                    case MERCURIUS -> "☿";
                    case SAL -> "🜔";
                    case AETHER -> "✧";
                    case VITA -> "⚕";
                    case MORTIS -> "☠";
                    case ORDO -> "⚖";
                    case CHAOS -> "❈";
                };
                Style colorStyle = Style.EMPTY.withColor(TextColor.fromRgb(aspect.getColor()));
                tooltip.add(Component.literal("  " + icon + " ").setStyle(colorStyle)
                        .append(Component.translatable(aspect.getTranslationKey()).setStyle(colorStyle))
                        .append(Component.literal(String.format(Locale.ROOT, " : %.2f", val)).withStyle(ChatFormatting.GRAY)));
            }
        }
    }
}
