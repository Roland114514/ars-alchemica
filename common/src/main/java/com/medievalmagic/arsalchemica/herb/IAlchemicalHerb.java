package com.medievalmagic.arsalchemica.herb;

import com.medievalmagic.arsalchemica.api.vector.AspectVector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 炼金草药核心契约接口。
 * <p>
 * 沟通生态学生长方块与炼金以太动力学引擎（Aspect Vector Engine）。
 * 定义草药在不同加工形态下的 12 维元素向量映射、挥发性衰减率、基础毒性及生境约束。
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public interface IAlchemicalHerb {

    /**
     * 获取草药唯一注册 ID。
     */
    String getId();

    /**
     * 获取该草药在指定加工形态下的 12 维炼金元素向量。
     *
     * @param form 草药的物理加工形态
     * @return 包含星图坐标与十二维元素的不可变 AspectVector
     */
    AspectVector getAspectVector(ProcessingForm form);

    /**
     * 获取草药的基础炼金品质（Base Quality），通常在 [0.5, 2.5] 区间内。
     *
     * @param form 加工形态
     * @return 基础品质系数
     */
    double getBaseQuality(ProcessingForm form);

    /**
     * 获取草药在热力学反应中的挥发性修饰符（Volatility Modifier）。
     *
     * @param form 加工形态
     * @return 挥发性修饰倍率
     */
    double getVolatilityModifier(ProcessingForm form);

    /**
     * 获取草药的基础毒性值（Base Toxicity in [0.0, 100.0]）。
     *
     * @param form 加工形态
     * @return 毒性数值
     */
    double getBaseToxicity(ProcessingForm form);

    /**
     * 判定当前世界环境是否满足该草药的生长或活性维持条件。
     */
    boolean canSurviveInEnvironment(Level level, BlockPos pos, BlockState state);

    /**
     * 判定玩家当前的采收行为是否安全（是否使用了正确的专业工具及满足时辰/天象要求）。
     */
    boolean isSafeToHarvest(Level level, BlockPos pos, BlockState state, Player player, ItemStack tool);

    // =========================================================================
    // 内部枚举与工具类
    // =========================================================================

    /**
     * 草药的物理加工形态枚举。
     */
    enum ProcessingForm {
        /** 原生形态：刚从野外采收的鲜草药，含水量高，性质原始 */
        RAW("raw", 1.0, 1.0),
        /** 粗磨形态：细胞壁破裂，元素释放速率与纯度提升 */
        MILLED_COARSE("milled_coarse", 1.15, 1.30),
        /** 精磨粉末：极细微粉，比表面积最大，溶解活性与挥发性最高 */
        MILLED_FINE("milled_fine", 1.40, 1.85),
        /** 压榨精油：萃取出的高纯度脂溶性精华，极适合用于剑油调制 */
        PRESSED_OIL("pressed_oil", 1.80, 2.20),
        /** 阴干形态：脱水处理后的草药，挥发性大幅降低，适合长期保存与慢火熬制 */
        DRIED("dried", 1.10, 0.45);

        private final String serializedName;
        private final double defaultPurityMultiplier;
        private final double defaultVolatilityMultiplier;

        ProcessingForm(String serializedName, double defaultPurityMultiplier, double defaultVolatilityMultiplier) {
            this.serializedName = serializedName;
            this.defaultPurityMultiplier = defaultPurityMultiplier;
            this.defaultVolatilityMultiplier = defaultVolatilityMultiplier;
        }

        public String getSerializedName() { return serializedName; }
        public double getDefaultPurityMultiplier() { return defaultPurityMultiplier; }
        public double getDefaultVolatilityMultiplier() { return defaultVolatilityMultiplier; }
    }

    /**
     * 生态与天象环境探针工具类。
     */
    final class EnvironmentProbe {
        private EnvironmentProbe() {}

        public static final int FULL_MOON = 0;
        public static final int NEW_MOON = 4;

        public static int getMoonPhase(Level level) {
            return level.getMoonPhase();
        }

        public static boolean isSyzygyNight(Level level) {
            if (level.isDay()) return false;
            int phase = getMoonPhase(level);
            return phase == FULL_MOON || phase == NEW_MOON;
        }

        public static int getCombinedLightLevel(Level level, BlockPos pos) {
            return level.getMaxLocalRawBrightness(pos);
        }

        public static int getBlockLightLevel(Level level, BlockPos pos) {
            return level.getBrightness(LightLayer.BLOCK, pos);
        }

        public static float getBiomeTemperature(Level level, BlockPos pos) {
            return level.getBiome(pos).value().getBaseTemperature();
        }
    }
}
