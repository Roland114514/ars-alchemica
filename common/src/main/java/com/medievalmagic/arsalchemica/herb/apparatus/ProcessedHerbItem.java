package com.medievalmagic.arsalchemica.herb.apparatus;

import com.medievalmagic.arsalchemica.api.aspect.AlchemyAspect;
import com.medievalmagic.arsalchemica.api.vector.AspectVector;
import com.medievalmagic.arsalchemica.herb.IAlchemicalHerb.ProcessingForm;
import com.medievalmagic.arsalchemica.herb.HerbRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * 统一草药物理加工产物载体（加固版）。
 * <p>
 * 承载 7 大基础草药在 5 种物理形态下的多态表现、动态以太向量解算及晨露品质继承。
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public class ProcessedHerbItem extends Item {

    public static final String TAG_ALCHEMICAL_DATA = "AlchemicalData";
    public static final String TAG_HERB_ID = "HerbId";
    public static final String TAG_FORM = "Form";
    public static final String TAG_QUALITY = "Quality";
    public static final String TAG_IS_SLAG = "IsSlag";
    public static final String TAG_DEW_BONUS = "DewBonus";

    private final HerbRegistry defaultHerb;
    private final ProcessingForm defaultForm;

    public ProcessedHerbItem(Properties properties, HerbRegistry defaultHerb, ProcessingForm defaultForm) {
        super(properties);
        this.defaultHerb = defaultHerb;
        this.defaultForm = defaultForm;
    }

    /**
     * 工厂方法：构建带有精确药学属性与品质系数的产物实例。
     */
    public static ItemStack create(Item itemInstance, HerbRegistry herb, ProcessingForm form, double qualityMultiplier) {
        ItemStack stack = new ItemStack(itemInstance);
        CompoundTag tag = stack.getOrCreateTagElement(TAG_ALCHEMICAL_DATA);
        tag.putString(TAG_HERB_ID, herb.getId());
        tag.putString(TAG_FORM, form.name());
        tag.putDouble(TAG_QUALITY, Math.max(0.1, Math.min(2.5, qualityMultiplier)));
        tag.putBoolean(TAG_IS_SLAG, false);
        return stack;
    }

    /**
     * 工厂方法：生成过度加工或脱脂后的碳化废渣 (Charred Slag)。
     */
    public static ItemStack createSlag(Item itemInstance, HerbRegistry sourceHerb) {
        ItemStack stack = new ItemStack(itemInstance);
        CompoundTag tag = stack.getOrCreateTagElement(TAG_ALCHEMICAL_DATA);
        tag.putString(TAG_HERB_ID, sourceHerb.getId());
        tag.putString(TAG_FORM, ProcessingForm.MILLED_FINE.name());
        tag.putDouble(TAG_QUALITY, 0.0);
        tag.putBoolean(TAG_IS_SLAG, true);
        return stack;
    }

    public HerbRegistry getHerb(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(TAG_ALCHEMICAL_DATA);
        if (tag != null && tag.contains(TAG_HERB_ID)) {
            String id = tag.getString(TAG_HERB_ID);
            for (HerbRegistry entry : HerbRegistry.values()) {
                if (entry.getId().equalsIgnoreCase(id)) {
                    return entry;
                }
            }
        }
        return this.defaultHerb;
    }

    public ProcessingForm getForm(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(TAG_ALCHEMICAL_DATA);
        if (tag != null && tag.contains(TAG_FORM)) {
            try {
                return ProcessingForm.valueOf(tag.getString(TAG_FORM));
            } catch (IllegalArgumentException ignored) {}
        }
        return this.defaultForm;
    }

    public double getQuality(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(TAG_ALCHEMICAL_DATA);
        return (tag != null && tag.contains(TAG_QUALITY)) ? tag.getDouble(TAG_QUALITY) : 1.0;
    }

    public boolean isSlag(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(TAG_ALCHEMICAL_DATA);
        return tag != null && tag.getBoolean(TAG_IS_SLAG);
    }

    public boolean hasDewBonus(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(TAG_ALCHEMICAL_DATA);
        return tag != null && tag.getBoolean(TAG_DEW_BONUS);
    }

    public static void setDewBonus(ItemStack stack, boolean dewBonus) {
        CompoundTag tag = stack.getOrCreateTagElement(TAG_ALCHEMICAL_DATA);
        tag.putBoolean(TAG_DEW_BONUS, dewBonus);
    }

    /**
     * 实时解算当前物品栈的 AspectVector。
     */
    public AspectVector getRuntimeAspectVector(ItemStack stack) {
        if (isSlag(stack)) {
            // 废渣仅残留极微量死寂与地相
            return AspectVector.builder()
                    .add(AlchemyAspect.TERRA, 0.5)
                    .add(AlchemyAspect.MORTIS, 0.8)
                    .build();
        }
        HerbRegistry herb = getHerb(stack);
        ProcessingForm form = getForm(stack);
        double quality = getQuality(stack);
        return herb.getAspectVector(form).multiply(quality);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (isSlag(stack)) {
            return Component.translatable("item.ars_alchemica.charred_slag")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
        }
        HerbRegistry herb = getHerb(stack);
        ProcessingForm form = getForm(stack);
        return Component.translatable("item.ars_alchemica.herb_form." + form.name().toLowerCase(Locale.ROOT),
                Component.translatable("herb.ars_alchemica." + herb.getId()));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (isSlag(stack)) {
            tooltip.add(Component.literal("❝ 药性已在粗暴的物理外力下彻底碳化崩解。 ❞")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            tooltip.add(Component.literal("☠ 残留毒性: 极高 (不可入药)").withStyle(ChatFormatting.DARK_RED));
            return;
        }

        HerbRegistry herb = getHerb(stack);
        ProcessingForm form = getForm(stack);
        double quality = getQuality(stack);
        AspectVector vec = getRuntimeAspectVector(stack);

        // 1. 形态学引文
        tooltip.add(Component.translatable("tooltip.ars_alchemica.form." + form.name().toLowerCase(Locale.ROOT))
                .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));

        if (hasDewBonus(stack)) {
            tooltip.add(Component.translatable("tooltip.ars_alchemica.herb.dew_inherited")
                    .withStyle(ChatFormatting.AQUA));
        }

        // 2. 纯度星级解算
        double purity = vec.calculatePurity() * quality;
        tooltip.add(buildPurityStars(Math.min(1.0, purity)));

        // 3. 动力学指标：挥发性与毒性
        double vol = herb.getVolatilityModifier(form);
        double tox = herb.getBaseToxicity(form);
        MutableComponent kinetics = Component.literal("❖ 挥发率: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.format(Locale.ROOT, "%.0f%%", vol * 100.0)).withStyle(ChatFormatting.AQUA))
                .append(Component.literal("  ☠ 毒性: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.format(Locale.ROOT, "%.1f", tox))
                        .withStyle(tox > 30.0 ? ChatFormatting.RED : ChatFormatting.GREEN));
        tooltip.add(kinetics);

        // 4. 分割线
        tooltip.add(Component.literal("━━━━━━━━━━━━━━━━━━━━━").withStyle(ChatFormatting.DARK_GRAY));

        // 5. 主导元素图标与数值渲染
        for (AlchemyAspect aspect : AlchemyAspect.VALUES) {
            double val = vec.getAmount(aspect);
            if (val > 0.05) {
                tooltip.add(formatAspectLine(aspect, val));
            }
        }
    }

    private Component buildPurityStars(double purity) {
        int fullStars = (int) (purity * 5.0);
        boolean halfStar = (purity * 5.0 - fullStars) >= 0.5;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            if (i < fullStars) sb.append("★");
            else if (i == fullStars && halfStar) sb.append("✪");
            else sb.append("☆");
        }
        ChatFormatting starColor = purity >= 0.8 ? ChatFormatting.LIGHT_PURPLE
                : (purity >= 0.5 ? ChatFormatting.YELLOW : ChatFormatting.GRAY);

        return Component.literal("✦ 纯度星级: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(sb.toString()).withStyle(starColor))
                .append(Component.literal(String.format(Locale.ROOT, " (%.0f%%)", purity * 100.0)).withStyle(ChatFormatting.DARK_GRAY));
    }

    private Component formatAspectLine(AlchemyAspect aspect, double value) {
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
        return Component.literal("  " + icon + " ").setStyle(colorStyle)
                .append(Component.translatable(aspect.getTranslationKey()).setStyle(colorStyle))
                .append(Component.literal(String.format(Locale.ROOT, " : %.2f", value)).withStyle(ChatFormatting.GRAY));
    }
}
