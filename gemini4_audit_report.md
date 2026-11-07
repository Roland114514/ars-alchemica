这是一份针对《Ars Alchemica》（以太炼金学）前六个里程碑架构与核心代码的**深度安全审计、性能剖析与架构演进诊断报告**。

作为一款拥有高维数学计算（12维向量流形）、实时热力学状态机、物理实体高频交互以及复杂玩家状态持久化的硬核模组，当前的架构已经展现出了极高的工程水准（如 Common-First 解耦、9字节紧凑二进制包、相位取模轮询）。但在**高并发多人服务器（SMP）极端压测场景**、**区块生命周期边界**以及**恶意玩家刷物利用（Exploit）**视角下，系统仍存在数个致命的底层隐患。

---

# 🔬 第一部分：五大维度深度审计诊断

## 1. 并发安全与多线程/生命周期安全 (Thread Safety & Lifecycle Integrity)

### 🚨 [P0 高危] 静态单例管理器跨服/跨存档内存泄漏与串档污染
* **问题定位**：`PlayerToxicityManager` 与 `PlayerKnowledgeManager`（若采用静态 `Map<UUID, Data>` 驻留内存）。
* **深度机理**：
  1. **集成服务端（Integrated Server / 单人游戏）串档**：当玩家在单人游戏中退出存档 A 并进入存档 B 时，JVM 进程并未重启。若静态 Map 未在 `ServerStoppedEvent` 中彻底 `clear()`，存档 A 的毒性值与配方解锁状态将直接污染存档 B。
  2. **多人服务器内存泄漏（Memory Leak）**：在日均千人流动的服务器中，离线玩家的 UUID 实例若只增不减，将导致 Old Gen（老年代）内存持续膨胀。
  3. **跨线程并发崩溃（CME）**：当 Netty IO 线程、维度切换逻辑或异步命令读取玩家数据时，若底层使用普通 `HashMap` 而非 `ConcurrentHashMap`，必将在高并发下抛出 `ConcurrentModificationException` 导致服务端看门狗（Watchdog）崩溃。
* **修复铁律**：必须移除脱离原版生命周期的裸静态缓存，将其绑定至 `ServerPlayer` 的接口注入（Mixin `ServerPlayer`）或原版 `SavedData`（`DimensionDataStorage`），并确保玩家离线时清理运行时引用。

### 🚨 [P0 高危] 区块卸载与重载下的“薛定谔灾变”（NBT 持久化缺失）
* **问题定位**：`AlchemicalCauldronBlockEntity` 与 `CauldronReactionState`。
* **深度机理**：
  在 Milestone 6 中，干烧计数（`dryBoilTicks` 临界 200 ticks）、搅拌角速度阻尼（`angularVelocity`）、瞬时热应力以及“黑泥凝结态（Sludge Coagulated）”如果仅作为内存字段存在，而未完整写入 `saveAdditional(CompoundTag)` 与 `load(CompoundTag)`：
  * **恶意利用（Chunk Unload Bypass）**：当玩家操作失误即将引发“高挥发爆燃”或“干烧炸膛”（第 195 tick）时，玩家只需瞬间使用末影珍珠传出该区块或全员下线使区块卸载，重新加载后状态机归零，从而**无成本规避一切七阶灾变惩罚**。

---

## 2. 逻辑漏洞与刷物/刷状态漏洞 (Exploits & Gameplay Loopholes)

### 🚨 [P0 高危] 投料吸收的“单 Tick 多重吞噬”与物品复制漏洞 (Item Duplication)
* **问题定位**：`AlchemicalCauldronBlockEntity` 的掉落物实体（`ItemEntity`）相位取模吸收逻辑。
* **深度机理**：
  在 `level.getEntitiesOfClass(ItemEntity.class, box)` 遍历吸收投料时，如果仅判断了 `!itemEntity.isRemoved()`，而在扣减 `ItemStack.shrink(1)` 或 `discard()` 之前触发了联动事件，或者在同一 Tick 内存在漏斗/其他模组（如新生魔艺、植物魔法、AE2）的拾取器同时操作该 `ItemEntity`：
  1. **幽灵投料（Ghost Input）**：若未校验 `itemStack.isEmpty()`，空栈实体在被 `discard()` 的同一 Tick 内可能被二次读取，向坩埚注入“0个物品却产生1份12维向量”的幽灵反应。
  2. **黑泥态吃物黑洞**：当坩埚已经处于 `SLUDGE`（黑泥凝结）或 `DRY_BOIL`（无水干烧）状态时，若未锁死投料吸收判定，玩家丢入的贵重药材会被直接吞噬销毁，或导致黑泥状态的向量无穷叠加引发数值溢出。

