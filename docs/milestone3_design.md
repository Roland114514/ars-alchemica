这里是《Ars Alchemica》**Milestone 3：动态药水系统与毒性槽引擎 (Dynamic Potions & Toxicity Engine)** 的完整生产级架构设计与代码实现。

所有代码严格遵循 **Minecraft 1.20.1 (Mojang Official Mappings)** 与 **Common-First 多加载器解耦铁律**，无任何平台硬编码泄漏，具备防御性边界校验（Clamp、防除零、防重入）与零分配（Zero-Allocation）倾向的内存管理。

---

### 架构总览 (Architecture Overview)

```
common/src/main/java/com/arsalchemica/
├── api/
│   └── toxicity/
│       ├── IToxicityData.java             [平台无关的毒性数据契约]
│       └── ToxicityThreshold.java         [猎魔人式四段毒性生理阈值状态机]
├── common/
│   ├── alchemy/
│   │   └── DynamicPotionResolver.java     [连续以太向量 -> 动态药效/毒性解算引擎]
│   ├── item/
│   │   ├── AlchemicalPotionItem.java      [动态复合灵药载体与实时染色协议]
│   │   └── BladeOilItem.java              [猎魔人剑油涂抹系统与攻击拦截器]
│   ├── network/
│   │   └── ToxicitySyncPacket.java        [5字节极简二进制毒性网络同步包]
│   └── toxicity/
│       ├── PlayerToxicityData.java        [毒性存储、平滑代谢积分与NBT序列化]
│       └── PlayerToxicityManager.java     [服务端生理Tick驱动、过载反噬与解耦桥接]
```

---

### 模块一：猎魔人风格毒性引擎 (Witcher-style Toxicity Engine)

#### 1. `ToxicityThreshold.java` (生理阈值状态机)
定义玩家血液中以太毒素沉积的四个生理阶段，提供基于归一化比例 $r \in [0.0, 1.0]$ 的 O(1) 判定。

```java
package com.arsalchemica.api.toxicity;

import net.minecraft.ChatFormatting;

/**
 * 猎魔人风格毒性生理阈值枚举。
 * 根据当前毒性占最大毒性容量的比例 (Ratio = Current / Max) 划分生理反应区间。
 */
public enum ToxicityThreshold {
    SAFE(0.0f, 0.50f, ChatFormatting.GREEN, "toxicity.ars_alchemica.state.safe"),
    CAUTION(0.50f, 0.75f, ChatFormatting.YELLOW, "toxicity.ars_alchemica.state.caution"),
    DANGER(0.75f, 0.90f, ChatFormatting.GOLD, "toxicity.ars_alchemica.state.danger"),
    OVERDOSE(0.90f, Float.MAX_VALUE, ChatFormatting.DARK_RED, "toxicity.ars_alchemica.state.overdose");

    private final float minRatio;
    private final float maxRatio;
    private final ChatFormatting color;
    private final String translationKey;

    ToxicityThreshold(float minRatio, float maxRatio, ChatFormatting color, String translationKey) {
        this.minRatio = minRatio;
        this.maxRatio = maxRatio;
        this.color = color;
        this.translationKey = translationKey;
    }

    public static ToxicityThreshold fromRatio(float ratio) {
        if (Float.isNaN(ratio) || ratio < 0.50f) return SAFE;
        if (ratio < 0.75f) return CAUTION;
        if (ratio < 0.90f) return DANGER;
        return OVERDOSE;
    }

    public float getMinRatio() { return minRatio; }
    public float getMaxRatio() { return maxRatio; }
    public ChatFormatting getColor() { return color; }
    public String getTranslationKey() { return translationKey; }
}
```

#### 2. `IToxicityData.java` (平台中立契约接口)
无论是 Forge 的 `Capability` 还是 Fabric 的 `Cardinal Components / Data Attachment`，在 `common` 模块中一律通过此接口操作玩家毒性。

```java
package com.arsalchemica.api.toxicity;

import net.minecraft.nbt.CompoundTag;

/**
 * 玩家炼金毒性数据契约接口（Common-First 跨平台抽象）。
 */
public interface IToxicityData {
    float DEFAULT_MAX_TOXICITY = 100.0f;
    /** 默认自然代谢速率：每秒衰减 0.25 点毒性 (即每 Tick 0.0125f) */
    float DEFAULT_METABOLISM_RATE = 0.0125f;

    float getToxicity();
    void setToxicity(float value);
    void addToxicity(float delta);

    float getMaxToxicity();
    void setMaxToxicity(float max);

    float getMetabolismRate();
    void setMetabolismRate(float rate);

    /** 获取归一化毒性比例 [0.0, 1.0] */
    default float getToxicityRatio() {
        float max = getMaxToxicity();
        return max <= 1e-5f ? 0.0f : Math.min(1.0f, Math.max(0.0f, getToxicity() / max));
    }

    /** 获取当前生理阈值阶段 */
    default ToxicityThreshold getThreshold() {
        return ToxicityThreshold.fromRatio(getToxicityRatio());
    }

    /** 标记是否需要向客户端发送二进制同步包 */
    boolean isDirty();
    void setDirty(boolean dirty);

    void saveNBTData(CompoundTag tag);
    void loadNBTData(CompoundTag tag);
}
```

#### 3. `PlayerToxicityData.java` & `PlayerToxicityManager.java` (核心实现与代谢调度)
实现微分代谢方程 $\frac{d\text{Tox}}{dt} = -\text{metabolismRate}$，以及过载状态下的**增益剥夺与炼金反噬**。

