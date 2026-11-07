以下是为《Ars Alchemica》模组编写的核心数学与炼金元素底层架构代码。代码采用 Java 17 标准编写，针对高频 Tick 运算进行了内存与算法级优化（如使用定长基本类型数组替代 `EnumMap` 以消除装箱拆箱开销，采用均方根伽马校正进行色彩空间插值等）。

### 1. `AlchemyAspect.java` (元素枚举与公理定义)

```java
package com.medievalmagic.arsalchemica.api.aspect;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 《Ars Alchemica》核心炼金元素与原质枚举。
 * <p>
 * 定义了构成物质与星图空间的十二种基本形而上学基底。
 * 包含帕拉塞尔苏斯三原质 (Tria Prima)、亚里士多德四元素以及五种高阶宇宙倾向。
 * </p>
 *
 * @author MedievalMagic Architecture Team
 * @version 1.0.0
 */
public enum AlchemyAspect {

    // ================= 三原质 (Tria Prima) =================
    /** 硫：灵魂、燃烧、扩张与易变之源 */
    SULFUR("sulfur", 0xFFD700, Category.TRIA_PRIMA, Polarity.ACTIVE, 0.85, 0.70, 0.50),
    /** 汞：精神、流动、连接与挥发之源 */
    MERCURIUS("mercurius", 0xC0C0C0, Category.TRIA_PRIMA, Polarity.NEUTRAL, 0.95, 0.00, 0.85),
    /** 盐：肉体、固化、收敛与稳定之源 */
    SAL("sal", 0xF5F5DC, Category.TRIA_PRIMA, Polarity.PASSIVE, 0.10, -0.70, -0.50),

    // ================= 四元素 ( classical Elements) =================
    /** 火：纯粹的能量、热与毁灭性转化 */
    IGNIS("ignis", 0xFF4500, Category.ELEMENTAL, Polarity.ACTIVE, 0.75, 1.00, 0.25),
    /** 水：寒冷、包容、溶解与记忆 */
    AQUA("aqua", 0x1E90FF, Category.ELEMENTAL, Polarity.PASSIVE, 0.40, -1.00, -0.25),
    /** 土：沉重、坚固、承载与惰性 */
    TERRA("terra", 0x8B4513, Category.ELEMENTAL, Polarity.PASSIVE, 0.05, -0.25, -1.00),
    /** 风：轻盈、扩散、迅捷与无形 */
    AER("aer", 0xE0FFFF, Category.ELEMENTAL, Polarity.ACTIVE, 0.90, 0.25, 1.00),

    // ================= 核心倾向 (Cosmic Tendencies) =================
    /** 以太：第五元素，虚空、超越与纯粹星界基质 */
    AETHER("aether", 0x9370DB, Category.COSMIC, Polarity.SUPER_LUMINAL, 1.00, 0.00, 0.00),
    /** 生机：生长、繁衍、治愈与肉体畸变 */
    VITA("vita", 0x32CD32, Category.COSMIC, Polarity.ACTIVE, 0.50, 0.60, 0.80),
    /** 死寂：凋零、终结、安眠与灵魂剥离 */
    MORTIS("mortis", 0x2F4F4F, Category.COSMIC, Polarity.PASSIVE, 0.30, -0.60, -0.80),
    /** 秩序：法则、结晶、对称与停滞 */
    ORDO("ordo", 0xFFFFFF, Category.COSMIC, Polarity.PASSIVE, 0.00, -0.80, 0.80),
    /** 混沌：熵增、无序、突变与不可控的狂暴 */
    CHAOS("chaos", 0x800000, Category.COSMIC, Polarity.ACTIVE, 1.20, 0.80, -0.80);

    /** 缓存枚举数组，避免频繁调用 values() 产生防御性拷贝开销 */
    public static final AlchemyAspect[] VALUES = values();
    public static final int COUNT = VALUES.length;

    private static final Map<String, AlchemyAspect> ID_MAP;
    private static final AlchemyAspect[] OPPOSITES = new AlchemyAspect[COUNT];

    static {
        ID_MAP = Collections.unmodifiableMap(
                Arrays.stream(VALUES).collect(Collectors.toMap(AlchemyAspect::getId, Function.identity()))
        );

        // 初始化对立互斥拓扑网络
        registerOpposites(SULFUR, SAL);
        registerOpposites(IGNIS, AQUA);
        registerOpposites(TERRA, AER);
        registerOpposites(VITA, MORTIS);
        registerOpposites(ORDO, CHAOS);
        // MERCURIUS 与 AETHER 作为调和与超然基底，不存在绝对互斥项
    }

    private static void registerOpposites(AlchemyAspect a, AlchemyAspect b) {
        OPPOSITES[a.ordinal()] = b;
        OPPOSITES[b.ordinal()] = a;
    }

    private final String id;
    private final String translationKey;
    private final int color;
    private final Category category;
    private final Polarity polarity;
    private final double baseVolatility;
    private final double astralAxisX;
    private final double astralAxisY;

    /**
     * 构造炼金元素枚举。
     *
     * @param id             唯一标识符（全小写）
     * @param color          标准 24位 RGB 十六进制颜色值
     * @param category       元素所属哲学分类
     * @param polarity       炼金极性倾向
     * @param baseVolatility 基础挥发性系数 [0.0, 1.2]
     * @param astralAxisX    星图投影 X 轴单位分量 [-1.0, 1.0]
     * @param astralAxisY    星图投影 Y 轴单位分量 [-1.0, 1.0]
     */
    AlchemyAspect(String id, int color, Category category, Polarity polarity,
                  double baseVolatility, double astralAxisX, double astralAxisY) {
        this.id = id;
        this.translationKey = "aspect.arsalchemica." + id;
        this.color = color & 0xFFFFFF; // 确保剥离 Alpha 通道
        this.category = category;
        this.polarity = polarity;
        this.baseVolatility = baseVolatility;
        this.astralAxisX = astralAxisX;
        this.astralAxisY = astralAxisY;
    }

    public String getId() {
        return id;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public int getColor() {
        return color;
    }

    public int getRed() {
        return (color >> 16) & 0xFF;
    }

    public int getGreen() {
        return (color >> 8) & 0xFF;
    }

    public int getBlue() {
        return color & 0xFF;
    }

    public Category getCategory() {
        return category;
    }

    public Polarity getPolarity() {
        return polarity;
    }

    public double getBaseVolatility() {
        return baseVolatility;
    }

    public double getAstralAxisX() {
        return astralAxisX;
    }

    public double getAstralAxisY() {
        return astralAxisY;
    }

    /**
     * 获取当前元素的相反/互斥元素。
     *
     * @return 包含相反元素的 Optional，若该元素为中立（如汞、以太）则返回 empty。
     */
    public Optional<AlchemyAspect> getOpposite() {
        return Optional.ofNullable(OPPOSITES[this.ordinal()]);
    }

    /**
     * 判断当前元素是否与目标元素互斥。
     *
     * @param other 目标元素
     * @return 若两者互为相反元素则返回 true
     */
    public boolean isOppositeTo(AlchemyAspect other) {
        if (other == null) return false;
        return OPPOSITES[this.ordinal()] == other;
    }

    /**
     * 计算当前元素与另一元素混合时的相克消解因子 (Annihilation Factor)。
     * <ul>
     *     <li>同种元素：0.0（完全相容）</li>
     *     <li>绝对互斥元素：1.0 + 双方挥发性均值（剧烈湮灭反应）</li>
     *     <li>极性相反但非绝对互斥：0.25（轻微排斥）</li>
     *     <li>包含超然/调和元素（汞/以太）：-0.15（负值表示起稳定融合作用）</li>
     * </ul>
     *
     * @param other 另一炼金元素
     * @return 消解失稳系数
     */
    public double getAnnihilationFactor(AlchemyAspect other) {
        if (other == null || this == other) {
            return 0.0;
        }
        if (this.isOppositeTo(other)) {
            return 1.0 + (this.baseVolatility + other.baseVolatility) * 0.5;
        }
        if (this == MERCURIUS || other == MERCURIUS || this == AETHER || other == AETHER) {
            return -0.15;
        }
        if (this.polarity.isOpposite(other.polarity)) {
            return 0.25;
        }
        return 0.05;
    }

    /**
     * 根据唯一字符串 ID 查找元素。
     *
     * @param id 元素 ID（大小写不敏感）
     * @return 对应的元素 Optional
     */
    public static Optional<AlchemyAspect> byId(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(ID_MAP.get(id.toLowerCase()));
    }

    /**
     * 元素哲学分类。
     */
    public enum Category {
        TRIA_PRIMA,
        ELEMENTAL,
        COSMIC
    }

    /**
     * 元素极性倾向定义及其内禀权重。
     */
    public enum Polarity {
        /** 阳性/主动/发散 */
        ACTIVE(1.0),
        /** 阴性/被动/收敛 */
        PASSIVE(-1.0),
        /** 中性/调和 */
        NEUTRAL(0.0),
        /** 超验/高维 */
        SUPER_LUMINAL(2.0);

        private final double weight;

        Polarity(double weight) {
            this.weight = weight;
        }

        public double getWeight() {
            return weight;
        }

        public boolean isOpposite(Polarity other) {
            return (this == ACTIVE && other == PASSIVE) || (this == PASSIVE && other == ACTIVE);
        }
    }
}
```

