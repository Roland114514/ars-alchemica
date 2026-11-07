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

    // ================= 四元素 (Classical Elements) =================
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
     * 根据唯一字符串 ID 查找元素（大小写不敏感，使用 Locale.ROOT 保证跨语言环境安全）。
     *
     * @param id 元素 ID
     * @return 对应的元素 Optional
     */
    public static Optional<AlchemyAspect> byId(String id) {
        if (id == null || id.isEmpty()) return Optional.empty();
        return Optional.ofNullable(ID_MAP.get(id.toLowerCase(java.util.Locale.ROOT)));
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
