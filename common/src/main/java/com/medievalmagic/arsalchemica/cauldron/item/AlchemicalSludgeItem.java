package com.medievalmagic.arsalchemica.cauldron.item;

import com.medievalmagic.arsalchemica.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 炼金败物腐蚀黑泥 (Alchemical Sludge Item)。
 * <p>
 * 使用铁铲从反应失控凝固的坩埚中清理出的恶臭有毒淤泥。
 * 右键掷出可在着弹点生成微型腐蚀毒雾，使生物受到减速与以太毒蚀。
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public class AlchemicalSludgeItem extends Item {

    public AlchemicalSludgeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            // 在目标前方生成微型腐蚀滞留黑泥雾
            AreaEffectCloud cloud = new AreaEffectCloud(level, player.getX(), player.getY() + 0.5, player.getZ());
            cloud.setRadius(2.0f);
            cloud.setDuration(120);
            cloud.addEffect(new MobEffectInstance(ModEffects.getCorrosion(), 80, 0));
            cloud.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            level.addFreshEntity(cloud);

            level.playSound(null, player.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 0.9f, 0.8f);
            if (!player.isCreative()) {
                held.shrink(1);
            }
        }

        return InteractionResultHolder.sidedSuccess(held, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.ars_alchemica.sludge.desc")
                .withStyle(ChatFormatting.DARK_GREEN));
        tooltip.add(Component.translatable("tooltip.ars_alchemica.sludge.use")
                .withStyle(ChatFormatting.GRAY));
    }
}