### ⚠️ [P1 中危] 铁铲清淤与水桶交互的“无限耐久/无限刷黑泥”漏洞
* **问题定位**：`AlchemicalCauldronBlock#use` 交互逻辑。
* **深度机理**：
  1. **客户端假动作与刷物**：若清淤生成 `AlchemicalSludgeItem`（黑泥）或扣减铁铲耐久的逻辑没有严格包裹在 `if (!level.isClientSide)` 中，或未按照**“先重置坩埚状态 -> 再校验状态变更成功 -> 最后掉落/给予产物”**的原子顺序（Check-Effects-Interactions 模式）执行，攻击者可利用发包器在同一 Tick 发送 50 次右键包（`ServerboundUseItemOnPacket`），在状态重置前刷出 50 份投掷黑泥武器。
  2. **创造模式/破坏保护绕过**：铁铲清淤使用 `hurtAndBreak` 时，若未正确传入回调，在耐久为 1 时可能导致物品变更为 0 耐久的幽灵铁铲。

### ⚠️ [P1 中危] 古代羊皮纸（Parchment）与学术残页的副手切换刷造诣漏洞
* **问题定位**：Milestone 5 羊皮纸多步研读解密与造诣提升。
* **深度机理**：
  当玩家长按右键研读（如使用 `use` + `onUseTick` + `finishUsingItem`）或直接右键消耗时，如果玩家在完成判定的同一 Tick 使用快捷键 `F`（主副手交换）或数字键切换快捷栏槽位：
  * 若代码直接对 `player.getItemInHand(hand)` 进行扣减，可能会扣错槽位（扣掉换上来的石头），从而实现**一张羊皮纸无限研读刷满药剂师造诣阶级**。

---

## 3. 热力学与状态机边界漏洞 (Edge Cases & Thermodynamic Glitches)

### ⚠️ [P1 中危] 牛顿冷却定律的“渐近线死锁”与浮点数精度漂移 (IEEE-754 Drift)
* **问题定位**：`CauldronReactionState` 温度更新与 `AspectVector` 连续衰减。
* **深度机理**：
  1. **Zeno 悖论（温度永远无法到达目标值）**：牛顿冷却公式 $T_{t+1} = T_t + k \cdot (T_{env} - T_t)$ 在使用 `double` / `float` 计算时，当环境温度为 $20^\circ\text{C}$，当前温度为 $20.00001^\circ\text{C}$ 时，差值乘以阻尼系数后会陷入无穷逼近但永远不等于 $20^\circ\text{C}$ 的状态。这会导致坩埚方块实体**永远处于 Dirty（脏数据）状态**，每 Tick 都在向磁盘请求保存 NBT 并向客户端触发同步！
  2. **NaN / Infinity 传染灾难**：在计算 12 维向量的余弦相似度 $\cos(\theta) = \frac{\mathbf{A} \cdot \mathbf{B}}{\|\mathbf{A}\| \|\mathbf{B}\|}$ 时，当溶液被稀释或顺时针搅拌平息至模长 $\|\mathbf{A}\| \to 0$（如 `< 1e-9`）时，除法极易产生 `Double.NaN` 或 `Infinity`。一旦 `NaN` 写入 `AspectVector`，整口坩埚的所有后续点积、纯度、毒性计算将全部变成 `NaN`，且写入 NBT 后会导致整个区块损坏无法读取！

### ⚠️ [P1 中危] 双向搅拌的角速度高频连点宏溢出 (Autoclicker Exploit)
* **问题定位**：顺时针（平息）与逆时针（剪切排浊）搅拌交互。
* **深度机理**：
  如果搅拌棒（Stirring Rod）缺乏内置冷却（`ItemCooldowns`）或坩埚缺乏单 Tick 搅拌冲量上限（Impulse Cap），玩家开启鼠标宏（每秒点击 100 次）可在 1 秒内将搅拌角速度推高至荒谬的数值，瞬间将溶液净度刷满或瞬间触发离心飞溅，完全破坏水动力学阻尼的手感设计。

