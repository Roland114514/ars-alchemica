package com.medievalmagic.arsalchemica.recipe.item;

import com.medievalmagic.arsalchemica.recipe.AlchemicalRecipe;
import com.medievalmagic.arsalchemica.recipe.AlchemicalRecipeRegistry;
import com.medievalmagic.arsalchemica.recipe.knowledge.PlayerKnowledgeManager;
import com.medievalmagic.arsalchemica.recipe.knowledge.PlayerRecipeKnowledge;
import net.minecraft.ChatFormatting;
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
 * 古代学术注脚残页 (Ancient Scholia Fragment Item)。
 * <p>
 * 专门针对特定古代秘方的补充学术批注。
 * 玩家研读使用后，可永久提升对应古代秘方的炼成造诣（每次提升 +15% 成功率，最高提升至 95%）。
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public class ScholiaFragmentItem extends Item {

    public static final String TAG_TARGET_RECIPE = "TargetRecipeId";

    public ScholiaFragmentItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(Item item, ResourceLocation recipeId) {
        ItemStack stack = new ItemStack(item);
        if (recipeId != null) {
            stack.getOrCreateTag().putString(TAG_TARGET_RECIPE, recipeId.toString());
        }
        return stack;
    }

    @Nullable
    public static ResourceLocation getTargetRecipeId(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(TAG_TARGET_RECIPE)) {
            return ResourceLocation.tryParse(stack.getTag().getString(TAG_TARGET_RECIPE));
        }
        return null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        ResourceLocation recipeId = getTargetRecipeId(held);

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

        boolean increased = knowledge.increaseMastery(recipeId, 1);
        if (!increased) {
            player.displayClientMessage(
                    Component.translatable("message.ars_alchemica.mastery_already_maxed", Component.translatable(recipe.getTranslationKey()))
                            .withStyle(ChatFormatting.GOLD),
                    true
            );
            return InteractionResultHolder.fail(held);
        }

        int newLevel = knowledge.getMastery(recipeId);
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.2f);
        player.displayClientMessage(
                Component.translatable("message.ars_alchemica.mastery_increased",
                                Component.translatable(recipe.getTranslationKey()),
                                newLevel,
                                PlayerRecipeKnowledge.MAX_MASTERY_LEVEL)
                        .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD),
                false
        );

        if (!player.isCreative()) {
            held.shrink(1);
        }

        return InteractionResultHolder.sidedSuccess(held, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation recipeId = getTargetRecipeId(stack);

        tooltip.add(Component.translatable("tooltip.ars_alchemica.scholia.desc")
                .withStyle(ChatFormatting.GRAY));

        if (recipeId != null) {
            Optional<AlchemicalRecipe> recipeOpt = AlchemicalRecipeRegistry.getInstance().get(recipeId);
            if (recipeOpt.isPresent()) {
                tooltip.add(Component.translatable("tooltip.ars_alchemica.scholia.target",
                                Component.translatable(recipeOpt.get().getTranslationKey()))
                        .withStyle(ChatFormatting.GOLD));
            }
        }

        tooltip.add(Component.translatable("tooltip.ars_alchemica.scholia.benefit", "+15%")
                .withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("tooltip.ars_alchemica.scholia.use_hint")
                .withStyle(ChatFormatting.AQUA));
    }
}
