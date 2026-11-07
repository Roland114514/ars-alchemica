以下是为您实现的《Ars Alchemica》Milestone 1 第二部分核心代码。代码采用 Java 17 规范编写，融入了连续场衰减算法、牛顿冷却定律与转动惯量模拟，并内置完整的 NBT 序列化接口以供方块实体（BlockEntity）直接调用。

---

### 1. `AlchemyMapNode.java` (以太星图节点与效果区域)

位于包：`com.medievalmagic.arsalchemica.api.map`

```java
package com.medievalmagic.arsalchemica.api.map;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Objects;

/**
 * 以太星图中的药效节点（Effect Node）。
 * <p>
 * 在二维炼金平面中，每一个节点代表一个隐藏在迷雾中的引力阱，当坩埚内的相位向量
 * 落入其半径范围内且满足纯度条件时，即可激发对应的超凡药理。
 */
public final class AlchemyMapNode {

    private final ResourceLocation nodeId;
    private final ResourceLocation effectId;
    private final double centerX;
    private final double centerY;
    private final double radius;
    private final double perfectRadius;
    private final double minPurityRequired;
    private final int tier;
    private final boolean isAncient;

    private AlchemyMapNode(Builder builder) {
        this.nodeId = Objects.requireNonNull(builder.nodeId, "Node ID cannot be null");
        this.effectId = Objects.requireNonNull(builder.effectId, "Effect ID cannot be null");
        this.centerX = builder.centerX;
        this.centerY = builder.centerY;
        this.radius = builder.radius;
        this.perfectRadius = builder.perfectRadius;
        this.minPurityRequired = builder.minPurityRequired;
        this.tier = Mth.clamp(builder.tier, 1, 5);
        this.isAncient = builder.isAncient;

        if (this.radius <= 0.0 || this.perfectRadius < 0.0 || this.perfectRadius > this.radius) {
            throw new IllegalArgumentException("Invalid radius configuration for node: " + nodeId);
        }
    }

    /**
     * 判定给定星图坐标是否命中该节点的有效激发圈。
     */
    public boolean contains(double x, double y) {
        double dx = x - this.centerX;
        double dy = y - this.centerY;
        return (dx * dx + dy * dy) <= (this.radius * this.radius);
    }

    /**
     * 计算给定坐标到节点靶心的欧氏距离。
     */
    public double distanceTo(double x, double y) {
        return Math.hypot(x - this.centerX, y - this.centerY);
    }

    /**
     * 计算命中精度评分（Precision Score）。
     * <p>
     * 数学定义：
     * <ul>
     *     <li>若距离 $d \le r_{\text{perfect}}$，得分为 $1.0$（完美共鸣，触发额外药效加成或时长翻倍）。</li>
     *     <li>若距离 $d > r_{\text{radius}}$，得分为 $0.0$（脱靶）。</li>
     *     <li>若处于两者之间，采用平滑 Hermite 插值（Smoothstep）进行非线性衰减，模拟药效浓度的中心聚集效应。</li>
     * </ul>
     *
     * @param x 当前溶液星图 X 坐标
     * @param y 当前溶液星图 Y 坐标
     * @return 归一化精度评分 [0.0, 1.0]
     */
    public double getPrecisionScore(double x, double y) {
        double d = distanceTo(x, y);
        if (d <= this.perfectRadius) {
            return 1.0;
        }
        if (d >= this.radius) {
            return 0.0;
        }
        // 归一化距离参数 t in [0, 1]
        double t = (d - this.perfectRadius) / (this.radius - this.perfectRadius);
        // 反向 Smoothstep: 1 - (3t^2 - 2t^3)
        return 1.0 - (t * t * (3.0 - 2.0 * t));
    }

    // --- Getters ---

    public ResourceLocation getNodeId() { return nodeId; }
    public ResourceLocation getEffectId() { return effectId; }
    public double getCenterX() { return centerX; }
    public double getCenterY() { return centerY; }
    public double getRadius() { return radius; }
    public double getPerfectRadius() { return perfectRadius; }
    public double getMinPurityRequired() { return minPurityRequired; }
    public int getTier() { return tier; }
    public boolean isAncient() { return isAncient; }

    // --- Builder ---

    public static class Builder {
        private final ResourceLocation nodeId;
        private final ResourceLocation effectId;
        private double centerX;
        private double centerY;
        private double radius = 3.5;
        private double perfectRadius = 0.8;
        private double minPurityRequired = 0.5;
        private int tier = 1;
        private boolean isAncient = false;

        public Builder(ResourceLocation nodeId, ResourceLocation effectId, double centerX, double centerY) {
            this.nodeId = nodeId;
            this.effectId = effectId;
            this.centerX = centerX;
            this.centerY = centerY;
        }

        public Builder radii(double radius, double perfectRadius) {
            this.radius = radius;
            this.perfectRadius = perfectRadius;
            return this;
        }

        public Builder purity(double minPurityRequired) {
            this.minPurityRequired = Mth.clamp(minPurityRequired, 0.0, 1.0);
            return this;
        }

        public Builder tier(int tier) {
            this.tier = tier;
            return this;
        }

        public Builder ancient() {
            this.isAncient = true;
            return this;
        }

        public AlchemyMapNode build() {
            return new AlchemyMapNode(this);
        }
    }
}
```

