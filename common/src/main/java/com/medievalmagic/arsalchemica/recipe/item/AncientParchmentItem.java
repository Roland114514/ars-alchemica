package com.medievalmagic.arsalchemica.recipe.item;

import com.medievalmagic.arsalchemica.recipe.AlchemicalRecipe;
import com.medievalmagic.arsalchemica.recipe.AlchemicalRecipeRegistry;
import com.medievalmagic.arsalchemica.recipe.knowledge.PlayerKnowledgeManager;
import com.medievalmagic.arsalchemica.recipe.knowledge.PlayerRecipeKnowledge;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * 失传古代残卷羊皮纸 (Ancient Parchment Item)。
 * <p>
 * 源自远古地牢、下界要塞与深暗之域的传奇战利品。
 * 记录着神话级超凡秘药工序，初次获得时处于神秘加密状态，需玩家进行多步深度研读破译。
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public class AncientParchmentItem extends Item {

    public static final String TAG_ANCIENT_RECIPE = "AncientRecipeId";
    public static final String TAG_DECIPHER_PROGRESS = "DecipherProgress";

    public AncientParchmentItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(Item item, ResourceLocation recipeId) {
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = stack.getOrCreateTag();
        if (recipeId != null) {
            tag.putString(TAG_ANCIENT_RECIPE, recipeId.toString());
        }
        tag.putInt(TAG_DECIPHER_PROGRESS, 0);
        return stack;
    }

    public static int getDecipherProgress(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(TAG_DECIPHER_PROGRESS)) {
            return stack.getTag().getInt(TAG_DECIPHER_PROGRESS);
        }
        return 0;
    }

    @Nullable
    public static ResourceLocation getAncientRecipeId(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(TAG_ANCIENT_RECIPE)) {
            return ResourceLocation.tryParse(stack.getTag().getString(TAG_ANCIENT_RECIPE));
        }
        return null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        ResourceLocation recipeId = getAncientRecipeId(held);

        if (recipeId == null) {
            return InteractionResultHolder.pass(held);
        }

        if (level.isClientSide()) {
            return InteractionResultHolder.sidedSuccess(held, true);
        }

        Optional<AlchemicalRecipe> recipeOpt = AlchemicalRecipeRegistry.getInstance().get(recipeId);
        if (recipeOpt.isEmpty()) {
            return InteractionResultHolder.pass(held);
        }

        AlchemicalRecipe recipe = recipeOpt.get();
        PlayerRecipeKnowledge knowledge = PlayerKnowledgeManager.getKnowledge(player);
        int currentProgress = getDecipherProgress(held);

        if (currentProgress >= 100 || knowledge.isDiscovered(recipeId)) {
            player.displayClientMessage(
                    Component.translatable("message.ars_alchemica.ancient_already_deciphered", Component.translatable(recipe.getTranslationKey()))
                            .withStyle(ChatFormatting.GOLD),
                    true
            );
            return InteractionResultHolder.fail(held);
        }

        // 推进破译进度 (每次研读 +25%)
        int newProgress = Math.min(100, currentProgress + 25);
        held.getOrCreateTag().putInt(TAG_DECIPHER_PROGRESS, newProgress);

        if (newProgress >= 100) {
            // 破译完成，彻底解锁古代传奇配方！
            knowledge.unlockRecipe(recipeId);
            level.playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
            player.displayClientMessage(
                    Component.translatable("message.ars_alchemica.ancient_decipher_complete", Component.translatable(recipe.getTranslationKey()))
                            .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                    false
            );
            if (!player.isCreative()) {
                held.shrink(1);
            }
        } else {
            // 仍在研读中
            level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8f, 1.1f);
            player.displayClientMessage(
                    Component.translatable("message.ars_alchemica.ancient_decipher_step", newProgress)
                            .withStyle(ChatFormatting.AQUA),
                    true
            );
        }

        return InteractionResultHolder.sidedSuccess(held, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation recipeId = getAncientRecipeId(stack);
        int progress = getDecipherProgress(stack);

        tooltip.add(Component.translatable("tooltip.ars_alchemica.ancient_parchment.desc")
                .withStyle(ChatFormatting.GRAY));

        if (recipeId != null) {
            Optional<AlchemicalRecipe> recipeOpt = AlchemicalRecipeRegistry.getInstance().get(recipeId);
            if (recipeOpt.isPresent()) {
                tooltip.add(Component.translatable("tooltip.ars_alchemica.ancient_parchment.target",
                                Component.translatable(recipeOpt.get().getTranslationKey()))
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }

        if (progress >= 100) {
            tooltip.add(Component.translatable("tooltip.ars_alchemica.ancient_parchment.deciphered")
                    .withStyle(ChatFormatting.GREEN));
        } else {
            tooltip.add(Component.translatable("tooltip.ars_alchemica.ancient_parchment.progress", progress)
                    .withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("tooltip.ars_alchemica.ancient_parchment.hint")
                    .withStyle(ChatFormatting.DARK_AQUA));
        }
    }
}