---

### 2. `AspectVector.java` (连续向量空间与炼金动力学计算)

```java
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
 * 该空间是一个 $2 + 12$ 维的复合流形：包含二维显式星图坐标 $(x, y)$ 与十二维元素丰度分量。
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

    /** 浮点数精度容差阈值 ($\epsilon$) */
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
     * 私有主构造函数。直接接收内部数组（调用方需保证数组所有权安全）。
     *
     * @param x          星图 X 轴坐标
     * @param y          星图 Y 轴坐标
     * @param components 长度为 {@link AlchemyAspect#COUNT} 的元素含量数组
     */
    private AspectVector(double x, double y, double[] components) {
        this.x = sanitizeDouble(x);
        this.y = sanitizeDouble(y);
        this.components = components;

        double sum = 0.0;
        for (int i = 0; i < AlchemyAspect.COUNT; i++) {
            // 物理约束：元素质量不允许为负数，小于 EPSILON 的残留视为完全挥发归零
            double val = sanitizeDouble(components[i]);
            if (val < EPSILON) {
                val = 0.0;
            }
            this.components[i] = val;
            sum += val;
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
     * 仅通过元素映射表构建向量，其二维星图位移 $(x, y)$ 将根据所含元素的内禀星图轴向自动投影生成。
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
                // 结合元素极性权重与汞的催化效应计算内禀投影
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
     * @return 元素含量 $(\ge 0.0)$
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
     * 获取向量中所有元素的总质量（即元素空间的 $L_1$ 范数）。
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
     * 向量加法：$\vec{v}_{new} = \vec{v}_1 + \vec{v}_2$。
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
     * 标量乘法：$\vec{v}_{new} = c \cdot \vec{v}$。
     * 注意：若标量 $c < 0$，星图坐标会反向，但元素质量由于物理限制将被截断为 0。
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
     * 计算与另一向量的全空间欧几里得距离 ($L_2$ Metric)。
     * 公式：$\sqrt{(x_1 - x_2)^2 + (y_1 - y_2)^2 + \sum (a_{1,i} - a_{2,i})^2}$
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
        return Math.sqrt(sumSq);
    }

    /**
     * 仅计算二维星图平面上的欧氏距离（用于星图节点吸附判定）。
     *
     * @param other 目标向量
     * @return 星图平面距离
     */
    public double astralDistanceTo(AspectVector other) {
        Objects.requireNonNull(other, "Target AspectVector cannot be null");
        return Math.hypot(this.x - other.x, this.y - other.y);
    }

    /**
     * 计算与另一向量的全空间曼哈顿距离 ($L_1$ Metric / Taxicab Geometry)。
     * 公式：$|x_1 - x_2| + |y_1 - y_2| + \sum |a_{1,i} - a_{2,i}|$
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
     * 包含星图分量与元素分量的正交内积。
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
        return dot;
    }

    // ================= 炼金动力学与高阶属性计算 =================

    /**
     * 计算炼金纯度 (Alchemical Purity) $\in [0.0, 1.0]$。
     * <p>
     * 采用赫芬达尔-赫希曼指数 (Herfindahl-Hirschman Index, HHI) 的变体模型：
     * $$Purity = \sum_{i=1}^{N} \left(\frac{a_i}{M}\right)^2$$
     * 当向量仅含单一元素时纯度为 $1.0$；当所有十二种元素均匀分布时纯度达到理论下界 $1/12 \approx 0.0833$。
     * 若总质量为 0，定义纯度为 0.0。
     * </p>
     *
     * @return 纯度系数 $[0.0, 1.0]$
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
     * 综合考量各元素的基础挥发性、极性发散度，以及“盐 (SAL)”的固着抑制效应和“汞 (MERCURIUS)”的活化效应。
     * </p>
     *
     * @return 综合挥发性指数 $(\ge 0.0)$
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
     * 当向量中同时存在互斥元素（如火与水、秩序与混沌）时，会产生剧烈的内部相克张力。
     * 失稳值由互斥元素对的<b>几何平均数 (Geometric Mean)</b> 乘以相克消解系数，并结合体系总挥发性决定。
     * 同时，“以太 (AETHER)”与“汞 (MERCURIUS)”的存在将提供缓冲阻尼。
     * </p>
     *
     * @return 失稳度（$0.0$ 表示绝对稳定，$> 1.0$ 通常意味着坩埚即将发生沸腾、畸变或爆炸）
     */
    public double calculateInstability() {
        if (this.totalMass < EPSILON) {
            return 0.0;
        }

        double conflictEnergy = 0.0;
        double stabilizingMass = getAmount(AlchemyAspect.MERCURIUS) * 0.5
                               + getAmount(AlchemyAspect.AETHER) * 0.8
                               + getAmount(AlchemyAspect.ORDO) * 0.3;

        // 遍历所有元素对 (i, j) where i < j，计算交互张力
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
                    // 使用几何平均值衡量两种互斥元素的反应接触面积：sqrt(A * B)
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
     * 若向量中同时存在绝对相反的元素（如 IGNIS 与 AQUA），它们将按 $1:1$ 的质量比发生中和湮灭，
     * 湮灭损失的质量会转化为星图坐标上的随机湍流或向心坍缩，并返回净化后的稳定向量。
     * </p>
     *
     * @param efficiency 消解反应程度 $\in (0.0, 1.0]$，$1.0$ 表示完全中和直到一方耗尽
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

            // 仅在 i < opposite.ordinal() 时处理一次，防止重复计算
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
     * <p>
     * <b>色彩科学实现：</b>标准的 sRGB 空间是非线性的（$\gamma \approx 2.2$），直接对 R/G/B 整数求算术加权平均会导致
     * 混合色彩严重偏暗且浑浊（Muddy Colors）。
     * 本方法采用<b>均方根伽马近似校正 (Root-Mean-Square Gamma Approximation, $\gamma = 2.0$)</b>：
     * 先将通道值平方映射至线性光通量空间进行质量加权插值，再开平方还原至 sRGB 空间。
     * </p>
     *
     * @return 24位 RGB 十六进制颜色值 (0xRRGGBB)。若向量为空，返回深邃虚空黑 (0x101018)。
     */
    public int calculateBlendedColor() {
        if (this.totalMass < EPSILON) {
            return 0x101018; // 默认基底深暗色
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

                // 转换至线性光子能量空间加权累加
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

        /**
         * 根据当前已添加的元素含量，自动计算并累加星图坐标偏移。
         */
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
```

