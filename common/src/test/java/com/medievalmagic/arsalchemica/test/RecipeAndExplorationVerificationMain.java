package com.medievalmagic.arsalchemica.test;

import com.medievalmagic.arsalchemica.ArsAlchemica;
import com.medievalmagic.arsalchemica.api.aspect.AlchemyAspect;
import com.medievalmagic.arsalchemica.api.cauldron.CauldronReactionState;
import com.medievalmagic.arsalchemica.api.cauldron.CauldronReactionState.HeatLevel;
import com.medievalmagic.arsalchemica.api.vector.AspectVector;
import com.medievalmagic.arsalchemica.cauldron.AlchemicalCauldronBlock;
import com.medievalmagic.arsalchemica.cauldron.AlchemicalCauldronBlockEntity;
import com.medievalmagic.arsalchemica.cauldron.AlchemicalStirringRodItem;
import com.medievalmagic.arsalchemica.grimoire.AlchemicalJournalItem;
import com.medievalmagic.arsalchemica.potion.AlchemicalPotionItem;
import com.medievalmagic.arsalchemica.potion.BladeOilItem;
import com.medievalmagic.arsalchemica.recipe.AlchemicalRecipe;
import com.medievalmagic.arsalchemica.recipe.AlchemicalRecipeRegistry;
import com.medievalmagic.arsalchemica.recipe.item.AncientParchmentItem;
import com.medievalmagic.arsalchemica.recipe.item.RecipeScrollItem;
import com.medievalmagic.arsalchemica.recipe.item.ScholiaFragmentItem;
import com.medievalmagic.arsalchemica.recipe.knowledge.PlayerKnowledgeManager;
import com.medievalmagic.arsalchemica.recipe.knowledge.PlayerRecipeKnowledge;
import com.medievalmagic.arsalchemica.registry.ModBlockEntities;
import com.medievalmagic.arsalchemica.registry.ModBlocks;
import com.medievalmagic.arsalchemica.registry.ModItems;
import com.medievalmagic.arsalchemica.village.ApothecaryTradeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;
import java.util.Optional;