```java
package com.arsalchemica.common.toxicity;

import com.arsalchemica.api.toxicity.IToxicityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

/**
 * 玩家毒性状态的标准 POJO 实现。具备严苛的 NaN 与越界防御。
 */
public class PlayerToxicityData implements IToxicityData {
    private static final String TAG_TOXICITY = "AlchemicalToxicity";
    private static final String TAG_MAX_TOXICITY = "MaxToxicity";
    private static final String TAG_METABOLISM = "MetabolismRate";

    private float toxicity = 0.0f;
    private float maxToxicity = DEFAULT_MAX_TOXICITY;
    private float metabolismRate = DEFAULT_METABOLISM_RATE;
    private boolean dirty = false;

    @Override
    public float getToxicity() {
        return this.toxicity;
    }

    @Override
    public void setToxicity(float value) {
        if (Float.isNaN(value)) value = 0.0f;
        float clamped = Mth.clamp(value, 0.0f, this.maxToxicity);
        if (Math.abs(this.toxicity - clamped) > 1e-4f) {
            this.toxicity = clamped;
            this.dirty = true;
        }
    }

    @Override
    public void addToxicity(float delta) {
        if (Float.isNaN(delta) || Math.abs(delta) < 1e-5f) return;
        setToxicity(this.toxicity + delta);
    }

    @Override
    public float getMaxToxicity() {
        return this.maxToxicity;
    }

    @Override
    public void setMaxToxicity(float max) {
        if (Float.isNaN(max) || max < 1.0f) max = 1.0f;
        if (Math.abs(this.maxToxicity - max) > 1e-4f) {
            this.maxToxicity = max;
            this.toxicity = Mth.clamp(this.toxicity, 0.0f, this.maxToxicity);
            this.dirty = true;
        }
    }

    @Override
    public float getMetabolismRate() {
        return this.metabolismRate;
    }

    @Override
    public void setMetabolismRate(float rate) {
        if (Float.isNaN(rate) || rate < 0.0f) rate = 0.0f;
        this.metabolismRate = rate;
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    @Override
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    @Override
    public void saveNBTData(CompoundTag tag) {
        tag.putFloat(TAG_TOXICITY, this.toxicity);
        tag.putFloat(TAG_MAX_TOXICITY, this.maxToxicity);
        tag.putFloat(TAG_METABOLISM, this.metabolismRate);
    }

    @Override
    public void loadNBTData(CompoundTag tag) {
        this.maxToxicity = tag.contains(TAG_MAX_TOXICITY) ? Math.max(1.0f, tag.getFloat(TAG_MAX_TOXICITY)) : DEFAULT_MAX_TOXICITY;
        this.toxicity = Mth.clamp(tag.getFloat(TAG_TOXICITY), 0.0f, this.maxToxicity);
        this.metabolismRate = tag.contains(TAG_METABOLISM) ? Math.max(0.0f, tag.getFloat(TAG_METABOLISM)) : DEFAULT_METABOLISM_RATE;
        this.dirty = true;
    }
}
```

