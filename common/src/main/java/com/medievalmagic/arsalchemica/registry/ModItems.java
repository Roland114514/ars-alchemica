package com.medievalmagic.arsalchemica.registry;

import com.medievalmagic.arsalchemica.cauldron.AlchemicalStirringRodItem;
import com.medievalmagic.arsalchemica.cauldron.item.AlchemicalSlagItem;
import com.medievalmagic.arsalchemica.cauldron.item.AlchemicalSludgeItem;
import com.medievalmagic.arsalchemica.grimoire.AlchemicalJournalItem;
import com.medievalmagic.arsalchemica.herb.apparatus.ProcessedHerbItem;
import com.medievalmagic.arsalchemica.herb.item.AlchemicalHerbItem;
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
    private static Supplier<AlchemicalSlagItem> SLAG = () -> null;
    private static Supplier<AlchemicalSludgeItem> SLUDGE = () -> null;

    // 7 大草药植株物品
    private static Supplier<AlchemicalHerbItem> CINDER_THISTLE = () -> null;
    private static Supplier<AlchemicalHerbItem> FROSTVEIN_ORCHID = () -> null;
    private static Supplier<AlchemicalHerbItem> STORM_MARIGOLD = () -> null;
    private static Supplier<AlchemicalHerbItem> ABYSSAL_LUMINA_MINT = () -> null;
    private static Supplier<AlchemicalHerbItem> SCREAMING_MANDRAGORA = () -> null;
    private static Supplier<AlchemicalHerbItem> MOONVEIL_BELLADONNA = () -> null;
    private static Supplier<AlchemicalHerbItem> CARRION_BOGMOSS = () -> null;

    // 统一草药加工产物载体
    private static Supplier<ProcessedHerbItem> PROCESSED_HERB = () -> null;

    public static void bindHerbs(
            Supplier<AlchemicalHerbItem> cinderThistle,
            Supplier<AlchemicalHerbItem> frostveinOrchid,
            Supplier<AlchemicalHerbItem> stormMarigold,
            Supplier<AlchemicalHerbItem> abyssalMint,
            Supplier<AlchemicalHerbItem> mandragora,
            Supplier<AlchemicalHerbItem> belladonna,
            Supplier<AlchemicalHerbItem> bogmoss,
            Supplier<ProcessedHerbItem> processedHerb
    ) {
        CINDER_THISTLE = Objects.requireNonNull(cinderThistle, "Cinder thistle item supplier cannot be null");
        FROSTVEIN_ORCHID = Objects.requireNonNull(frostveinOrchid, "Frostvein orchid item supplier cannot be null");
        STORM_MARIGOLD = Objects.requireNonNull(stormMarigold, "Storm marigold item supplier cannot be null");
        ABYSSAL_LUMINA_MINT = Objects.requireNonNull(abyssalMint, "Abyssal mint item supplier cannot be null");
        SCREAMING_MANDRAGORA = Objects.requireNonNull(mandragora, "Mandragora item supplier cannot be null");
        MOONVEIL_BELLADONNA = Objects.requireNonNull(belladonna, "Belladonna item supplier cannot be null");
        CARRION_BOGMOSS = Objects.requireNonNull(bogmoss, "Bogmoss item supplier cannot be null");
        PROCESSED_HERB = Objects.requireNonNull(processedHerb, "Processed herb item supplier cannot be null");
    }

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

    public static void bindMishapItems(
            Supplier<AlchemicalSlagItem> slagSupplier,
            Supplier<AlchemicalSludgeItem> sludgeSupplier
    ) {
        SLAG = Objects.requireNonNull(slagSupplier, "Slag supplier cannot be null");
        SLUDGE = Objects.requireNonNull(sludgeSupplier, "Sludge supplier cannot be null");
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

    public static AlchemicalSlagItem getSlag() {
        return SLAG.get();
    }

    public static AlchemicalSludgeItem getSludge() {
        return SLUDGE.get();
    }

    public static AlchemicalHerbItem getCinderThistle() { return CINDER_THISTLE.get(); }
    public static AlchemicalHerbItem getFrostveinOrchid() { return FROSTVEIN_ORCHID.get(); }
    public static AlchemicalHerbItem getStormMarigold() { return STORM_MARIGOLD.get(); }
    public static AlchemicalHerbItem getAbyssalLuminaMint() { return ABYSSAL_LUMINA_MINT.get(); }
    public static AlchemicalHerbItem getScreamingMandragora() { return SCREAMING_MANDRAGORA.get(); }
    public static AlchemicalHerbItem getMoonveilBelladonna() { return MOONVEIL_BELLADONNA.get(); }
    public static AlchemicalHerbItem getCarrionBogmoss() { return CARRION_BOGMOSS.get(); }
    public static ProcessedHerbItem getProcessedHerb() { return PROCESSED_HERB.get(); }
}
