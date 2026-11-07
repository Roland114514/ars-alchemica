package com.medievalmagic.arsalchemica.potion;

import com.medievalmagic.arsalchemica.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * 猎魔人式炼金剑油载体与武器涂层引擎。
 * <p>
 * 涂抹于近战兵刃后赋予固定命中次数的生物种群特攻（亡灵银刃、节肢破壳、类人流血、首领弱化）与炼金毒素穿透。
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public class BladeOilItem extends Item {

    public static final String TAG_BLADE_OIL = "ArsAlchemicaBladeOil";
    public static final String TAG_CHARGES = "RemainingCharges";
    public static final String TAG_MAX_CHARGES = "MaxCharges";
    public static final String TAG_BANE_TYPE = "BaneType";
    public static final String TAG_BONUS_DAMAGE = "BonusDamage";
    public static final String TAG_OIL_COLOR = "OilColor";

    public static final int DEFAULT_MAX_CHARGES = 30;

    /**
     * 剑油克制种群分类
     */
    public enum BaneType {
        NECROPHAGE("necrophage", ChatFormatting.LIGHT_PURPLE), // 亡灵与死灵特攻（银刃灼烧）
        INSECTOID("insectoid", ChatFormatting.DARK_GREEN),     // 节肢与昆虫破壳特攻（神经迟缓）
        HUMANOID("humanoid", ChatFormatting.RED),              // 类人与灾厄流血特攻（剧毒流血）
        DRACONID("draconid", ChatFormatting.GOLD);             // 龙裔与世界首领特攻（奥术虚弱）

        private final String id;
        private final ChatFormatting color;

        BaneType(String id, ChatFormatting color) {
            this.id = id;
            this.color = color;
        }

        public String getId() { return id; }
        public ChatFormatting getColor() { return color; }

        public static BaneType fromId(String id) {
            if (id == null) return NECROPHAGE;
            for (BaneType type : values()) {
                if (type.id.equalsIgnoreCase(id)) return type;
            }
            return NECROPHAGE;
        }
    }

    public BladeOilItem(Properties properties) {
        super(properties);
    }

    /**
     * 工厂方法：创建特定特攻属性的剑油 ItemStack。
     */
    public static ItemStack create(Item item, BaneType bane, float bonusDamage, int charges) {
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TAG_BANE_TYPE, bane.getId());
        tag.putFloat(TAG_BONUS_DAMAGE, Math.max(0.5f, bonusDamage));
        tag.putInt(TAG_CHARGES, Math.max(1, charges));
        tag.putInt(TAG_MAX_CHARGES, Math.max(1, charges));
        return stack;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack oilStack = player.getItemInHand(usedHand);
        InteractionHand targetHand = (usedHand == InteractionHand.MAIN_HAND) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack weaponStack = player.getItemInHand(targetHand);

        if (!isValidWeapon(weaponStack)) {
            if (level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.ars_alchemica.oil.invalid_weapon")
                        .withStyle(ChatFormatting.RED), true);
            }
            return InteractionResultHolder.fail(oilStack);
        }

        if (!level.isClientSide()) {
            applyOilToWeapon(oilStack, weaponStack);
            level.playSound(player, player.blockPosition(), SoundEvents.HONEYCOMB_WAX_ON, SoundSource.PLAYERS, 1.0f, 1.15f);
            player.displayClientMessage(Component.translatable("message.ars_alchemica.oil.applied", weaponStack.getHoverName())
                    .withStyle(ChatFormatting.GOLD), true);

            if (!player.isCreative()) {
                oilStack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(oilStack, level.isClientSide());
    }

    public static boolean isValidWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item instanceof SwordItem || item instanceof AxeItem
                || (item instanceof TieredItem && (item.getClass().getSimpleName().contains("Sword") || item.getClass().getSimpleName().contains("Axe")));
    }

    public static void applyOilToWeapon(ItemStack oilStack, ItemStack weaponStack) {
        CompoundTag oilRoot = oilStack.getOrCreateTag();
        String baneId = oilRoot.contains(TAG_BANE_TYPE) ? oilRoot.getString(TAG_BANE_TYPE) : BaneType.NECROPHAGE.getId();
        float bonusDmg = oilRoot.contains(TAG_BONUS_DAMAGE) ? oilRoot.getFloat(TAG_BONUS_DAMAGE) : 4.5f;
        int maxCharges = oilRoot.contains(TAG_MAX_CHARGES) ? oilRoot.getInt(TAG_MAX_CHARGES) : DEFAULT_MAX_CHARGES;
        int charges = oilRoot.contains(TAG_CHARGES) ? oilRoot.getInt(TAG_CHARGES) : maxCharges;
        int color = DynamicPotionResolver.getBlendedColor(oilRoot);

        CompoundTag weaponOilTag = new CompoundTag();
        weaponOilTag.putInt(TAG_CHARGES, charges);
        weaponOilTag.putInt(TAG_MAX_CHARGES, maxCharges);
        weaponOilTag.putString(TAG_BANE_TYPE, baneId);
        weaponOilTag.putFloat(TAG_BONUS_DAMAGE, bonusDmg);
        weaponOilTag.putInt(TAG_OIL_COLOR, color);

        weaponStack.getOrCreateTag().put(TAG_BLADE_OIL, weaponOilTag);
    }

    /**
     * 武器命中拦截结算器（在 Common 实体受击事件或 Mixin LivingEntity#hurt 中调用）。
     * 自动判定种族克制、施加额外特攻伤害与破甲/流血毒素，并原子扣减剩余耐久次数。
     *
     * @return 剑油提供的额外伤害值
     */
    public static float onWeaponHitEntity(ItemStack weaponStack, LivingEntity attacker, LivingEntity target) {
        if (weaponStack == null || weaponStack.isEmpty() || !weaponStack.hasTag()) {
            return 0.0f;
        }
        if (attacker.level() != null && attacker.level().isClientSide()) {
            return 0.0f;
        }

        CompoundTag root = weaponStack.getTag();
        if (root == null || !root.contains(TAG_BLADE_OIL)) {
            return 0.0f;
        }

        CompoundTag oilData = root.getCompound(TAG_BLADE_OIL);
        int charges = oilData.getInt(TAG_CHARGES);
        if (charges <= 0) {
            root.remove(TAG_BLADE_OIL);
            return 0.0f;
        }

        BaneType bane = BaneType.fromId(oilData.getString(TAG_BANE_TYPE));
        float baseBonus = oilData.getFloat(TAG_BONUS_DAMAGE);
        float finalExtraDamage;

        if (matchesBaneTarget(bane, target)) {
            finalExtraDamage = baseBonus;
            // 命中特攻种族：附加专属炼金毒理穿透反应
            switch (bane) {
                case NECROPHAGE -> target.setSecondsOnFire(4);
                case INSECTOID -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
                case HUMANOID -> target.addEffect(new MobEffectInstance(ModEffects.getCorrosion(), 100, 1));
                case DRACONID -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
            }
        } else {
            // 非目标种族：仍承受 30% 基础炼金腐蚀伤害
            finalExtraDamage = baseBonus * 0.30f;
        }

        // 扣减涂层耐久次数，归零时自动剥离 NBT，杜绝无用数据残留
        charges -= 1;
        if (charges <= 0) {
            root.remove(TAG_BLADE_OIL);
            if (attacker instanceof Player player) {
                player.displayClientMessage(Component.translatable("message.ars_alchemica.oil.depleted")
                        .withStyle(ChatFormatting.GRAY), true);
            }
        } else {
            oilData.putInt(TAG_CHARGES, charges);
        }

        return finalExtraDamage;
    }

    public static boolean matchesBaneTarget(BaneType bane, LivingEntity target) {
        if (target == null) return false;
        MobType mobType = target.getMobType();
        return switch (bane) {
            case NECROPHAGE -> mobType == MobType.UNDEAD;
            case INSECTOID -> mobType == MobType.ARTHROPOD;
            case HUMANOID -> mobType == MobType.ILLAGER || target instanceof Player;
            case DRACONID -> !target.canChangeDimensions(); // Boss 级生物特征
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        BaneType bane = (tag != null && tag.contains(TAG_BANE_TYPE))
                ? BaneType.fromId(tag.getString(TAG_BANE_TYPE))
                : BaneType.NECROPHAGE;
        float bonus = (tag != null && tag.contains(TAG_BONUS_DAMAGE)) ? tag.getFloat(TAG_BONUS_DAMAGE) : 4.5f;
        int max = (tag != null && tag.contains(TAG_MAX_CHARGES)) ? tag.getInt(TAG_MAX_CHARGES) : DEFAULT_MAX_CHARGES;
        int charges = (tag != null && tag.contains(TAG_CHARGES)) ? tag.getInt(TAG_CHARGES) : max;

        tooltip.add(Component.translatable("tooltip.ars_alchemica.oil.bane." + bane.getId())
                .withStyle(bane.getColor()));
        tooltip.add(Component.translatable("tooltip.ars_alchemica.oil.damage", String.format(Locale.ROOT, "+%.1f", bonus))
                .withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("tooltip.ars_alchemica.oil.charges", charges, max)
                .withStyle(ChatFormatting.GRAY));
    }

    /**
     * 供客户端 ItemTooltipEvent 挂载：为任何涂有剑油的原版或模组武器动态追加剑油剩余次数提示。
     */
    public static void appendWeaponCoatTooltip(ItemStack weaponStack, List<Component> tooltip) {
        if (weaponStack == null || !weaponStack.hasTag()) return;
        CompoundTag root = weaponStack.getTag();
        if (root != null && root.contains(TAG_BLADE_OIL)) {
            CompoundTag oil = root.getCompound(TAG_BLADE_OIL);
            int remaining = oil.getInt(TAG_CHARGES);
            int max = oil.getInt(TAG_MAX_CHARGES);
            BaneType bane = BaneType.fromId(oil.getString(TAG_BANE_TYPE));

            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("tooltip.ars_alchemica.weapon.coated",
                    Component.translatable("tooltip.ars_alchemica.oil.bane." + bane.getId()),
                    remaining, max).withStyle(bane.getColor()));
        }
    }
}