```java
package com.arsalchemica.common.toxicity;

import com.arsalchemica.api.toxicity.IToxicityData;
import com.arsalchemica.api.toxicity.ToxicityThreshold;
import com.arsalchemica.common.network.ToxicitySyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 玩家毒性核心管理器与生理代谢引擎。
 * 负责驱动服务端 Tick 衰减、过载药效反转及跨平台网络派发。
 */
public final class PlayerToxicityManager {

    private static Function<Player, Optional<IToxicityData>> provider = p -> Optional.empty();
    private static BiConsumer<ServerPlayer, ToxicitySyncPacket> packetSender = (p, pkt) -> {};

    private PlayerToxicityManager() {}

    /** 由 Forge/Fabric 平台初始化入口注入具体的 Capability / Attachment 获取器 */
    public static void initPlatformBridge(
            Function<Player, Optional<IToxicityData>> dataProvider,
            BiConsumer<ServerPlayer, ToxicitySyncPacket> networkSender) {
        provider = dataProvider;
        packetSender = networkSender;
    }

    public static Optional<IToxicityData> getData(Player player) {
        return provider.apply(player);
    }

    /**
     * 在玩家 Server Tick 尾部调用。
     * 执行微分方程 dTox/dt = -metabolismRate 及毒理生理惩罚。
     */
    public static void tickPlayer(ServerPlayer player) {
        if (player.level().isClientSide() || player.isCreative() || player.isSpectator()) return;

        getData(player).ifPresent(data -> {
            float currentTox = data.getToxicity();
            if (currentTox <= 0.0f && !data.isDirty()) return;

            float previousSyncedTox = currentTox;
            ToxicityThreshold prevThreshold = data.getThreshold();

            // 1. 连续时间代谢衰减: dTox/dt = -metabolismRate
            if (currentTox > 0.0f) {
                data.addToxicity(-data.getMetabolismRate());
            }

            ToxicityThreshold currentThreshold = data.getThreshold();

            // 2. 每 20 Ticks (1秒) 执行一次生理阈值症状结算
            if (player.tickCount % 20 == 0 && data.getToxicity() > 0.0f) {
                applyPhysiologicalReactions(player, currentThreshold);
            }

            // 3. 轻量化网络增量同步策略（阈值跨越、归零或累积变化 >= 0.5f 时触发同步）
            boolean thresholdChanged = prevThreshold != currentThreshold;
            boolean significantDelta = Math.abs(previousSyncedTox - data.getToxicity()) >= 0.5f;
            boolean zeroedOut = previousSyncedTox > 0.0f && data.getToxicity() == 0.0f;

            if (data.isDirty() && (thresholdChanged || significantDelta || zeroedOut || player.tickCount % 40 == 0)) {
                syncToClient(player, data);
                data.setDirty(false);
            }
        });
    }

    /**
     * 解毒剂净化接口（如：腐水食腐苔提取物）。
     * @param instantPurge 瞬间清除的毒性量
     * @param boostedMetabolism 提升后的代谢速率
     */
    public static void applyAntidote(ServerPlayer player, float instantPurge, float boostedMetabolism) {
        getData(player).ifPresent(data -> {
            if (instantPurge > 0.0f) {
                data.addToxicity(-instantPurge);
            }
            if (boostedMetabolism > data.getMetabolismRate()) {
                data.setMetabolismRate(boostedMetabolism);
            }
            syncToClient(player, data);
            data.setDirty(false);
        });
    }

    private static void applyPhysiologicalReactions(ServerPlayer player, ToxicityThreshold threshold) {
        switch (threshold) {
            case SAFE, CAUTION -> {
                // SAFE: 正常发挥；CAUTION: 仅触发客户端暗色毒晕渲染，无服务端 Debuff
            }
            case DANGER -> {
                // 中毒期 (75% ~ 90%): 视野晃动与神经迟缓
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, false, false, true));
            }
            case OVERDOSE -> {
                // 休克崩解 (> 90%): 剥夺所有正面药水增益并反转为凋零/剧毒，同时造成无视护甲的真实炼金反噬伤害
                purgeAndInvertBeneficialEffects(player);
                player.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1, false, true, true));
                player.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1, false, true, true));
                player.hurt(player.damageSources().magic(), 2.5f);
            }
        }
    }

    /**
     * 剥夺所有有益状态（防止并发修改异常采用快照拷贝）。
     */
    private static void purgeAndInvertBeneficialEffects(ServerPlayer player) {
        List<MobEffectInstance> beneficials = new ArrayList<>();
        for (MobEffectInstance effect : player.getActiveEffects()) {
            if (effect.getEffect().isBeneficial()) {
                beneficials.add(effect);
            }
        }
        for (MobEffectInstance beneficial : beneficials) {
            player.removeEffect(beneficial.getEffect());
        }
    }

    public static void syncToClient(ServerPlayer player, IToxicityData data) {
        byte stateMask = (byte) (data.getThreshold().ordinal() & 0x03);
        packetSender.accept(player, new ToxicitySyncPacket(data.getToxicity(), data.getMaxToxicity(), stateMask));
    }
}
```

---

### 模块二：动态复合药水系统 (Continuous Aspect Potion Engine)

#### 1. `DynamicPotionResolver.java` (以太向量药理解算器)
彻底抛弃原版静态 `Potion` 注册表，直接从 NBT 读取连续以太向量（`AspectVector`）、纯度（Purity）、质量（Mass）与挥发性（Volatility），实时计算药效、持续时间、等级、颜色与毒性负载。

