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
 * 炼金配方卷轴 / 学徒手稿 (Recipe Scroll Item)。
 * <p>
 * 可在村庄药剂师处交易购得，或自地牢宝箱发掘。
 * 玩家手持右键研读，若未曾掌握则将其永久收录入副手《炼金手记》中。
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public class RecipeScrollItem extends Item {

    public static final String TAG_RECIPE_ID = "RecipeId";

    public RecipeScrollItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(Item item, ResourceLocation recipeId) {
        ItemStack stack = new ItemStack(item);
        if (recipeId != null) {
            stack.getOrCreateTag().putString(TAG_RECIPE_ID, recipeId.toString());
        }
        return stack;
    }

    @Nullable
    public static ResourceLocation getRecipeId(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(TAG_RECIPE_ID)) {
            return ResourceLocation.tryParse(stack.getTag().getString(TAG_RECIPE_ID));
        }
        return null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        ResourceLocation recipeId = getRecipeId(held);

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

        if (knowledge.isDiscovered(recipeId)) {
            player.displayClientMessage(
                    Component.translatable("message.ars_alchemica.recipe_already_known")
                            .withStyle(ChatFormatting.YELLOW),
                    true
            );
            return InteractionResultHolder.fail(held);
        }

        // 成功收录新配方
        knowledge.unlockRecipe(recipeId);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);
        player.displayClientMessage(
                Component.translatable("message.ars_alchemica.recipe_learned", Component.translatable(recipe.getTranslationKey()))
                        .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                false
        );

        if (!player.isCreative()) {
            held.shrink(1);
        }

        return InteractionResultHolder.sidedSuccess(held, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation recipeId = getRecipeId(stack);
        if (recipeId != null) {
            Optional<AlchemicalRecipe> recipeOpt = AlchemicalRecipeRegistry.getInstance().get(recipeId);
            if (recipeOpt.isPresent()) {
                AlchemicalRecipe recipe = recipeOpt.get();
                tooltip.add(Component.translatable("tooltip.ars_alchemica.scroll.recipe",
                                Component.translatable(recipe.getTranslationKey()))
                        .withStyle(ChatFormatting.GOLD));
                tooltip.add(Component.translatable("tooltip.ars_alchemica.scroll.rarity",
                                Component.translatable(recipe.getRarity().getTranslationKey()))
                        .withStyle(ChatFormatting.GRAY));
            } else {
                tooltip.add(Component.literal(recipeId.toString()).withStyle(ChatFormatting.DARK_GRAY));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.ars_alchemica.scroll.blank").withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.translatable("tooltip.ars_alchemica.scroll.use_hint").withStyle(ChatFormatting.AQUA));
    }
}