---

## 4. Common-First 规范与多模组兼容性 (Multi-Loader & DataPack Extensibility)

### ⚠️ [P1 中危] 内存硬编码注册表对 Datapack / KubeJS / JEI 的架构阻断
* **问题定位**：`AlchemicalRecipeRegistry` 与 `AlchemyMap`。
* **深度机理**：
  目前 `AlchemicalRecipeRegistry` 采用静态内存注册模式。在现代 Minecraft 1.20.1+ 生态中，这存在三大架构阻碍：
  1. **无法热重载（`/reload` 失效）**：整合包作者无法通过数据包（JSON）修改某味药材的 12 维向量，也无法通过 JSON 新增星图配方。
  2. **客户端/服务端同步断层**：多人联机时，若服务端通过配置调整了配方靶心坐标或容差半径（Tolerance Radius），客户端的 JEI/EMI 和“学者直觉 HUD”若读取本地静态注册表，将出现严重的显示与实际结果不符（Desync）。
* **演进方案**：必须为 `AspectVector` 和 `AlchemicalRecipe` 编写基于 Mojang `Codec`（`RecordCodecBuilder`）的序列化器，并接入原版 `RecipeManager`（实现 `Recipe<Container>`）或 `SimpleJsonResourceReloadListener`。

---

## 5. Zero-Lag 与极致性能优化 (Zero-Lag Profiling)

### 🔧 [P2 优化] 高频 Tick 中的 `double[]` 堆内存分配与 GC 抖动
* **问题定位**：`AspectVector` 的加减乘除与投影运算。
* **深度机理**：
  如果 `AspectVector` 设计为纯不可变对象（Immutable），每次调用 `add()`, `scale()`, `normalize()` 都在内部 `new double[12]`。假设服务器中有 200 口正在沸腾反应的坩埚，每口坩埚每 Tick 进行 10 次向量代数与星图涡流场距离计算，每秒将产生 $200 \times 20 \times 10 = 40,000$ 个短命数组对象。这会给 G1GC 的 Eden 区带来持续的分配压力（Allocation Rate 飙升）。
* **优化方案**：
  * 对 API 层暴露不可变视图（`AspectVector`），但在 `CauldronReactionState` 内部引入一个可变的算子缓冲区 **`MutableAspectVector`**（复用同一个 `double[12]` 数组进行就地运算 `addInPlace`, `scaleInPlace`），将高频 Tick 路径上的内存分配降至 **0 Byte/tick**。

### 🔧 [P2 优化] 区块网格与网络包风暴 (Network Packet Storm)
* **问题定位**：`AlchemicalCauldronBlockEntity#setChanged` 与客户端 HUD 同步。
* **深度机理**：
  由于热力学温度和搅拌衰减是每 Tick 连续变化的，如果每 Tick 都调用 `level.sendBlockUpdated(pos, state, state, 3)`，服务端每 Tick 都会将该方块的完整 NBT 序列化并通过网络广播给周围所有玩家。
* **优化方案**：
  * **死区阈值同步（Deadband Sync）**：仅当温度变化超过 $\pm 1.5^\circ\text{C}$、向量模长变化超过 $0.05$、或状态机发生枚举跃迁（如进入沸腾/灾变态）时，才触发客户端同步包。其余平滑过渡完全由客户端本地插值（Client-side Interpolation）完成。

---

# 🛠️ 第二部分：核心问题分级汇总矩阵

