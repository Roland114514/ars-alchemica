package com.medievalmagic.arsalchemica.api.cauldron;

import com.medievalmagic.arsalchemica.api.aspect.AlchemyAspect;
import com.medievalmagic.arsalchemica.cauldron.mishap.AlchemicalMishapType;
import com.medievalmagic.arsalchemica.api.map.AlchemyMap;
import com.medievalmagic.arsalchemica.api.map.AlchemyMap.HazardZone;
import com.medievalmagic.arsalchemica.api.vector.AspectVector;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

import java.util.Optional;

/**
 * 坩埚内溶液的微观热力学与以太动力学状态机。
 * <p>
 * 负责在每个游戏刻（Tick）演进溶液的温度、星图坐标推进、搅拌流体惯性、对立元素湮灭以及失稳事故判定。
 * </p>
 *
 * @author MedievalMagic Architecture Team
 * @version 1.0.0
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
        /** 剧烈沸腾（岩浆/地狱火）：150°C，常规大宗药剂合成温度 */
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
        /** 沸溢毒雾：溶液失稳过高但温度未达极温，向四周喷发剧毒滞留药水云 */
        TOXIC_BOILOVER,
        /** 炼金败物（Sludge）：浑浊度满溢，药液彻底报废并凝结成腐蚀性黑泥 */
        SLUDGE_COAGULATION,
        /** 剧烈以太爆炸：高温伴随失稳满溢，引发摧毁周围并点燃烈焰的爆炸 */
        ARCANE_EXPLOSION
    }

    /** 坩埚单次反应的最大溶液溶解容量（质量单位），超过此容量将引发过饱和杂质沉淀 */
    public static final double MAX_CAPACITY_MASS = 250.0;

    // --- 核心物理状态量 ---

    /** 当前溶液的累积已溶解元素向量 */
    private AspectVector currentVector = AspectVector.ZERO;
    /** 刚投入但尚未完全溶解进主溶液的缓冲向量（随温度与搅拌逐步融合） */
    private AspectVector dissolvingBuffer = AspectVector.ZERO;

    /** 当前溶液实时摄氏度 (°C) */
    private double temperature = 20.0;
    /** 当前外接热源等级 */
    private HeatLevel activeHeatLevel = HeatLevel.COLD;

    /** 搅拌当前相位进度（范围归一化在 [-2pi, 2pi]，防止长期运算产生浮点精度漂移） */
    private double stirProgress = 0.0;
    /** 瞬时搅拌角速度（正值为顺时针，负值为逆时针） */
    private double stirVelocity = 0.0;
    /** 顺时针完整旋转圈数 */
    private int clockwiseTurns = 0;
    /** 逆时针完整旋转圈数 */
    private int counterClockwiseTurns = 0;

    /** 瞬时失稳蓄积值 [0.0 ~ 1.0+]，超过 1.0 将触发严重炼金事故 */
    private double accumulatedInstability = 0.0;
    /** 溶液浑浊度/杂质度 [0.0 ~ 1.0+]，由死灵/混沌元素或对立湮灭残渣产生 */
    private double turbidity = 0.0;
    /** 是否已完成退火结晶定型（防止每 tick 重复触发成功结算） */
    private boolean crystallized = false;
    /** 干烧累积 Ticks (当无水且底部受热时持续攀升，达到 200 ticks 发生干烧爆裂) */
    private int dryBoilTicks = 0;

    // --- 状态机推进核心方法 ---

    /**
     * 每个服务端 Tick 调用一次，演进坩埚内的热力学与流体反应。
     *
     * @param hasHeatSource 下方是否存在有效热源方块
     * @param heatLevel     当前热源提供的热量阶级
     */
    public void tick(boolean hasHeatSource, HeatLevel heatLevel) {
        tick(hasHeatSource, heatLevel, 1);
    }

    /**
     * 带液面刻度的服务端 Tick 调用。
     */
    public void tick(boolean hasHeatSource, HeatLevel heatLevel, int waterLevel) {
        this.activeHeatLevel = hasHeatSource ? heatLevel : HeatLevel.COLD;

        // 1. 牛顿冷却与加热定律：dT/dt = k * (T_env - T) (包含死区防渐近漂移)
        double targetTemp = this.activeHeatLevel.getTargetTemperature();
        double k = this.activeHeatLevel.getThermalConductivity();
        double diff = targetTemp - this.temperature;
        if (Math.abs(diff) < 0.05) {
            this.temperature = targetTemp;
        } else {
            this.temperature += diff * k;
        }

        // 干烧累积判定：无水且持续受热
        if (waterLevel <= 0 && hasHeatSource && heatLevel != HeatLevel.COLD) {
            this.dryBoilTicks++;
        } else {
            this.dryBoilTicks = Math.max(0, this.dryBoilTicks - 2);
        }

        // 2. 搅拌流体阻尼与圈数统计
        updateStirPhysics();

        // 3. 缓冲元素溶解与星图坐标推进
        processDissolutionAndReaction();

        // 4. 星图环境场交互（危险区辐射与自然热力学熵增）
        processEnvironmentalInstability();
    }

    /**
     * 向坩埚内投入新的炼金材料向量。
     * 材料不会瞬间改变当前坐标，而是进入溶解缓冲池，并产生一次性的入水热冲击。
     */
    public void addIngredient(AspectVector ingredientVector) {
        if (ingredientVector == null || ingredientVector.isEmpty() || this.crystallized) {
            return;
        }

        // 饱和度保护：若总质量已达上限，额外添加的材料无法溶解并转化为杂质废渣
        double currentTotalMass = this.currentVector.getTotalAspectMass() + this.dissolvingBuffer.getTotalAspectMass();
        if (currentTotalMass >= MAX_CAPACITY_MASS) {
            this.turbidity = Mth.clamp(this.turbidity + ingredientVector.getTotalAspectMass() * 0.02, 0.0, 1.5);
            this.accumulatedInstability += 0.05;
            return;
        }

        // 若在剧烈沸腾时贸然投入高不稳定性药材，直接产生热冲击失稳
        double thermalFactor = Math.max(0.0, (this.temperature - 80.0) / 100.0);
        double shock = ingredientVector.calculateInstability() * 0.12 * (1.0 + thermalFactor);
        this.accumulatedInstability += shock;

        // 混沌与死灵元素会直接增加初始浑浊度
        double darkMass = ingredientVector.getAmount(AlchemyAspect.MORTIS) + ingredientVector.getAmount(AlchemyAspect.CHAOS);
        if (darkMass > 0.0) {
            this.turbidity = Mth.clamp(this.turbidity + darkMass * 0.015, 0.0, 1.5);
        }

        // 汇入待溶解缓冲池
        this.dissolvingBuffer = this.dissolvingBuffer.add(ingredientVector);
    }

    /**
     * 玩家使用搅拌棒（Stirring Rod）对溶液施加旋转力矩。
     * <p>
     * 物理特性：
     * <ul>
     *     <li><b>顺时针搅拌（Clockwise）</b>：产生向心涡流，平复并消散瞬时失稳度（Instability）。</li>
     *     <li><b>逆时针搅拌（Counter-Clockwise）</b>：产生离心剪切力，离心排出杂质沉淀，降低浑浊度（Turbidity），但会轻微扰动能量导致少许失稳。</li>
     * </ul>
     * </p>
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
     * 清理凝固黑泥废渣。
     *
     * @return true 若成功清除了黑泥；false 若当前并非凝固黑泥状态。
     */
    public boolean clearSludge() {
        if (this.turbidity >= 1.0) {
            this.turbidity = 0.0;
            this.accumulatedInstability = 0.0;
            this.currentVector = AspectVector.ZERO;
            this.dissolvingBuffer = AspectVector.ZERO;
            return true;
        }
        return false;
    }

    /**
     * 深度检查当前状态是否触发具体的细分炼金事故。
     */
    public AlchemicalMishapType checkDetailedMishap() {
        if (this.crystallized) {
            return AlchemicalMishapType.NONE;
        }

        // 1. 干烧超载崩解
        if (this.dryBoilTicks >= 200) {
            return AlchemicalMishapType.DRY_BOIL_MELTDOWN;
        }

        // 2. 浑浊度爆表 -> 凝结成废渣黑泥
        if (this.turbidity >= 1.0) {
            return AlchemicalMishapType.SLUDGE_COAGULATION;
        }

        // 3. 高挥发易燃质点爆燃火灾
        if (this.currentVector.calculateVolatility() > 0.80 && this.accumulatedInstability >= 0.60 && this.temperature >= 120.0) {
            return AlchemicalMishapType.INFERNAL_CONFLAGRATION;
        }

        // 4. 失稳度爆表 -> 根据当前热力学能级决定是毒雾沸溢还是剧烈殉爆
        if (this.accumulatedInstability >= 1.0) {
            if (this.temperature >= HeatLevel.ROILING_BOIL.getTargetTemperature() - 10.0) {
                return AlchemicalMishapType.ARCANE_DETONATION;
            } else {
                return AlchemicalMishapType.TOXIC_MIASMA_OUTBREAK;
            }
        }

        // 5. 判定是否满足正常退火结晶条件：
        // 缓冲池已完全溶解 + 溶液冷却至 45°C 以下 + 处于有效星图节点内 + 纯度达标
        if (this.dissolvingBuffer.getTotalAspectMass() < 0.05 && this.temperature <= 45.0 && this.currentVector.getTotalAspectMass() > 1.0) {
            double x = this.currentVector.getX();
            double y = this.currentVector.getY();
            double effectivePurity = this.currentVector.calculatePurity() * (1.0 - this.turbidity);

            Optional<com.medievalmagic.arsalchemica.api.map.AlchemyMapNode> hit = AlchemyMap.getInstance().findHitNode(x, y);
            if (hit.isPresent() && effectivePurity >= hit.get().getMinPurityRequired()) {
                this.crystallized = true;
                return AlchemicalMishapType.CRYSTALLIZE_SUCCESS;
            }
        }

        return AlchemicalMishapType.NONE;
    }

    /**
     * 兼容老旧接口的快速事故检查。
     */
    public MishapResult checkMishap() {
        AlchemicalMishapType detailed = checkDetailedMishap();
        return switch (detailed) {
            case ARCANE_DETONATION, DRY_BOIL_MELTDOWN -> MishapResult.ARCANE_EXPLOSION;
            case TOXIC_MIASMA_OUTBREAK -> MishapResult.TOXIC_BOILOVER;
            case INFERNAL_CONFLAGRATION -> MishapResult.ARCANE_EXPLOSION;
            case SLUDGE_COAGULATION -> MishapResult.SLUDGE_COAGULATION;
            case CRYSTALLIZE_SUCCESS -> MishapResult.CRYSTALLIZE_SUCCESS;
            default -> MishapResult.NONE;
        };
    }

    // --- 内部物理演进子例程 ---

    private void updateStirPhysics() {
        if (Math.abs(this.stirVelocity) > 1e-4) {
            this.stirProgress += this.stirVelocity;

            // 相位归一化防溢出：每满 2pi 增加一圈并扣除相位，保持 stirProgress 在 [-2pi, 2pi] 范围
            if (this.stirProgress >= Mth.TWO_PI) {
                int turns = (int) (this.stirProgress / Mth.TWO_PI);
                this.clockwiseTurns += turns;
                this.stirProgress -= turns * Mth.TWO_PI;
            } else if (this.stirProgress <= -Mth.TWO_PI) {
                int turns = (int) (-this.stirProgress / Mth.TWO_PI);
                this.counterClockwiseTurns += turns;
                this.stirProgress += turns * Mth.TWO_PI;
            }

            // 流体粘滞阻尼（浑浊度越高，溶液越粘稠，阻尼越大）
            double viscosityDrag = 0.92 - (this.turbidity * 0.08);
            this.stirVelocity *= viscosityDrag;
        } else {
            this.stirVelocity = 0.0;
        }
    }

    private void processDissolutionAndReaction() {
        double bufferMass = this.dissolvingBuffer.getTotalAspectMass();
        if (bufferMass <= 1e-4) {
            if (bufferMass > 0) this.dissolvingBuffer = AspectVector.ZERO;
            return;
        }

        // 溶解速率取决于温度与当前搅拌速率
        double thermalFactor = Math.max(0.05, (this.temperature - 20.0) / 130.0);
        double stirBoost = 1.0 + Math.abs(this.stirVelocity) * 2.5;
        double stepRatio = Mth.clamp(0.02 * thermalFactor * stirBoost, 0.002, 0.25);

        AspectVector delta = this.dissolvingBuffer.multiply(stepRatio);
        this.dissolvingBuffer = this.dissolvingBuffer.multiply(1.0 - stepRatio);
        this.currentVector = this.currentVector.add(delta);

        // 高温下内部消解反应
        if (this.temperature > 60.0) {
            this.currentVector = this.currentVector.resolveAnnihilation(0.005);
        }
    }

    private void processEnvironmentalInstability() {
        double x = this.currentVector.getX();
        double y = this.currentVector.getY();

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
        double intrinsicInstability = this.currentVector.calculateInstability();
        if (this.temperature > 100.0) {
            // 高温沸腾下，不稳定的溶液会持续累积失稳值
            double overheatRatio = (this.temperature - 100.0) / 200.0;
            this.accumulatedInstability += intrinsicInstability * overheatRatio * 0.008;
        } else {
            // 低温静置时，溶液的瞬时失稳会缓慢自然平复
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
        tag.putBoolean("Crystallized", this.crystallized);
        tag.putInt("DryBoilTicks", this.dryBoilTicks);
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
        if (Double.isNaN(this.temperature) || Double.isInfinite(this.temperature)) {
            this.temperature = 20.0;
        }
        try {
            this.activeHeatLevel = HeatLevel.valueOf(tag.getString("HeatLevel"));
        } catch (IllegalArgumentException e) {
            this.activeHeatLevel = HeatLevel.COLD;
        }
        this.stirProgress = tag.getDouble("StirProgress");
        this.stirVelocity = tag.getDouble("StirVelocity");
        this.clockwiseTurns = tag.getInt("ClockwiseTurns");
        this.counterClockwiseTurns = tag.getInt("CounterClockwiseTurns");
        this.accumulatedInstability = Math.max(0.0, tag.getDouble("Instability"));
        this.turbidity = Math.max(0.0, tag.getDouble("Turbidity"));
        this.crystallized = tag.getBoolean("Crystallized");
        this.dryBoilTicks = tag.getInt("DryBoilTicks");
    }

    // --- Getters & Reset ---

    public boolean isCrystallized() { return crystallized; }
    public int getDryBoilTicks() { return dryBoilTicks; }
    public boolean isDryBoiling() { return dryBoilTicks > 0; }

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

    public void reset() {
        this.currentVector = AspectVector.ZERO;
        this.dissolvingBuffer = AspectVector.ZERO;
        this.temperature = 20.0;
        this.activeHeatLevel = HeatLevel.COLD;
        this.stirProgress = 0.0;
        this.stirVelocity = 0.0;
        this.clockwiseTurns = 0;
        this.counterClockwiseTurns = 0;
        this.accumulatedInstability = 0.0;
        this.turbidity = 0.0;
        this.crystallized = false;
        this.dryBoilTicks = 0;
    }
}