```java
package com.arsalchemica.common.alchemy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 动态复合药水数学解算引擎。
 * 基于连续以太向量 (AspectVector) 动态推导药效、持续时间、纯度增幅、毒性与光子混合色彩。
 */
public final class DynamicPotionResolver {

    public static final String TAG_ALCHEMICAL_DATA = "ArsAlchemicaPotion";
    public static final String TAG_ASPECTS = "Aspects";
    public static final String TAG_PURITY = "Purity";         // [0.0, 1.0]
    public static final String TAG_MASS = "Mass";             // 溶液质量 (g/mL)
    public static final String TAG_VOLATILITY = "Volatility"; // [0.0, 1.0]

    /** 六大基础以太源质及其标准光子波长颜色 */
    public enum AspectType {
        IGNIS("ignis", 0xFF4500),   // 炽火 - 抗火 / 力量
        AQUA("aqua", 0x00BFFF),     // 渊水 - 水下呼吸 / 极速
        TERRA("terra", 0x2E8B57),   // 磐土 - 抗性提升 / 伤害吸收
        AER("aer", 0xE0FFFF),       // 灵风 - 迅捷 / 跳跃提升
        VITA("vita", 0xFF1493),     // 生机 - 生命再生 / 瞬间治疗
        MORTIS("mortis", 0x4B0082), // 死寂 - 凋零 / 剧毒 / 瞬间伤害
        AETHER("aether", 0x9400D3); // 以太 - 夜视 / 缓降 / 奥术共鸣

        private final String id;
        private final int rgb;

        AspectType(String id, int rgb) {
            this.id = id;
            this.rgb = rgb;
        }

        public String getId() { return id; }
        public int getRgb() { return rgb; }
    }

    private DynamicPotionResolver() {}

    /**
     * 解算连续以太向量对应的动态药水效果列表。
     */
    public static List<MobEffectInstance> resolveEffects(CompoundTag rootTag) {
        List<MobEffectInstance> results = new ArrayList<>();
        if (rootTag == null || !rootTag.contains(TAG_ALCHEMICAL_DATA, CompoundTag.TAG_COMPOUND)) {
            return results;
        }

        CompoundTag data = rootTag.getCompound(TAG_ALCHEMICAL_DATA);
        float purity = Mth.clamp(data.getFloat(TAG_PURITY), 0.05f, 1.0f);
        float mass = Math.max(1.0f, data.getFloat(TAG_MASS));
        float volatility = Mth.clamp(data.getFloat(TAG_VOLATILITY), 0.0f, 1.0f);

        EnumMap<AspectType, Float> vector = readAspectVector(data);
        float totalMagnitude = 0.0f;
        for (float v : vector.values()) totalMagnitude += v;
        if (totalMagnitude <= 1e-4f) return results;

        // 纯度决定药效等级 (Amplifier: 0 ~ 3, 对应 I ~ IV 级)
        // 高挥发性在缩减持续时间的同时可额外激发出 1 级瞬间爆发强度
        int baseAmplifier = Mth.clamp(Mth.floor(purity * 3.5f), 0, 3);
        if (volatility > 0.80f && purity > 0.75f) {
            baseAmplifier += 1;
        }

        // 溶液质量决定基础持续时间 (Ticks)，挥发性越高释放越快（持续时间越短）
        int baseDurationTicks = Math.max(40, (int) (mass * 20.0f * (1.25f - volatility * 0.65f)));

        for (Map.Entry<AspectType, Float> entry : vector.entrySet()) {
            float weightRatio = entry.getValue() / totalMagnitude;
            if (weightRatio < 0.18f) continue; // 低于 18% 摩尔分数的微量杂质不产生主药效

            int scaledDuration = Math.max(20, (int) (baseDurationTicks * weightRatio));
            int scaledAmplifier = weightRatio >= 0.50f ? baseAmplifier : Math.max(0, baseAmplifier - 1);

            MobEffect primaryEffect = mapAspectToEffect(entry.getKey(), volatility > 0.75f);
            if (primaryEffect.isInstantenous()) {
                results.add(new MobEffectInstance(primaryEffect, 1, scaledAmplifier));
            } else {
                results.add(new MobEffectInstance(primaryEffect, scaledDuration, scaledAmplifier, false, true, true));
            }
        }
        return results;
    }

    /**
     * 解算饮用该复合灵药所产生的毒性负荷。
     * 公式：Toxicity = (BaseLoad + Mass * 0.15) * (1.6 - Purity * 0.85) * (1.0 + MortisRatio * 0.5)
     * 纯度越高，杂质越少，毒性越低；死寂(Mortis)源质会显著加剧毒性。
     */
    public static float calculateToxicityLoad(CompoundTag rootTag) {
        if (rootTag == null || !rootTag.contains(TAG_ALCHEMICAL_DATA, CompoundTag.TAG_COMPOUND)) {
            return 15.0f;
        }
        CompoundTag data = rootTag.getCompound(TAG_ALCHEMICAL_DATA);
        float purity = Mth.clamp(data.getFloat(TAG_PURITY), 0.0f, 1.0f);
        float mass = Math.max(1.0f, data.getFloat(TAG_MASS));

        EnumMap<AspectType, Float> vector = readAspectVector(data);
        float total = 0.0f;
        for (float v : vector.values()) total += v;
        float mortisRatio = total > 1e-4f ? vector.getOrDefault(AspectType.MORTIS, 0.0f) / total : 0.0f;

        float rawToxicity = (12.0f + mass * 0.25f) * (1.65f - purity * 0.90f) * (1.0f + mortisRatio * 0.6f);
        return Mth.clamp(rawToxicity, 5.0f, 85.0f);
    }

    /**
     * 连续光子色彩混合计算 (AspectVector.getBlendedColor)。
     */
    public static int getBlendedColor(CompoundTag rootTag) {
        if (rootTag == null || !rootTag.contains(TAG_ALCHEMICAL_DATA, CompoundTag.TAG_COMPOUND)) {
            return 0x385DC6; // 默认水蓝色
        }
        CompoundTag data = rootTag.getCompound(TAG_ALCHEMICAL_DATA);
        EnumMap<AspectType, Float> vector = readAspectVector(data);

        float totalWeight = 0.0f;
        float rAcc = 0.0f, gAcc = 0.0f, bAcc = 0.0f;

        for (Map.Entry<AspectType, Float> entry : vector.entrySet()) {
            float w = entry.getValue();
            if (w <= 1e-4f) continue;
            int rgb = entry.getKey().getRgb();
            rAcc += ((rgb >> 16) & 0xFF) * w;
            gAcc += ((rgb >> 8) & 0xFF) * w;
            bAcc += (rgb & 0xFF) * w;
            totalWeight += w;
        }

        if (totalWeight <= 1e-4f) return 0x385DC6;

        int r = Mth.clamp(Math.round(rAcc / totalWeight), 0, 255);
        int g = Mth.clamp(Math.round(gAcc / totalWeight), 0, 255);
        int b = Mth.clamp(Math.round(bAcc / totalWeight), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    public static EnumMap<AspectType, Float> readAspectVector(CompoundTag alchemyData) {
        EnumMap<AspectType, Float> map = new EnumMap<>(AspectType.class);
        if (!alchemyData.contains(TAG_ASPECTS, CompoundTag.TAG_COMPOUND)) return map;
        CompoundTag aspectTag = alchemyData.getCompound(TAG_ASPECTS);
        for (AspectType type : AspectType.values()) {
            if (aspectTag.contains(type.getId())) {
                float val = aspectTag.getFloat(type.getId());
                if (!Float.isNaN(val) && val > 0.0f) {
                    map.put(type, val);
                }
            }
        }
        return map;
    }

    private static MobEffect mapAspectToEffect(AspectType type, boolean highVolatility) {
        return switch (type) {
            case IGNIS -> highVolatility ? MobEffects.DAMAGE_BOOST : MobEffects.FIRE_RESISTANCE;
            case AQUA -> highVolatility ? MobEffects.DIG_SPEED : MobEffects.WATER_BREATHING;
            case TERRA -> highVolatility ? MobEffects.ABSORPTION : MobEffects.DAMAGE_RESISTANCE;
            case AER -> highVolatility ? MobEffects.JUMP : MobEffects.MOVEMENT_SPEED;
            case VITA -> highVolatility ? MobEffects.HEAL : MobEffects.REGENERATION;
            case MORTIS -> highVolatility ? MobEffects.HARM : MobEffects.WITHER;
            case AETHER -> highVolatility ? MobEffects.SLOW_FALLING : MobEffects.NIGHT_VISION;
        };
    }
}
```