| 优先级 | 编号 | 模块 | 漏洞/隐患简述 | 风险后果 |
| :--- | :--- | :--- | :--- | :--- |
| **P0 (致命)** | **SEC-01** | `CauldronReactionState` / `AspectVector` | 向量零模长除零产生 `NaN`/`Infinity` 污染 NBT | 坩埚永久报废、区块存档序列化崩溃 |
| **P0 (致命)** | **SEC-02** | `AlchemicalCauldronBlockEntity` | 热力学/干烧/搅拌/黑泥状态未完整持久化至 NBT | 区块卸载无伤逃逸七阶灾变 |
| **P0 (致命)** | **SEC-03** | `AlchemicalCauldronBlockEntity` | 掉落物吞噬未做原子锁与空栈/满载状态拦截 | 跨模组交互复制物品、黑泥态吞噬贵重物品 |
| **P0 (致命)** | **CON-01** | `PlayerToxicityManager` / `Knowledge` | 静态 Map 缺乏并发保护与生命周期清理 | 多线程 `CME` 崩溃、单人跨存档串档、内存泄漏 |
| **P1 (严重)** | **EXP-01** | `AlchemicalCauldronBlock` | 铁铲清淤与搅拌缺乏速率限制与原子状态优先校验 | 鼠标宏瞬间刷满净度、发包器单 Tick 刷出多份黑泥 |
| **P1 (严重)** | **PHY-01** | `CauldronReactionState` | 牛顿冷却无穷渐近线导致方块实体永不休眠 | 区块永远处于 Dirty 状态，引发高频磁盘 I/O |
| **P1 (严重)** | **ARC-01** | `AlchemicalRecipeRegistry` | 缺乏 Mojang `Codec` 支持，难以对接 Datapack | 无法通过数据包热重载，阻碍 KubeJS/JEI 生态接入 |
| **P2 (优化)** | **PRF-01** | `AspectVector` / `CauldronBe` | 每 Tick 创建大量临时 `double[12]` 及高频全量同步 | Eden 区 GC 抖动、多人同屏炼金时网络带宽激增 |

---

# 💻 第三部分：P0 / P1 / P2 核心代码重构与加固方案

以下提供可直接落地的生产级加固代码方案：

### 1. [修复 SEC-01 & PRF-01] `AspectVector` 算术防爆（NaN 免疫）与零分配重构

在 `AspectVector.java` 中加入严格的 IEEE-754 边界守卫与 `Codec` 支持：

```java
package com.medievalmagic.arsalchemica.api.vector;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.Arrays;
import java.util.List;

public final class AspectVector {
    public static final int DIMENSIONS = 12;
    private static final double EPSILON = 1.0E-7D;
    public static final AspectVector ZERO = new AspectVector(new double[DIMENSIONS], false);

    // 支持 Datapack / JSON 序列化的 Mojang Codec (修复 ARC-01)
    public static final Codec<AspectVector> CODEC = Codec.DOUBLE.listOf().comapFlatMap(
        list -> {
            if (list.size() != DIMENSIONS) {
                return com.mojang.serialization.DataResult.error(() -> "AspectVector must have exactly 12 dimensions, got " + list.size());
            }
            double[] arr = new double[DIMENSIONS];
            for (int i = 0; i < DIMENSIONS; i++) {
                double val = list.get(i);
                arr[i] = Double.isFinite(val) ? val : 0.0D;
            }
            return com.mojang.serialization.DataResult.success(new AspectVector(arr, false));
        },
        vec -> {
            Double[] boxed = new Double[DIMENSIONS];
            for (int i = 0; i < DIMENSIONS; i++) boxed[i] = vec.values[i];
            return Arrays.asList(boxed);
        }
    );

    private final double[] values;
    private double cachedNorm = -1.0D;

    private AspectVector(double[] values, boolean copy) {
        this.values = copy ? Arrays.copyOf(values, DIMENSIONS) : values;
        sanitize();
    }

    public static AspectVector of(double... values) {
        if (values == null || values.length != DIMENSIONS) {
            throw new IllegalArgumentException("AspectVector requires 12 dimensions");
        }
        return new AspectVector(values, true);
    }

    /**
     * 核心防爆卫士：彻底阻断 NaN 与 Infinity 污染 (修复 SEC-01)
     */
    private void sanitize() {
        for (int i = 0; i < DIMENSIONS; i++) {
            double v = this.values[i];
            if (!Double.isFinite(v) || Math.abs(v) < EPSILON) {
                this.values[i] = 0.0D;
            }
        }
    }

    public double norm() {
        if (this.cachedNorm < 0.0D) {
            double sum = 0.0D;
            for (double v : this.values) {
                sum += v * v;
            }
            double result = Math.sqrt(sum);
            this.cachedNorm = (!Double.isFinite(result) || result < EPSILON) ? 0.0D : result;
        }
        return this.cachedNorm;
    }

    public boolean isZero() {
        return norm() < EPSILON;
    }

    /**
     * 安全的余弦投影计算，彻底杜绝除零导致的 NaN
     */
    public double cosineSimilarity(AspectVector other) {
        double n1 = this.norm();
        double n2 = other.norm();
        if (n1 < EPSILON || n2 < EPSILON) {
            return 0.0D;
        }
        double dot = this.dot(other);
        double cos = dot / (n1 * n2);
        if (!Double.isFinite(cos)) return 0.0D;
        return Math.max(-1.0D, Math.min(1.0D, cos));
    }

    public double dot(AspectVector other) {
        double sum = 0.0D;
        for (int i = 0; i < DIMENSIONS; i++) {
            sum += this.values[i] * other.values[i];
        }
        return Double.isFinite(sum) ? sum : 0.0D;
    }

    // 紧凑安全的 NBT 序列化
    public ListTag toNbt() {
        ListTag list = new ListTag();
        for (int i = 0; i < DIMENSIONS; i++) {
            list.add(DoubleTag.valueOf(Double.isFinite(this.values[i]) ? this.values[i] : 0.0D));
        }
        return list;
    }

    public static AspectVector fromNbt(ListTag list) {
        if (list == null || list.size() != DIMENSIONS) return ZERO;
        double[] arr = new double[DIMENSIONS];
        for (int i = 0; i < DIMENSIONS; i++) {
            double v = list.getDouble(i);
            arr[i] = Double.isFinite(v) ? v : 0.0D;
        }
        return new AspectVector(arr, false);
    }
}
```

