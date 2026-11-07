package com.medievalmagic.arsalchemica.potion;

import com.medievalmagic.arsalchemica.api.vector.AspectVector;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * 动态复合炼金药水载体。
 * <p>
 * 支持直接饮用灵药 (ELIXIR) 与范围挥发熏香 (INCENSE) 双重交付形态。
 * 基于 NBT 存储的连续以太流形空间实时解算药效。
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public class AlchemicalPotionItem extends Item {

    public enum DeliveryType {
        ELIXIR,  // 直接饮用型复合灵药
        INCENSE  // 范围挥发型炼金熏香
    }

    private final DeliveryType deliveryType;

    public AlchemicalPotionItem(Properties properties, DeliveryType deliveryType) {
        super(properties);
        this.deliveryType = deliveryType != null ? deliveryType : DeliveryType.ELIXIR;
    }

    public AlchemicalPotionItem(Properties properties) {
        this(properties, DeliveryType.ELIXIR);
    }

    public DeliveryType getDeliveryType() {
        return deliveryType;
    }

    /**
     * 工厂方法：从以太向量构建动态药剂物品实例。
     */
    public static ItemStack create(Item itemInstance, AspectVector vector, double mass, double purity, double volatility) {
        ItemStack stack = new ItemStack(itemInstance);
        CompoundTag root = stack.getOrCreateTagElement(DynamicPotionResolver.TAG_POTION_DATA);
        root.put(DynamicPotionResolver.TAG_VECTOR, vector.serializeNBT());
        root.putDouble(DynamicPotionResolver.TAG_MASS, mass);
        root.putDouble(DynamicPotionResolver.TAG_PURITY, purity);
        root.putDouble(DynamicPotionResolver.TAG_VOLATILITY, volatility);
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return this.deliveryType == DeliveryType.INCENSE ? 20 : 32;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide());
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide()) {
            CompoundTag rootTag = stack.getTag();
            List<MobEffectInstance> effects = DynamicPotionResolver.resolveEffects(rootTag);
            float toxicityLoad = DynamicPotionResolver.calculateToxicityLoad(rootTag);

            if (this.deliveryType == DeliveryType.ELIXIR) {
                for (MobEffectInstance inst : effects) {
                    entity.addEffect(new MobEffectInstance(inst));
                }

                if (entity instanceof Player player) {
                    PlayerToxicityManager.getData(player).ifPresent(data -> {
                        data.addToxicity(toxicityLoad);
                        PlayerToxicityManager.syncToClient(player, data);
                    });
                }
            } else if (this.deliveryType == DeliveryType.INCENSE) {
                spawnIncenseCloud(level, entity, rootTag, effects, toxicityLoad * 0.35f);
            }
        }

        if (entity instanceof Player player && !player.isCreative()) {
            stack.shrink(1);
            if (stack.isEmpty()) {
                return new ItemStack(Items.GLASS_BOTTLE);
            }
            player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE));
        }

        return stack;
    }

    private void spawnIncenseCloud(Level level, LivingEntity owner, @Nullable CompoundTag rootTag, List<MobEffectInstance> effects, float ambientTox) {
        AreaEffectCloud cloud = new AreaEffectCloud(level, owner.getX(), owner.getY(), owner.getZ());
        cloud.setOwner(owner);
        cloud.setRadius(3.5f);
        cloud.setRadiusOnUse(-0.25f);
        cloud.setWaitTime(10);
        cloud.setDuration(240);
        cloud.setRadiusPerTick(-cloud.getRadius() / (float) cloud.getDuration());
        cloud.setFixedColor(DynamicPotionResolver.getBlendedColor(rootTag));

        for (MobEffectInstance effect : effects) {
            cloud.addEffect(new MobEffectInstance(effect.getEffect(), Math.max(40, effect.getDuration() / 2), effect.getAmplifier()));
        }
        level.addFreshEntity(cloud);
        if (owner instanceof Player player) {
            level.playSound(player, owner.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1.0f, 0.85f);
        }

        // 释放熏香者自身吸入 35% 挥发以太毒素
        if (owner instanceof Player player) {
            PlayerToxicityManager.getData(player).ifPresent(data -> {
                data.addToxicity(ambientTox);
                PlayerToxicityManager.syncToClient(player, data);
            });
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTagElement(DynamicPotionResolver.TAG_POTION_DATA);
        if (tag == null) {
            tooltip.add(Component.translatable("tooltip.ars_alchemica.potion.inert").withStyle(ChatFormatting.GRAY));
            return;
        }

        double purity = tag.getDouble(DynamicPotionResolver.TAG_PURITY) * 100.0;
        float tox = DynamicPotionResolver.calculateToxicityLoad(stack.getTag());

        tooltip.add(Component.translatable("tooltip.ars_alchemica.potion.purity", String.format(Locale.ROOT, "%.1f%%", purity))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.ars_alchemica.potion.toxicity", String.format(Locale.ROOT, "+%.1f", tox))
                .withStyle(tox >= 45.0f ? ChatFormatting.RED : ChatFormatting.DARK_GREEN));

        List<MobEffectInstance> effects = DynamicPotionResolver.resolveEffects(stack.getTag());
        for (MobEffectInstance inst : effects) {
            tooltip.add(Component.literal("  ✦ ")
                    .append(Component.translatable(inst.getDescriptionId()))
                    .append(Component.literal(String.format(Locale.ROOT, " (等级 %d, %d秒)", inst.getAmplifier() + 1, inst.getDuration() / 20)))
                    .withStyle(ChatFormatting.GREEN));
        }
    }

    public static int getItemColor(ItemStack stack, int tintIndex) {
        return tintIndex == 0 ? DynamicPotionResolver.getBlendedColor(stack.getTag()) : 0xFFFFFF;
    }
}