---

### 2. `AlchemyMap.java` (探索星图拓扑与危险区管理)

位于包：`com.medievalmagic.arsalchemica.api.map`

```java
package com.medievalmagic.arsalchemica.api.map;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 以太星图拓扑管理器（The Astral Alchemy Map）。
 * <p>
 * 负责维护整张二维炼金平面上的所有药效节点（Nodes）以及以太乱流/危险区（Hazard Zones）。
 * 支持服务端数据包（Datapack）热重载。
 */
public final class AlchemyMap {

    private static final AlchemyMap INSTANCE = new AlchemyMap();

    private final Map<ResourceLocation, AlchemyMapNode> nodes = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, HazardZone> hazards = new ConcurrentHashMap<>();

    private AlchemyMap() {
        registerDefaultTopology();
    }

    public static AlchemyMap getInstance() {
        return INSTANCE;
    }

    /**
     * 星图危险区（如：骸骨碎裂带、虚空漩涡、以太死水区）。
     * 当溶液坐标漂移至此区域时，将持续引发高额失稳或浑浊度飙升。
     */
    public record HazardZone(
            ResourceLocation hazardId,
            HazardType type,
            double centerX,
            double centerY,
            double radius,
            double maxIntensity
    ) {
        public enum HazardType {
            /** 虚空漩涡：剧烈增加失稳度，极易引爆坩埚 */
            VOID_VORTEX,
            /** 骸骨荒漠：剧烈增加溶液浑浊度（杂质），导致炼成黒泥 */
            BONE_GRAVEYARD,
            /** 极寒死域：强制吸收溶液热量，导致反应停滞 */
            FROST_DEADZONE
        }

        public boolean contains(double x, double y) {
            double dx = x - centerX;
            double dy = y - centerY;
            return (dx * dx + dy * dy) <= (radius * radius);
        }

        /**
         * 计算在给定坐标处的危险辐射强度。越靠近危险区中心，强度呈指数级攀升。
         */
        public double getIntensityAt(double x, double y) {
            double d = Math.hypot(x - centerX, y - centerY);
            if (d >= radius) return 0.0;
            double normalizedDist = d / radius;
            return maxIntensity * Math.pow(1.0 - normalizedDist, 1.5);
        }
    }

    // --- 拓扑查询 API ---

    /**
     * 查询当前坐标命中的药效节点。
     * 若多个节点边缘重叠，优先返回离靶心最近的节点。
     */
    public Optional<AlchemyMapNode> findHitNode(double x, double y) {
        return nodes.values().stream()
                .filter(node -> node.contains(x, y))
                .min(Comparator.comparingDouble(node -> node.distanceTo(x, y)));
    }

    /**
     * 查找距离当前坐标最近的节点（用于占卜罗盘、星象仪指引或迷雾微光提示）。
     *
     * @param includeAncient 是否包含极其隐秘的古代失落秘方节点
     */
    public Optional<AlchemyMapNode> findNearestNode(double x, double y, boolean includeAncient) {
        return nodes.values().stream()
                .filter(node -> includeAncient || !node.isAncient())
                .min(Comparator.comparingDouble(node -> node.distanceTo(x, y)));
    }

    /**
     * 计算当前坐标承受的综合危险区强度（多危险区叠加）。
     */
    public double getHazardIntensity(double x, double y) {
        double totalIntensity = 0.0;
        for (HazardZone hazard : hazards.values()) {
            totalIntensity += hazard.getIntensityAt(x, y);
        }
        return totalIntensity;
    }

    /**
     * 获取当前坐标所处的首要危险区类型（用于决定触发何种炼金事故特效）。
     */
    public Optional<HazardZone> getActiveHazard(double x, double y) {
        return hazards.values().stream()
                .filter(h -> h.contains(x, y))
                .max(Comparator.comparingDouble(h -> h.getIntensityAt(x, y)));
    }

    // --- 注册与重载管理 ---

    public void registerNode(AlchemyMapNode node) {
        nodes.put(node.getNodeId(), node);
    }

    public void registerHazard(HazardZone hazard) {
        hazards.put(hazard.hazardId(), hazard);
    }

    public void clearAll() {
        nodes.clear();
        hazards.clear();
    }

    public Collection<AlchemyMapNode> getAllNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    public Collection<HazardZone> getAllHazards() {
        return Collections.unmodifiableCollection(hazards.values());
    }

    /**
     * 初始化模组内置的基础中世纪暗黑星图拓扑。
     */
    private void registerDefaultTopology() {
        // 1. 生命恢复 (Tier 1) - 位于第一象限 (Solaris + Corporis 偏上)
        registerNode(new AlchemyMapNode.Builder(
                new ResourceLocation("arsalchemica", "node_regeneration"),
                new ResourceLocation("minecraft", "regeneration"),
                12.5, 14.0)
                .radii(4.0, 1.0).purity(0.45).tier(1).build());

        // 2. 烈焰庇护 (Tier 2) - 位于远东高温区 (高 Solaris + 中 Corporis)
        registerNode(new AlchemyMapNode.Builder(
                new ResourceLocation("arsalchemica", "node_fire_resistance"),
                new ResourceLocation("minecraft", "fire_resistance"),
                28.0, 6.5)
                .radii(3.5, 0.8).purity(0.60).tier(2).build());

        // 3. 暗影潜行/隐身 (Tier 3) - 位于第二象限深处 (高 Lunaris + 高 Aetheris)
        registerNode(new AlchemyMapNode.Builder(
                new ResourceLocation("arsalchemica", "node_invisibility"),
                new ResourceLocation("minecraft", "invisibility"),
                -22.0, 25.0)
                .radii(3.0, 0.6).purity(0.75).tier(3).build());

        // 4. 古代秘方：不死鸟涅槃 (Tier 5, Ancient) - 极远端坐标，紧贴虚空漩涡边缘
        registerNode(new AlchemyMapNode.Builder(
                new ResourceLocation("arsalchemica", "node_phoenix_rebirth"),
                new ResourceLocation("arsalchemica", "phoenix_rebirth"),
                55.0, 48.0)
                .radii(2.2, 0.4).purity(0.92).tier(5).ancient().build());

        // --- 危险区注册 ---
        // 骸骨碎裂带：横亘在第三象限的死亡陷阱
        registerHazard(new HazardZone(
                new ResourceLocation("arsalchemica", "hazard_bone_graveyard"),
                HazardZone.HazardType.BONE_GRAVEYARD,
                -18.0, -18.0, 8.5, 0.08
        ));

        // 虚空漩涡：守护在不死鸟涅槃节点必经之路上的高危引力阱
        registerHazard(new HazardZone(
                new ResourceLocation("arsalchemica", "hazard_void_vortex"),
                HazardZone.HazardType.VOID_VORTEX,
                46.0, 40.0, 6.0, 0.15
        ));
    }
}
```