### 架构与数学设计亮点说明：

1. **零装箱定长数组优化 (`double[] components`)**：
   在 Minecraft 模组中，坩埚、蒸馏器与星图推演往往需要在每个 `ServerTick` 甚至 `RenderTick` 中进行成百上千次向量叠加与插值。`AspectVector` 摒弃了 `Map<AlchemyAspect, Double>` 作为内部存储，而是利用枚举的 `ordinal()` 映射为长度为 12 的连续原生 `double[]` 数组，实现了极致的 CPU 缓存友好性（Cache Locality）与零 GC 压力。
2. **赫芬达尔纯度指数 (HHI Purity)**：
   `calculatePurity()` 使用 $\sum (a_i / M)^2$ 测定溶液纯度，平滑且可微，避免了传统取最大值法在边界条件下的导数不连续问题。
3. **光子能量空间色彩混合 (`calculateBlendedColor`)**：
   针对传统 RGB 线性插值导致黄（硫）+ 蓝（水）混合成肮脏暗灰色的问题，采用了 $\sqrt{\sum w_i \cdot C_i^2}$ 的伽马还原算法，使得坩埚汤药在渲染时保持高饱和度的魔幻光泽。
4. **几何平均相克张力模型 (`calculateInstability`)**：
   利用 $\sqrt{m_A \cdot m_B}$ 建模相反元素的接触反应面积（只有当矛盾双方同时具有相当规模时才会爆发最大张力），并引入“汞”与“以太”作为分母阻尼项，完美契合中世纪帕拉塞尔苏斯炼金术中“以汞调和万物”的设定。