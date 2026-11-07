package com.medievalmagic.arsalchemica.api.map;

import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 以太星图拓扑管理器（The Astral Alchemy Map）。
 * <p>
 * 负责维护整张二维炼金平面上的所有药效节点（Nodes）以及以太乱流/危险区（Hazard Zones）。
 * 既提供默认拓扑，又支持通过数据包进行热重载扩展。
 * </p>
 *
 * @author MedievalMagic Architecture Team
 * @version 1.0.0
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

    // --- 拓扑查询 API (经过零 GC 循环优化，替代低效的 Stream Pipeline) ---

    /**
     * 查询当前坐标命中的药效节点。
     * 若多个节点边缘重叠，优先返回离靶心最近的节点。
     * <p>性能优化：采用无装箱循环，单次查询零内存分配。</p>
     */
    public Optional<AlchemyMapNode> findHitNode(double x, double y) {
        AlchemyMapNode bestNode = null;
        double minDistance = Double.MAX_VALUE;

        for (AlchemyMapNode node : nodes.values()) {
            if (node.contains(x, y)) {
                double dist = node.distanceTo(x, y);
                if (dist < minDistance) {
                    minDistance = dist;
                    bestNode = node;
                }
            }
        }
        return Optional.ofNullable(bestNode);
    }

    /**
     * 查找距离当前坐标最近的节点（用于占卜罗盘、星象仪指引或迷雾微光提示）。
     *
     * @param includeAncient 是否包含极其隐秘的古代失落秘方节点
     */
    public Optional<AlchemyMapNode> findNearestNode(double x, double y, boolean includeAncient) {
        AlchemyMapNode nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (AlchemyMapNode node : nodes.values()) {
            if (includeAncient || !node.isAncient()) {
                double dist = node.distanceTo(x, y);
                if (dist < minDistance) {
                    minDistance = dist;
                    nearest = node;
                }
            }
        }
        return Optional.ofNullable(nearest);
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
        HazardZone dominant = null;
        double maxIntensity = 0.0;

        for (HazardZone hazard : hazards.values()) {
            if (hazard.contains(x, y)) {
                double intensity = hazard.getIntensityAt(x, y);
                if (intensity > maxIntensity) {
                    maxIntensity = intensity;
                    dominant = hazard;
                }
            }
        }
        return Optional.ofNullable(dominant);
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
        // 1. 生命恢复 (Tier 1)
        registerNode(new AlchemyMapNode.Builder(
                com.medievalmagic.arsalchemica.ArsAlchemica.id("node_regeneration"),
                new ResourceLocation("minecraft", "regeneration"),
                12.5, 14.0)
                .radii(4.0, 1.0).purity(0.45).tier(1).build());

        // 2. 烈焰庇护 (Tier 2) - 高温偏区
        registerNode(new AlchemyMapNode.Builder(
                com.medievalmagic.arsalchemica.ArsAlchemica.id("node_fire_resistance"),
                new ResourceLocation("minecraft", "fire_resistance"),
                28.0, 6.5)
                .radii(3.5, 0.8).purity(0.60).tier(2).build());

        // 3. 暗影潜行/隐身 (Tier 3)
        registerNode(new AlchemyMapNode.Builder(
                com.medievalmagic.arsalchemica.ArsAlchemica.id("node_invisibility"),
                new ResourceLocation("minecraft", "invisibility"),
                -22.0, 25.0)
                .radii(3.0, 0.6).purity(0.75).tier(3).build());

        // 4. 古代失传秘方：不死鸟涅槃 (Tier 5, Ancient) - 极远端坐标，紧贴虚空漩涡边缘
        registerNode(new AlchemyMapNode.Builder(
                com.medievalmagic.arsalchemica.ArsAlchemica.id("node_phoenix_rebirth"),
                com.medievalmagic.arsalchemica.ArsAlchemica.id("phoenix_rebirth"),
                55.0, 48.0)
                .radii(2.2, 0.4).purity(0.90).tier(5).ancient().build());

        // --- 危险区注册 ---
        // 骸骨碎裂带
        registerHazard(new HazardZone(
                com.medievalmagic.arsalchemica.ArsAlchemica.id("hazard_bone_graveyard"),
                HazardZone.HazardType.BONE_GRAVEYARD,
                -18.0, -18.0, 8.5, 0.08
        ));

        // 虚空漩涡：通往不死鸟节点的危险陷阱
        registerHazard(new HazardZone(
                com.medievalmagic.arsalchemica.ArsAlchemica.id("hazard_void_vortex"),
                HazardZone.HazardType.VOID_VORTEX,
                46.0, 40.0, 6.0, 0.15
        ));
    }
}