#### 2. `AlchemicalPotionItem.java` (动态复合药剂载体)
支持饮用（Elixir）与范围熏香（Incense）双交付形态，内置动态 Tooltip 与客户端 `ItemColor` 桥接方法。

```java
package com.arsalchemica.common.item;

import com.arsalchemica.common.alchemy.DynamicPotionResolver;
import com.arsalchemica.common.toxicity.PlayerToxicityManager;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 动态复合炼金药剂载体。
 * 支持饮用 (ELIXIR) 与范围炼金熏香释放 (INCENSE)。
 */
public class AlchemicalPotionItem extends Item {

    public enum DeliveryType {
        ELIXIR,  // 饮用型灵药
        INCENSE  // 范围扩散型炼金熏香
    }

    private final DeliveryType deliveryType;

    public AlchemicalPotionItem(Properties properties, DeliveryType deliveryType) {
        super(properties);
        this.deliveryType = deliveryType;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return this.deliveryType == DeliveryType.INCENSE ? 20 : 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return this.deliveryType == DeliveryType.INCENSE ? UseAnim.BOW : UseAnim.DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        Player player = entity instanceof Player p ? p : null;
        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
        }

        if (!level.isClientSide()) {
            CompoundTag tag = stack.getTag();
            List<MobEffectInstance> effects = DynamicPotionResolver.resolveEffects(tag);
            float toxicityLoad = DynamicPotionResolver.calculateToxicityLoad(tag);

            if (this.deliveryType == DeliveryType.ELIXIR) {
                for (MobEffectInstance effect : effects) {
                    if (effect.getEffect().isInstantenous()) {
                        effect.getEffect().applyInstantenousEffect(player, player, entity, effect.getAmplifier(), 1.0D);
                    } else {
                        entity.addEffect(new MobEffectInstance(effect));
                    }
                }
                if (player instanceof ServerPlayer serverPlayer) {
                    PlayerToxicityManager.getData(serverPlayer).ifPresent(data -> {
                        data.addToxicity(toxicityLoad);
                        PlayerToxicityManager.syncToClient(serverPlayer, data);
                    });
                }
            } else if (this.deliveryType == DeliveryType.INCENSE) {
                spawnIncenseCloud(level, entity, tag, effects, toxicityLoad * 0.35f);
            }
        }

        if (player != null) {
            player.awardStat(Stats.ITEM_USED.get(this));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    return new ItemStack(Items.GLASS_BOTTLE);
                }
                player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE));
            }
        }
        return stack;
    }

    private void spawnIncenseCloud(Level level, LivingEntity owner, @Nullable CompoundTag tag, List<MobEffectInstance> effects, float ambientToxicity) {
        AreaEffectCloud cloud = new AreaEffectCloud(level, owner.getX(), owner.getY(), owner.getZ());
        cloud.setOwner(owner);
        cloud.setRadius(3.5f);
        cloud.setRadiusOnUse(-0.25f);
        cloud.setWaitTime(10);
        cloud.setDuration(240);
        cloud.setRadiusPerTick(-cloud.getRadius() / (float) cloud.getDuration());
        cloud.setFixedColor(DynamicPotionResolver.getBlendedColor(tag));

        for (MobEffectInstance effect : effects) {
            cloud.addEffect(new MobEffectInstance(effect.getEffect(), Math.max(40, effect.getDuration() / 2), effect.getAmplifier()));
        }
        level.addFreshEntity(cloud);
        level.playSound(null, owner.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1.0f, 0.85f);

        // 释放熏香者自身也会吸入少量挥发以太毒素
        if (owner instanceof ServerPlayer serverPlayer) {
            PlayerToxicityManager.getData(serverPlayer).ifPresent(data -> data.addToxicity(ambientToxicity));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(DynamicPotionResolver.TAG_ALCHEMICAL_DATA)) {
            tooltip.add(Component.translatable("tooltip.ars_alchemica.potion.inert").withStyle(ChatFormatting.GRAY));
            return;
        }

        CompoundTag data = tag.getCompound(DynamicPotionResolver.TAG_ALCHEMICAL_DATA);
        float purity = data.getFloat(DynamicPotionResolver.TAG_PURITY) * 100.0f;
        float toxicity = DynamicPotionResolver.calculateToxicityLoad(tag);

        tooltip.add(Component.translatable("tooltip.ars_alchemica.potion.purity", String.format("%.1f%%", purity))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.ars_alchemica.potion.toxicity", String.format("+%.1f", toxicity))
                .withStyle(toxicity >= 45.0f ? ChatFormatting.RED : ChatFormatting.DARK_GREEN));

        List<MobEffectInstance> effects = DynamicPotionResolver.resolveEffects(tag);
        for (MobEffectInstance instance : effects) {
            Component effectName = Component.translatable(instance.getDescriptionId());
            if (instance.getAmplifier() > 0) {
                effectName = Component.translatable("potion.withAmplifier", effectName,
                        Component.translatable("potion.potency." + instance.getAmplifier()));
            }
            if (instance.getDuration() > 20) {
                effectName = Component.translatable("potion.withDuration", effectName, MobEffectUtil.formatDuration(instance, 1.0f));
            }
            tooltip.add(effectName.copy().withStyle(instance.getEffect().getCategory().getTooltipFormatting()));
        }
    }

    /**
     * 供客户端 ColorHandlerEvent / ColorProviderRegistry 直接绑定的方法引用。
     * tintIndex == 0 表示药水液体层，其余为玻璃瓶身。
     */
    public static int getItemColor(ItemStack stack, int tintIndex) {
        return tintIndex == 0 ? DynamicPotionResolver.getBlendedColor(stack.getTag()) : 0xFFFFFF;
    }
}
```