---

### 2. [修复 PHY-01 & SEC-02] `CauldronReactionState` 牛顿冷却死区钳制与完整 NBT 持久化

在 `CauldronReactionState.java` 中解决“渐近线死锁”与“区块卸载规避灾变”问题：

```java
// 在 CauldronReactionState 的热力学更新方法中：
private static final float TEMP_SNAP_EPSILON = 0.05F;
private static final float VELOCITY_SNAP_EPSILON = 0.001F;
private static final double MAX_ANGULAR_VELOCITY = 15.0D; // 限制鼠标宏连点上限 (修复 EXP-01)

public boolean tickThermodynamics(float targetEnvTemp, boolean hasHeatSource, boolean hasWater) {
    boolean stateChanged = false;

    // 1. 牛顿冷却/加热死区钳制 (修复 PHY-01：防止无穷渐近导致区块永不休眠)
    float diff = targetEnvTemp - this.temperature;
    if (Math.abs(diff) <= TEMP_SNAP_EPSILON) {
        if (this.temperature != targetEnvTemp) {
            this.temperature = targetEnvTemp;
            stateChanged = true;
        }
    } else {
        float k = hasHeatSource ? 0.025F : 0.012F;
        this.temperature += diff * k;
        stateChanged = true;
    }

    // 2. 搅拌角速度流体阻尼死区钳制
    if (Math.abs(this.angularVelocity) <= VELOCITY_SNAP_EPSILON) {
        this.angularVelocity = 0.0F;
    } else {
        this.angularVelocity *= 0.92F; // 流体粘滞阻尼
        stateChanged = true;
    }

    // 3. 干烧热应力累积
    if (!hasWater && this.temperature >= 100.0F) {
        this.dryBoilTicks++;
        stateChanged = true;
    } else if (this.dryBoilTicks > 0) {
        this.dryBoilTicks = Math.max(0, this.dryBoilTicks - 2);
        stateChanged = true;
    }

    return stateChanged;
}

// 搅拌冲量注入：增加速率与极值钳制 (修复 EXP-01)
public void applyStirImpulse(double deltaVelocity) {
    this.angularVelocity = (float) Math.max(-MAX_ANGULAR_VELOCITY,
            Math.min(MAX_ANGULAR_VELOCITY, this.angularVelocity + deltaVelocity));
}

// 完整 NBT 持久化：彻底封堵区块卸载逃逸灾变漏洞 (修复 SEC-02)
public CompoundTag saveToNbt() {
    CompoundTag tag = new CompoundTag();
    tag.putFloat("Temp", this.temperature);
    tag.putFloat("AngVel", this.angularVelocity);
    tag.putInt("DryBoilTicks", this.dryBoilTicks);
    tag.putFloat("SolutionClarity", this.solutionClarity);
    tag.putBoolean("IsSludge", this.isSludgeCoagulated);
    tag.put("ActiveVector", this.currentVector.toNbt());
    return tag;
}

public void loadFromNbt(CompoundTag tag) {
    this.temperature = tag.getFloat("Temp");
    this.angularVelocity = tag.getFloat("AngVel");
    this.dryBoilTicks = tag.getInt("DryBoilTicks");
    this.solutionClarity = tag.getFloat("SolutionClarity");
    this.isSludgeCoagulated = tag.getBoolean("IsSludge");
    this.currentVector = AspectVector.fromNbt(tag.getList("ActiveVector", Tag.TAG_DOUBLE));
}
```

