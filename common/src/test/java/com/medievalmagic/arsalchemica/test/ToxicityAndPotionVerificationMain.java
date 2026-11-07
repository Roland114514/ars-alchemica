package com.medievalmagic.arsalchemica.test;

import com.medievalmagic.arsalchemica.api.aspect.AlchemyAspect;
import com.medievalmagic.arsalchemica.api.toxicity.IToxicityData;
import com.medievalmagic.arsalchemica.api.toxicity.ToxicityThreshold;
import com.medievalmagic.arsalchemica.api.vector.AspectVector;
import com.medievalmagic.arsalchemica.client.AlchemicalHeartRenderer;
import com.medievalmagic.arsalchemica.client.ToxicityHudRenderer;
import com.medievalmagic.arsalchemica.potion.AlchemicalCorrosionEffect;
import com.medievalmagic.arsalchemica.potion.AlchemicalPotionItem;
import com.medievalmagic.arsalchemica.potion.BladeOilItem;
import com.medievalmagic.arsalchemica.potion.DynamicPotionResolver;
import com.medievalmagic.arsalchemica.potion.PlayerToxicityData;
import com.medievalmagic.arsalchemica.potion.PlayerToxicityManager;
import com.medievalmagic.arsalchemica.potion.ToxicitySyncPacket;
import com.medievalmagic.arsalchemica.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Milestone 3 自动化测试与严格数学/逻辑校验套件 (含以太毒蚀 Debuff 与客户端 UI 表现测试)。
 * <p>
 * 覆盖：
 * 1. 猎魔人毒性生理阈值状态机与 NaN/Infinity 防护
 * 2. 玩家毒性数据 POJO、NBT 往返、死亡重置与代谢平滑回归
 * 3. 服务端毒性代谢微分衰减、反噬剥夺、解毒净化与防死亡循环
 * 4. 专属以太毒蚀 (AlchemicalCorrosionEffect) 机制：脉冲周期、非致死保底与过载致死判定
 * 5. 客户端生命条暗夜以太心形渲染 (AlchemicalHeartRenderer) 与过载心律失常震颤
 * 6. 客户端 HUD 毒性条与屏幕暗角毒晕渲染计算 (ToxicityHudRenderer)
 * 7. 连续以太向量连续药效解算、纯度等级加成与合理药效时长
 * 8. 动态复合灵药 (ELIXIR) 与范围熏香 (INCENSE) 双交付形态
 * 9. 猎魔人涂抹剑油、种族克制穿透、严密兵刃校验与耐久归零剥离
 * 10. 9 字节极简二进制网络包编解码
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public class ToxicityAndPotionVerificationMain {

    private static class TestLevel extends Level {
        @Override
        public BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }
    }

    public static void main(String[] args) {
        System.out.println("==========================================================");
        System.out.println("=== ARS ALCHEMICA: MILESTONE 3 VERIFICATION SUITE       ===");
        System.out.println("==========================================================");

        testToxicityThresholdStateMachine();
        testPlayerToxicityDataStorageAndNBT();
        testPlayerToxicityManagerAndMetabolism();
        testAlchemicalCorrosionMechanics();
        testAlchemicalHeartAndToxicityUI();
        testDynamicPotionResolver();
        testAlchemicalPotionItemDeliveries();
        testBladeOilApplicationAndCombat();
        testToxicitySyncPacketBinaryRoundtrip();

        System.out.println("\n>>> ALL MILESTONE 3 TESTS PASSED PERFECTLY (100% SUCCESS) <<<");
    }

    private static void testToxicityThresholdStateMachine() {
        System.out.print("[TEST 1/9] Toxicity Threshold State Machine... ");

        assertEquals(ToxicityThreshold.SAFE, ToxicityThreshold.fromRatio(0.0f));
        assertEquals(ToxicityThreshold.SAFE, ToxicityThreshold.fromRatio(0.4999f));
        assertEquals(ToxicityThreshold.CAUTION, ToxicityThreshold.fromRatio(0.50f));
        assertEquals(ToxicityThreshold.CAUTION, ToxicityThreshold.fromRatio(0.7499f));
        assertEquals(ToxicityThreshold.DANGER, ToxicityThreshold.fromRatio(0.75f));
        assertEquals(ToxicityThreshold.DANGER, ToxicityThreshold.fromRatio(0.8999f));
        assertEquals(ToxicityThreshold.OVERDOSE, ToxicityThreshold.fromRatio(0.90f));
        assertEquals(ToxicityThreshold.OVERDOSE, ToxicityThreshold.fromRatio(1.20f));
        assertEquals(ToxicityThreshold.SAFE, ToxicityThreshold.fromRatio(Float.NaN));
        assertEquals(ToxicityThreshold.SAFE, ToxicityThreshold.fromRatio(Float.POSITIVE_INFINITY));

        assertEquals("toxicity.ars_alchemica.state.safe", ToxicityThreshold.SAFE.getTranslationKey());
        assertEquals("toxicity.ars_alchemica.state.overdose", ToxicityThreshold.OVERDOSE.getTranslationKey());

        System.out.println("PASS");
    }

    private static void testPlayerToxicityDataStorageAndNBT() {
        System.out.print("[TEST 2/9] Player Toxicity Data, Death Reset & NBT Roundtrip... ");

        PlayerToxicityData data = new PlayerToxicityData();
        assertEquals(0.0f, data.getToxicity(), 1e-4f);
        assertEquals(100.0f, data.getMaxToxicity(), 1e-4f);
        assertEquals(0.0125f, data.getMetabolismRate(), 1e-5f);
        assertFalse(data.isDirty());

        // 测试增加毒性与 Clamp
        data.addToxicity(45.0f);
        assertEquals(45.0f, data.getToxicity(), 1e-4f);
        assertTrue(data.isDirty());
        assertEquals(ToxicityThreshold.SAFE, data.getThreshold());

        data.addToxicity(35.0f); // 80.0f -> DANGER
        assertEquals(80.0f, data.getToxicity(), 1e-4f);
        assertEquals(ToxicityThreshold.DANGER, data.getThreshold());

        // 测试越界保护与 Infinity 过滤
        data.addToxicity(Float.POSITIVE_INFINITY);
        assertEquals(80.0f, data.getToxicity(), 1e-4f);
        data.addToxicity(50.0f);
        assertEquals(100.0f, data.getToxicity(), 1e-4f); // 受到 maxToxicity 截断
        assertEquals(ToxicityThreshold.OVERDOSE, data.getThreshold());

        // 死亡重置测试 (防止死后重生无限暴毙)
        data.resetOnDeath();
        assertEquals(0.0f, data.getToxicity(), 1e-4f);
        assertEquals(0.0125f, data.getMetabolismRate(), 1e-5f);

        // NBT 序列化与反序列化
        data.addToxicity(65.0f);
        CompoundTag tag = new CompoundTag();
        data.saveNBTData(tag);

        PlayerToxicityData restored = new PlayerToxicityData();
        restored.loadNBTData(tag);
        assertEquals(65.0f, restored.getToxicity(), 1e-4f);
        assertEquals(100.0f, restored.getMaxToxicity(), 1e-4f);
        assertEquals(0.0125f, restored.getMetabolismRate(), 1e-5f);

        System.out.println("PASS");
    }

    private static void testPlayerToxicityManagerAndMetabolism() {
        System.out.print("[TEST 3/9] Player Toxicity Manager & Physiological Reactions... ");

        Map<Player, IToxicityData> playerStorage = new HashMap<>();
        List<ToxicitySyncPacket> sentPackets = new ArrayList<>();

        PlayerToxicityManager.initPlatformBridge(
                p -> Optional.ofNullable(playerStorage.computeIfAbsent(p, k -> new PlayerToxicityData())),
                (p, pkt) -> sentPackets.add(pkt)
        );

        TestLevel level = new TestLevel();
        level.isClientSide = false;
        Player player = new Player();
        player.level = level;

        IToxicityData data = PlayerToxicityManager.getData(player).orElseThrow();
        data.setToxicity(60.0f); // CAUTION
        data.setDirty(true);

        // 1. 测试单 Tick 连续衰减
        player.tickCount = 1;
        PlayerToxicityManager.tickPlayer(player);
        assertEquals(60.0f - 0.0125f, data.getToxicity(), 1e-4f);

        // 2. 测试 OVERDOSE 反噬惩罚
        data.setToxicity(95.0f); // OVERDOSE (>90%)
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1)); // 添加一个有益 Buff
        assertEquals(1, player.getActiveEffects().size());

        player.tickCount = 20; // 触发 20-tick 周期生理结算
        PlayerToxicityManager.tickPlayer(player);

        // 验证有益效果已被剥夺清除，且施加了专属以太毒蚀 (Corrosion)
        boolean hasCorrosion = false;
        for (MobEffectInstance inst : player.getActiveEffects()) {
            if (inst.getEffect() == ModEffects.getCorrosion()) {
                hasCorrosion = true;
            }
            assertFalse(inst.getEffect().isBeneficial());
        }
        assertTrue(hasCorrosion);
        assertEquals(2.5f, player.lastDamageTaken, 1e-4f);

        // 3. 测试解毒剂净化
        sentPackets.clear();
        PlayerToxicityManager.applyAntidote(player, 50.0f, 0.05f);
        assertTrue(data.getToxicity() <= 45.0f);
        assertEquals(0.05f, data.getMetabolismRate(), 1e-5f);
        assertFalse(sentPackets.isEmpty()); // 验证触发了客户端同步

        // 4. 测试代谢平滑回归
        for (int i = 0; i < 200; i++) {
            data.tickMetabolism();
        }
        assertTrue(data.getMetabolismRate() < 0.05f); // 验证代谢速率逐渐向基础值回退，而非永久暴涨

        System.out.println("PASS");
    }

    private static void testAlchemicalCorrosionMechanics() {
        System.out.print("[TEST 4/9] Alchemical Corrosion Debuff Mechanics... ");

        AlchemicalCorrosionEffect effect = ModEffects.getCorrosion();

        // 1. 触发周期判定
        assertTrue(effect.isDurationEffectTick(25, 0)); // Amp 0: 每 25 ticks
        assertFalse(effect.isDurationEffectTick(24, 0));
        assertTrue(effect.isDurationEffectTick(12, 1)); // Amp 1: 每 12 ticks

        // 2. 常规非致死保底测试 (模仿原版中毒：生命值 > 1.0f 才会造成扣减)
        TestLevel level = new TestLevel();
        level.isClientSide = false;

        LivingEntity victim = new LivingEntity();
        victim.level = level;
        victim.setHealth(0.8f); // 濒死（低于半颗心）

        effect.applyEffectTick(victim, 0);
        assertEquals(0.8f, victim.getHealth(), 1e-4f); // 受到保底保护，不会致死！

        // 3. 毒性过载致死模式测试
        Player overdosedPlayer = new Player();
        overdosedPlayer.level = level;
        overdosedPlayer.setHealth(0.8f);
        IToxicityData data = PlayerToxicityManager.getData(overdosedPlayer).orElseThrow();
        data.setToxicity(95.0f); // 处于 OVERDOSE

        effect.applyEffectTick(overdosedPlayer, 0);
        assertTrue(overdosedPlayer.getHealth() < 0.8f); // 过载状态破除保底，致死反噬生效！

        System.out.println("PASS");
    }

    private static void testAlchemicalHeartAndToxicityUI() {
        System.out.print("[TEST 5/9] Alchemical Heart & Toxicity HUD Presentation... ");

        Player player = new Player();
        IToxicityData data = PlayerToxicityManager.getData(player).orElseThrow();

        // 1. 心形渲染模式切换
        data.setToxicity(10.0f);
        assertEquals(AlchemicalHeartRenderer.HeartVisualState.VANILLA, AlchemicalHeartRenderer.getVisualState(player));

        data.setToxicity(80.0f); // DANGER
        assertEquals(AlchemicalHeartRenderer.HeartVisualState.ALCHEMICAL_CORROSION, AlchemicalHeartRenderer.getVisualState(player));

        data.setToxicity(95.0f); // OVERDOSE
        assertEquals(AlchemicalHeartRenderer.HeartVisualState.OVERDOSE_COLLAPSE, AlchemicalHeartRenderer.getVisualState(player));

        // 2. 过载心律失常垂直抖动
        int jitter = AlchemicalHeartRenderer.getHeartTremorOffsetY(AlchemicalHeartRenderer.HeartVisualState.OVERDOSE_COLLAPSE, 0, 100L);
        assertTrue(jitter >= -2 && jitter <= 2);

        // 3. HUD 暗角滤镜 Alpha 计算
        assertEquals(0.0f, ToxicityHudRenderer.getScreenVignetteAlpha(0.20f, 0L), 1e-4f);
        assertTrue(ToxicityHudRenderer.getScreenVignetteAlpha(0.60f, 0L) > 0.0f);
        assertTrue(ToxicityHudRenderer.getScreenVignetteAlpha(0.85f, 0L) > 0.35f);
        assertTrue(ToxicityHudRenderer.getScreenVignetteAlpha(0.95f, 0L) >= 0.65f);

        System.out.println("PASS");
    }

    private static void testDynamicPotionResolver() {
        System.out.print("[TEST 6/9] Dynamic Potion Mathematical Resolver & Duration Scaling... ");

        // 构建纯粹火相 (IGNIS) 以太向量
        AspectVector ignisVector = AspectVector.builder().add(AlchemyAspect.IGNIS, 10.0).build();
        List<MobEffectInstance> ignisEffects = DynamicPotionResolver.resolveEffects(ignisVector, 15.0, 1.0, 1.0);
        assertFalse(ignisEffects.isEmpty());
        assertEquals(MobEffects.FIRE_RESISTANCE, ignisEffects.get(0).getEffect());
        assertTrue(ignisEffects.get(0).getDuration() >= 2000); // 检验合理的药效持续时间 (大于1.5分钟)

        // 高挥发性火相激发 DAMAGE_BOOST
        List<MobEffectInstance> volatileIgnis = DynamicPotionResolver.resolveEffects(ignisVector, 15.0, 1.0, 2.0);
        assertEquals(MobEffects.DAMAGE_BOOST, volatileIgnis.get(0).getEffect());

        // 死寂 (MORTIS) 源质映射为专属以太毒蚀
        AspectVector mortisVector = AspectVector.builder().add(AlchemyAspect.MORTIS, 10.0).build();
        List<MobEffectInstance> mortisEffects = DynamicPotionResolver.resolveEffects(mortisVector, 10.0, 1.0, 1.0);
        assertEquals(ModEffects.getCorrosion(), mortisEffects.get(0).getEffect());

        // 测试毒性负载公式 (高纯度 vs 低纯度)
        float highPurityTox = DynamicPotionResolver.calculateToxicityLoad(ignisVector, 10.0, 1.0);
        float lowPurityTox = DynamicPotionResolver.calculateToxicityLoad(ignisVector, 10.0, 0.1);
        assertTrue(highPurityTox < lowPurityTox);

        // 混色测试
        int ignisColor = ignisVector.calculateBlendedColor();
        assertTrue(ignisColor != 0);

        System.out.println("PASS");
    }

    private static void testAlchemicalPotionItemDeliveries() {
        System.out.print("[TEST 7/9] Alchemical Potion Items (Elixir & Incense)... ");

        AlchemicalPotionItem elixir = new AlchemicalPotionItem(new Item.Properties(), AlchemicalPotionItem.DeliveryType.ELIXIR);
        AlchemicalPotionItem incense = new AlchemicalPotionItem(new Item.Properties(), AlchemicalPotionItem.DeliveryType.INCENSE);

        AspectVector vitaVector = AspectVector.builder().add(AlchemyAspect.VITA, 8.0).build();
        ItemStack elixirStack = AlchemicalPotionItem.create(elixir, vitaVector, 15.0, 0.85, 1.0);
        ItemStack incenseStack = AlchemicalPotionItem.create(incense, vitaVector, 15.0, 0.85, 1.0);

        TestLevel level = new TestLevel();
        level.isClientSide = false;
        Player player = new Player();
        player.level = level;

        // 1. 验证灵药饮用
        ItemStack resultStack = elixir.finishUsingItem(elixirStack, level, player);
        assertEquals(Items.GLASS_BOTTLE, resultStack.getItem());
        assertFalse(player.getActiveEffects().isEmpty());
        assertEquals(MobEffects.REGENERATION, player.getActiveEffects().iterator().next().getEffect());

        // 2. 验证熏香释放
        incense.finishUsingItem(incenseStack, level, player);
        boolean foundCloud = false;
        for (Entity e : level.getEntities()) {
            if (e instanceof AreaEffectCloud cloud) {
                foundCloud = true;
                assertEquals(3.5f, cloud.getRadius(), 1e-4f);
                assertEquals(240, cloud.getDuration());
                assertFalse(cloud.getEffects().isEmpty());
            }
        }
        assertTrue(foundCloud);

        // 3. Tooltip 文本测试
        List<Component> tooltip = new ArrayList<>();
        elixir.appendHoverText(elixirStack, level, tooltip, TooltipFlag.NORMAL);
        assertFalse(tooltip.isEmpty());

        System.out.println("PASS");
    }

    private static void testBladeOilApplicationAndCombat() {
        System.out.print("[TEST 8/9] Blade Oil Coating & Monster Bane Interception... ");

        BladeOilItem oilItem = new BladeOilItem(new Item.Properties());
        ItemStack oilStack = BladeOilItem.create(oilItem, BladeOilItem.BaneType.NECROPHAGE, 5.0f, 30);

        SwordItem swordItem = new SwordItem(new Item.Properties());
        ItemStack weaponStack = new ItemStack(swordItem);

        // 严密武器校验测试 (剑与斧合法，普通杂物非法)
        assertTrue(BladeOilItem.isValidWeapon(weaponStack));
        assertTrue(BladeOilItem.isValidWeapon(new ItemStack(new AxeItem(new Item.Properties()))));
        assertFalse(BladeOilItem.isValidWeapon(new ItemStack(Items.GLASS_BOTTLE)));

        TestLevel level = new TestLevel();
        level.isClientSide = false;
        Player player = new Player();
        player.level = level;
        player.setItemInHand(InteractionHand.MAIN_HAND, weaponStack);
        player.setItemInHand(InteractionHand.OFF_HAND, oilStack);

        // 1. 涂抹剑油
        oilItem.use(level, player, InteractionHand.OFF_HAND);
        assertTrue(weaponStack.hasTag());
        CompoundTag oilTag = weaponStack.getTag().getCompound(BladeOilItem.TAG_BLADE_OIL);
        assertEquals(30, oilTag.getInt(BladeOilItem.TAG_CHARGES));
        assertEquals(BladeOilItem.BaneType.NECROPHAGE.getId(), oilTag.getString(BladeOilItem.TAG_BANE_TYPE));

        // 2. 命中克制目标 (UNDEAD)
        LivingEntity zombie = new LivingEntity();
        zombie.level = level;
        zombie.setMobType(MobType.UNDEAD);

        float extraDamage = BladeOilItem.onWeaponHitEntity(weaponStack, player, zombie);
        assertEquals(5.0f, extraDamage, 1e-4f);
        assertEquals(4, zombie.secondsOnFire); // 亡灵附加 4 秒灼烧
        assertEquals(29, weaponStack.getTag().getCompound(BladeOilItem.TAG_BLADE_OIL).getInt(BladeOilItem.TAG_CHARGES));

        // 3. 命中非克制目标 (ARTHROPOD)
        LivingEntity spider = new LivingEntity();
        spider.level = level;
        spider.setMobType(MobType.ARTHROPOD);

        float partialDamage = BladeOilItem.onWeaponHitEntity(weaponStack, player, spider);
        assertEquals(5.0f * 0.30f, partialDamage, 1e-4f); // 30% 腐蚀伤害
        assertEquals(28, weaponStack.getTag().getCompound(BladeOilItem.TAG_BLADE_OIL).getInt(BladeOilItem.TAG_CHARGES));

        // 4. 模拟消耗殆尽与 NBT 自动剥离
        weaponStack.getTag().getCompound(BladeOilItem.TAG_BLADE_OIL).putInt(BladeOilItem.TAG_CHARGES, 1);
        BladeOilItem.onWeaponHitEntity(weaponStack, player, zombie);
        assertFalse(weaponStack.getTag().contains(BladeOilItem.TAG_BLADE_OIL));

        System.out.println("PASS");
    }

    private static void testToxicitySyncPacketBinaryRoundtrip() {
        System.out.print("[TEST 9/9] Zero-Lag 9-Byte Binary Packet Roundtrip... ");

        ToxicitySyncPacket packet = new ToxicitySyncPacket(68.5f, 100.0f, (byte) ToxicityThreshold.CAUTION.ordinal());
        FriendlyByteBuf buf = new FriendlyByteBuf();
        packet.encode(buf);

        assertEquals(9, buf.toByteArray().length); // 4 + 4 + 1 = 9 bytes

        ToxicitySyncPacket decoded = ToxicitySyncPacket.decode(buf);
        assertEquals(68.5f, decoded.toxicity(), 1e-4f);
        assertEquals(100.0f, decoded.maxToxicity(), 1e-4f);
        assertEquals(ToxicityThreshold.CAUTION, decoded.getDecodedThreshold());

        System.out.println("PASS");
    }

    private static void assertEquals(float expected, float actual, float delta) {
        if (Math.abs(expected - actual) > delta) {
            throw new AssertionError(String.format("Expected %.5f, but got %.5f (diff: %.5f)", expected, actual, Math.abs(expected - actual)));
        }
    }

    private static void assertEquals(double expected, double actual, double delta) {
        if (Math.abs(expected - actual) > delta) {
            throw new AssertionError(String.format("Expected %.5f, but got %.5f", expected, actual));
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
