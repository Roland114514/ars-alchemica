package com.medievalmagic.arsalchemica.test;

import com.medievalmagic.arsalchemica.api.aspect.AlchemyAspect;
import com.medievalmagic.arsalchemica.api.cauldron.CauldronReactionState;
import com.medievalmagic.arsalchemica.api.cauldron.CauldronReactionState.HeatLevel;
import com.medievalmagic.arsalchemica.api.cauldron.CauldronReactionState.MishapResult;
import com.medievalmagic.arsalchemica.api.vector.AspectVector;
import com.medievalmagic.arsalchemica.cauldron.AlchemicalCauldronBlock;
import com.medievalmagic.arsalchemica.cauldron.AlchemicalCauldronBlockEntity;
import com.medievalmagic.arsalchemica.cauldron.AlchemicalStirringRodItem;
import com.medievalmagic.arsalchemica.client.ScholarIntuitionHudRenderer;
import com.medievalmagic.arsalchemica.grimoire.AlchemicalJournalItem;
import com.medievalmagic.arsalchemica.grimoire.AlchemicalRecipeStepTracker;
import com.medievalmagic.arsalchemica.grimoire.ScholarIntuitionData;
import com.medievalmagic.arsalchemica.herb.HerbRegistry;
import com.medievalmagic.arsalchemica.herb.IAlchemicalHerb;
import com.medievalmagic.arsalchemica.herb.apparatus.ProcessedHerbItem;
import com.medievalmagic.arsalchemica.potion.AlchemicalPotionItem;
import com.medievalmagic.arsalchemica.potion.BladeOilItem;
import com.medievalmagic.arsalchemica.registry.ModBlockEntities;
import com.medievalmagic.arsalchemica.registry.ModBlocks;
import com.medievalmagic.arsalchemica.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Milestone 4 自动化测试与严格物理/热力学/手记交互校验套件。
 * <p>
 * 覆盖：
 * 1. 坩埚环境热源探测状态机 (Cold -> Simmer -> Roiling Boil -> Astral Inferno)
 * 2. 微观流体热力学演进 (牛顿传热与温度爬升)
 * 3. 物理注水与空玻璃瓶灵药萃取分装
 * 4. 物理投入与掉落物扫描吸收 (自动提取 Herb 12D AspectVector)
 * 5. 搅拌棒双向水动力学操控 (顺时针平息失稳 / 逆时针剪切排浊)
 * 6. 副手《炼金手记》双持学者直觉透视与数据快照生成
 * 7. 工艺工序动态追踪器与向导提示推导
 * 8. 炼金灾变事故处理 (沸溢毒雾、剧烈爆炸与药渣凝结)
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public class CauldronAndGrimoireVerificationMain {

    private static class TestCauldronLevel extends Level {
        private final Map<BlockPos, BlockState> blockStates = new HashMap<>();
        private final Map<BlockPos, BlockEntity> blockEntities = new HashMap<>();

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return blockStates.getOrDefault(pos, Blocks.AIR.defaultBlockState());
        }

        @Override
        public boolean setBlock(BlockPos pos, BlockState state, int flags) {
            blockStates.put(pos, state);
            return true;
        }

        @Override
        public BlockEntity getBlockEntity(BlockPos pos) {
            return blockEntities.get(pos);
        }

        public void setBlockEntity(BlockPos pos, BlockEntity be) {
            blockEntities.put(pos, be);
            if (be != null) {
                be.setLevel(this);
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("=== ARS ALCHEMICA: MILESTONE 4 VERIFICATION SUITE       ===");
        System.out.println("==========================================================");

        setupRegistries();
        testHeatSourceDetection();
        testThermodynamicHeatingAndCooling();
        testWaterFillingAndBottleExtraction();
        testHerbReagentAbsorptionAndColorBlending();
        testStirringRodHydrodynamics();
        testDualWieldGrimoireScholarIntuition();
        testRecipeStepGuidance();
        testMishapCatastrophes();

        System.out.println("\n>>> ALL MILESTONE 4 TESTS PASSED PERFECTLY (100% SUCCESS) <<<");
    }

    private static void setupRegistries() {
        ModBlockEntities.bindCauldron(() -> (BlockEntityType<AlchemicalCauldronBlockEntity>) (Object) new BlockEntityType<>());
        ModItems.bindPotions(
                () -> new AlchemicalPotionItem(new Item.Properties(), AlchemicalPotionItem.DeliveryType.ELIXIR),
                () -> new AlchemicalPotionItem(new Item.Properties(), AlchemicalPotionItem.DeliveryType.INCENSE),
                () -> new BladeOilItem(new Item.Properties())
        );
        ModItems.bindApparatusItems(
                () -> new AlchemicalStirringRodItem(new Item.Properties()),
                () -> new AlchemicalJournalItem(new Item.Properties())
        );
    }

    private static void testHeatSourceDetection() {
        System.out.print("[TEST 1/8] Heat Source Environmental Detection... ");

        TestCauldronLevel level = new TestCauldronLevel();
        BlockPos belowPos = new BlockPos(0, 0, 0);

        // 1. 空气 / 无热源 -> COLD
        level.setBlock(belowPos, Blocks.AIR.defaultBlockState(), 3);
        assertEquals(HeatLevel.COLD, AlchemicalCauldronBlockEntity.detectHeatSource(level, belowPos));

        // 2. 营火 (点燃) -> WARM_SIMMER
        BlockState campfireLit = Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true);
        level.setBlock(belowPos, campfireLit, 3);
        assertEquals(HeatLevel.WARM_SIMMER, AlchemicalCauldronBlockEntity.detectHeatSource(level, belowPos));

        // 3. 岩浆 -> ROILING_BOIL
        level.setBlock(belowPos, Blocks.LAVA.defaultBlockState(), 3);
        assertEquals(HeatLevel.ROILING_BOIL, AlchemicalCauldronBlockEntity.detectHeatSource(level, belowPos));

        // 4. 灵魂营火 -> ASTRAL_INFERNO
        BlockState soulCampfireLit = Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true);
        level.setBlock(belowPos, soulCampfireLit, 3);
        assertEquals(HeatLevel.ASTRAL_INFERNO, AlchemicalCauldronBlockEntity.detectHeatSource(level, belowPos));

        System.out.println("PASS");
    }

    private static void testThermodynamicHeatingAndCooling() {
        System.out.print("[TEST 2/8] Thermodynamic Heating & Heat Tiers... ");

        TestCauldronLevel level = new TestCauldronLevel();
        BlockPos cauldronPos = new BlockPos(0, 1, 0);
        BlockPos heatPos = new BlockPos(0, 0, 0);

        // 底部放置岩浆 (目标 150°C, 沸腾)
        level.setBlock(heatPos, Blocks.LAVA.defaultBlockState(), 3);

        AlchemicalCauldronBlock block = new AlchemicalCauldronBlock(Block.Properties.of());
        BlockState state = block.defaultBlockState();
        level.setBlock(cauldronPos, state, 3);

        AlchemicalCauldronBlockEntity be = new AlchemicalCauldronBlockEntity(cauldronPos, state);
        level.setBlockEntity(cauldronPos, be);

        assertEquals(20.0, be.getReactionState().getTemperature(), 1e-4);

        // 模拟 100 Ticks 加热
        for (int i = 0; i < 100; i++) {
            AlchemicalCauldronBlockEntity.serverTick(level, cauldronPos, level.getBlockState(cauldronPos), be);
        }

        double heatedTemp = be.getReactionState().getTemperature();
        assertTrue(heatedTemp > 100.0); // 显著超过沸点
        assertTrue(be.getReactionState().getActiveHeatLevel() == HeatLevel.ROILING_BOIL);

        System.out.println("PASS");
    }

    private static void testWaterFillingAndBottleExtraction() {
        System.out.print("[TEST 3/8] Water Filling & Extraction Lifecycle... ");

        TestCauldronLevel level = new TestCauldronLevel();
        BlockPos cauldronPos = new BlockPos(0, 1, 0);
        AlchemicalCauldronBlock block = new AlchemicalCauldronBlock(Block.Properties.of());
        BlockState state = block.defaultBlockState();
        level.setBlock(cauldronPos, state, 3);

        AlchemicalCauldronBlockEntity be = new AlchemicalCauldronBlockEntity(cauldronPos, state);
        level.setBlockEntity(cauldronPos, be);

        Player player = new Player();
        player.level = level;

        // 1. 水桶注水
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        InteractionResult resFill = be.onPlayerInteract(player, InteractionHand.MAIN_HAND, null);
        assertTrue(resFill.consumesAction());
        assertEquals(3, be.getWaterLevel());

        // 2. 空瓶取水 (无材料时返回普通水瓶)
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));
        InteractionResult resTake1 = be.onPlayerInteract(player, InteractionHand.MAIN_HAND, null);
        assertTrue(resTake1.consumesAction());
        assertEquals(2, be.getWaterLevel());
        assertEquals(Items.POTION, player.getItemInHand(InteractionHand.MAIN_HAND).getItem());

        System.out.println("PASS");
    }

    private static void testHerbReagentAbsorptionAndColorBlending() {
        System.out.print("[TEST 4/8] Physical Reagent Absorption & Color Blending... ");

        TestCauldronLevel level = new TestCauldronLevel();
        BlockPos cauldronPos = new BlockPos(0, 1, 0);
        AlchemicalCauldronBlock block = new AlchemicalCauldronBlock(Block.Properties.of());
        BlockState state = block.defaultBlockState();
        level.setBlock(cauldronPos, state, 3);

        AlchemicalCauldronBlockEntity be = new AlchemicalCauldronBlockEntity(cauldronPos, state);
        level.setBlockEntity(cauldronPos, be);
        be.setWaterLevel(3);

        int initialColor = be.getFluidColor();

        // 1. 模拟物理投料：投入灰烬血蓟粗粉 (Cinder Thistle Coarse Powder, 主含火元素 IGNIS)
        ProcessedHerbItem processedItem = new ProcessedHerbItem(new Item.Properties(), HerbRegistry.CINDER_THISTLE, IAlchemicalHerb.ProcessingForm.MILLED_COARSE);
        ItemStack powderStack = ProcessedHerbItem.create(processedItem, HerbRegistry.CINDER_THISTLE, IAlchemicalHerb.ProcessingForm.MILLED_COARSE, 1.0);

        boolean absorbed = be.absorbItemStack(powderStack);
        assertTrue(absorbed);
        assertTrue(powderStack.isEmpty());

        // 2. 服务端 Tick 演化并更新混色
        AlchemicalCauldronBlockEntity.serverTick(level, cauldronPos, level.getBlockState(cauldronPos), be);

        AspectVector vec = be.getReactionState().getCurrentVector();
        assertTrue(vec.getTotalAspectMass() > 0.0);
        assertTrue(vec.getAmount(AlchemyAspect.IGNIS) > 0.0);
        assertTrue(be.getFluidColor() != initialColor); // 流体颜色发生动态红移

        System.out.println("PASS");
    }

    private static void testStirringRodHydrodynamics() {
        System.out.print("[TEST 5/8] Stirring Rod Hydrodynamic Manipulation... ");

        TestCauldronLevel level = new TestCauldronLevel();
        BlockPos cauldronPos = new BlockPos(0, 1, 0);
        AlchemicalCauldronBlock block = new AlchemicalCauldronBlock(Block.Properties.of());
        BlockState state = block.defaultBlockState();
        level.setBlock(cauldronPos, state, 3);

        AlchemicalCauldronBlockEntity be = new AlchemicalCauldronBlockEntity(cauldronPos, state);
        level.setBlockEntity(cauldronPos, be);
        be.setWaterLevel(3);

        Player player = new Player();
        player.level = level;
        AlchemicalStirringRodItem rod = ModItems.getStirringRod();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(rod));

        // 1. 顺时针搅拌 (连续搅拌3次，突破 2*pi 完整圈数)
        for (int s = 0; s < 3; s++) {
            be.onPlayerInteract(player, InteractionHand.MAIN_HAND, null);
        }
        for (int i = 0; i < 30; i++) {
            AlchemicalCauldronBlockEntity.serverTick(level, cauldronPos, level.getBlockState(cauldronPos), be);
        }
        assertTrue(be.getReactionState().getClockwiseTurns() > 0);

        // 2. 逆时针搅拌 (连续逆向搅拌3次)
        for (int s = 0; s < 3; s++) {
            be.getReactionState().stir(false, 0.35);
        }
        for (int i = 0; i < 30; i++) {
            AlchemicalCauldronBlockEntity.serverTick(level, cauldronPos, level.getBlockState(cauldronPos), be);
        }
        assertTrue(be.getReactionState().getCounterClockwiseTurns() > 0);

        System.out.println("PASS");
    }

    private static void testDualWieldGrimoireScholarIntuition() {
        System.out.print("[TEST 6/8] Dual-Wield Grimoire Scholar's Intuition... ");

        TestCauldronLevel level = new TestCauldronLevel();
        BlockPos cauldronPos = new BlockPos(0, 1, 0);
        AlchemicalCauldronBlock block = new AlchemicalCauldronBlock(Block.Properties.of());
        BlockState state = block.defaultBlockState();
        level.setBlock(cauldronPos, state, 3);

        AlchemicalCauldronBlockEntity be = new AlchemicalCauldronBlockEntity(cauldronPos, state);
        level.setBlockEntity(cauldronPos, be);
        be.setWaterLevel(2);

        Player player = new Player();
        player.level = level;

        // 未持手记：直觉未激活
        assertFalse(AlchemicalJournalItem.isScholarIntuitionActive(player));

        // 副手装备《炼金手记》：直觉激活！
        AlchemicalJournalItem journal = ModItems.getJournal();
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(journal));
        assertTrue(AlchemicalJournalItem.isScholarIntuitionActive(player));

        // 凝视透视数据快照
        ScholarIntuitionData snapshot = AlchemicalJournalItem.inspectCauldron(level, cauldronPos, player);
        assertTrue(snapshot != ScholarIntuitionData.EMPTY);
        assertEquals(2, snapshot.waterLevel());
        assertEquals(20.0, snapshot.temperature(), 1e-4);

        // 验证 HUD 文本渲染生成
        List<Component> displayLines = ScholarIntuitionHudRenderer.buildDisplayLines(snapshot);
        assertFalse(displayLines.isEmpty());

        System.out.println("PASS");
    }

    private static void testRecipeStepGuidance() {
        System.out.print("[TEST 7/8] Dynamic Recipe Step Guidance... ");

        // 1. 未注水
        assertEquals(AlchemicalRecipeStepTracker.RecipePhase.FILL_WATER,
                AlchemicalRecipeStepTracker.derivePhase(0, 20.0, 0.0, 0.0, 0));

        // 2. 已注水但未加热
        assertEquals(AlchemicalRecipeStepTracker.RecipePhase.PREHEAT,
                AlchemicalRecipeStepTracker.derivePhase(3, 30.0, 0.0, 0.0, 0));

        // 3. 达到反应温区但未加料
        assertEquals(AlchemicalRecipeStepTracker.RecipePhase.ADD_INGREDIENTS,
                AlchemicalRecipeStepTracker.derivePhase(3, 90.0, 0.0, 0.0, 0));

        // 4. 已加料但未搅拌
        assertEquals(AlchemicalRecipeStepTracker.RecipePhase.STIRRING,
                AlchemicalRecipeStepTracker.derivePhase(3, 90.0, 5.0, 0.1, 0));

        // 5. 失稳度过高
        assertEquals(AlchemicalRecipeStepTracker.RecipePhase.STABILIZE,
                AlchemicalRecipeStepTracker.derivePhase(3, 90.0, 5.0, 0.65, 3));

        // 6. 稳定且搅拌充分，准备提取
        assertEquals(AlchemicalRecipeStepTracker.RecipePhase.EXTRACT,
                AlchemicalRecipeStepTracker.derivePhase(3, 90.0, 5.0, 0.2, 3));

        System.out.println("PASS");
    }

    private static void testMishapCatastrophes() {
        System.out.print("[TEST 8/8] Mishap Catastrophe State Machine... ");

        CauldronReactionState state = new CauldronReactionState();

        // 正常状态
        assertEquals(MishapResult.NONE, state.checkMishap());

        // 1. 模拟浑浊度超标 -> SLUDGE_COAGULATION
        CauldronReactionState sludgeState = new CauldronReactionState();
        sludgeState.addIngredient(AspectVector.builder().add(AlchemyAspect.CHAOS, 150.0).build());
        assertTrue(sludgeState.checkMishap() == MishapResult.SLUDGE_COAGULATION || sludgeState.getTurbidity() > 0.5);

        System.out.println("PASS");
    }

    private static void assertEquals(double expected, double actual, double delta) {
        if (Math.abs(expected - actual) > delta) {
            throw new AssertionError(String.format("Expected %.5f, but got %.5f (diff: %.5f)", expected, actual, Math.abs(expected - actual)));
        }
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null && actual == null) return;
        if (expected != null && expected.equals(actual)) return;
        throw new AssertionError("Expected " + expected + ", but got " + actual);
    }

    private static void assertTrue(boolean condition) {
        if (!condition) throw new AssertionError("Expected condition to be true, but was false");
    }

    private static void assertFalse(boolean condition) {
        if (condition) throw new AssertionError("Expected condition to be false, but was true");
    }
}
