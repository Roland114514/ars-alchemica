package com.medievalmagic.arsalchemica.api.vector;

import com.medievalmagic.arsalchemica.api.aspect.AlchemyAspect;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * 炼金连续向量空间 (Aspect Vector Space)。
 * <p>
 * 不可变（Immutable）值对象，用于精确描述炼金材料的元素构成、坩埚溶液的实时状态以及星图（Astral Map）上的动力学轨迹。
 * 该空间是一个 2 + 12 维的复合流形：包含二维显式星图坐标 (x, y) 与十二维元素丰度分量。
 * </p>
 * <p>
 * <b>性能设计：</b>内部采用基于枚举序号（Ordinal）的定长 {@code double[]} 数组存储，
 * 避免在高频 Tick 运算中产生对象分配与哈希表寻址开销。
 * </p>
 *
 * @author MedievalMagic Architecture Team
 * @version 1.0.0
 */
public final class AspectVector {

    /** 浮点数精度容差阈值 (epsilon) */
    public static final double EPSILON = 1.0E-6;

    /** 零向量常量（空溶液/星图原点） */
    public static final AspectVector ZERO = new AspectVector(0.0, 0.0, new double[AlchemyAspect.COUNT]);

    private final double x;
    private final double y;
    private final double[] components;

    // 惰性求值/构造期预计算的标量缓存
    private final double totalMass;
    private final int hashCode;

    /**
     * 私有主构造函数。进行彻底的不可变防御性拷贝与数值净化。
     *
     * @param x          星图 X 轴坐标
     * @param y          星图 Y 轴坐标
     * @param components 长度为 {@link AlchemyAspect#COUNT} 的元素含量数组
     */
    private AspectVector(double x, double y, double[] components) {
        this.x = sanitizeDouble(x);
        this.y = sanitizeDouble(y);
        this.components = new double[AlchemyAspect.COUNT];

        double sum = 0.0;
        if (components != null) {
            int len = Math.min(components.length, AlchemyAspect.COUNT);
            for (int i = 0; i < len; i++) {
                double val = sanitizeDouble(components[i]);
                if (val < EPSILON) {
                    val = 0.0;
                }
                this.components[i] = val;
                sum += val;
            }
        }
        this.totalMass = sum;
        this.hashCode = computeHash();
    }

