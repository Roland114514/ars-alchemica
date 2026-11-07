package com.medievalmagic.arsalchemica.recipe;

import com.medievalmagic.arsalchemica.api.cauldron.CauldronReactionState.HeatLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 * 炼金配方抽象定义模型 (Alchemical Recipe Model)。
 * <p>
 * 定义以太星图目标坐标、火候等级、水动力学搅拌圈数要求、基准纯度以及产物构建器。
 * 支持常规配方与失传古代传奇配方 (Ancient Recipes)。
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public final class AlchemicalRecipe {

    public enum RecipeRarity {
        COMMON("rarity.ars_alchemica.common", 0xFF55FF55, 1.0),
        UNCOMMON("rarity.ars_alchemica.uncommon", 0xFF55FFFF, 0.90),
        RARE("rarity.ars_alchemica.rare", 0xFFAA00AA, 0.75),
        LEGENDARY("rarity.ars_alchemica.legendary", 0xFFFFAA00, 0.30);

        private final String translationKey;
        private final int color;
        private final double defaultBaseSuccessRate;

        RecipeRarity(String key, int color, double defaultSuccess) {
            this.translationKey = key;
            this.color = color;
            this.defaultBaseSuccessRate = defaultSuccess;
        }

        public String getTranslationKey() {
            return translationKey;
        }

        public int getColor() {
            return color;
        }

        public double getDefaultBaseSuccessRate() {
            return defaultBaseSuccessRate;
        }
    }

    private final ResourceLocation recipeId;
    private final String translationKey;
    private final RecipeRarity rarity;
    private final double targetMapX;
    private final double targetMapY;
    private final double toleranceRadius;
    private final HeatLevel requiredHeat;
    private final int requiredClockwiseTurns;
    private final int requiredCounterClockwiseTurns;
    private final double minPurity;
    private final double baseSuccessRate;
    private final boolean ancient;
    private final BiFunction<Double, Double, ItemStack> resultFactory;

    private AlchemicalRecipe(Builder builder) {
        this.recipeId = Objects.requireNonNull(builder.recipeId, "Recipe ID cannot be null");
        this.translationKey = builder.translationKey != null ? builder.translationKey : "recipe." + recipeId.getNamespace() + "." + recipeId.getPath();
        this.rarity = builder.rarity != null ? builder.rarity : RecipeRarity.COMMON;
        this.targetMapX = builder.targetMapX;
        this.targetMapY = builder.targetMapY;
        this.toleranceRadius = Math.max(0.5, builder.toleranceRadius);
        this.requiredHeat = builder.requiredHeat != null ? builder.requiredHeat : HeatLevel.WARM_SIMMER;
        this.requiredClockwiseTurns = Math.max(0, builder.requiredClockwiseTurns);
        this.requiredCounterClockwiseTurns = Math.max(0, builder.requiredCounterClockwiseTurns);
        this.minPurity = Mth.clamp(builder.minPurity, 0.0, 1.0);
        this.baseSuccessRate = Mth.clamp(builder.baseSuccessRate, 0.05, 1.0);
        this.ancient = builder.ancient;
        this.resultFactory = Objects.requireNonNull(builder.resultFactory, "Result factory cannot be null");
    }

    /**
     * 判定当前坩埚状态是否满足该配方的激发条件。
     */
    public boolean matches(double currentX, double currentY, HeatLevel heat, int cwTurns, int ccwTurns, double purity) {
        if (purity < this.minPurity) {
            return false;
        }
        if (heat.ordinal() < this.requiredHeat.ordinal()) {
            return false;
        }
        if (cwTurns < this.requiredClockwiseTurns || ccwTurns < this.requiredCounterClockwiseTurns) {
            return false;
        }
        return distanceTo(currentX, currentY) <= this.toleranceRadius;
    }

    /**
     * 计算当前坐标到配方星图靶心的欧氏距离。
     */
    public double distanceTo(double x, double y) {
        return Math.hypot(x - this.targetMapX, y - this.targetMapY);
    }

    /**
     * 计算命中精度评分 (0.0 ~ 1.0)。
     */
    public double getPrecisionScore(double x, double y) {
        double dist = distanceTo(x, y);
        if (dist >= this.toleranceRadius) return 0.0;
        double ratio = dist / this.toleranceRadius;
        return 1.0 - (ratio * ratio);
    }

    /**
     * 产出目标灵药/剑油。
     */
    public ItemStack createResult(double mass, double purity) {
        return this.resultFactory.apply(mass, purity);
    }

    // --- Getters ---

    public ResourceLocation getRecipeId() { return recipeId; }
    public String getTranslationKey() { return translationKey; }
    public RecipeRarity getRarity() { return rarity; }
    public double getTargetMapX() { return targetMapX; }
    public double getTargetMapY() { return targetMapY; }
    public double getToleranceRadius() { return toleranceRadius; }
    public HeatLevel getRequiredHeat() { return requiredHeat; }
    public int getRequiredClockwiseTurns() { return requiredClockwiseTurns; }
    public int getRequiredCounterClockwiseTurns() { return requiredCounterClockwiseTurns; }
    public double getMinPurity() { return minPurity; }
    public double getBaseSuccessRate() { return baseSuccessRate; }
    public boolean isAncient() { return ancient; }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public static class Builder {
        private final ResourceLocation recipeId;
        private String translationKey;
        private RecipeRarity rarity = RecipeRarity.COMMON;
        private double targetMapX;
        private double targetMapY;
        private double toleranceRadius = 3.5;
        private HeatLevel requiredHeat = HeatLevel.WARM_SIMMER;
        private int requiredClockwiseTurns = 0;
        private int requiredCounterClockwiseTurns = 0;
        private double minPurity = 0.40;
        private double baseSuccessRate = 1.0;
        private boolean ancient = false;
        private BiFunction<Double, Double, ItemStack> resultFactory;

        public Builder(ResourceLocation id) {
            this.recipeId = id;
        }

        public Builder translationKey(String key) { this.translationKey = key; return this; }
        public Builder rarity(RecipeRarity rarity) {
            this.rarity = rarity;
            this.baseSuccessRate = rarity.getDefaultBaseSuccessRate();
            return this;
        }
        public Builder target(double x, double y) { this.targetMapX = x; this.targetMapY = y; return this; }
        public Builder tolerance(double radius) { this.toleranceRadius = radius; return this; }
        public Builder heat(HeatLevel heat) { this.requiredHeat = heat; return this; }
        public Builder stirring(int cw, int ccw) {
            this.requiredClockwiseTurns = cw;
            this.requiredCounterClockwiseTurns = ccw;
            return this;
        }
        public Builder minPurity(double purity) { this.minPurity = purity; return this; }
        public Builder baseSuccessRate(double rate) { this.baseSuccessRate = rate; return this; }
        public Builder ancient() {
            this.ancient = true;
            this.rarity = RecipeRarity.LEGENDARY;
            this.baseSuccessRate = 0.25;
            return this;
        }
        public Builder result(BiFunction<Double, Double, ItemStack> factory) {
            this.resultFactory = factory;
            return this;
        }

        public AlchemicalRecipe build() {
            return new AlchemicalRecipe(this);
        }
    }
}