---

### 3. [修复 SEC-03 & EXP-01 & PRF-01] `AlchemicalCauldronBlockEntity` 投料防刷、清淤原子化与死区同步

在 `AlchemicalCauldronBlockEntity.java` 中加固实体交互与网络同步：

```java
// 1. 死区缓存字段，避免每 Tick 发送网络包风暴 (修复 PRF-01)
private float lastSyncedTemp = -999.0F;
private double lastSyncedNorm = -1.0D;
private boolean lastSyncedSludge = false;

public static void serverTick(Level level, BlockPos pos, BlockState state, AlchemicalCauldronBlockEntity be) {
    // 驱动热力学状态机
    boolean dirty = be.reactionState.tickThermodynamics(be.computeTargetTemp(), be.isHeated(), be.hasWater());

    // 相位取模吸收掉落物：严格限制状态与原子扣减 (修复 SEC-03)
    if ((level.getGameTime() + pos.asLong()) % 5L == 0L) {
        if (be.canAcceptIngredients()) {
            be.absorbDroppedIngredients(level, pos);
        }
    }

    // 检查七阶灾变触发
    if (be.reactionState.shouldTriggerCatastrophe()) {
        AlchemicalCatastropheHandler.executeCatastrophe(level, pos, state, be);
        return; // 灾变已接管并可能销毁方块，立即终止后续逻辑
    }

    if (dirty) {
        be.setChanged(); // 仅标记区块需要在卸载/保存时写盘，不发网络包
        be.syncIfThresholdReached(level, pos, state);
    }
}

private boolean canAcceptIngredients() {
    // 黑泥凝结态或无水干烧态下，严禁吞噬玩家投入的物品！
    return this.hasWater() && !this.reactionState.isSludgeCoagulated();
}

private void absorbDroppedIngredients(Level level, BlockPos pos) {
    List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, this.getAbsorbBox(pos),
            entity -> entity.isAlive() && !entity.getItem().isEmpty() && !entity.hasPickUpDelay());

    for (ItemEntity itemEntity : items) {
        ItemStack stack = itemEntity.getItem();
        if (stack.isEmpty()) continue; // 二次防御其他模组同 Tick 截胡

        Optional<AspectVector> ingredientVec = AlchemicalIngredientRegistry.getVector(stack);
        if (ingredientVec.isPresent()) {
            // 严格遵循 Check-Effects-Interactions 顺序：先扣减物品，再注入向量
            ItemStack consumed = stack.split(1);
            if (!consumed.isEmpty()) {
                this.reactionState.injectIngredient(ingredientVec.get());
                if (stack.isEmpty()) {
                    itemEntity.discard();
                } else {
                    itemEntity.setItem(stack); // 触发实体元数据同步
                }
                this.forceSync(level, pos, this.getBlockState());
                break; // 每次轮询最多吸收 1 个物品，不仅防止瞬间溢出，还极具炼金仪式感
            }
        }
    }
}

/**
 * 铁铲清淤原子化交互：彻底杜绝发包器单 Tick 刷黑泥漏洞 (修复 EXP-01)
 */
public InteractionResult tryShovelSludge(Player player, InteractionHand hand, ItemStack shovelStack) {
    if (this.level == null || this.level.isClientSide()) {
        return InteractionResult.SUCCESS;
    }
    // 1. Check: 严格校验当前是否处于黑泥凝结态
    if (!this.reactionState.isSludgeCoagulated()) {
        return InteractionResult.PASS;
    }
    // 2. Effects: 立即清除黑泥状态并重置坩埚（原子锁，后续同 Tick 数据包将在第1步直接被拦截）
    this.reactionState.clearSludgeAndReset();
    this.setChanged();
    this.forceSync(this.level, this.worldPosition, this.getBlockState());

    // 3. Interactions: 扣减耐久并生成黑泥产物
    shovelStack.hurtAndBreak(2, player, p -> p.broadcastBreakEvent(hand));
    ItemStack sludgeDrop = new ItemStack(ModItems.ALCHEMICAL_SLUDGE.get(), 1);
    if (!player.getInventory().add(sludgeDrop)) {
        player.drop(sludgeDrop, false);
    }
    this.level.playSound(null, this.worldPosition, SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 1.0F, 0.8F);
    return InteractionResult.CONSUME;
}

/**
 * 死区阈值网络同步 (修复 PRF-01)
 */
private void syncIfThresholdReached(Level level, BlockPos pos, BlockState state) {
    float curTemp = this.reactionState.getTemperature();
    double curNorm = this.reactionState.getCurrentVector().norm();
    boolean curSludge = this.reactionState.isSludgeCoagulated();

    if (Math.abs(curTemp - this.lastSyncedTemp) >= 1.5F
            || Math.abs(curNorm - this.lastSyncedNorm) >= 0.05D
            || curSludge != this.lastSyncedSludge) {
        forceSync(level, pos, state);
    }
}

public void forceSync(Level level, BlockPos pos, BlockState state) {
    this.lastSyncedTemp = this.reactionState.getTemperature();
    this.lastSyncedNorm = this.reactionState.getCurrentVector().norm();
    this.lastSyncedSludge = this.reactionState.isSludgeCoagulated();
    level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
}
```