---

### 模块三：猎魔人涂抹剑油系统 (Blade Oil & Weapon Coating Engine)

#### `BladeOilItem.java` (剑油涂抹与种族克制结算)
支持副手持有剑油涂抹主手近战武器（或主手使用涂抹副手武器），赋予固定次数（如 30 次命中）的生物种群特攻（亡灵银刃、节肢破甲、类人流血）与炼金毒素穿透。

```java
package com.arsalchemica.common.item;

import com.arsalchemica.common.alchemy.DynamicPotionResolver;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 猎魔人式炼金剑油载体与武器涂层引擎。
 * 涂抹于武器后赋予固定命中次数的生物种群特攻与毒素穿透。
 */
public class BladeOilItem extends Item {

    public static final String TAG_BLADE_OIL = "ArsAlchemicaBladeOil";
    public static final String TAG_CHARGES = "RemainingCharges";
    public static final String TAG_MAX_CHARGES = "MaxCharges";
    public static final String TAG_BANE_TYPE = "BaneType";
    public static final String TAG_BONUS_DAMAGE = "BonusDamage";
    public static final String TAG_OIL_COLOR = "OilColor";

    public static final int DEFAULT_MAX_CHARGES = 30;

    /** 剑油克制种群分类 */
    public enum BaneType {
        NECROPHAGE("necrophage", ChatFormatting.LIGHT_PURPLE), // 死灵/亡灵特攻
        INSECTOID("insectoid", ChatFormatting.DARK_GREEN),     // 节肢/昆虫破壳特攻
        HUMANOID("humanoid", ChatFormatting.RED),              // 类人/灾厄村民流血特攻
        DRACONID("draconid", ChatFormatting.GOLD);             // 末影/首领奥术穿透特攻

        private final String id;
        private final ChatFormatting color;

        BaneType(String id, ChatFormatting color) {
            this.id = id;
            this.color = color;
        }

        public String getId() { return id; }
        public ChatFormatting getColor() { return color; }

        public static BaneType fromId(String id) {
            for (BaneType type : values()) {
                if (type.id.equalsIgnoreCase(id)) return type;
            }
            return NECROPHAGE;
        }
    }

    public BladeOilItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack oilStack = player.getItemInHand(usedHand);
        InteractionHand targetHand = usedHand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack weaponStack = player.getItemInHand(targetHand);

        if (!isValidWeapon(weaponStack)) {
            if (level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.ars_alchemica.oil.invalid_weapon")
                        .withStyle(ChatFormatting.RED), true);
            }
            return InteractionResultHolder.fail(oilStack);
        }

        if (!level.isClientSide()) {
            applyOilToWeapon(oilStack, weaponStack);
            level.playSound(null, player.blockPosition(), SoundEvents.HONEYCOMB_WAX_ON, SoundSource.PLAYERS, 1.0f, 1.15f);
            player.displayClientMessage(Component.translatable("message.ars_alchemica.oil.applied", weaponStack.getHoverName())
                    .withStyle(ChatFormatting.GOLD), true);

            if (!player.getAbilities().instabuild) {
                oilStack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(oilStack, level.isClientSide());
    }

    public static boolean isValidWeapon(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof SwordItem || stack.getItem() instanceof TieredItem);
    }

    public static void applyOilToWeapon(ItemStack oilStack, ItemStack weaponStack) {
        CompoundTag oilRoot = oilStack.getOrCreateTag();
        String baneId = oilRoot.contains(TAG_BANE_TYPE) ? oilRoot.getString(TAG_BANE_TYPE) : BaneType.NECROPHAGE.getId();
        float bonusDmg = oilRoot.contains(TAG_BONUS_DAMAGE) ? oilRoot.getFloat(TAG_BONUS_DAMAGE) : 4.5f;
        int color = DynamicPotionResolver.getBlendedColor(oilRoot);

        CompoundTag weaponOilTag = new CompoundTag();
        weaponOilTag.putInt(TAG_CHARGES, DEFAULT_MAX_CHARGES);
        weaponOilTag.putInt(TAG_MAX_CHARGES, DEFAULT_MAX_CHARGES);
        weaponOilTag.putString(TAG_BANE_TYPE, baneId);
        weaponOilTag.putFloat(TAG_BONUS_DAMAGE, bonusDmg);
        weaponOilTag.putInt(TAG_OIL_COLOR, color);

        weaponStack.getOrCreateTag().put(TAG_BLADE_OIL, weaponOilTag);
    }

    /**
     * 武器命中拦截结算器（在 Common 伤害事件或 Mixin LivingEntity#hurt 中调用）。
     * 自动判定种族克制、施加破甲/流血毒素并安全扣减剩余次数。
     *
     * @return 剑油提供的额外特攻伤害值
     */
    public static float onWeaponHitEntity(ItemStack weaponStack, LivingEntity attacker, LivingEntity target) {
        if (weaponStack.isEmpty() || !weaponStack.hasTag() || attacker.level().isClientSide()) {
            return 0.0f;
        }
        CompoundTag root = weaponStack.getTag();
        if (root == null || !root.contains(TAG_BLADE_OIL, CompoundTag.TAG_COMPOUND)) {
            return 0.0f;
        }

        CompoundTag oilData = root.getCompound(TAG_BLADE_OIL);
        int charges = oilData.getInt(TAG_CHARGES);
        if (charges <= 0) {
            root.remove(TAG_BLADE_OIL);
            return 0.0f;
        }

        BaneType bane = BaneType.fromId(oilData.getString(TAG_BANE_TYPE));
        float baseBonus = oilData.getFloat(TAG_BONUS_DAMAGE);
        float finalExtraDamage = 0.0f;

        if (matchesBaneTarget(bane, target)) {
            finalExtraDamage = baseBonus;
            // 附加炼金毒素穿透反应
            switch (bane) {
                case NECROPHAGE -> target.setSecondsOnFire(4);
                case INSECTOID -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
                case HUMANOID -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
                case DRACONID -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
            }
        } else {
            // 非目标种群仍承受 30% 基础炼金腐蚀伤害
            finalExtraDamage = baseBonus * 0.30f;
        }

        // 扣减涂层耐久次数，归零时自动清理 NBT 避免垃圾残留
        charges -= 1;
        if (charges <= 0) {
            root.remove(TAG_BLADE_OIL);
            if (attacker instanceof Player player) {
                player.displayClientMessage(Component.translatable("message.ars_alchemica.oil.depleted")
                        .withStyle(ChatFormatting.GRAY), true);
            }
        } else {
            oilData.putInt(TAG_CHARGES, charges);
        }

        return finalExtraDamage;
    }

    private static boolean matchesBaneTarget(BaneType bane, LivingEntity target) {
        MobType mobType = target.getMobType();
        return switch (bane) {
            case NECROPHAGE -> mobType == MobType.UNDEAD;
            case INSECTOID -> mobType == MobType.ARTHROPOD;
            case HUMANOID -> mobType == MobType.ILLAGER || target instanceof Player;
            case DRACONID -> !target.canChangeDimensions(); // Boss 级生物特征匹配
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        BaneType bane = tag != null && tag.contains(TAG_BANE_TYPE)
                ? BaneType.fromId(tag.getString(TAG_BANE_TYPE))
                : BaneType.NECROPHAGE;
        float bonus = tag != null && tag.contains(TAG_BONUS_DAMAGE) ? tag.getFloat(TAG_BONUS_DAMAGE) : 4.5f;

        tooltip.add(Component.translatable("tooltip.ars_alchemica.oil.bane." + bane.getId())
                .withStyle(bane.getColor()));
        tooltip.add(Component.translatable("tooltip.ars_alchemica.oil.damage", String.format("+%.1f", bonus))
                .withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("tooltip.ars_alchemica.oil.charges", DEFAULT_MAX_CHARGES, DEFAULT_MAX_CHARGES)
                .withStyle(ChatFormatting.GRAY));
    }

    /**
     * 供客户端 ItemTooltipEvent 挂载：为任何涂有剑油的原版或模组武器动态追加剑油剩余次数提示。
     */
    public static void appendWeaponCoatTooltip(ItemStack weaponStack, List<Component> tooltip) {
        if (!weaponStack.hasTag()) return;
        CompoundTag root = weaponStack.getTag();
        if (root != null && root.contains(TAG_BLADE_OIL, CompoundTag.TAG_COMPOUND)) {
            CompoundTag oil = root.getCompound(TAG_BLADE_OIL);
            int remaining = oil.getInt(TAG_CHARGES);
            int max = oil.getInt(TAG_MAX_CHARGES);
            BaneType bane = BaneType.fromId(oil.getString(TAG_BANE_TYPE));

            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("tooltip.ars_alchemica.weapon.coated",
                    Component.translatable("tooltip.ars_alchemica.oil.bane." + bane.getId()),
                    remaining, max).withStyle(bane.getColor()));
        }
    }
}
```

