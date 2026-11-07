package com.medievalmagic.arsalchemica.village;

import com.medievalmagic.arsalchemica.ArsAlchemica;
import com.medievalmagic.arsalchemica.recipe.item.RecipeScrollItem;
import com.medievalmagic.arsalchemica.recipe.item.ScholiaFragmentItem;
import com.medievalmagic.arsalchemica.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.*;

/**
 * 药剂师村民阶级交易定义管理器 (Common-First)。
 * <p>
 * 为各加载器 (NeoForge / Fabric) 提供 1 至 5 阶的药剂、草药、器具与【配方卷轴】交易池。
 * 允许玩家在游戏前期通过绿宝石在村庄中向药剂师购置简单实用的高品质药剂配方。
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public final class ApothecaryTradeManager {

    public record ApothecaryTradeEntry(
            ItemStack costA,
            ItemStack costB,
            ItemStack result,
            int maxUses,
            int villagerXp,
            float priceMultiplier
    ) {
        public MerchantOffer toMerchantOffer() {
            return new MerchantOffer(costA, costB, result, maxUses, villagerXp, priceMultiplier);
        }
    }

    private static final Map<Integer, List<ApothecaryTradeEntry>> TRADES = new HashMap<>();

    static {
        initializeDefaultTrades();
    }

    private ApothecaryTradeManager() {}

    public static List<ApothecaryTradeEntry> getTradesForTier(int tier) {
        return TRADES.getOrDefault(tier, Collections.emptyList());
    }

    public static void registerTrade(int tier, ApothecaryTradeEntry entry) {
        Objects.requireNonNull(entry, "Trade entry cannot be null");
        TRADES.computeIfAbsent(tier, k -> new ArrayList<>()).add(entry);
    }

    /**
     * 初始化药剂师村民的 5 阶交易表。
     */
    public static void initializeDefaultTrades() {
        TRADES.clear();

        // --- Tier 1: 新手 (Novice) ---
        // 1. 收购草药原料 -> 1 绿宝石
        registerTrade(1, new ApothecaryTradeEntry(
                new ItemStack(Items.WHEAT, 16),
                ItemStack.EMPTY,
                new ItemStack(Items.EMERALD, 1),
                16, 2, 0.05f
        ));
        // 2. 1 绿宝石 -> 4 个空玻璃瓶
        registerTrade(1, new ApothecaryTradeEntry(
                new ItemStack(Items.EMERALD, 1),
                ItemStack.EMPTY,
                new ItemStack(Items.GLASS_BOTTLE, 4),
                12, 1, 0.05f
        ));
        // 3. 3 绿宝石 -> 《生机灵药配方卷轴》 (Draught of Vitality)
        registerTrade(1, new ApothecaryTradeEntry(
                new ItemStack(Items.EMERALD, 3),
                ItemStack.EMPTY,
                RecipeScrollItem.create(ModItems.getRecipeScroll(), ArsAlchemica.id("draught_of_vitality")),
                1, 4, 0.2f
        ));

        // --- Tier 2: 学徒 (Apprentice) ---
        // 1. 2 绿宝石 -> 1 硬木炼金搅拌棒
        registerTrade(2, new ApothecaryTradeEntry(
                new ItemStack(Items.EMERALD, 2),
                ItemStack.EMPTY,
                new ItemStack(ModItems.getStirringRod(), 1),
                6, 5, 0.1f
        ));
        // 2. 5 绿宝石 -> 《猫眼夜视灵药配方卷轴》 (Cat's Eyes Elixir)
        registerTrade(2, new ApothecaryTradeEntry(
                new ItemStack(Items.EMERALD, 5),
                ItemStack.EMPTY,
                RecipeScrollItem.create(ModItems.getRecipeScroll(), ArsAlchemica.id("cat_eyes_elixir")),
                1, 8, 0.2f
        ));

        // --- Tier 3: 老手 (Journeyman) ---
        // 1. 8 绿宝石 -> 《迅疾灵药配方卷轴》 (Swift Step Elixir)
        registerTrade(3, new ApothecaryTradeEntry(
                new ItemStack(Items.EMERALD, 8),
                ItemStack.EMPTY,
                RecipeScrollItem.create(ModItems.getRecipeScroll(), ArsAlchemica.id("swift_step_elixir")),
                1, 15, 0.2f
        ));
        // 2. 8 绿宝石 -> 《烈火御守灵药配方卷轴》 (Ignis Ward Elixir)
        registerTrade(3, new ApothecaryTradeEntry(
                new ItemStack(Items.EMERALD, 8),
                ItemStack.EMPTY,
                RecipeScrollItem.create(ModItems.getRecipeScroll(), ArsAlchemica.id("ignis_ward_elixir")),
                1, 15, 0.2f
        ));

        // --- Tier 4: 专家 (Expert) ---
        // 1. 14 绿宝石 -> 《尸鬼特攻剑油配方卷轴》 (Necrophage Oil)
        registerTrade(4, new ApothecaryTradeEntry(
                new ItemStack(Items.EMERALD, 14),
                ItemStack.EMPTY,
                RecipeScrollItem.create(ModItems.getRecipeScroll(), ArsAlchemica.id("necro_bane_oil")),
                1, 20, 0.25f
        ));

        // --- Tier 5: 大师 (Master) ---
        // 1. 28 绿宝石 -> 1 张【古代学术注脚残页】(Scholia Fragment - 凤凰涅槃)
        registerTrade(5, new ApothecaryTradeEntry(
                new ItemStack(Items.EMERALD, 28),
                ItemStack.EMPTY,
                ScholiaFragmentItem.create(ModItems.getScholiaFragment(), ArsAlchemica.id("phoenix_rebirth_elixir")),
                1, 30, 0.3f
        ));
    }
}
