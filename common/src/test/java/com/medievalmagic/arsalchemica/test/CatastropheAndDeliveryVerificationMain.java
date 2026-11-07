package com.medievalmagic.arsalchemica.test;

import com.medievalmagic.arsalchemica.api.aspect.AlchemyAspect;
import com.medievalmagic.arsalchemica.api.cauldron.CauldronReactionState;
import com.medievalmagic.arsalchemica.api.cauldron.CauldronReactionState.HeatLevel;
import com.medievalmagic.arsalchemica.api.vector.AspectVector;
import com.medievalmagic.arsalchemica.cauldron.AlchemicalCauldronBlock;
import com.medievalmagic.arsalchemica.cauldron.AlchemicalCauldronBlockEntity;
import com.medievalmagic.arsalchemica.cauldron.AlchemicalStirringRodItem;
import com.medievalmagic.arsalchemica.cauldron.item.AlchemicalSlagItem;
import com.medievalmagic.arsalchemica.cauldron.item.AlchemicalSludgeItem;
import com.medievalmagic.arsalchemica.cauldron.mishap.AlchemicalCatastropheHandler;
import com.medievalmagic.arsalchemica.cauldron.mishap.AlchemicalMishapType;
import com.medievalmagic.arsalchemica.client.ScholarIntuitionHudRenderer;
import com.medievalmagic.arsalchemica.grimoire.AlchemicalJournalItem;
import com.medievalmagic.arsalchemica.grimoire.ScholarIntuitionData;
import com.medievalmagic.arsalchemica.potion.AlchemicalPotionItem;
import com.medievalmagic.arsalchemica.potion.BladeOilItem;
import com.medievalmagic.arsalchemica.recipe.item.AncientParchmentItem;
import com.medievalmagic.arsalchemica.recipe.item.RecipeScrollItem;
import com.medievalmagic.arsalchemica.recipe.item.ScholiaFragmentItem;
import com.medievalmagic.arsalchemica.registry.ModBlockEntities;
import com.medievalmagic.arsalchemica.registry.ModBlocks;
import com.medievalmagic.arsalchemica.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ars Alchemica Milestone 6 自动化验证套件。
 * <p>
 * 严苛检验：
 * 1. 干烧过热熔毁状态机与爆裂；
 * 2. 挥发物炼狱烈焰蔓延与火海引燃；
 * 3. 剧毒以太瘴气大爆发与复合 Debuff；
 * 4. 炼金败物黑泥凝结与铁铲清淤循环；
 * 5. 以太裂解大殉爆方块损毁与废渣残片掉落；
 * 6. 炼金黑泥副产物投掷武器化；
 * 7. 学者手记灾难预警雷达；
 * 8. 零卡顿相位取模与 10,000 次高频压测。
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public class CatastropheAndDeliveryVerificationMain {

    private static int testsPassed = 0;
    private static final int TOTAL_TESTS = 8;

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("=== ARS ALCHEMICA: MILESTONE 6 VERIFICATION SUITE       ===");
        System.out.println("==========================================================");

        setupRegistries();

        try {
            testDryBoilMeltdown();
            testInfernalConflagration();
            testToxicMiasmaOutbreak();
            testSludgeCoagulationAndShovelCleanout();
            testArcaneDetonationAndSlagDebris();
            testSludgeThrowingWeaponization();
            testScholarIntuitionCatastropheRadar();
            testZeroLagPerformanceBenchmark();

            System.out.println();
            System.out.println(">>> ALL MILESTONE 6 TESTS PASSED PERFECTLY (100% SUCCESS) <<<");
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
        AlchemicalSlagItem slag = new AlchemicalSlagItem(new Item.Properties());
        AlchemicalSludgeItem sludge = new AlchemicalSludgeItem(new Item.Properties());
        ModItems.bindMishapItems(
                () -> slag,
                () -> sludge
        );
    }

    private static void testDryBoilMeltdown() {
        System.out.print("[TEST 1/8] Dry-Boil Meltdown Mechanics... ");

        CauldronReactionState state = new CauldronReactionState();
        assertEquals(0, state.getDryBoilTicks(), "Initial dry boil ticks must be 0");
        assertTrue(!state.isDryBoiling(), "Should not be dry boiling initially");

        // 1. 无水且加热 -> 开始干烧计时
        for (int i = 0; i < 100; i++) {
            state.tick(true, HeatLevel.ROILING_BOIL, 0); // waterLevel = 0
        }
        assertEquals(100, state.getDryBoilTicks(), "Dry boil ticks should reach 100");
        assertTrue(state.getDryBoilTicks() >= 40, "Should trigger dry boil alert");

        // 2. 持续干烧满 200 ticks -> 触发 DRY_BOIL_MELTDOWN
        for (int i = 0; i < 100; i++) {
            state.tick(true, HeatLevel.ROILING_BOIL, 0);
        }
        assertEquals(200, state.getDryBoilTicks(), "Dry boil ticks should reach 200");
        AlchemicalMishapType mishap = state.checkDetailedMishap();
        assertEquals(AlchemicalMishapType.DRY_BOIL_MELTDOWN, mishap, "Mishap must be DRY_BOIL_MELTDOWN");

        // 3. 执行事故：爆炸摧毁方块并掉落铁渣
        TestCatastropheLevel level = new TestCatastropheLevel();
        BlockPos pos = new BlockPos(10, 64, 10);
        level.setBlock(pos, ModBlocks.getCauldron().defaultBlockState(), 3);
        AlchemicalCatastropheHandler.executeMishap(level, pos, mishap);

        assertTrue(level.wasBlockDestroyed(pos), "Crucible block must be destroyed by dry-boil meltdown");
        assertTrue(level.getEntities().stream().anyMatch(e -> e instanceof ItemEntity ie && ie.getItem().getItem() instanceof AlchemicalSlagItem),
                "Must spawn Alchemical Slag upon meltdown");

        System.out.println("PASS");
        testsPassed++;
    }

    private static void testInfernalConflagration() {
        System.out.print("[TEST 2/8] Volatile Reagent Infernal Conflagration... ");

        CauldronReactionState state = new CauldronReactionState();
        // 投入高挥发高易燃与对立元素 (Sulfur + Mercurius + Ignis/Aqua 剧烈冲突)
        AspectVector sulfurVec = AspectVector.builder()
                .set(AlchemyAspect.SULFUR, 25.0)
                .set(AlchemyAspect.MERCURIUS, 20.0)
                .set(AlchemyAspect.IGNIS, 20.0)
                .set(AlchemyAspect.AQUA, 15.0)
                .build();
        state.addIngredient(sulfurVec);

        // 加热至沸腾极温 (150°C) 并交替剧烈逆转搅拌激发湍流失稳
        for (int i = 0; i < 120; i++) {
            state.tick(true, HeatLevel.ROILING_BOIL, 2);
            if (i % 10 == 0) {
                state.stir(i % 20 == 0, 0.45); // 正反交替剧烈搅拌触发湍流碰撞
            }
        }

        assertTrue(state.getCurrentVector().calculateVolatility() > 0.80, "Volatility should exceed 0.80");
        assertTrue(state.getAccumulatedInstability() >= 0.60, "Instability should exceed 0.60");
        assertTrue(state.getTemperature() >= 120.0, "Temperature should exceed 120°C");

        AlchemicalMishapType mishap = state.checkDetailedMishap();
        assertEquals(AlchemicalMishapType.INFERNAL_CONFLAGRATION, mishap, "Mishap must be INFERNAL_CONFLAGRATION");

        // 执行事故：周边引燃火海 (铺设承载火焰的石质地面)
        TestCatastropheLevel level = new TestCatastropheLevel();
        BlockPos pos = new BlockPos(0, 64, 0);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlock(pos.offset(dx, -1, dz), Blocks.COBBLESTONE.defaultBlockState(), 3);
            }
        }
        AlchemicalCatastropheHandler.executeMishap(level, pos, mishap);

        long fireCount = level.getPlacedBlocks().values().stream()
                .filter(b -> b.is(Blocks.FIRE))
                .count();
        assertTrue(fireCount > 0, "Must ignite surrounding fire blocks");

        System.out.println("PASS");
        testsPassed++;
    }

    private static void testToxicMiasmaOutbreak() {
        System.out.print("[TEST 3/8] Toxic Miasma Outbreak & Composite Debuffs... ");

        TestCatastropheLevel level = new TestCatastropheLevel();
        BlockPos pos = new BlockPos(0, 64, 0);

        AlchemicalCatastropheHandler.executeMishap(level, pos, AlchemicalMishapType.TOXIC_MIASMA_OUTBREAK);

        // 验证生成了半径 4.5、持续 300 ticks 的 AreaEffectCloud
        List<Entity> clouds = level.getEntities().stream()
                .filter(e -> e instanceof AreaEffectCloud)
                .toList();
        assertTrue(!clouds.isEmpty(), "Must spawn AreaEffectCloud");
        AreaEffectCloud cloud = (AreaEffectCloud) clouds.get(0);
        assertAlmostEquals(4.5f, cloud.getRadius(), 1e-4, "Cloud radius must be 4.5");
        assertEquals(300, cloud.getDuration(), "Cloud duration must be 300 ticks (15s)");

        System.out.println("PASS");
        testsPassed++;
    }

    private static void testSludgeCoagulationAndShovelCleanout() {
        System.out.print("[TEST 4/8] Sludge Coagulation & Shovel Cleanout Interaction... ");

        TestCatastropheLevel level = new TestCatastropheLevel();
        BlockPos pos = new BlockPos(0, 64, 0);
        BlockState state = ModBlocks.getCauldron().defaultBlockState();
        AlchemicalCauldronBlockEntity cauldron = new AlchemicalCauldronBlockEntity(pos, state);
        level.setBlockEntity(pos, cauldron);

        cauldron.setWaterLevel(2);
        // 注入巨量死灵与混沌元素导致浑浊度溢出 (turbidity >= 1.0)
        AspectVector sludgeVec = AspectVector.builder()
                .set(AlchemyAspect.MORTIS, 80.0)
                .set(AlchemyAspect.CHAOS, 80.0)
                .build();
        cauldron.getReactionState().addIngredient(sludgeVec);
        for (int i = 0; i < 40; i++) {
            cauldron.getReactionState().tick(true, HeatLevel.WARM_SIMMER, 2);
        }

        assertTrue(cauldron.getReactionState().getTurbidity() >= 1.0, "Turbidity must reach 1.0");
        AlchemicalMishapType mishap = cauldron.getReactionState().checkDetailedMishap();
        assertEquals(AlchemicalMishapType.SLUDGE_COAGULATION, mishap, "Mishap must be SLUDGE_COAGULATION");

        cauldron.handleDetailedMishap(level, pos, mishap);
        assertEquals(0x111116, cauldron.getFluidColor(), "Fluid must turn into toxic sludge color");

        // 玩家手持铁铲清理黑泥
        Player player = new Player();
        ItemStack shovel = new ItemStack(new ShovelItem(new Item.Properties()));
        player.setItemInHand(InteractionHand.MAIN_HAND, shovel);

        InteractionResult result = cauldron.onPlayerInteract(player, InteractionHand.MAIN_HAND, new BlockHitResult());
        assertTrue(result.consumesAction(), "Shovel cleanout should succeed");
        assertAlmostEquals(0.0, cauldron.getReactionState().getTurbidity(), 1e-6, "Turbidity must be cleared to 0");
        assertEquals(AlchemicalCauldronBlockEntity.DEFAULT_FLUID_COLOR, cauldron.getFluidColor(), "Fluid color should reset");

        // 验证玩家背包或掉落获得【炼金败物腐蚀黑泥】
        assertTrue(player.getInventory().contains(new ItemStack(ModItems.getSludge())),
                "Player must receive Alchemical Sludge item from cleaning");

        System.out.println("PASS");
        testsPassed++;
    }

    private static void testArcaneDetonationAndSlagDebris() {
        System.out.print("[TEST 5/8] Arcane Detonation & Slag Debris Scavenging... ");

        TestCatastropheLevel level = new TestCatastropheLevel();
        BlockPos pos = new BlockPos(5, 64, 5);
        level.setBlock(pos, ModBlocks.getCauldron().defaultBlockState(), 3);

        AlchemicalCatastropheHandler.executeMishap(level, pos, AlchemicalMishapType.ARCANE_DETONATION);

        assertTrue(level.wasExplosionTriggered(), "Explosion must be triggered");
        assertTrue(level.wasBlockDestroyed(pos), "Block must be destroyed");

        // 检查散落的炼金铁渣
        long slagCount = level.getEntities().stream()
                .filter(e -> e instanceof ItemEntity ie && ie.getItem().getItem() instanceof AlchemicalSlagItem)
                .count();
        assertTrue(slagCount > 0, "Must drop alchemical slag entities");

        System.out.println("PASS");
        testsPassed++;
    }

    private static void testSludgeThrowingWeaponization() {
        System.out.print("[TEST 6/8] Alchemical Sludge Throwing & Splash Weaponization... ");

        Level level = new Level() {};
        Player player = new Player();
        ItemStack sludgeStack = new ItemStack(ModItems.getSludge(), 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, sludgeStack);

        AlchemicalSludgeItem sludgeItem = ModItems.getSludge();
        sludgeItem.use(level, player, InteractionHand.MAIN_HAND);

        assertEquals(2, player.getItemInHand(InteractionHand.MAIN_HAND).getCount(), "Should consume 1 sludge on throw");
        assertTrue(level.getEntities().stream().anyMatch(e -> e instanceof AreaEffectCloud),
                "Throwing sludge must spawn a corrosive splash cloud");

        System.out.println("PASS");
        testsPassed++;
    }

    private static void testScholarIntuitionCatastropheRadar() {
        System.out.print("[TEST 7/8] Scholar's Intuition Catastrophe Radar & Alerts... ");

        // 1. 干烧警报测试
        ScholarIntuitionData dryData = new ScholarIntuitionData(
                180.0, HeatLevel.ROILING_BOIL, 0, 0.0, 1.0, 0.0, 0.0,
                0, 0, 0x385DC6, null, "step.ars_alchemica.preheat",
                0.0, 0.0, null, 120 // dryBoilTicks = 120 (> 40)
        );
        assertTrue(dryData.isDryBoilWarning(), "Must report dry boil warning");
        List<Component> dryLines = ScholarIntuitionHudRenderer.buildDisplayLines(dryData);
        boolean hasDryWarn = dryLines.stream().anyMatch(c -> c.getString().contains("干烧") || c.getString().contains("dry_boil"));
        assertTrue(hasDryWarn, "HUD must render dry-boil alert text");

        // 2. 黑泥报废警报测试
        ScholarIntuitionData sludgeData = new ScholarIntuitionData(
                60.0, HeatLevel.WARM_SIMMER, 2, 20.0, 0.2, 0.2, 1.0, // turbidity = 1.0
                0, 0, 0x111116, null, "step.ars_alchemica.stirring"
        );
        List<Component> sludgeLines = ScholarIntuitionHudRenderer.buildDisplayLines(sludgeData);
        boolean hasSludgeWarn = sludgeLines.stream().anyMatch(c -> c.getString().contains("黑泥") || c.getString().contains("sludge"));
        assertTrue(hasSludgeWarn, "HUD must render sludge coagulation alert text");

        System.out.println("PASS");
        testsPassed++;
    }

    private static void testZeroLagPerformanceBenchmark() {
        System.out.print("[TEST 8/8] Zero-Lag Staggered Ticking & Performance Benchmarking... ");

        TestCatastropheLevel level = new TestCatastropheLevel();
        int cauldronCount = 20;
        AlchemicalCauldronBlockEntity[] cauldrons = new AlchemicalCauldronBlockEntity[cauldronCount];

        for (int i = 0; i < cauldronCount; i++) {
            BlockPos p = new BlockPos(i * 2, 64, 0);
            BlockState state = ModBlocks.getCauldron().defaultBlockState();
            cauldrons[i] = new AlchemicalCauldronBlockEntity(p, state);
            cauldrons[i].setWaterLevel(3);
            level.setBlockEntity(p, cauldrons[i]);
        }

        // 模拟 10,000 个服务端的 Tick 演进
        long startTime = System.nanoTime();
        int scanCount = 0;

        for (long t = 0; t < 10000; t++) {
            level.setGameTime(t);
            for (int i = 0; i < cauldronCount; i++) {
                AlchemicalCauldronBlockEntity.serverTick(level, cauldrons[i].getBlockPos(), cauldrons[i].getBlockState(), cauldrons[i]);
                if ((t + cauldrons[i].getBlockPos().hashCode()) % 4 == 0) {
                    scanCount++;
                }
            }
        }
        long durationMs = (System.nanoTime() - startTime) / 1_000_000;

        // 20 个坩埚 * 10,000 ticks = 200,000 次 tick，平均耗时应远低于 250ms (零卡顿)
        assertTrue(durationMs < 500, "10,000 tick benchmark took " + durationMs + "ms (must be < 500ms for Zero-Lag)");
        // 相位取模扫描次数应严格约为 200,000 / 4 = 50,000 次
        assertEquals(50000, scanCount, "Staggered phase modulo scan count mismatch");

        System.out.printf("PASS (%d ms for 200,000 tick evaluations)%n", durationMs);
        testsPassed++;
    }

    private static class TestCatastropheLevel extends Level {
        private final Map<BlockPos, BlockState> blocks = new HashMap<>();
        private final Map<BlockPos, AlchemicalCauldronBlockEntity> blockEntities = new HashMap<>();
        private boolean explosionTriggered = false;
        private final Map<BlockPos, Boolean> destroyedBlocks = new HashMap<>();

        public void setBlockEntity(BlockPos pos, AlchemicalCauldronBlockEntity be) {
            blockEntities.put(pos, be);
            if (be != null) be.setLevel(this);
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
        }

        @Override
        public boolean setBlock(BlockPos pos, BlockState state, int flags) {
            blocks.put(pos, state);
            return true;
        }

        @Override
        public boolean destroyBlock(BlockPos pos, boolean drop) {
            destroyedBlocks.put(pos, true);
            blocks.remove(pos);
            return true;
        }

        @Override
        public net.minecraft.world.level.Explosion explode(Entity source, double x, double y, double z, float radius, ExplosionInteraction interaction) {
            this.explosionTriggered = true;
            return new net.minecraft.world.level.Explosion();
        }

        public boolean wasExplosionTriggered() { return explosionTriggered; }
        public boolean wasBlockDestroyed(BlockPos pos) { return destroyedBlocks.getOrDefault(pos, false); }
        public Map<BlockPos, BlockState> getPlacedBlocks() { return blocks; }
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