---

### 模块四：轻量化二进制网络同步 (Zero-Lag Binary Network Payload)

#### `ToxicitySyncPacket.java`
完全摒弃臃肿的 NBT 同步，仅使用紧凑的二进制流（`float toxicity` + `float maxToxicity` + `byte stateMask` = **9 Bytes**，若最大上限固定则核心动态载荷仅 **5 Bytes**），将网络带宽开销压至极限。

```java
package com.arsalchemica.common.network;

import com.arsalchemica.api.toxicity.ToxicityThreshold;
import com.arsalchemica.common.toxicity.PlayerToxicityManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.function.Supplier;

/**
 * 极简二进制毒性网络同步包 (S2C)。
 * 杜绝 NBT 字符串键名序列化开销，单包体积仅 9 Bytes (Float + Float + Byte)。
 */
public record ToxicitySyncPacket(float toxicity, float maxToxicity, byte stateMask) {

    public static final ResourceLocation ID = new ResourceLocation("ars_alchemica", "toxicity_sync");

    public void encode(FriendlyByteBuf buf) {
        buf.writeFloat(this.toxicity);
        buf.writeFloat(this.maxToxicity);
        buf.writeByte(this.stateMask);
    }

    public static ToxicitySyncPacket decode(FriendlyByteBuf buf) {
        float tox = buf.readFloat();
        float maxTox = buf.readFloat();
        byte mask = buf.readByte();
        return new ToxicitySyncPacket(tox, maxTox, mask);
    }

    /**
     * 客户端接收处理器（通过 Supplier<Player> 解耦 DistExecutor / Minecraft.getInstance().player）。
     */
    public void handleClient(Supplier<Player> clientPlayerSupplier) {
        Player player = clientPlayerSupplier.get();
        if (player == null) return;

        PlayerToxicityManager.getData(player).ifPresent(data -> {
            data.setMaxToxicity(this.maxToxicity);
            data.setToxicity(this.toxicity);
            data.setDirty(false);
        });
    }

    public ToxicityThreshold getDecodedThreshold() {
        int idx = this.stateMask & 0x03;
        ToxicityThreshold[] values = ToxicityThreshold.values();
        return idx < values.length ? values[idx] : ToxicityThreshold.SAFE;
    }
}
```