---

### 3. `CauldronReactionState.java` (坩埚实时反应物理状态机)

位于包：`com.medievalmagic.arsalchemica.api.cauldron`

```java
package com.medievalmagic.arsalchemica.api.cauldron;

import com.medievalmagic.arsalchemica.api.aspect.AspectType;
import com.medievalmagic.arsalchemica.api.aspect.AspectVector;
import com.medievalmagic.arsalchemica.api.map.AlchemyMap;
import com.medievalmagic.arsalchemica.api.map.AlchemyMap.HazardZone;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * 坩埚内溶液的微观热力学与以太动力学状态机。
 * <p>
 * 负责在每个游戏刻（Tick）演进溶液的温度、相位移动、搅拌惯性、对立源质湮灭反应以及失稳事故判定。
 */
public class CauldronReactionState {

    /**
     * 热量阶级枚举：决定热源的目标温度上限与热传导速率。
     */
    public enum HeatLevel {
        /** 无热源/熄灭：室温 20°C */
        COLD(20.0, 0.005, 0.0),
        /** 文火慢炖（营火/木炭）：80°C，适合温和提纯与调香 */
        WARM_SIMMER(80.0, 0.015, 0.2),
        /** 剧烈沸腾（熔岩/烈焰人燃烧室）：150°C，常规大宗药剂合成温度 */
        ROILING_BOIL(150.0, 0.025, 0.6),
        /** 星界炼狱（灵魂火/以太聚焦镜）：300°C，古代禁忌秘方必需，伴随极高热失稳 */
        ASTRAL_INFERNO(300.0, 0.045, 1.4);

        private final double targetTemperature;
        private final double thermalConductivity;
        private final double baseReactionRate;

        HeatLevel(double targetTemperature, double thermalConductivity, double baseReactionRate) {
            this.targetTemperature = targetTemperature;
            this.thermalConductivity = thermalConductivity;
            this.baseReactionRate = baseReactionRate;
        }

        public double getTargetTemperature() { return targetTemperature; }
        public double getThermalConductivity() { return thermalConductivity; }
        public double getBaseReactionRate() { return baseReactionRate; }
    }

    /**
     * 炼金事故（Mishap）判定结果。
     */
    public enum MishapResult {
        /** 状态稳定，未发生事故 */
        NONE,
        /** 完美结晶：溶液在目标节点内达到极高纯度并完成冷却定型 */
        CRYSTALLIZE_SUCCESS,
        /** 沸溢毒雾：溶液失稳过高但温度未达燃点，向四周喷发剧毒蚀骨瘴气 */
        TOXIC_BOILOVER,
        /** 炼金败物（Sludge）：浑浊度满溢，药液彻底报废并凝结成腐蚀性黑泥 */
        SLUDGE_COAGULATION,
        /** 以太殉爆：高温伴随失稳满溢或虚空坍缩，引发摧毁工作台的魔法爆炸 */
        ARCANE_EXPLOSION
    }

    // --- 核心物理状态量 ---

    /** 当前溶液的累积已溶解源质向量 */
    private AspectVector currentVector = AspectVector.EMPTY;
    /** 刚投入但尚未完全溶解进主溶液的缓冲向量（随温度与搅拌逐步融合） */
    private AspectVector dissolvingBuffer = AspectVector.EMPTY;

    /** 当前溶液实时摄氏度 (°C) */
    private double temperature = 20.0;
    /** 当前外接热源等级 */
    private HeatLevel activeHeatLevel = HeatLevel.COLD;

    /** 搅拌累计弧度进度 */
    private double stirProgress = 0.0;
    /** 瞬时搅拌角速度（正值为顺时针，负值为逆时针） */
    private double stirVelocity = 0.0;
    /** 顺时针完整旋转圈数 */
    private int clockwiseTurns = 0;
    /** 逆时针完整旋转圈数 */
    private int counterClockwiseTurns = 0;

    /** 瞬时失稳蓄积值 [0.0 ~ 1.0+]，超过 1.0 将触发严重炼金事故 */
    private double accumulatedInstability = 0.0;
    /** 溶液浑浊度/杂质度 [0.0 ~ 1.0+]，由腐化源质或对立湮灭残渣产生 */
    private double turbidity = 0.0;

    // --- 状态机推进核心方法 ---

    /**
     * 每个服务端 Tick 调用一次，演进坩埚内的热力学与流体反应。
     *
     * @param hasHeatSource 下方是否存在有效热源方块
     * @param heatLevel     当前热源提供的热量阶级
     */
    public void tick(boolean hasHeatSource, HeatLevel heatLevel) {
        this.activeHeatLevel = hasHeatSource ? heatLevel : HeatLevel.COLD;

        // 1. 牛顿冷却与加热定律：dT/dt = k * (T_env - T)
        double targetTemp = this.activeHeatLevel.getTargetTemperature();
        double k = this.activeHeatLevel.getThermalConductivity();
        this.temperature += (targetTemp - this.temperature) * k;

        // 2. 搅拌流体阻尼与圈数统计
        updateStirPhysics();

        // 3. 缓冲源质溶解与星图坐标推进
        processDissolutionAndReaction();

        // 4. 星图环境场交互（危险区辐射与自然热力学熵增）
        processEnvironmentalInstability();
    }

    /**
     * 向坩埚内投入新的炼金材料向量。
     * 材料不会瞬间改变当前坐标，而是进入溶解缓冲池，并产生一次性的入水热冲击。
     */
    public void addIngredient(AspectVector ingredientVector) {
        if (ingredientVector == null || ingredientVector.getTotalMagnitude() <= 1e-6) {
            return;
        }

        // 若在剧烈沸腾时贸然投入高不稳定性药材，直接产生热冲击失稳
        double thermalFactor = Math.max(0.0, (this.temperature - 80.0) / 100.0);
        double shock = ingredientVector.getInstabilityFactor() * 0.12 * (1.0 + thermalFactor);
        this.accumulatedInstability += shock;

        // 腐化维度会直接污染溶液底色，增加初始浑浊度
        double corrupitAmount = ingredientVector.getAmount(AspectType.CORRUPTI);
        if (corrupitAmount > 0) {
            this.turbidity = Mth.clamp(this.turbidity + corrupitAmount * 0.015, 0.0, 1.5);
        }

        // 汇入待溶解缓冲池
        this.dissolvingBuffer = this.dissolvingBuffer.add(ingredientVector);
    }

    /**
     * 玩家使用搅拌棒（Stirring Rod）或机械动力装置对溶液施加旋转力矩。
     * <p>
     * 物理特性：
     * <ul>
     *     <li><b>顺时针搅拌（Clockwise）</b>：产生向心涡流，镇压并消散瞬时失稳度（Instability）。</li>
     *     <li><b>逆时针搅拌（Counter-Clockwise）</b>：产生离心剪切力，利用旋转离心将高密度的杂质沉淀撇除，降低浑浊度（Turbidity），但会轻微扰动以太导致少许失稳。</li>
     * </ul>
     *
     * @param clockwise true 为顺时针，false 为逆时针
     * @param speed     施加的角速度增量 (建议范围 0.05 ~ 0.35)
     */
    public void stir(boolean clockwise, double speed) {
        double clampedSpeed = Mth.clamp(speed, 0.01, 0.5);
        double signedSpeed = clockwise ? clampedSpeed : -clampedSpeed;

        // 若突然逆转搅拌方向，流体湍流对撞会引发额外的失稳惩罚
        if (this.stirVelocity * signedSpeed < -0.01) {
            this.accumulatedInstability += Math.abs(this.stirVelocity - signedSpeed) * 0.15;
        }

        this.stirVelocity = Mth.clamp(this.stirVelocity + signedSpeed, -1.0, 1.0);

        if (clockwise) {
            // 顺时针：平复以太躁动
            this.accumulatedInstability = Math.max(0.0, this.accumulatedInstability - clampedSpeed * 0.08);
        } else {
            // 逆时针：离心排杂，但略微激化能量
            this.turbidity = Math.max(0.0, this.turbidity - clampedSpeed * 0.06);
            this.accumulatedInstability += clampedSpeed * 0.02;
        }
    }

    /**
     * 检查当前溶液是否触发灾难性炼金事故或完成结晶。
     */
    public MishapResult checkMishap() {
        // 1. 浑浊度爆表 -> 凝结成废渣黑泥
        if (this.turbidity >= 1.0) {
            return MishapResult.SLUDGE_COAGULATION;
        }

        // 2. 失稳度爆表 -> 根据当前热力学能级决定是毒雾沸溢还是剧烈殉爆
        if (this.accumulatedInstability >= 1.0) {
            if (this.temperature >= HeatLevel.ROILING_BOIL.getTargetTemperature() - 10.0) {
                return MishapResult.ARCANE_EXPLOSION;
            } else {
                return MishapResult.TOXIC_BOILOVER;
            }
        }

        // 3. 判定是否满足正常退火结晶条件：
        // 缓冲池已完全溶解 + 溶液冷却至 45°C 以下 + 处于有效星图节点内 + 纯度达标
        if (this.dissolvingBuffer.getTotalMagnitude() < 0.05 && this.temperature <= 45.0 && this.currentVector.getTotalMagnitude() > 1.0) {
            double x = this.currentVector.getMapX();
            double y = this.currentVector.getMapY();
            double effectivePurity = this.currentVector.getPurity() * (1.0 - this.turbidity);

            return AlchemyMap.getInstance().findHitNode(x, y)
                    .filter(node -> effectivePurity >= node.getMinPurityRequired())
                    .map(node -> MishapResult.CRYSTALLIZE_SUCCESS)
                    .orElse(MishapResult.NONE);
        }

        return MishapResult.NONE;
    }

    // --- 内部物理演进子例程 ---

    private void updateStirPhysics() {
        if (Math.abs(this.stirVelocity) > 1e-4) {
            double prevProgress = this.stirProgress;
            this.stirProgress += this.stirVelocity;

            // 每满 2π 弧度计为一整圈
            int prevTurnIndex = (int) (prevProgress / Mth.TWO_PI);
            int currTurnIndex = (int) (this.stirProgress / Mth.TWO_PI);

            if (currTurnIndex > prevTurnIndex) {
                this.clockwiseTurns += (currTurnIndex - prevTurnIndex);
            } else if (currTurnIndex < prevTurnIndex) {
                this.counterClockwiseTurns += (prevTurnIndex - currTurnIndex);
            }

            // 流体粘滞阻尼（浑浊度越高，溶液越粘稠，阻尼越大）
            double viscosityDrag = 0.92 - (this.turbidity * 0.08);
            this.stirVelocity *= viscosityDrag;
        } else {
            this.stirVelocity = 0.0;
        }
    }

    private void processDissolutionAndReaction() {
        double bufferMass = this.dissolvingBuffer.getTotalMagnitude();
        if (bufferMass <= 1e-4) {
            if (bufferMass > 0) this.dissolvingBuffer = AspectVector.EMPTY;
            return;
        }

        // 溶解速率取决于温度（阿伦尼乌斯方程近似）与当前搅拌速率
        double thermalFactor = Math.max(0.05, (this.temperature - 20.0) / 130.0);
        double stirBoost = 1.0 + Math.abs(this.stirVelocity) * 2.5;
        double stepRatio = Mth.clamp(0.02 * thermalFactor * stirBoost, 0.002, 0.25);

        AspectVector delta = this.dissolvingBuffer.scale(stepRatio);
        this.dissolvingBuffer = this.dissolvingBuffer.scale(1.0 - stepRatio);
        this.currentVector = this.currentVector.add(delta);

        // 对立源质湮灭反应（Solaris vs Lunaris, Aetheris vs Corporis）：
        // 在高温下，溶液中同时存在的对立源质会发生缓慢湮灭，抵消彼此并释放废热与浑浊残渣
        if (this.temperature > 60.0) {
            performOppositeAnnihilation(AspectType.SOLARIS, AspectType.LUNARIS, 0.008);
            performOppositeAnnihilation(AspectType.AETHERIS, AspectType.CORPORIS, 0.005);
        }
    }

    private void performOppositeAnnihilation(AspectType a, AspectType b, double rate) {
        double valA = this.currentVector.getAmount(a);
        double valB = this.currentVector.getAmount(b);
        double overlap = Math.min(valA, valB);

        if (overlap > 0.1) {
            double annihilated = overlap * rate;
            Map<AspectType, Double> deltaMap = new EnumMap<>(AspectType.class);
            for (AspectType type : AspectType.values()) {
                double curr = this.currentVector.getAmount(type);
                if (type == a || type == b) {
                    deltaMap.put(type, Math.max(0.0, curr - annihilated));
                } else {
                    deltaMap.put(type, curr);
                }
            }
            this.currentVector = new AspectVector(deltaMap);
            // 湮灭反应放热并产生少量焦化杂质与失稳
            this.temperature += annihilated * 0.4;
            this.turbidity = Mth.clamp(this.turbidity + annihilated * 0.004, 0.0, 1.5);
            this.accumulatedInstability += annihilated * 0.006;
        }
    }

    private void processEnvironmentalInstability() {
        double x = this.currentVector.getMapX();
        double y = this.currentVector.getMapY();

        // 1. 检查星图危险区效应
        Optional<HazardZone> activeHazard = AlchemyMap.getInstance().getActiveHazard(x, y);
        if (activeHazard.isPresent()) {
            HazardZone zone = activeHazard.get();
            double intensity = zone.getIntensityAt(x, y);
            switch (zone.type()) {
                case VOID_VORTEX -> this.accumulatedInstability += intensity;
                case BONE_GRAVEYARD -> this.turbidity = Mth.clamp(this.turbidity + intensity, 0.0, 1.5);
                case FROST_DEADZONE -> this.temperature = Math.max(0.0, this.temperature - intensity * 25.0);
            }
        }

        // 2. 溶液自身内在张力与温度的耦合演进
        double intrinsicInstability = this.currentVector.getInstabilityFactor();
        if (this.temperature > 100.0) {
            // 高温沸腾下，不稳定的溶液会持续累积失稳值
            double overheatRatio = (this.temperature - 100.0) / 200.0;
            this.accumulatedInstability += intrinsicInstability * overheatRatio * 0.008;
        } else {
            // 低温静置时，溶液的瞬时失稳会极其缓慢地自然平复
            this.accumulatedInstability = Math.max(0.0, this.accumulatedInstability - 0.0015);
        }
    }

    // --- NBT 持久化序列化 (供 CauldronBlockEntity 存档与网络同步使用) ---

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.put("CurrentVector", this.currentVector.serializeNBT());
        tag.put("DissolvingBuffer", this.dissolvingBuffer.serializeNBT());
        tag.putDouble("Temperature", this.temperature);
        tag.putString("HeatLevel", this.activeHeatLevel.name());
        tag.putDouble("StirProgress", this.stirProgress);
        tag.putDouble("StirVelocity", this.stirVelocity);
        tag.putInt("ClockwiseTurns", this.clockwiseTurns);
        tag.putInt("CounterClockwiseTurns", this.counterClockwiseTurns);
        tag.putDouble("Instability", this.accumulatedInstability);
        tag.putDouble("Turbidity", this.turbidity);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        if (tag.contains("CurrentVector")) {
            this.currentVector = AspectVector.deserializeNBT(tag.getCompound("CurrentVector"));
        }
        if (tag.contains("DissolvingBuffer")) {
            this.dissolvingBuffer = AspectVector.deserializeNBT(tag.getCompound("DissolvingBuffer"));
        }
        this.temperature = tag.getDouble("Temperature");
        try {
            this.activeHeatLevel = HeatLevel.valueOf(tag.getString("HeatLevel"));
        } catch (IllegalArgumentException e) {
            this.activeHeatLevel = HeatLevel.COLD;
        }
        this.stirProgress = tag.getDouble("StirProgress");
        this.stirVelocity = tag.getDouble("StirVelocity");
        this.clockwiseTurns = tag.getInt("ClockwiseTurns");
        this.counterClockwiseTurns = tag.getInt("CounterClockwiseTurns");
        this.accumulatedInstability = tag.getDouble("Instability");
        this.turbidity = tag.getDouble("Turbidity");
    }

    // --- Getters ---

    public AspectVector getCurrentVector() { return currentVector; }
    public AspectVector getDissolvingBuffer() { return dissolvingBuffer; }
    public double getTemperature() { return temperature; }
    public HeatLevel getActiveHeatLevel() { return activeHeatLevel; }
    public double getStirProgress() { return stirProgress; }
    public double getStirVelocity() { return stirVelocity; }
    public int getClockwiseTurns() { return clockwiseTurns; }
    public int getCounterClockwiseTurns() { return counterClockwiseTurns; }
    public double getAccumulatedInstability() { return accumulatedInstability; }
    public double getTurbidity() { return turbidity; }
}
```