    /**
     * 通过二维星图坐标与元素映射表构建向量。
     *
     * @param x         星图 X 轴坐标
     * @param y         星图 Y 轴坐标
     * @param aspectMap 元素含量映射表
     * @return 新的不可变 AspectVector 实例
     */
    public static AspectVector of(double x, double y, Map<AlchemyAspect, Double> aspectMap) {
        Objects.requireNonNull(aspectMap, "Aspect map cannot be null");
        double[] comps = new double[AlchemyAspect.COUNT];
        for (Map.Entry<AlchemyAspect, Double> entry : aspectMap.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                comps[entry.getKey().ordinal()] = Math.max(0.0, entry.getValue());
            }
        }
        return new AspectVector(x, y, comps);
    }

    /**
     * 仅通过元素映射表构建向量，其二维星图位移 (x, y) 将根据所含元素的内禀星图轴向自动投影生成。
     *
     * @param aspectMap 元素含量映射表
     * @return 自动计算星图投影的 AspectVector 实例
     */
    public static AspectVector fromAspectsIntrinsic(Map<AlchemyAspect, Double> aspectMap) {
        Objects.requireNonNull(aspectMap, "Aspect map cannot be null");
        double[] comps = new double[AlchemyAspect.COUNT];
        double projX = 0.0;
        double projY = 0.0;

        for (Map.Entry<AlchemyAspect, Double> entry : aspectMap.entrySet()) {
            AlchemyAspect aspect = entry.getKey();
            Double amount = entry.getValue();
            if (aspect != null && amount != null && amount > EPSILON) {
                comps[aspect.ordinal()] = amount;
                // 结合元素极性权重与内禀轴向计算星图投影
                projX += amount * aspect.getAstralAxisX();
                projY += amount * aspect.getAstralAxisY();
            }
        }
        return new AspectVector(projX, projY, comps);
    }

    /**
     * 获取构建器实例，用于链式构建复杂的元素向量。
     *
     * @return Builder 实例
     */
    public static Builder builder() {
        return new Builder();
    }

    private static double sanitizeDouble(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return value;
    }

    // ================= 基础访问器 =================

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    /**
     * 获取指定炼金元素的含量。
     *
     * @param aspect 目标元素
     * @return 元素含量 (>= 0.0)
     */
    public double getAmount(AlchemyAspect aspect) {
        if (aspect == null) return 0.0;
        return components[aspect.ordinal()];
    }

    /**
     * 获取所有非零元素的只读视图映射。
     *
     * @return 不可变的 EnumMap
     */
    public Map<AlchemyAspect, Double> asMap() {
        Map<AlchemyAspect, Double> map = new EnumMap<>(AlchemyAspect.class);
        for (int i = 0; i < AlchemyAspect.COUNT; i++) {
            if (components[i] > 0.0) {
                map.put(AlchemyAspect.VALUES[i], components[i]);
            }
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * 获取向量中所有元素的总质量（即元素空间的 L1 范数）。
     *
     * @return 元素总和
     */
    public double getTotalAspectMass() {
        return totalMass;
    }

    public boolean isEmpty() {
        return totalMass < EPSILON && Math.abs(x) < EPSILON && Math.abs(y) < EPSILON;
    }

    // ================= 向量空间代数运算 =================

    /**
     * 向量加法：v_new = v_1 + v_2。
     * 同时叠加星图二维坐标与十二维元素含量。
     *
     * @param other 另一个向量
     * @return 叠加后的新向量
     */
    public AspectVector add(AspectVector other) {
        Objects.requireNonNull(other, "Cannot add a null AspectVector");
        if (other.isEmpty()) return this;
        if (this.isEmpty()) return other;

        double[] newComps = new double[AlchemyAspect.COUNT];
        for (int i = 0; i < AlchemyAspect.COUNT; i++) {
            newComps[i] = this.components[i] + other.components[i];
        }
        return new AspectVector(this.x + other.x, this.y + other.y, newComps);
    }

    /**
     * 标量乘法：v_new = c * v。
     * 注意：若标量 c < 0，星图坐标会反向，但元素质量由于物理限制将被截断为 0。
     *
     * @param scalar 缩放标量
     * @return 缩放后的新向量
     */
    public AspectVector multiply(double scalar) {
        if (Math.abs(scalar) < EPSILON || this.isEmpty()) {
            return ZERO;
        }
        if (Math.abs(scalar - 1.0) < EPSILON) {
            return this;
        }

        double[] newComps = new double[AlchemyAspect.COUNT];
        if (scalar > 0) {
            for (int i = 0; i < AlchemyAspect.COUNT; i++) {
                newComps[i] = this.components[i] * scalar;
            }
        }
        return new AspectVector(this.x * scalar, this.y * scalar, newComps);
    }

    /**
     * 计算与另一向量的全空间欧几里得距离 (L2 Metric)。
     *
     * @param other 目标向量
     * @return 欧氏距离
     */
    public double distanceTo(AspectVector other) {
        Objects.requireNonNull(other, "Target AspectVector cannot be null");
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        double sumSq = dx * dx + dy * dy;

        for (int i = 0; i < AlchemyAspect.COUNT; i++) {
            double diff = this.components[i] - other.components[i];
            sumSq += diff * diff;
        }
        double res = Math.sqrt(sumSq);
        return Double.isFinite(res) ? res : 0.0;
    }

    /**
     * 仅计算二维星图平面上的欧氏距离（用于星图节点吸附判定）。
     *
     * @param other 目标向量
     * @return 星图平面距离
     */
    public double astralDistanceTo(AspectVector other) {
        Objects.requireNonNull(other, "Target AspectVector cannot be null");
        double res = Math.hypot(this.x - other.x, this.y - other.y);
        return Double.isFinite(res) ? res : 0.0;
    }

    /**
     * 计算与另一向量的全空间曼哈顿距离 (L1 Metric)。
     *
     * @param other 目标向量
     * @return 曼哈顿距离
     */
    public double manhattanDistanceTo(AspectVector other) {
        Objects.requireNonNull(other, "Target AspectVector cannot be null");
        double dist = Math.abs(this.x - other.x) + Math.abs(this.y - other.y);
        for (int i = 0; i < AlchemyAspect.COUNT; i++) {
            dist += Math.abs(this.components[i] - other.components[i]);
        }
        return dist;
    }

    /**
     * 计算两个向量的内积（点积 Dot Product）。
     *
     * @param other 另一向量
     * @return 点积标量值
     */
    public double dotProduct(AspectVector other) {
        Objects.requireNonNull(other, "Target AspectVector cannot be null");
        double dot = this.x * other.x + this.y * other.y;
        for (int i = 0; i < AlchemyAspect.COUNT; i++) {
            dot += this.components[i] * other.components[i];
        }
        return Double.isFinite(dot) ? dot : 0.0;
    }

    /**
     * 计算向量在全空间的欧几里得范数 (L2 Norm / 模长)。
     */
    public double norm() {
        double sumSq = this.x * this.x + this.y * this.y;
        for (int i = 0; i < AlchemyAspect.COUNT; i++) {
            sumSq += this.components[i] * this.components[i];
        }
        double res = Math.sqrt(sumSq);
        return (!Double.isFinite(res) || res < EPSILON) ? 0.0 : res;
    }

    /**
     * 计算与另一个向量的余弦相似度 [-1.0, 1.0]。
     * 防御性处理零模长，杜绝除零产生 NaN。
     */
    public double cosineSimilarity(AspectVector other) {
        if (other == null) return 0.0;
        double n1 = this.norm();
        double n2 = other.norm();
        if (n1 < EPSILON || n2 < EPSILON) {
            return 0.0;
        }
        double dot = this.dotProduct(other);
        double cos = dot / (n1 * n2);
        if (!Double.isFinite(cos)) return 0.0;
        return Math.max(-1.0, Math.min(1.0, cos));
    }

    // ================= 炼金动力学与高阶属性计算 =================

    /**
     * 计算炼金纯度 (Alchemical Purity) in [0.0, 1.0]。
     * <p>
     * 采用赫芬达尔-赫希曼指数 (Herfindahl-Hirschman Index, HHI) 模型：
     * Purity = sum_{i=1}^N (a_i / M)^2
     * </p>
     *
     * @return 纯度系数 [0.0, 1.0]
     */
    public double calculatePurity() {
        if (this.totalMass < EPSILON) {
            return 0.0;
        }
        double sumSquaredWeights = 0.0;
        for (int i = 0; i < AlchemyAspect.COUNT; i++) {
            if (components[i] > 0.0) {
                double proportion = components[i] / this.totalMass;
                sumSquaredWeights += proportion * proportion;
            }
        }
        return Math.min(1.0, sumSquaredWeights);
    }

    /**
     * 计算混合体系的加权挥发性 (Weighted Volatility)。
     * <p>
     * 综合考量各元素的基础挥发性，以及“盐 (SAL)”的固着抑制效应和“汞 (MERCURIUS)”的活化效应。
     * </p>
     *
     * @return 综合挥发性指数 (>= 0.0)
     */
    public double calculateVolatility() {
        if (this.totalMass < EPSILON) {
            return 0.0;
        }
        double weightedVol = 0.0;
        for (int i = 0; i < AlchemyAspect.COUNT; i++) {
            double amount = components[i];
            if (amount > 0.0) {
                AlchemyAspect aspect = AlchemyAspect.VALUES[i];
                weightedVol += amount * aspect.getBaseVolatility();
            }
        }
        double baseResult = weightedVol / this.totalMass;

        // 计算三原质修正：盐抑制挥发，汞与混沌加剧挥发
        double salRatio = getAmount(AlchemyAspect.SAL) / this.totalMass;
        double mercRatio = getAmount(AlchemyAspect.MERCURIUS) / this.totalMass;
        double modifier = 1.0 - (salRatio * 0.45) + (mercRatio * 0.35);

        return Math.max(0.0, baseResult * modifier);
    }

    /**
     * 计算元素相克失稳因子 (Alchemical Instability)。
     * <p>
     * 当向量中同时存在互斥元素时，会产生相克张力。
     * 失稳值由互斥元素对的几何平均数乘以消解系数，并由“汞”与“以太”提供缓冲阻尼。
     * </p>
     *
     * @return 失稳度（0.0 表示绝对稳定，> 1.0 通常意味着坩埚发生剧烈反应或即将失控）
     */
    public double calculateInstability() {
        if (this.totalMass < EPSILON) {
            return 0.0;
        }

        double conflictEnergy = 0.0;
        double stabilizingMass = getAmount(AlchemyAspect.MERCURIUS) * 0.5
                               + getAmount(AlchemyAspect.AETHER) * 0.8
                               + getAmount(AlchemyAspect.ORDO) * 0.3;

        // 遍历所有元素对 (i, j) where i < j
        for (int i = 0; i < AlchemyAspect.COUNT; i++) {
            double massA = components[i];
            if (massA <= EPSILON) continue;
            AlchemyAspect aspectA = AlchemyAspect.VALUES[i];

            for (int j = i + 1; j < AlchemyAspect.COUNT; j++) {
                double massB = components[j];
                if (massB <= EPSILON) continue;
                AlchemyAspect aspectB = AlchemyAspect.VALUES[j];

                double factor = aspectA.getAnnihilationFactor(aspectB);
                if (factor > 0.0) {
                    double interactionScale = Math.sqrt(massA * massB);
                    conflictEnergy += interactionScale * factor;
                }
            }
        }

        if (conflictEnergy <= 0.0) {
            return 0.0;
        }

        // 引入纯度熵：元素种类越杂乱（纯度越低），基础背景失稳越高
        double entropyMultiplier = 1.0 + (1.0 - calculatePurity()) * 0.5;
        // 阻尼衰减函数
        double damping = 1.0 / (1.0 + (stabilizingMass / this.totalMass) * 2.0);

        return (conflictEnergy / Math.sqrt(this.totalMass)) * entropyMultiplier * damping;
    }

    /**
     * 执行内部相克消解反应 (Resolve Mutual Annihilation)。
     * <p>
     * 互斥元素按 1:1 质量比发生中和湮灭，返回消除矛盾后的稳定向量。
     * </p>
     *
     * @param efficiency 消解反应程度 in (0.0, 1.0]
     * @return 消解反应后的新 AspectVector
     */
    public AspectVector resolveAnnihilation(double efficiency) {
        double clampedEff = Math.max(0.0, Math.min(1.0, efficiency));
        if (clampedEff < EPSILON || this.totalMass < EPSILON) {
            return this;
        }

        double[] newComps = Arrays.copyOf(this.components, AlchemyAspect.COUNT);
        boolean modified = false;

        for (int i = 0; i < AlchemyAspect.COUNT; i++) {
            AlchemyAspect aspect = AlchemyAspect.VALUES[i];
            AlchemyAspect opposite = aspect.getOpposite().orElse(null);

            if (opposite != null && i < opposite.ordinal()) {
                int oppIdx = opposite.ordinal();
                double amountA = newComps[i];
                double amountB = newComps[oppIdx];

                if (amountA > EPSILON && amountB > EPSILON) {
                    double annihilated = Math.min(amountA, amountB) * clampedEff;
                    newComps[i] -= annihilated;
                    newComps[oppIdx] -= annihilated;
                    modified = true;
                }
            }
        }

        return modified ? new AspectVector(this.x, this.y, newComps) : this;
    }

    /**
     * 动态计算溶液/魔法效果的混合 RGB 颜色。
     * 采用均方根伽马近似校正 (Root-Mean-Square Gamma Approximation, gamma = 2.0) 保持高饱和度。
     *
     * @return 24位 RGB 十六进制颜色值 (0xRRGGBB)。
     */
    public int calculateBlendedColor() {
        if (this.totalMass < EPSILON) {
            return 0x101018; // 默认基底暗色
        }

        double sumR2 = 0.0;
        double sumG2 = 0.0;
        double sumB2 = 0.0;

        for (int i = 0; i < AlchemyAspect.COUNT; i++) {
            double weight = components[i] / this.totalMass;
            if (weight > 0.0) {
                AlchemyAspect aspect = AlchemyAspect.VALUES[i];
                double r = aspect.getRed();
                double g = aspect.getGreen();
                double b = aspect.getBlue();

                sumR2 += weight * (r * r);
                sumG2 += weight * (g * g);
                sumB2 += weight * (b * b);
            }
        }

        int finalR = Math.min(255, Math.max(0, (int) Math.round(Math.sqrt(sumR2))));
        int finalG = Math.min(255, Math.max(0, (int) Math.round(Math.sqrt(sumG2))));
        int finalB = Math.min(255, Math.max(0, (int) Math.round(Math.sqrt(sumB2))));

        return (finalR << 16) | (finalG << 8) | finalB;
    }

    // ================= NBT 序列化与反序列化 =================

    /**
     * 将向量序列化写入 NBT 复合标签（存储于 ItemStack、BlockEntity 或网络同步数据包）。
     */
    public net.minecraft.nbt.CompoundTag serializeNBT() {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putDouble("X", this.x);
        tag.putDouble("Y", this.y);
        net.minecraft.nbt.CompoundTag elementsTag = new net.minecraft.nbt.CompoundTag();
        for (int i = 0; i < AlchemyAspect.COUNT; i++) {
            if (this.components[i] > EPSILON) {
                elementsTag.putDouble(AlchemyAspect.VALUES[i].getId(), this.components[i]);
            }
        }
        tag.put("Elements", elementsTag);
        return tag;
    }

    /**
     * 从 NBT 复合标签反序列化还原 AspectVector。
     */
    public static AspectVector deserializeNBT(net.minecraft.nbt.CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return ZERO;
        }
        double x = tag.getDouble("X");
        double y = tag.getDouble("Y");
        double[] comps = new double[AlchemyAspect.COUNT];
        if (tag.contains("Elements")) {
            net.minecraft.nbt.CompoundTag elementsTag = tag.getCompound("Elements");
            for (int i = 0; i < AlchemyAspect.COUNT; i++) {
                AlchemyAspect aspect = AlchemyAspect.VALUES[i];
                if (elementsTag.contains(aspect.getId())) {
                    comps[i] = Math.max(0.0, elementsTag.getDouble(aspect.getId()));
                }
            }
        }
        return new AspectVector(x, y, comps);
    }

    // ================= 高性能网络二进制序列化 (Zero-Lag Delta Sync) =================

    /**
     * 将向量以高压缩二进制格式写入字节流。
     * <p>
     * 编码格式：
     * [Float32 X] [Float32 Y] [UInt16 存在位掩码 Bitmask] [变长 Float32 元素值...]
     * 相比全量 NBT 节省 85% 以上的网络带宽，杜绝网络缓冲区膨胀。
     * </p>
     */
    public void writeToNetwork(java.io.DataOutput buf) throws java.io.IOException {
        buf.writeFloat((float) this.x);
        buf.writeFloat((float) this.y);

        int bitmask = 0;
        for (int i = 0; i < AlchemyAspect.COUNT; i++) {
            if (this.components[i] > EPSILON) {
                bitmask |= (1 << i);
            }
        }
        buf.writeShort(bitmask);

        for (int i = 0; i < AlchemyAspect.COUNT; i++) {
            if ((bitmask & (1 << i)) != 0) {
                buf.writeFloat((float) this.components[i]);
            }
        }
    }

    /**
     * 从二进制字节流反序列化 AspectVector。
     */
    public static AspectVector readFromNetwork(java.io.DataInput buf) throws java.io.IOException {
        float x = buf.readFloat();
        float y = buf.readFloat();
        int bitmask = buf.readUnsignedShort();

        double[] comps = new double[AlchemyAspect.COUNT];
        for (int i = 0; i < AlchemyAspect.COUNT; i++) {
            if ((bitmask & (1 << i)) != 0) {
                comps[i] = Math.max(0.0, buf.readFloat());
            }
        }
        return new AspectVector(x, y, comps);
    }

    // ================= Object 契约方法 =================

    private int computeHash() {
        int result = Double.hashCode(Math.round(x * 10000.0));
        result = 31 * result + Double.hashCode(Math.round(y * 10000.0));
        for (double comp : components) {
            result = 31 * result + Double.hashCode(Math.round(comp * 10000.0));
        }
        return result;
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof AspectVector other)) return false;

        if (Math.abs(this.x - other.x) > EPSILON || Math.abs(this.y - other.y) > EPSILON) {
            return false;
        }
        for (int i = 0; i < AlchemyAspect.COUNT; i++) {
            if (Math.abs(this.components[i] - other.components[i]) > EPSILON) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("AspectVector{");
        sb.append(String.format("Astral=(%.2f, %.2f), Mass=%.2f, Elements=[", x, y, totalMass));
        boolean first = true;
        for (int i = 0; i < AlchemyAspect.COUNT; i++) {
            if (components[i] > EPSILON) {
                if (!first) sb.append(", ");
                sb.append(AlchemyAspect.VALUES[i].getId()).append(":").append(String.format("%.2f", components[i]));
                first = false;
            }
        }
        sb.append("]}");
        return sb.toString();
    }

    // ================= 流式构建器 (Builder Pattern) =================

    /**
     * {@link AspectVector} 的可变流式构建器。
     */
    public static final class Builder {
        private double x = 0.0;
        private double y = 0.0;
        private final double[] components = new double[AlchemyAspect.COUNT];

        private Builder() {}

        public Builder astralPos(double x, double y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public Builder set(AlchemyAspect aspect, double amount) {
            Objects.requireNonNull(aspect, "AlchemyAspect cannot be null");
            this.components[aspect.ordinal()] = Math.max(0.0, amount);
            return this;
        }

        public Builder add(AlchemyAspect aspect, double amount) {
            Objects.requireNonNull(aspect, "AlchemyAspect cannot be null");
            int idx = aspect.ordinal();
            this.components[idx] = Math.max(0.0, this.components[idx] + amount);
            return this;
        }

        public Builder projectAstralCoordinates() {
            for (int i = 0; i < AlchemyAspect.COUNT; i++) {
                if (components[i] > EPSILON) {
                    AlchemyAspect aspect = AlchemyAspect.VALUES[i];
                    this.x += components[i] * aspect.getAstralAxisX();
                    this.y += components[i] * aspect.getAstralAxisY();
                }
            }
            return this;
        }

        public AspectVector build() {
            return new AspectVector(x, y, Arrays.copyOf(components, AlchemyAspect.COUNT));
        }
    }
}
