package com.medievalmagic.arsalchemica.herb;

import com.medievalmagic.arsalchemica.api.aspect.AlchemyAspect;
import com.medievalmagic.arsalchemica.api.vector.AspectVector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumMap;
import java.util.Map;

/**
 * 7 大奇特魔法草药静态注册表与炼金动力学数据容器。
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public enum HerbRegistry implements IAlchemicalHerb {

    /**
     * 1. 灰烬血蓟 (Cinder Thistle)
     * 主导元素：火 (IGNIS) 与 硫 (SULFUR)。
     */
    CINDER_THISTLE(
            "cinder_thistle",
            createBaseVector(new AspectSpec(AlchemyAspect.IGNIS, 8.5), new AspectSpec(AlchemyAspect.SULFUR, 4.0), new AspectSpec(AlchemyAspect.TERRA, 2.0), new AspectSpec(AlchemyAspect.CHAOS, 2.5)),
            1.20, 1.80, 15.0
    ) {
        @Override
        public boolean canSurviveInEnvironment(Level level, BlockPos pos, BlockState state) {
            return IAlchemicalHerb.EnvironmentProbe.getBiomeTemperature(level, pos) >= 0.5F;
        }

        @Override
        public boolean isSafeToHarvest(Level level, BlockPos pos, BlockState state, Player player, ItemStack tool) {
            return player != null && (tool.is(Items.SHEARS));
        }
    },

    /**
     * 2. 深渊幽光薄荷 (Abyssal Lumina Mint)
     * 主导元素：水 (AQUA) 与 秩序 (ORDO)。
     */
    ABYSSAL_LUMINA_MINT(
            "abyssal_lumina_mint",
            createBaseVector(new AspectSpec(AlchemyAspect.AQUA, 7.5), new AspectSpec(AlchemyAspect.ORDO, 6.5), new AspectSpec(AlchemyAspect.AETHER, 3.0), new AspectSpec(AlchemyAspect.MERCURIUS, 2.0)),
            1.50, 1.10, 5.0
    ) {
        @Override
        public boolean canSurviveInEnvironment(Level level, BlockPos pos, BlockState state) {
            return IAlchemicalHerb.EnvironmentProbe.getCombinedLightLevel(level, pos) <= 9;
        }

        @Override
        public boolean isSafeToHarvest(Level level, BlockPos pos, BlockState state, Player player, ItemStack tool) {
            return IAlchemicalHerb.EnvironmentProbe.getCombinedLightLevel(level, pos) <= 7;
        }
    },

    /**
     * 3. 哀嚎曼陀罗 (Screaming Mandragora)
     * 主导元素：死寂 (MORTIS) 与 土 (TERRA)。生食剧毒，研磨或阴干可褪毒。
     */
    SCREAMING_MANDRAGORA(
            "screaming_mandragora",
            createBaseVector(new AspectSpec(AlchemyAspect.MORTIS, 9.0), new AspectSpec(AlchemyAspect.TERRA, 5.5), new AspectSpec(AlchemyAspect.CHAOS, 4.5), new AspectSpec(AlchemyAspect.MERCURIUS, 2.0)),
            1.65, 1.45, 65.0
    ) {
        @Override
        public boolean canSurviveInEnvironment(Level level, BlockPos pos, BlockState state) {
            return true;
        }

        @Override
        public boolean isSafeToHarvest(Level level, BlockPos pos, BlockState state, Player player, ItemStack tool) {
            return player != null && tool.is(Items.SHEARS);
        }
    },

    /**
     * 4. 凝霜幽兰 (Frostvein Orchid)
     * 主导元素：水 (AQUA) 与 盐 (SAL)。火属性为绝对零值，极抗拒高温。
     */
    FROSTVEIN_ORCHID(
            "frostvein_orchid",
            createBaseVector(new AspectSpec(AlchemyAspect.AQUA, 8.0), new AspectSpec(AlchemyAspect.SAL, 6.5), new AspectSpec(AlchemyAspect.TERRA, 3.0), new AspectSpec(AlchemyAspect.ORDO, 4.0)),
            1.75, 2.10, 8.0
    ) {
        @Override
        public boolean canSurviveInEnvironment(Level level, BlockPos pos, BlockState state) {
            return IAlchemicalHerb.EnvironmentProbe.getBiomeTemperature(level, pos) <= 0.35F
                    && IAlchemicalHerb.EnvironmentProbe.getBlockLightLevel(level, pos) <= 8;
        }

        @Override
        public boolean isSafeToHarvest(Level level, BlockPos pos, BlockState state, Player player, ItemStack tool) {
            return player != null && tool.is(Items.SHEARS);
        }
    },

    /**
     * 5. 月影颠茄 (Moonveil Belladonna)
     * 主导元素：以太 (AETHER) 与 风 (AER)。剧毒暗影花，满月/新月夜晚盛开。
     */
    MOONVEIL_BELLADONNA(
            "moonveil_belladonna",
            createBaseVector(new AspectSpec(AlchemyAspect.AETHER, 9.0), new AspectSpec(AlchemyAspect.AER, 5.5), new AspectSpec(AlchemyAspect.CHAOS, 5.0), new AspectSpec(AlchemyAspect.MORTIS, 3.0)),
            1.80, 1.60, 80.0
    ) {
        @Override
        public boolean canSurviveInEnvironment(Level level, BlockPos pos, BlockState state) {
            return true;
        }

        @Override
        public boolean isSafeToHarvest(Level level, BlockPos pos, BlockState state, Player player, ItemStack tool) {
            return !level.isDay() && (IAlchemicalHerb.EnvironmentProbe.isSyzygyNight(level) || (player != null && tool.is(Items.SHEARS)));
        }
    },

    /**
     * 6. 腐水食腐苔 (Carrion Bogmoss)
     * 主导元素：盐 (SAL) 与 土 (TERRA)。具备极强吸附能力，清除体内毒性槽的核心原料。
     */
    CARRION_BOGMOSS(
            "carrion_bogmoss",
            createBaseVector(new AspectSpec(AlchemyAspect.SAL, 8.5), new AspectSpec(AlchemyAspect.TERRA, 7.0), new AspectSpec(AlchemyAspect.VITA, 4.5), new AspectSpec(AlchemyAspect.AQUA, 4.0)),
            1.10, 0.55, 40.0
    ) {
        @Override
        public boolean canSurviveInEnvironment(Level level, BlockPos pos, BlockState state) {
            return true;
        }

        @Override
        public boolean isSafeToHarvest(Level level, BlockPos pos, BlockState state, Player player, ItemStack tool) {
            return true;
        }
    },

    /**
     * 7. 避雷金盏花 (Storm Marigold)
     * 主导元素：风 (AER) 与 火 (IGNIS)。雷暴天自动充能，释放电弧。
     */
    STORM_MARIGOLD(
            "storm_marigold",
            createBaseVector(new AspectSpec(AlchemyAspect.AER, 9.5), new AspectSpec(AlchemyAspect.IGNIS, 5.5), new AspectSpec(AlchemyAspect.CHAOS, 3.5), new AspectSpec(AlchemyAspect.SULFUR, 2.5)),
            1.60, 2.35, 20.0
    ) {
        @Override
        public boolean canSurviveInEnvironment(Level level, BlockPos pos, BlockState state) {
            return level.canSeeSky(pos);
        }

        @Override
        public boolean isSafeToHarvest(Level level, BlockPos pos, BlockState state, Player player, ItemStack tool) {
            return !level.isThundering() || (player != null && tool.is(Items.SHEARS));
        }
    };

    private final String id;
    private final AspectVector baseRawVector;
    private final double baseQuality;
    private final double baseVolatility;
    private final double baseToxicity;
    private final Map<ProcessingForm, AspectVector> cachedVectors = new EnumMap<>(ProcessingForm.class);

    HerbRegistry(String id, AspectVector baseRawVector, double baseQuality, double baseVolatility, double baseToxicity) {
        this.id = id;
        this.baseRawVector = baseRawVector;
        this.baseQuality = baseQuality;
        this.baseVolatility = baseVolatility;
        this.baseToxicity = baseToxicity;
        precomputeForms();
    }

    private void precomputeForms() {
        for (ProcessingForm form : ProcessingForm.values()) {
            double multiplier = form.getDefaultPurityMultiplier();
            // 根据加工形态对向量进行标量缩放并自动重新投影星图坐标
            AspectVector scaled = this.baseRawVector.multiply(multiplier);
            cachedVectors.put(form, scaled);
        }
    }

    @Override
    public String getId() { return id; }

    @Override
    public AspectVector getAspectVector(ProcessingForm form) {
        return cachedVectors.getOrDefault(form, baseRawVector);
    }

    @Override
    public double getBaseQuality(ProcessingForm form) {
        return baseQuality * form.getDefaultPurityMultiplier();
    }

    @Override
    public double getVolatilityModifier(ProcessingForm form) {
        return baseVolatility * form.getDefaultVolatilityMultiplier();
    }

    @Override
    public double getBaseToxicity(ProcessingForm form) {
        return switch (form) {
            case DRIED -> baseToxicity * 0.45;
            case PRESSED_OIL -> baseToxicity * 1.35;
            default -> baseToxicity;
        };
    }

    // --- 内部辅助构建记录 ---
    private record AspectSpec(AlchemyAspect aspect, double amount) {}

    private static AspectVector createBaseVector(AspectSpec... specs) {
        AspectVector.Builder builder = AspectVector.builder();
        for (AspectSpec spec : specs) {
            builder.add(spec.aspect, spec.amount);
        }
        return builder.projectAstralCoordinates().build();
    }
}
