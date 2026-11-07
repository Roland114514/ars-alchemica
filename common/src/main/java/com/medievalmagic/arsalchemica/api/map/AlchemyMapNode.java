package com.medievalmagic.arsalchemica.api.map;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Objects;

/**
 * 以太星图中的药效节点（Effect Node）。
 * <p>
 * 在二维炼金平面中，每一个节点代表一个隐藏在迷雾中的引力阱，当坩埚内的相位向量
 * 落入其半径范围内且满足纯度条件时，即可激发对应的超凡药理。
 * </p>
 *
 * @author MedievalMagic Architecture Team
 * @version 1.0.0
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

        if (this.radius <= 0.0 || this.perfectRadius < 0.0 || this.perfectRadius >= this.radius) {
            throw new IllegalArgumentException(
                    String.format("Invalid radius configuration for node '%s': radius=%.2f, perfectRadius=%.2f (must satisfy radius > perfectRadius >= 0)",
                            nodeId, radius, perfectRadius)
            );
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
     *     <li>若距离 d <= perfectRadius，得分为 1.0（完美共鸣，触发药效等级翻倍或隐藏特效）。</li>
     *     <li>若距离 d >= radius，得分为 0.0（脱靶）。</li>
     *     <li>处于两者之间时，采用平滑 Hermite 插值（Smoothstep）进行非线性衰减。</li>
     * </ul>
     * </p>
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
        double span = this.radius - this.perfectRadius;
        if (span <= 1.0E-6) {
            return (d <= this.perfectRadius) ? 1.0 : 0.0;
        }
        // 归一化距离参数 t in [0, 1]
        double t = Mth.clamp((d - this.perfectRadius) / span, 0.0, 1.0);
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
        private final double centerX;
        private final double centerY;
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