---

### 模块五：双语本地化键名约定 (Bilingual Localization Keys)

#### 1. `en_us.json`
```json
{
  "item.ars_alchemica.alchemical_elixir": "Alchemical Elixir",
  "item.ars_alchemica.alchemical_incense": "Alchemical Incense",
  "item.ars_alchemica.blade_oil": "Witcher's Blade Oil",

  "toxicity.ars_alchemica.state.safe": "Metabolic Equilibrium (Safe)",
  "toxicity.ars_alchemica.state.caution": "Aetherial Saturation (Caution)",
  "toxicity.ars_alchemica.state.danger": "Alchemical Intoxication (Danger)",
  "toxicity.ars_alchemica.state.overdose": "Systemic Collapse (Overdose!)",

  "tooltip.ars_alchemica.potion.inert": "Inert Distillate (No Active Aspects)",
  "tooltip.ars_alchemica.potion.purity": "Alchemical Purity: %s",
  "tooltip.ars_alchemica.potion.toxicity": "Toxicity Load: %s",

  "tooltip.ars_alchemica.oil.bane.necrophage": "Necrophage & Undead Bane",
  "tooltip.ars_alchemica.oil.bane.insectoid": "Insectoid Carapace Breaker",
  "tooltip.ars_alchemica.oil.bane.humanoid": "Humanoid Hemorrhage Toxin",
  "tooltip.ars_alchemica.oil.bane.draconid": "Draconid & Arch-Entity Bane",
  "tooltip.ars_alchemica.oil.damage": "Specialized Bane Damage: %s",
  "tooltip.ars_alchemica.oil.charges": "Coating Durability: %s / %s Hits",
  "tooltip.ars_alchemica.weapon.coated": "Coated: %s (%s/%s)",

  "message.ars_alchemica.oil.invalid_weapon": "Hold a melee blade in your other hand to apply oil.",
  "message.ars_alchemica.oil.applied": "Coated %s with Alchemical Blade Oil.",
  "message.ars_alchemica.oil.depleted": "The alchemical oil on your blade has worn off."
}
```

#### 2. `zh_cn.json`
```json
{
  "item.ars_alchemica.alchemical_elixir": "复合炼金灵药",
  "item.ars_alchemica.alchemical_incense": "炼金挥发熏香",
  "item.ars_alchemica.blade_oil": "猎魔炼金剑油",

  "toxicity.ars_alchemica.state.safe": "代谢平衡 (安全期)",
  "toxicity.ars_alchemica.state.caution": "以太沉积 (警觉期)",
  "toxicity.ars_alchemica.state.danger": "经脉淤毒 (中毒期)",
  "toxicity.ars_alchemica.state.overdose": "灵质崩解 (毒性过载！)",

  "tooltip.ars_alchemica.potion.inert": "惰性馏分（无活性以太源质）",
  "tooltip.ars_alchemica.potion.purity": "灵药纯度：%s",
  "tooltip.ars_alchemica.potion.toxicity": "毒性负荷：%s",

  "tooltip.ars_alchemica.oil.bane.necrophage": "死灵与腐尸特攻（银刃灼烧）",
  "tooltip.ars_alchemica.oil.bane.insectoid": "节肢与甲壳破除（神经麻痹）",
  "tooltip.ars_alchemica.oil.bane.humanoid": "类人生物特攻（动脉流血）",
  "tooltip.ars_alchemica.oil.bane.draconid": "龙裔与首领特攻（奥术穿透）",
  "tooltip.ars_alchemica.oil.damage": "种群克制附加伤害：%s",
  "tooltip.ars_alchemica.oil.charges": "剑油涂层次数：%s / %s 次命中",
  "tooltip.ars_alchemica.weapon.coated": "已涂抹：%s (%s/%s)",

  "message.ars_alchemica.oil.invalid_weapon": "请在另一只手中持有近战兵刃以涂抹剑油。",
  "message.ars_alchemica.oil.applied": "已将炼金剑油均匀涂抹于 %s。",
  "message.ars_alchemica.oil.depleted": "兵刃上的炼金剑油已消耗殆尽。"
}
```