---

### 4. [修复 CON-01] `PlayerToxicityManager` 与 `PlayerKnowledgeManager` 并发与生命周期加固

将内部存储容器升级为 `ConcurrentHashMap`，并提供完整的服务器生命周期钩子：

```java
// 在 PlayerToxicityManager & PlayerKnowledgeManager 中：
private static final Map<UUID, PlayerToxicityData> SERVER_CACHE = new ConcurrentHashMap<>();

// 1. 玩家退出服务器时：持久化后立即从内存移除，防止长周期运行服务器 OOM
public static void onPlayerLogout(ServerPlayer player) {
    PlayerToxicityData removed = SERVER_CACHE.remove(player.getUUID());
    if (removed != null) {
        // 确保最后一次状态已写入玩家实体的 PersistentData / Mixin 字段
        saveToPlayerNbt(player, removed);
    }
}

// 2. 服务器关闭时（ServerStoppedEvent）：彻底清空静态容器，防止单人游戏跨存档串档
public static void onServerStopped() {
    SERVER_CACHE.clear();
}

// 3. 玩家死亡重生/末地返回时（PlayerEvent.Clone）：确保配方知识不丢失、毒性按规则重置或保留
public static void onPlayerClone(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
    PlayerRecipeKnowledge knowledge = PlayerKnowledgeManager.get(oldPlayer);
    PlayerKnowledgeManager.set(newPlayer, knowledge.copy());

    if (alive) {
        // 从末地返回：保留当前毒性
        PlayerToxicityData tox = get(oldPlayer);
        set(newPlayer, tox.copy());
    } else {
        // 死亡复活：清空毒性槽并同步 9 字节二进制包
        resetAndSync(newPlayer);
    }
}
```

---

# 🧭 第四部分：架构演进路线图建议（面向 Milestone 7+）

1. **全面拥抱 Codec 与数据包驱动（Data-Driven Alchemy）**：
   * 建议在下一阶段将 `AlchemicalRecipe` 实现原版 `Recipe<Container>` 接口，通过 `RecipeType` 和 `RecipeSerializer`（内部直接调用我们上方编写的 `AspectVector.CODEC`）注册。这样无需编写任何额外同步网络包，原版会在玩家进服时自动将所有星图配方同步至客户端，同时天然打通 **JEI / REI / EMI** 与 **KubeJS**。
2. **搅拌棒交互防宏冷却**：
   * 在搅拌棒右键大坩埚生效后，调用 `player.getCooldowns().addCooldown(rodStack.getItem(), 4)`（0.2秒冷却），既能契合玩家手持搅拌棒规律画圈的物理节奏，又能从根源上废除连点器的物理作弊空间。
3. **羊皮纸研读槽位锁定**：
   * 在古代羊皮纸研读时，记录起始槽位索引（或对比 `ItemStack.isSameItemSameTags`），在 `finishUsingItem` 中直接对传入的 `ItemStack` 参数执行 `shrink(1)` 并返回该栈，切勿重新通过 `player.getMainHandItem()` 获取，彻底杜绝副手切换刷造诣漏洞。