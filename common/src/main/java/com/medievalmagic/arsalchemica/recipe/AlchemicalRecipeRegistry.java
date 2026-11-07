package com.medievalmagic.arsalchemica.recipe;

import com.medievalmagic.arsalchemica.ArsAlchemica;
import com.medievalmagic.arsalchemica.api.aspect.AlchemyAspect;
import com.medievalmagic.arsalchemica.api.cauldron.CauldronReactionState.HeatLevel;
import com.medievalmagic.arsalchemica.api.vector.AspectVector;
import com.medievalmagic.arsalchemica.potion.AlchemicalPotionItem;
import com.medievalmagic.arsalchemica.potion.BladeOilItem;
import com.medievalmagic.arsalchemica.potion.BladeOilItem.BaneType;
import com.medievalmagic.arsalchemica.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 炼金配方注册表与拓扑查询引擎。
 * <p>
 * 维护全模组已注册的炼金配方，并提供星图距离与操作条件的高性能非装箱拓扑匹配。
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public final class AlchemicalRecipeRegistry {

    private static final AlchemicalRecipeRegistry INSTANCE = new AlchemicalRecipeRegistry();

    private final Map<ResourceLocation, AlchemicalRecipe> recipes = new ConcurrentHashMap<>();

    private AlchemicalRecipeRegistry() {
        registerCanonRecipes();
    }

    public static AlchemicalRecipeRegistry getInstance() {
        return INSTANCE;
    }

    public void register(AlchemicalRecipe recipe) {
        Objects.requireNonNull(recipe, "Recipe cannot be null");
        this.recipes.put(recipe.getRecipeId(), recipe);
    }

    public Optional<AlchemicalRecipe> get(ResourceLocation id) {
        return Optional.ofNullable(this.recipes.get(id));
    }

    public Collection<AlchemicalRecipe> getAll() {
        return Collections.unmodifiableCollection(this.recipes.values());
    }

    /**
     * 拓扑匹配：在当前坩埚状态下寻找最佳命中的配方。
     * 若同时命中多个，优先返回距离靶心最近的配方。
     */
    public Optional<AlchemicalRecipe> findMatchingRecipe(
            double currentX,
            double currentY,
            HeatLevel heat,
            int cwTurns,
            int ccwTurns,
            double purity
    ) {
        AlchemicalRecipe bestMatch = null;
        double minDistance = Double.MAX_VALUE;

        for (AlchemicalRecipe recipe : this.recipes.values()) {
            if (recipe.matches(currentX, currentY, heat, cwTurns, ccwTurns, purity)) {
                double dist = recipe.distanceTo(currentX, currentY);
                if (dist < minDistance) {
                    minDistance = dist;
                    bestMatch = recipe;
                }
            }
        }
        return Optional.ofNullable(bestMatch);
    }

    /**
     * 查找距离当前星图坐标最近的配方（用于学者手记或探针的导航提示）。
     */
    public Optional<AlchemicalRecipe> findNearestRecipe(double currentX, double currentY) {
        AlchemicalRecipe nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (AlchemicalRecipe recipe : this.recipes.values()) {
            double dist = recipe.distanceTo(currentX, currentY);
            if (dist < minDistance) {
                minDistance = dist;
                nearest = recipe;
            }
        }
        return Optional.ofNullable(nearest);
    }

    /**
     * 注册模组经典内置配方库。
     */
    private void registerCanonRecipes() {
        // 1. 生机灵药 (Draught of Vitality - 基础入门)
        register(AlchemicalRecipe.builder(ArsAlchemica.id("draught_of_vitality"))
                .translationKey("recipe.ars_alchemica.draught_of_vitality")
                .rarity(AlchemicalRecipe.RecipeRarity.COMMON)
                .target(12.5, 14.0)
                .tolerance(4.0)
                .heat(HeatLevel.WARM_SIMMER)
                .stirring(1, 0)
                .minPurity(0.40)
                .baseSuccessRate(1.0)
                .result((mass, purity) -> {
                    AspectVector vec = AspectVector.builder()
                            .set(AlchemyAspect.VITA, 12.0)
                            .set(AlchemyAspect.AQUA, 6.0)
                            .build();
                    return AlchemicalPotionItem.create(ModItems.getElixir(), vec, mass, purity, 0.2);
                })
                .build());

        // 2. 猫眼夜视灵药 (Cat's Eyes Elixir - 探索入门)
        register(AlchemicalRecipe.builder(ArsAlchemica.id("cat_eyes_elixir"))
                .translationKey("recipe.ars_alchemica.cat_eyes_elixir")
                .rarity(AlchemicalRecipe.RecipeRarity.COMMON)
                .target(-8.0, 16.0)
                .tolerance(3.5)
                .heat(HeatLevel.WARM_SIMMER)
                .stirring(1, 1)
                .minPurity(0.45)
                .baseSuccessRate(1.0)
                .result((mass, purity) -> {
                    AspectVector vec = AspectVector.builder()
                            .set(AlchemyAspect.AER, 10.0)
                            .set(AlchemyAspect.MERCURIUS, 6.0)
                            .build();
                    return AlchemicalPotionItem.create(ModItems.getElixir(), vec, mass, purity, 0.4);
                })
                .build());

        // 3. 迅疾灵药 (Swift Step Elixir - 二阶进阶)
        register(AlchemicalRecipe.builder(ArsAlchemica.id("swift_step_elixir"))
                .translationKey("recipe.ars_alchemica.swift_step_elixir")
                .rarity(AlchemicalRecipe.RecipeRarity.UNCOMMON)
                .target(18.0, -12.0)
                .tolerance(3.0)
                .heat(HeatLevel.ROILING_BOIL)
                .stirring(2, 0)
                .minPurity(0.55)
                .baseSuccessRate(0.90)
                .result((mass, purity) -> {
                    AspectVector vec = AspectVector.builder()
                            .set(AlchemyAspect.AER, 14.0)
                            .set(AlchemyAspect.SULFUR, 8.0)
                            .build();
                    return AlchemicalPotionItem.create(ModItems.getElixir(), vec, mass, purity, 0.7);
                })
                .build());

        // 4. 烈火御守灵药 (Ignis Ward Elixir - 二阶进阶)
        register(AlchemicalRecipe.builder(ArsAlchemica.id("ignis_ward_elixir"))
                .translationKey("recipe.ars_alchemica.ignis_ward_elixir")
                .rarity(AlchemicalRecipe.RecipeRarity.UNCOMMON)
                .target(28.0, 6.5)
                .tolerance(3.5)
                .heat(HeatLevel.ROILING_BOIL)
                .stirring(2, 1)
                .minPurity(0.60)
                .baseSuccessRate(0.85)
                .result((mass, purity) -> {
                    AspectVector vec = AspectVector.builder()
                            .set(AlchemyAspect.IGNIS, 16.0)
                            .set(AlchemyAspect.TERRA, 8.0)
                            .build();
                    return AlchemicalPotionItem.create(ModItems.getElixir(), vec, mass, purity, 0.3);
                })
                .build());

        // 5. 尸鬼特攻剑油 (Necrophage Oil - 三阶猎魔剑油)
        register(AlchemicalRecipe.builder(ArsAlchemica.id("necro_bane_oil"))
                .translationKey("recipe.ars_alchemica.necro_bane_oil")
                .rarity(AlchemicalRecipe.RecipeRarity.RARE)
                .target(-20.0, -14.0)
                .tolerance(2.8)
                .heat(HeatLevel.WARM_SIMMER)
                .stirring(3, 2)
                .minPurity(0.70)
                .baseSuccessRate(0.75)
                .result((mass, purity) -> {
                    double bonus = 4.0 + (purity * 3.5);
                    return BladeOilItem.create(ModItems.getBladeOil(), BaneType.NECROPHAGE, (float) bonus, 35);
                })
                .build());

        // 6. 凤凰涅槃秘药 (Phoenix Rebirth Elixir - 传奇古代秘方)
        register(AlchemicalRecipe.builder(ArsAlchemica.id("phoenix_rebirth_elixir"))
                .translationKey("recipe.ars_alchemica.phoenix_rebirth_elixir")
                .ancient()
                .target(55.0, 48.0)
                .tolerance(2.2)
                .heat(HeatLevel.ASTRAL_INFERNO)
                .stirring(4, 3)
                .minPurity(0.85)
                .baseSuccessRate(0.25)
                .result((mass, purity) -> {
                    AspectVector vec = AspectVector.builder()
                            .set(AlchemyAspect.VITA, 25.0)
                            .set(AlchemyAspect.IGNIS, 20.0)
                            .set(AlchemyAspect.AETHER, 15.0)
                            .build();
                    ItemStack stack = AlchemicalPotionItem.create(ModItems.getElixir(), vec, mass, purity, 0.9);
                    stack.getOrCreateTag().putBoolean("PhoenixRebirthLegendary", true);
                    return stack;
                })
                .build());

        // 7. 虚空穿刺剑油 (Void Strike Blade Oil - 传奇古代秘方)
        register(AlchemicalRecipe.builder(ArsAlchemica.id("void_strike_oil"))
                .translationKey("recipe.ars_alchemica.void_strike_oil")
                .ancient()
                .target(-45.0, 52.0)
                .tolerance(2.0)
                .heat(HeatLevel.ASTRAL_INFERNO)
                .stirring(3, 4)
                .minPurity(0.80)
                .baseSuccessRate(0.30)
                .result((mass, purity) -> {
                    double bonus = 7.5 + (purity * 4.5);
                    ItemStack stack = BladeOilItem.create(ModItems.getBladeOil(), BaneType.DRACONID, (float) bonus, 50);
                    stack.getOrCreateTag().putBoolean("ArmorPiercingVoid", true);
                    return stack;
                })
                .build());
    }
}
