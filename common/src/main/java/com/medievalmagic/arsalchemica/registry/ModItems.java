package com.medievalmagic.arsalchemica.registry;

import com.medievalmagic.arsalchemica.cauldron.AlchemicalStirringRodItem;
import com.medievalmagic.arsalchemica.grimoire.AlchemicalJournalItem;
import com.medievalmagic.arsalchemica.potion.AlchemicalPotionItem;
import com.medievalmagic.arsalchemica.potion.BladeOilItem;
import com.medievalmagic.arsalchemica.recipe.item.AncientParchmentItem;
import com.medievalmagic.arsalchemica.recipe.item.RecipeScrollItem;
import com.medievalmagic.arsalchemica.recipe.item.ScholiaFragmentItem;
import net.minecraft.world.item.Item;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 跨平台 Item 延迟绑定中心 (Common-First)。
 * <p>
 * 提供多加载器解耦的物品引用与工厂支持。
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public final class ModItems {

    private ModItems() {}

    private static Supplier<AlchemicalPotionItem> ELIXIR = () -> null;
    private static Supplier<AlchemicalPotionItem> INCENSE = () -> null;
    private static Supplier<BladeOilItem> BLADE_OIL = () -> null;
    private static Supplier<AlchemicalStirringRodItem> STIRRING_ROD = () -> null;
    private static Supplier<AlchemicalJournalItem> JOURNAL = () -> null;
    private static Supplier<RecipeScrollItem> RECIPE_SCROLL = () -> null;
    private static Supplier<AncientParchmentItem> ANCIENT_PARCHMENT = () -> null;
    private static Supplier<ScholiaFragmentItem> SCHOLIA_FRAGMENT = () -> null;

    public static void bindPotions(
            Supplier<AlchemicalPotionItem> elixirSupplier,
            Supplier<AlchemicalPotionItem> incenseSupplier,
            Supplier<BladeOilItem> bladeOilSupplier
    ) {
        ELIXIR = Objects.requireNonNull(elixirSupplier, "Elixir supplier cannot be null");
        INCENSE = Objects.requireNonNull(incenseSupplier, "Incense supplier cannot be null");
        BLADE_OIL = Objects.requireNonNull(bladeOilSupplier, "Blade oil supplier cannot be null");
    }

    public static void bindApparatusItems(
            Supplier<AlchemicalStirringRodItem> rodSupplier,
            Supplier<AlchemicalJournalItem> journalSupplier
    ) {
        STIRRING_ROD = Objects.requireNonNull(rodSupplier, "Stirring rod supplier cannot be null");
        JOURNAL = Objects.requireNonNull(journalSupplier, "Journal supplier cannot be null");
    }

    public static void bindExplorationItems(
            Supplier<RecipeScrollItem> scrollSupplier,
            Supplier<AncientParchmentItem> parchmentSupplier,
            Supplier<ScholiaFragmentItem> fragmentSupplier
    ) {
        RECIPE_SCROLL = Objects.requireNonNull(scrollSupplier, "Recipe scroll supplier cannot be null");
        ANCIENT_PARCHMENT = Objects.requireNonNull(parchmentSupplier, "Ancient parchment supplier cannot be null");
        SCHOLIA_FRAGMENT = Objects.requireNonNull(fragmentSupplier, "Scholia fragment supplier cannot be null");
    }

    public static AlchemicalPotionItem getElixir() {
        return ELIXIR.get();
    }

    public static AlchemicalPotionItem getIncense() {
        return INCENSE.get();
    }

    public static BladeOilItem getBladeOil() {
        return BLADE_OIL.get();
    }

    public static AlchemicalStirringRodItem getStirringRod() {
        return STIRRING_ROD.get();
    }

    public static AlchemicalJournalItem getJournal() {
        return JOURNAL.get();
    }

    public static RecipeScrollItem getRecipeScroll() {
        return RECIPE_SCROLL.get();
    }

    public static AncientParchmentItem getAncientParchment() {
        return ANCIENT_PARCHMENT.get();
    }

    public static ScholiaFragmentItem getScholiaFragment() {
        return SCHOLIA_FRAGMENT.get();
    }
}