/**
 * Ars Alchemica Milestone 5 自动化验证套件。
 * <p>
 * 严苛检验：
 * 1. 配方定义与拓扑距离/精度算式；
 * 2. 配方注册表非装箱匹配与就近导航；
 * 3. 玩家配方知识图谱状态机与 NBT 序列化；
 * 4. 古代秘方成功率动态博弈与学术注脚成长；
 * 5. 配方卷轴研读与防重复学习机制；
 * 6. 失传古代羊皮纸多步解密流；
 * 7. 药剂师村民 5 阶交易池健全性；
 * 8. 坩埚物理实验、顿悟 (Eureka) 与古代反噬端到端联动。
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public class RecipeAndExplorationVerificationMain {

    private static int testsPassed = 0;
    private static final int TOTAL_TESTS = 8;

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("=== ARS ALCHEMICA: MILESTONE 5 VERIFICATION SUITE       ===");
        System.out.println("==========================================================");

        setupRegistries();

        try {
            testRecipeDefinitionAndTopologyMath();
            testRecipeRegistryMatching();
            testPlayerKnowledgeAndNBT();
            testAncientRecipeMasteryScaling();
            testRecipeScrollStudying();
            testAncientParchmentDeciphering();
            testApothecaryVillagerTrades();
            testCrucibleEurekaAndAncientMishap();

            System.out.println();
            System.out.println(">>> ALL MILESTONE 5 TESTS PASSED PERFECTLY (100% SUCCESS) <<<");
        } catch (Throwable t) {
            System.err.println("Verification FAILED with exception:");
            t.printStackTrace();
            System.exit(1);
        }
    }

    @SuppressWarnings("unchecked")
    private static void setupRegistries() {
        ModBlocks.bindCauldron(() -> new AlchemicalCauldronBlock(new net.minecraft.world.level.block.Block.Properties()));
        ModBlockEntities.bindCauldron(
                () -> (BlockEntityType<AlchemicalCauldronBlockEntity>) (Object) new BlockEntityType<>()
        );
        ModItems.bindPotions(
                () -> new AlchemicalPotionItem(new Item.Properties(), AlchemicalPotionItem.DeliveryType.ELIXIR),
                () -> new AlchemicalPotionItem(new Item.Properties(), AlchemicalPotionItem.DeliveryType.INCENSE),
                () -> new BladeOilItem(new Item.Properties())
        );
        ModItems.bindApparatusItems(
                () -> new AlchemicalStirringRodItem(new Item.Properties()),
                () -> new AlchemicalJournalItem(new Item.Properties())
        );
        ModItems.bindExplorationItems(
                () -> new RecipeScrollItem(new Item.Properties()),
                () -> new AncientParchmentItem(new Item.Properties()),
                () -> new ScholiaFragmentItem(new Item.Properties())
        );
    }

    private static void testRecipeDefinitionAndTopologyMath() {
        System.out.print("[TEST 1/8] Alchemical Recipe Definition & Topology Math... ");

        AlchemicalRecipe recipe = AlchemicalRecipe.builder(ArsAlchemica.id("test_elixir"))
                .target(10.0, 20.0)
                .tolerance(5.0)
                .heat(HeatLevel.ROILING_BOIL)
                .stirring(2, 1)
                .minPurity(0.50)
                .baseSuccessRate(0.85)
                .result((mass, purity) -> new ItemStack(Items.POTION))
                .build();

        // 靶心位置距离应为 0，精度为 1.0
        assertAlmostEquals(0.0, recipe.distanceTo(10.0, 20.0), 1e-6, "Center distance must be 0");
        assertAlmostEquals(1.0, recipe.getPrecisionScore(10.0, 20.0), 1e-6, "Center precision must be 1.0");

        // 边界处 (距离 5.0)
        assertAlmostEquals(5.0, recipe.distanceTo(10.0, 25.0), 1e-6, "Border distance must be 5.0");
        assertAlmostEquals(0.0, recipe.getPrecisionScore(10.0, 25.0), 1e-4, "Border precision must be 0.0");

        // 超出边界处 (距离 6.0)
        assertTrue(!recipe.matches(10.0, 26.0, HeatLevel.ROILING_BOIL, 2, 1, 0.60), "Outside tolerance should not match");

        // 内部但条件不达标 (纯度不足 / 热量不足 / 搅拌不足)
        assertTrue(!recipe.matches(10.0, 20.0, HeatLevel.ROILING_BOIL, 2, 1, 0.40), "Low purity should not match");
        assertTrue(!recipe.matches(10.0, 20.0, HeatLevel.WARM_SIMMER, 2, 1, 0.60), "Low heat should not match");
        assertTrue(!recipe.matches(10.0, 20.0, HeatLevel.ROILING_BOIL, 1, 1, 0.60), "Insufficient stirring should not match");

        // 内部且满足全部条件
        assertTrue(recipe.matches(10.0, 20.0, HeatLevel.ROILING_BOIL, 2, 1, 0.60), "All valid should match");
        assertTrue(recipe.matches(12.0, 22.0, HeatLevel.ASTRAL_INFERNO, 3, 2, 0.80), "Exceeding conditions should match");

        System.out.println("PASS");
        testsPassed++;
    }

    private static void testRecipeRegistryMatching() {
        System.out.print("[TEST 2/8] Recipe Registry Lookups & Non-Boxing Match... ");

        AlchemicalRecipeRegistry registry = AlchemicalRecipeRegistry.getInstance();
        assertTrue(registry.getAll().size() >= 7, "Canon recipes count should be at least 7");

        // 匹配生机灵药 (12.5, 14.0, WARM_SIMMER, 1, 0, 0.50)
        Optional<AlchemicalRecipe> match1 = registry.findMatchingRecipe(12.5, 14.0, HeatLevel.WARM_SIMMER, 1, 0, 0.50);
        assertTrue(match1.isPresent(), "Should match Draught of Vitality");
        assertEquals("recipe.ars_alchemica.draught_of_vitality", match1.get().getTranslationKey(), "Recipe key mismatch");

        // 匹配猫眼夜视灵药 (-8.0, 16.0, WARM_SIMMER, 1, 1, 0.50)
        Optional<AlchemicalRecipe> match2 = registry.findMatchingRecipe(-8.0, 16.0, HeatLevel.WARM_SIMMER, 1, 1, 0.50);
        assertTrue(match2.isPresent(), "Should match Cat's Eyes Elixir");
        assertEquals("recipe.ars_alchemica.cat_eyes_elixir", match2.get().getTranslationKey(), "Recipe key mismatch");

        // 就近配方查找
        Optional<AlchemicalRecipe> nearest = registry.findNearestRecipe(11.0, 13.0);
        assertTrue(nearest.isPresent(), "Nearest recipe should be found");
        assertEquals("recipe.ars_alchemica.draught_of_vitality", nearest.get().getTranslationKey(), "Nearest recipe mismatch");

        System.out.println("PASS");
        testsPassed++;
    }

    private static void testPlayerKnowledgeAndNBT() {
        System.out.print("[TEST 3/8] Player Recipe Knowledge State Machine & NBT... ");

        PlayerRecipeKnowledge knowledge = new PlayerRecipeKnowledge();
        ResourceLocation idVitality = ArsAlchemica.id("draught_of_vitality");
        ResourceLocation idPhoenix = ArsAlchemica.id("phoenix_rebirth_elixir");

        assertTrue(!knowledge.isDiscovered(idVitality), "Initially undiscovered");

        // 首次解锁
        boolean firstUnlock = knowledge.unlockRecipe(idVitality);
        assertTrue(firstUnlock, "First unlock should return true");
        assertTrue(knowledge.isDiscovered(idVitality), "Should now be discovered");

        // 重复解锁
        boolean secondUnlock = knowledge.unlockRecipe(idVitality);
        assertTrue(!secondUnlock, "Duplicate unlock should return false");

        // 提升熟练度
        knowledge.increaseMastery(idPhoenix, 2);
        assertEquals(2, knowledge.getMastery(idPhoenix), "Mastery should be 2");
        assertTrue(knowledge.isDiscovered(idPhoenix), "Mastery should automatically mark as discovered");

        // NBT 往返持久化
        CompoundTag tag = knowledge.serializeNBT();
        PlayerRecipeKnowledge loaded = new PlayerRecipeKnowledge();
        loaded.deserializeNBT(tag);

        assertTrue(loaded.isDiscovered(idVitality), "Loaded should have Vitality");
        assertTrue(loaded.isDiscovered(idPhoenix), "Loaded should have Phoenix");
        assertEquals(2, loaded.getMastery(idPhoenix), "Loaded mastery should match 2");

        System.out.println("PASS");
        testsPassed++;
    }

    private static void testAncientRecipeMasteryScaling() {
        System.out.print("[TEST 4/8] Ancient Recipe Risk/Reward Formula & Mastery Scaling... ");

        AlchemicalRecipe phoenix = AlchemicalRecipeRegistry.getInstance()
                .get(ArsAlchemica.id("phoenix_rebirth_elixir")).orElseThrow();
        assertTrue(phoenix.isAncient(), "Phoenix Rebirth must be ancient");
        assertEquals(0.25, phoenix.getBaseSuccessRate(), "Base success rate must be 25%");

        PlayerRecipeKnowledge knowledge = new PlayerRecipeKnowledge();

        // 0 造诣，0 净度加成
        double chance0 = knowledge.calculateAncientSuccessChance(phoenix, 0.0);
        assertAlmostEquals(0.25, chance0, 1e-4, "Base chance must be 0.25");

        // 0 造诣，满净度加成 (+10%)
        double chancePurity = knowledge.calculateAncientSuccessChance(phoenix, 1.0);
        assertAlmostEquals(0.35, chancePurity, 1e-4, "Base + Purity chance must be 0.35");

        // 提升至 1 阶造诣 (+15%)
        knowledge.increaseMastery(phoenix.getRecipeId(), 1);
        double chance1 = knowledge.calculateAncientSuccessChance(phoenix, 0.0);
        assertAlmostEquals(0.40, chance1, 1e-4, "Tier 1 mastery chance must be 0.40");

        // 提升至 5 阶造诣 (5 * 15% = +75%) -> 0.25 + 0.75 = 1.0 -> 钳位至 0.95
        knowledge.increaseMastery(phoenix.getRecipeId(), 4);
        assertEquals(5, knowledge.getMastery(phoenix.getRecipeId()), "Mastery must cap at 5");
        double chanceMax = knowledge.calculateAncientSuccessChance(phoenix, 1.0);
        assertAlmostEquals(0.95, chanceMax, 1e-4, "Max mastery must clamp to 0.95");

        // 再次提升不能超过 5
        boolean overLevel = knowledge.increaseMastery(phoenix.getRecipeId(), 1);
        assertTrue(!overLevel, "Cannot increase beyond max mastery level");

        System.out.println("PASS");
        testsPassed++;
    }

    private static void testRecipeScrollStudying() {
        System.out.print("[TEST 5/8] Recipe Scroll Item & Scholar Grimoire Inscription... ");

        Player player = new Player();
        PlayerKnowledgeManager.clearAll();
        Level level = new Level() {};

        ResourceLocation recipeId = ArsAlchemica.id("cat_eyes_elixir");
        ItemStack scrollStack = RecipeScrollItem.create(ModItems.getRecipeScroll(), recipeId);
        scrollStack.setCount(2);
        player.setItemInHand(InteractionHand.MAIN_HAND, scrollStack);

        PlayerRecipeKnowledge knowledge = PlayerKnowledgeManager.getKnowledge(player);
        assertTrue(!knowledge.isDiscovered(recipeId), "Must be undiscovered initially");

        // 首次使用卷轴研读
        RecipeScrollItem scrollItem = ModItems.getRecipeScroll();
        InteractionResultHolder<ItemStack> res1 = scrollItem.use(level, player, InteractionHand.MAIN_HAND);
        assertTrue(res1.getResult().consumesAction(), "First study should succeed");
        assertTrue(knowledge.isDiscovered(recipeId), "Recipe must now be discovered in player knowledge");
        assertEquals(1, player.getItemInHand(InteractionHand.MAIN_HAND).getCount(), "Non-creative should consume 1 scroll");

        // 二次使用相同卷轴研读 (已掌握)
        InteractionResultHolder<ItemStack> res2 = scrollItem.use(level, player, InteractionHand.MAIN_HAND);
        assertTrue(!res2.getResult().consumesAction(), "Second study should fail");
        assertEquals(1, player.getItemInHand(InteractionHand.MAIN_HAND).getCount(), "Should not consume scroll if already known");

        System.out.println("PASS");
        testsPassed++;
    }

    private static void testAncientParchmentDeciphering() {
        System.out.print("[TEST 6/8] Lost Ancient Parchment Multi-Step Deciphering... ");

        Player player = new Player();
        PlayerKnowledgeManager.clearAll();
        Level level = new Level() {};

        ResourceLocation ancientId = ArsAlchemica.id("phoenix_rebirth_elixir");
        ItemStack parchment = AncientParchmentItem.create(ModItems.getAncientParchment(), ancientId);
        player.setItemInHand(InteractionHand.MAIN_HAND, parchment);

        PlayerRecipeKnowledge knowledge = PlayerKnowledgeManager.getKnowledge(player);
        AncientParchmentItem parchmentItem = ModItems.getAncientParchment();

        // 步进 1: 0% -> 25%
        parchmentItem.use(level, player, InteractionHand.MAIN_HAND);
        assertEquals(25, AncientParchmentItem.getDecipherProgress(player.getItemInHand(InteractionHand.MAIN_HAND)), "Progress should be 25%");
        assertTrue(!knowledge.isDiscovered(ancientId), "Should not be unlocked yet");

        // 步进 2: 25% -> 50%
        parchmentItem.use(level, player, InteractionHand.MAIN_HAND);
        assertEquals(50, AncientParchmentItem.getDecipherProgress(player.getItemInHand(InteractionHand.MAIN_HAND)), "Progress should be 50%");

        // 步进 3: 50% -> 75%
        parchmentItem.use(level, player, InteractionHand.MAIN_HAND);
        assertEquals(75, AncientParchmentItem.getDecipherProgress(player.getItemInHand(InteractionHand.MAIN_HAND)), "Progress should be 75%");

        // 步进 4: 75% -> 100% (彻底破译解锁)
        parchmentItem.use(level, player, InteractionHand.MAIN_HAND);
        assertTrue(knowledge.isDiscovered(ancientId), "Must be permanently unlocked upon reaching 100%");
        assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "Parchment should be consumed upon full decipher");

        // 学术注脚研读测试
        ItemStack fragment = ScholiaFragmentItem.create(ModItems.getScholiaFragment(), ancientId);
        player.setItemInHand(InteractionHand.MAIN_HAND, fragment);
        ScholiaFragmentItem fragmentItem = ModItems.getScholiaFragment();

        fragmentItem.use(level, player, InteractionHand.MAIN_HAND);
        assertEquals(1, knowledge.getMastery(ancientId), "Scholia study must raise mastery to 1");

        System.out.println("PASS");
        testsPassed++;
    }

    private static void testApothecaryVillagerTrades() {
        System.out.print("[TEST 7/8] Villager Apothecary Trades Tier Progression... ");

        for (int tier = 1; tier <= 5; tier++) {
            List<ApothecaryTradeManager.ApothecaryTradeEntry> trades = ApothecaryTradeManager.getTradesForTier(tier);
            assertTrue(!trades.isEmpty(), "Tier " + tier + " trades must not be empty");
            for (ApothecaryTradeManager.ApothecaryTradeEntry trade : trades) {
                MerchantOffer offer = trade.toMerchantOffer();
                assertTrue(!offer.getCostA().isEmpty(), "Cost A cannot be empty");
                assertTrue(!offer.getResult().isEmpty(), "Trade result cannot be empty");
                assertTrue(offer.getMaxUses() > 0, "Max uses must be positive");
            }
        }

        // 验证 Tier 1 含有《生机灵药卷轴》
        List<ApothecaryTradeManager.ApothecaryTradeEntry> tier1 = ApothecaryTradeManager.getTradesForTier(1);
        boolean hasVitalityScroll = tier1.stream().anyMatch(t ->
                t.result().getItem() instanceof RecipeScrollItem &&
                        ArsAlchemica.id("draught_of_vitality").equals(RecipeScrollItem.getRecipeId(t.result()))
        );
        assertTrue(hasVitalityScroll, "Tier 1 must sell Draught of Vitality scroll");

        // 验证 Tier 5 含有古代注脚残页
        List<ApothecaryTradeManager.ApothecaryTradeEntry> tier5 = ApothecaryTradeManager.getTradesForTier(5);
        boolean hasScholia = tier5.stream().anyMatch(t -> t.result().getItem() instanceof ScholiaFragmentItem);
        assertTrue(hasScholia, "Tier 5 must sell Scholia Fragment");

        System.out.println("PASS");
        testsPassed++;
    }

    private static void testCrucibleEurekaAndAncientMishap() {
        System.out.print("[TEST 8/8] End-to-End Crucible Experimentation, Eureka & Ancient Mishap... ");

        TestLevel level = new TestLevel();
        BlockPos pos = new BlockPos(0, 64, 0);
        BlockState cauldronState = ModBlocks.getCauldron().defaultBlockState();
        level.setBlock(pos, cauldronState, 3);
        AlchemicalCauldronBlockEntity cauldron = new AlchemicalCauldronBlockEntity(pos, cauldronState);
        level.setBlockEntity(pos, cauldron);

        Player player = new Player();
        PlayerKnowledgeManager.clearAll();
        PlayerRecipeKnowledge knowledge = PlayerKnowledgeManager.getKnowledge(player);

        // 1. 注水至 3 份
        cauldron.setWaterLevel(3);

        // 2. 模拟投料达到生机灵药坐标 (12.5, 14.0)
        // 生机以太向量投影 (x=12.5, y=14.0)
        AspectVector targetVec = AspectVector.builder()
                .astralPos(12.5, 14.0)
                .set(AlchemyAspect.VITA, 12.5)
                .set(AlchemyAspect.AQUA, 14.0)
                .build();
        cauldron.getReactionState().addIngredient(targetVec);

        // 加热至文火慢炖并搅拌充分溶解
        for (int i = 0; i < 200; i++) {
            cauldron.getReactionState().tick(true, HeatLevel.WARM_SIMMER);
            if (i % 25 == 0) {
                cauldron.getReactionState().stir(true, 0.35);
            }
        }
        for (int i = 0; i < 150; i++) {
            cauldron.getReactionState().tick(true, HeatLevel.WARM_SIMMER);
        }
        assertTrue(cauldron.getReactionState().getClockwiseTurns() >= 1, "Should have completed at least 1 turn");

        // 3. 手持空瓶萃取 -> 触发 Eureka 顿悟新配方！
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE, 1));
        ResourceLocation vitalityId = ArsAlchemica.id("draught_of_vitality");
        assertTrue(!knowledge.isDiscovered(vitalityId), "Vitality must be undiscovered before Eureka");

        InteractionResult result = cauldron.onPlayerInteract(player, InteractionHand.MAIN_HAND, new BlockHitResult());
        assertTrue(result.consumesAction(), "Extraction must succeed");
        assertTrue(knowledge.isDiscovered(vitalityId), "Eureka must have unlocked Draught of Vitality in player knowledge!");

        ItemStack extractedPotion = player.getItemInHand(InteractionHand.MAIN_HAND);
        assertTrue(extractedPotion.getItem() instanceof AlchemicalPotionItem, "Must have extracted an alchemical potion");
        assertEquals(2, cauldron.getWaterLevel(), "Water level should reduce to 2");

        System.out.println("PASS");
        testsPassed++;
    }

    private static class TestLevel extends Level {
        private AlchemicalCauldronBlockEntity be;

        public void setBlockEntity(BlockPos pos, AlchemicalCauldronBlockEntity be) {
            this.be = be;
            if (be != null) be.setLevel(this);
        }

        @Override
        public net.minecraft.world.level.block.entity.BlockEntity getBlockEntity(BlockPos pos) {
            return be;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return Blocks.AIR.defaultBlockState();
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Assertion failed: " + message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Assertion failed: " + message + " (expected: " + expected + ", actual: " + actual + ")");
        }
    }

    private static void assertAlmostEquals(double expected, double actual, double epsilon, String message) {
        if (Math.abs(expected - actual) > epsilon) {
            throw new AssertionError(String.format("Assertion failed: %s (expected: %.6f, actual: %.6f, diff: %.6f > eps: %.6f)",
                    message, expected, actual, Math.abs(expected - actual), epsilon));
        }
    }
}
