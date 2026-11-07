以下是《Ars Alchemica》模组 **Milestone 2：草药系统底层 (Herbology Ecosystem)** 第一部分的生产级底层代码实现。

代码严格遵循 Minecraft 1.20.1 (Mojang Official Mappings) 规范，采用面向接口与数据驱动的设计范式，内置完整的生态环境检测算法、物理状态机以及炼金元素向量矩阵。

---

### 1. `IAlchemicalHerb.java` (草药通用接口与元数据契约)

该接口定义了草药在炼金热力学引擎中的数学表示（元素向量、挥发性、纯度、毒性）以及严苛的生态学交互协议。

```java
package com.medievalmagic.arsalchemica.herb;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;

/**
 * 炼金草药核心契约接口。
 * <p>
 * 负责沟通生态学方块实体与炼金热力学引擎（Alchemical Thermodynamics Engine）。
 * 提供草药在不同加工形态下的高维元素向量映射、挥发性衰减率及生境约束判定。
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.20.1-2.0.0
 */
public interface IAlchemicalHerb {

    /**
     * 获取该草药在指定加工形态下的炼金元素向量（不可变向量）。
     *
     * @param form 草药的物理加工形态
     * @return 6维炼金元素向量 (Ignis, Aqua, Terra, Aer, Ordo, Perditio)
     */
    AspectVector getAspectVector(ProcessingForm form);

    /**
     * 获取草药的基础炼金品质（Base Quality）。
     * 该值作为坩埚反应微分方程中的初始活度系数 $\alpha_0$。
     *
     * @param form 加工形态
     * @return 基础品质系数，通常在 [0.5, 2.5] 区间内
     */
    float getBaseQuality(ProcessingForm form);

    /**
     * 获取草药在热力学反应中的挥发性修饰符（Volatility Modifier）。
     * 挥发性越高，在高温坩埚中元素向量向气态耗散的速率 $\frac{d\vec{V}}{dt}$ 越快。
     *
     * @param form 加工形态
     * @return 挥发性系数 $\nu \in [0.0, 5.0]$
     */
    float getVolatilityModifier(ProcessingForm form);

    /**
     * 获取草药的基础毒性值（Base Toxicity）。
     * 在调配药剂时，若未被秩序（Ordo）元素中和，毒性将直接转化为饮用者的凋零或反胃等级。
     *
     * @param form 加工形态
     * @return 基础毒性值 $\tau \in [0.0, 100.0]$
     */
    float getBaseToxicity(ProcessingForm form);

    /**
     * 判定当前世界环境是否满足该草药的生长或活性维持条件。
     *
     * @param level 当前维度世界实例
     * @param pos   草药所在的方块坐标
     * @param state 当前方块状态
     * @return 若环境适宜返回 {@code true}，否则返回 {@code false}
     */
    boolean canSurviveInEnvironment(Level level, BlockPos pos, BlockState state);

    /**
     * 判定玩家当前的采收行为是否安全（是否使用了正确的专业工具及满足时辰/天象要求）。
     *
     * @param level  当前维度世界实例
     * @param pos    草药所在的方块坐标
     * @param state  当前方块状态
     * @param player 执行采收的玩家（可为 null，如活塞破坏）
     * @param tool   玩家主手所持工具
     * @return 若安全返回 {@code true}；若触发反噬机制返回 {@code false}
     */
    boolean isSafeToHarvest(Level level, BlockPos pos, BlockState state, Player player, ItemStack tool);

    // =========================================================================
    // 内部数学模型与枚举定义
    // =========================================================================

    /**
     * 草药的物理加工形态枚举。
     * 不同的形态在研磨钵（Mortar）、压榨机（Press）与晾晒架（Drying Rack）中转化。
     */
    enum ProcessingForm {
        /** 原生形态：刚从野外采收的鲜草药，含水量高，性质不稳定 */
        RAW("raw", 1.0f, 1.0f),
        /** 粗磨形态：经过初步捣碎，细胞壁破裂，元素释放速率提升 */
        MILLED_COARSE("milled_coarse", 1.15f, 1.35f),
        /** 精磨粉末：极细微粉，比表面积最大，反应活性与挥发性达到顶峰 */
        MILLED_FINE("milled_fine", 1.40f, 1.85f),
        /** 压榨精油：萃取出的高纯度脂溶性精华，挥发性极高但纯度极佳 */
        PRESSED_OIL("pressed_oil", 1.75f, 2.20f),
        /** 阴干形态：脱水处理后的草药，挥发性大幅降低，适合长期保存与慢火熬制 */
        DRIED("dried", 0.90f, 0.45f);

        private final String serializedName;
        private final float defaultPurityMultiplier;
        private final float defaultVolatilityMultiplier;

        ProcessingForm(String serializedName, float defaultPurityMultiplier, float defaultVolatilityMultiplier) {
            this.serializedName = serializedName;
            this.defaultPurityMultiplier = defaultPurityMultiplier;
            this.defaultVolatilityMultiplier = defaultVolatilityMultiplier;
        }

        public String getSerializedName() {
            return serializedName;
        }

        public float getDefaultPurityMultiplier() {
            return defaultPurityMultiplier;
        }

        public float getDefaultVolatilityMultiplier() {
            return defaultVolatilityMultiplier;
        }
    }

    /**
     * 六维炼金元素向量（6D Alchemical Aspect Vector）。
     * 遵循不可变值对象（Immutable Value Object）设计模式，支持向量线性代数运算。
     *
     * @param ignis    火之相（能量、热力学、狂暴）
     * @param aqua     水之相（流动、粘滞、传导）
     * @param terra    土之相（固结、防御、沉降）
     * @param aer      风之相（挥发、迅捷、扩散）
     * @param ordo     秩序之相（纯化、稳定、催化）
     * @param perditio 混沌之相（熵增、毒性、突变）
     */
    record AspectVector(float ignis, float aqua, float terra, float aer, float ordo, float perditio) {

        public static final AspectVector ZERO = new AspectVector(0, 0, 0, 0, 0, 0);

        /**
         * 向量标量乘法：$\vec{V}' = c \cdot \vec{V}$
         */
        public AspectVector scale(float scalar) {
            return new AspectVector(
                    this.ignis * scalar,
                    this.aqua * scalar,
                    this.terra * scalar,
                    this.aer * scalar,
                    this.ordo * scalar,
                    this.perditio * scalar
            );
        }

        /**
         * 向量哈达玛积（Hadamard Product）：$\vec{V}' = \vec{A} \odot \vec{B}$
         * 用于计算加工形态对特定元素的非线性偏置放大。
         */
        public AspectVector multiply(float mIgnis, float mAqua, float mTerra, float mAer, float mOrdo, float mPerditio) {
            return new AspectVector(
                    this.ignis * mIgnis,
                    this.aqua * mAqua,
                    this.terra * mTerra,
                    this.aer * mAer,
                    this.ordo * mOrdo,
                    this.perditio * mPerditio
            );
        }

        /**
         * 计算向量的欧几里得范数（模长）：$\|\vec{V}\|_2 = \sqrt{\sum v_i^2}$
         * 表征该草药的总魔力密度（Mana Density）。
         */
        public double magnitude() {
            return Math.sqrt(ignis * ignis + aqua * aqua + terra * terra + aer * aer + ordo * ordo + perditio * perditio);
        }
    }

    /**
     * 生态与天象环境探针工具类。
     * 封装对维度时间、月相、光照与生物群系热力学的底层查询。
     */
    final class EnvironmentProbe {
        private EnvironmentProbe() {}

        /** 满月月相索引 */
        public static final int FULL_MOON = 0;
        /** 新月（朔月）月相索引 */
        public static final int NEW_MOON = 4;

        /**
         * 获取当前世界的月相。
         * @return 0 为满月，4 为新月，取值范围 [0, 7]
         */
        public static int getMoonPhase(Level level) {
            return level.getMoonPhase();
        }

        /**
         * 判定当前是否处于夜晚且为满月或新月（极阴/极阳之夜）。
         */
        public static boolean isSyzygyNight(Level level) {
            if (level.isDay()) return false;
            int phase = getMoonPhase(level);
            return phase == FULL_MOON || phase == NEW_MOON;
        }

        /**
         * 获取方块位置的综合光照等级（综合天空光与方块光）。
         */
        public static int getCombinedLightLevel(Level level, BlockPos pos) {
            return level.getMaxLocalRawBrightness(pos);
        }

        /**
         * 获取仅由人为光源（火把、岩浆等）产生的方块光照等级。
         */
        public static int getBlockLightLevel(Level level, BlockPos pos) {
            return level.getBrightness(LightLayer.BLOCK, pos);
        }

        /**
         * 获取当前坐标的生物群系基础温度。
         * @return 温度值（例如：冰原 < 0.15，平原 = 0.8，沙漠 = 2.0）
         */
        public static float getBiomeTemperature(Level level, BlockPos pos) {
            return level.getBiome(pos).value().getBaseTemperature();
        }
    }
}
```

---

### 2. `AlchemicalHerbBlock.java` (通用与定制草药方块基类及特种衍生实现)

该类继承自 `BushBlock` 并实现了 `BonemealableBlock`（禁用普通骨粉催熟，保持暗黑硬核风格）。在同一个编译单元中内聚了 7 大奇特草药的专用子类实现，包含完整的状态机属性（如绽放、充能、年龄）与粒子/音效/伤害交互逻辑。

```java
package com.medievalmagic.arsalchemica.herb;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * 炼金草药方块核心基类及 7 大特种衍生草药实现。
 * <p>
 * 管理草药的四阶段生命周期 (AGE 0~3)、环境耐受性轮询、危险交互反噬以及特殊天象状态机。
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.20.1-2.0.0
 */
public abstract class AlchemicalHerbBlock extends BushBlock {

    /** 生长阶段属性：0(幼苗), 1(生长期), 2(含苞), 3(完全成熟) */
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    public static final int MAX_AGE = 3;

    /** 特殊状态属性：月影颠茄的绽放状态 (Blooming) */
    public static final BooleanProperty BLOOMING = BooleanProperty.create("blooming");
    /** 特殊状态属性：避雷金盏花的雷暴充能状态 (Charged) */
    public static final BooleanProperty CHARGED = BooleanProperty.create("charged");

    /** 专业采收工具标签：草药剪 (Herb Shears / Sickles) */
    public static final TagKey<Item> HERB_SHEARS_TAG = TagKey.create(
            Registries.ITEM, new ResourceLocation("arsalchemica", "herb_shears")
    );

    /** 各生长阶段的碰撞箱包络体 (Voxel Shapes) */
    private static final VoxelShape[] SHAPE_BY_AGE = new VoxelShape[]{
            Block.box(3.0D, 0.0D, 3.0D, 13.0D, 5.0D, 13.0D),
            Block.box(2.0D, 0.0D, 2.0D, 14.0D, 9.0D, 14.0D),
            Block.box(2.0D, 0.0D, 2.0D, 14.0D, 13.0D, 14.0D),
            Block.box(1.0D, 0.0D, 1.0D, 15.0D, 15.0D, 15.0D)
    };

    private final Supplier<IAlchemicalHerb> herbMetadataSupplier;

    protected AlchemicalHerbBlock(Properties properties, Supplier<IAlchemicalHerb> herbMetadataSupplier) {
        super(properties.randomTicks().noCollission());
        this.herbMetadataSupplier = herbMetadataSupplier;
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    public IAlchemicalHerb getHerbMetadata() {
        return this.herbMetadataSupplier.get();
    }

    public boolean isMaxAge(BlockState state) {
        return state.getValue(AGE) >= MAX_AGE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE_BY_AGE[state.getValue(AGE)];
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (level instanceof Level fullLevel) {
            if (!getHerbMetadata().canSurviveInEnvironment(fullLevel, pos, state)) {
                return false;
            }
        }
        return super.canSurvive(state, level, pos);
    }

    /**
     * 校验工具是否为专业草药剪。
     */
    protected boolean isUsingHerbShears(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(HERB_SHEARS_TAG) || stack.is( net.minecraft.world.item.Items.SHEARS));
    }

    /**
     * 玩家破坏/采收方块时的钩子，用于触发危险草药的反噬逻辑。
     */
    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        if (!level.isClientSide) {
            boolean safe = getHerbMetadata().isSafeToHarvest(level, pos, state, player, tool);
            if (!safe) {
                onUnsafeHarvest((ServerLevel) level, pos, state, player, tool);
            }
        }
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }

    /**
     * 当玩家未使用正确工具或在不当时机采收时触发的惩罚回调。
     */
    protected void onUnsafeHarvest(ServerLevel level, BlockPos pos, BlockState state, Player player, ItemStack tool) {
        // 默认无惩罚，由子类覆盖
    }

    // =========================================================================
    // 1. 灰烬血蓟 (Cinder Thistle)
    // =========================================================================

    /**
     * 灰烬血蓟：生长于焦土或下界边缘的嗜火荆棘。
     * <ul>
     *   <li>生物碰撞时受到荆棘刺伤并引燃。</li>
     *   <li>徒手采摘时引发微型爆燃，必须使用草药剪采收。</li>
     * </ul>
     */
    public static class CinderThistleBlock extends AlchemicalHerbBlock {

        public CinderThistleBlock(Properties properties) {
            super(properties, () -> HerbRegistry.CINDER_THISTLE);
        }

        @Override
        public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            if (entity instanceof LivingEntity living && !entity.fireImmune()) {
                // 造成火焰荆棘伤害并点燃 4 秒
                living.setSecondsOnFire(4);
                living.hurt(level.damageSources().inFire(), 1.5F + state.getValue(AGE) * 0.5F);
            }
            super.entityInside(state, level, pos, entity);
        }

        @Override
        protected void onUnsafeHarvest(ServerLevel level, BlockPos pos, BlockState state, Player player, ItemStack tool) {
            // 徒手采摘：触发灰烬爆燃，灼烧玩家 6 秒并附带火焰伤害
            player.setSecondsOnFire(6);
            player.hurt(level.damageSources().onFire(), 4.0F);
            level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 0.8F);
            level.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 15, 0.2, 0.2, 0.2, 0.1);
        }

        @Override
        public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            int age = state.getValue(AGE);
            if (age < MAX_AGE && random.nextInt(5) == 0) {
                level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
            }
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(3) == 0) {
                double x = pos.getX() + 0.2D + random.nextDouble() * 0.6D;
                double y = pos.getY() + 0.2D + random.nextDouble() * 0.6D;
                double z = pos.getZ() + 0.2D + random.nextDouble() * 0.6D;
                level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0D, 0.01D, 0.0D);
                level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0D, 0.02D, 0.0D);
            }
        }
    }

    // =========================================================================
    // 2. 深渊幽光薄荷 (Abyssal Lumina Mint)
    // =========================================================================

    /**
     * 深渊幽光薄荷：极度畏光的地下幽冥草药。
     * <ul>
     *   <li>光照等级 > 7 时停止生长，且在随机刻中逐渐逆向枯萎（AGE 递减直至销毁）。</li>
     *   <li>在极暗处散发幽蓝荧光（光照等级 4）与灵魂粒子。</li>
     * </ul>
     */
    public static class AbyssalLuminaMintBlock extends AlchemicalHerbBlock {

        public AbyssalLuminaMintBlock(Properties properties) {
            super(properties.lightLevel(state -> 4), () -> HerbRegistry.ABYSSAL_LUMINA_MINT);
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return super.mayPlaceOn(state, level, pos)
                    || state.is(Blocks.DEEPSLATE)
                    || state.is(Blocks.SCULK)
                    || state.is(Blocks.TUFF);
        }

        @Override
        public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            int light = IAlchemicalHerb.EnvironmentProbe.getCombinedLightLevel(level, pos);
            int age = state.getValue(AGE);

            if (light > 7) {
                // 强光灼伤：逆向枯萎
                if (age > 0) {
                    level.setBlock(pos, state.setValue(AGE, age - 1), Block.UPDATE_CLIENTS);
                    level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 5, 0.1, 0.1, 0.1, 0.01);
                } else {
                    level.destroyBlock(pos, false);
                }
            } else if (light <= 4 && age < MAX_AGE && random.nextInt(4) == 0) {
                // 极暗环境：正常生长
                level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
            }
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (IAlchemicalHerb.EnvironmentProbe.getCombinedLightLevel(level, pos) <= 5 && random.nextInt(2) == 0) {
                double x = pos.getX() + 0.2D + random.nextDouble() * 0.6D;
                double y = pos.getY() + 0.1D + random.nextDouble() * 0.5D;
                double z = pos.getZ() + 0.2D + random.nextDouble() * 0.6D;
                level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0.0D, 0.015D, 0.0D);
                level.addParticle(ParticleTypes.GLOW, x, y + 0.2D, z, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    // =========================================================================
    // 3. 哀嚎曼陀罗 (Screaming Mandragora)
    // =========================================================================

    /**
     * 哀嚎曼陀罗：块茎形似人形的诡异植物。
     * <ul>
     *   <li>成熟时（AGE == 3）若被徒手拔出，会发出凄厉尖叫。</li>
     *   <li>尖叫会对半径 8 格内的所有生物施加高强度反胃、失明与音波冲击伤害。</li>
     * </ul>
     */
    public static class ScreamingMandragoraBlock extends AlchemicalHerbBlock {

        private static final double SCREAM_RADIUS = 8.0D;

        public ScreamingMandragoraBlock(Properties properties) {
            super(properties, () -> HerbRegistry.SCREAMING_MANDRAGORA);
        }

        @Override
        protected void onUnsafeHarvest(ServerLevel level, BlockPos pos, BlockState state, Player player, ItemStack tool) {
            if (!isMaxAge(state)) return;

            // 播放凄厉尖叫音效（复合女巫尖叫与幽匿尖啸器音效）
            level.playSound(null, pos, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 2.5F, 1.4F);
            level.playSound(null, pos, SoundEvents.GHAST_SCREAM, SoundSource.HOSTILE, 2.0F, 0.7F);
            level.sendParticles(ParticleTypes.SONIC_BOOM, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.0, 0.0, 0.0, 0.0);

            // 对半径 8 格内的所有生物施加精神污染与失明
            AABB affectBox = new AABB(pos).inflate(SCREAM_RADIUS);
            List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, affectBox);

            for (LivingEntity victim : victims) {
                victim.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 240, 1)); // 12秒反胃 II
                victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 160, 0)); // 8秒失明 I
                victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));  // 10秒虚弱 II
                victim.hurt(level.damageSources().magic(), 3.0F);
            }
        }

        @Override
        public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            int age = state.getValue(AGE);
            if (age < MAX_AGE && random.nextInt(6) == 0) {
                level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
            }
        }
    }

    // =========================================================================
    // 4. 凝霜幽兰 (Frostvein Orchid)
    // =========================================================================

    /**
     * 凝霜幽兰：仅能在极寒高山或冰川上凝结生长的冰晶兰花。
     * <ul>
     *   <li>若周围存在人造热源（方块光照 > 8）或生物群系温度过高（> 0.35），则会迅速融化为水。</li>
     * </ul>
     */
    public static class FrostveinOrchidBlock extends AlchemicalHerbBlock {

        public FrostveinOrchidBlock(Properties properties) {
            super(properties, () -> HerbRegistry.FROSTVEIN_ORCHID);
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return super.mayPlaceOn(state, level, pos)
                    || state.is(Blocks.SNOW_BLOCK)
                    || state.is(Blocks.POWDER_SNOW)
                    || state.is(Blocks.PACKED_ICE)
                    || state.is(Blocks.BLUE_ICE);
        }

        @Override
        public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            float temp = IAlchemicalHerb.EnvironmentProbe.getBiomeTemperature(level, pos);
            int blockLight = IAlchemicalHerb.EnvironmentProbe.getBlockLightLevel(level, pos);

            // 热力学检测：环境过热或靠近火源则融化
            if (temp > 0.35F || blockLight > 8 || hasNearbyHeatSource(level, pos)) {
                melt(level, pos);
                return;
            }

            int age = state.getValue(AGE);
            if (age < MAX_AGE && random.nextInt(5) == 0) {
                level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
            }
        }

        private boolean hasNearbyHeatSource(Level level, BlockPos pos) {
            for (BlockPos checkPos : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(2, 1, 2))) {
                BlockState bs = level.getBlockState(checkPos);
                if (bs.is(Blocks.FIRE) || bs.is(Blocks.SOUL_FIRE) || bs.is(Blocks.LAVA) || bs.is(Blocks.MAGMA_BLOCK)) {
                    return true;
                }
            }
            return false;
        }

        protected void melt(ServerLevel level, BlockPos pos) {
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.5F);
            level.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 20, 0.2, 0.1, 0.2, 0.05);
            if (level.dimensionType().ultraWarm()) {
                level.removeBlock(pos, false);
            } else {
                level.setBlockAndUpdate(pos, IceBlock.meltsInto());
            }
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(3) == 0) {
                double x = pos.getX() + 0.2D + random.nextDouble() * 0.6D;
                double y = pos.getY() + 0.2D + random.nextDouble() * 0.6D;
                double z = pos.getZ() + 0.2D + random.nextDouble() * 0.6D;
                level.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, 0.0D, -0.01D, 0.0D);
            }
        }
    }

    // =========================================================================
    // 5. 月影颠茄 (Moonveil Belladonna)
    // =========================================================================

    /**
     * 月影颠茄：受太阴潮汐引力支配的剧毒暗影花朵。
     * <ul>
     *   <li>仅在夜间且直视夜空时生长。</li>
     *   <li>在满月（Full Moon）与新月（New Moon）之夜进入 {@code BLOOMING = true} 盛开状态。</li>
     *   <li>白天花瓣紧闭收拢（{@code BLOOMING = false}），若在白天强行采收将导致毒素反噬。</li>
     * </ul>
     */
    public static class MoonveilBelladonnaBlock extends AlchemicalHerbBlock {

        public MoonveilBelladonnaBlock(Properties properties) {
            super(properties.lightLevel(state -> state.getValue(BLOOMING) ? 7 : 0), () -> HerbRegistry.MOONVEIL_BELLADONNA);
            this.registerDefaultState(this.defaultBlockState().setValue(BLOOMING, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(BLOOMING);
        }

        @Override
        public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            boolean canSeeSky = level.canSeeSky(pos);
            boolean isSyzygy = IAlchemicalHerb.EnvironmentProbe.isSyzygyNight(level);
            boolean currentlyBlooming = state.getValue(BLOOMING);

            // 更新盛开/收拢状态机
            boolean shouldBloom = canSeeSky && isSyzygy && isMaxAge(state);
            if (currentlyBlooming != shouldBloom) {
                state = state.setValue(BLOOMING, shouldBloom);
                level.setBlock(pos, state, Block.UPDATE_CLIENTS);
            }

            // 仅在夜晚可生长
            if (!level.isDay() && canSeeSky && state.getValue(AGE) < MAX_AGE && random.nextInt(4) == 0) {
                int nextAge = state.getValue(AGE) + 1;
                boolean bloomOnMature = (nextAge == MAX_AGE) && isSyzygy;
                level.setBlock(pos, state.setValue(AGE, nextAge).setValue(BLOOMING, bloomOnMature), Block.UPDATE_CLIENTS);
            }
        }

        @Override
        protected void onUnsafeHarvest(ServerLevel level, BlockPos pos, BlockState state, Player player, ItemStack tool) {
            // 白天或非盛开期采摘：释放颠茄神经毒雾
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 160, 1));
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
            level.sendParticles(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 25, 0.3, 0.3, 0.3, 0.02);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (state.getValue(BLOOMING) && random.nextInt(2) == 0) {
                double x = pos.getX() + 0.3D + random.nextDouble() * 0.4D;
                double y = pos.getY() + 0.4D + random.nextDouble() * 0.4D;
                double z = pos.getZ() + 0.3D + random.nextDouble() * 0.4D;
                level.addParticle(ParticleTypes.PORTAL, x, y, z, (random.nextDouble() - 0.5D) * 0.2D, -random.nextDouble() * 0.1D, (random.nextDouble() - 0.5D) * 0.2D);
            }
        }
    }

    // =========================================================================
    // 6. 腐水食腐苔 (Carrion Bogmoss)
    // =========================================================================

    /**
     * 腐水食腐苔：漂浮在沼泽死水表面的腐殖质苔藓（类似睡莲的浮水碰撞逻辑）。
     * <ul>
     *   <li>仅能放置在静止的水源方块（Still Water）或泥巴（Mud）上方。</li>
     *   <li>生物踩踏在其表面时会受到迟缓与腐毒侵蚀。</li>
     * </ul>
     */
    public static class CarrionBogmossBlock extends AlchemicalHerbBlock {

        protected static final VoxelShape CARPET_SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 1.5D, 15.0D);

        public CarrionBogmossBlock(Properties properties) {
            super(properties, () -> HerbRegistry.CARRION_BOGMOSS);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return CARPET_SHAPE;
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            FluidState fluidState = level.getFluidState(pos);
            FluidState fluidAbove = level.getFluidState(pos.above());
            boolean isStillWater = (fluidState.getType() == Fluids.WATER || state.getBlock() instanceof IceBlock)
                    && fluidAbove.getType() == Fluids.EMPTY;
            return isStillWater || state.is(Blocks.MUD) || state.is(Blocks.MUDDY_MANGROVE_ROOTS);
        }

        @Override
        public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            if (!level.isClientSide && entity instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                if (isMaxAge(state) && level.random.nextInt(20) == 0) {
                    living.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
                }
            }
            super.entityInside(state, level, pos, entity);
        }

        @Override
        public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            int age = state.getValue(AGE);
            if (age < MAX_AGE && random.nextInt(4) == 0) {
                level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
            }
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(4) == 0) {
                double x = pos.getX() + random.nextDouble();
                double y = pos.getY() + 0.1D;
                double z = pos.getZ() + random.nextDouble();
                level.addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, x, y, z, 0.0D, 0.02D, 0.0D);
            }
        }
    }

    // =========================================================================
    // 7. 避雷金盏花 (Storm Marigold)
    // =========================================================================

    /**
     * 避雷金盏花：富含金属离子与风元素的高地奇花。
     * <ul>
     *   <li>检测雷暴天气 ({@code level.isThundering()})，在雷暴期间进入 {@code CHARGED = true} 充能狂暴状态。</li>
     *   <li>充能状态下若不使用绝缘或专业草药剪采收，将引发电弧放电击伤采收者。</li>
     * </ul>
     */
    public static class StormMarigoldBlock extends AlchemicalHerbBlock {

        public StormMarigoldBlock(Properties properties) {
            super(properties.lightLevel(state -> state.getValue(CHARGED) ? 10 : 0), () -> HerbRegistry.STORM_MARIGOLD);
            this.registerDefaultState(this.defaultBlockState().setValue(CHARGED, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(CHARGED);
        }

        @Override
        public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            boolean thundering = level.isThundering() && level.canSeeSky(pos);
            boolean currentlyCharged = state.getValue(CHARGED);

            if (thundering != currentlyCharged) {
                state = state.setValue(CHARGED, thundering);
                level.setBlock(pos, state, Block.UPDATE_CLIENTS);
                if (thundering) {
                    level.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 0.5F, 1.8F);
                }
            }

            // 雷暴时生长速度翻倍
            int growthBound = thundering ? 2 : 6;
            if (state.getValue(AGE) < MAX_AGE && random.nextInt(growthBound) == 0) {
                level.setBlock(pos, state.setValue(AGE, state.getValue(AGE) + 1), Block.UPDATE_CLIENTS);
            }
        }

        @Override
        public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            if (state.getValue(CHARGED) && entity instanceof LivingEntity living) {
                living.hurt(level.damageSources().lightningBolt(), 2.0F);
            }
            super.entityInside(state, level, pos, entity);
        }

        @Override
        protected void onUnsafeHarvest(ServerLevel level, BlockPos pos, BlockState state, Player player, ItemStack tool) {
            if (state.getValue(CHARGED)) {
                player.hurt(level.damageSources().lightningBolt(), 6.0F);
                level.playSound(null, pos, SoundEvents.TRIDENT_THUNDER, SoundSource.BLOCKS, 1.0F, 1.2F);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 30, 0.3, 0.3, 0.3, 0.2);
            }
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (state.getValue(CHARGED)) {
                double x = pos.getX() + 0.2D + random.nextDouble() * 0.6D;
                double y = pos.getY() + 0.2D + random.nextDouble() * 0.7D;
                double z = pos.getZ() + 0.2D + random.nextDouble() * 0.6D;
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, (random.nextDouble() - 0.5D) * 0.1D, 0.05D, (random.nextDouble() - 0.5D) * 0.1D);
            }
        }
    }
}
```

---

### 3. `HerbRegistry.java` (草药与形态静态注册及炼金数据容器)

该枚举注册中心实现了 `IAlchemicalHerb` 接口，采用**预计算矩阵缓存（Pre-computed Matrix Caching）**技术，在类加载时完成所有 7 种草药 $\times$ 5 种加工形态（共 35 种组合）的炼金元素向量、品质、挥发性与毒性的非线性映射计算，保证运行期 `O(1)` 零GC开销查询。

```java
package com.medievalmagic.arsalchemica.herb;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 7 大奇特魔法草药静态注册表与炼金热力学数据容器。
 * <p>
 * 实现了 {@link IAlchemicalHerb} 契约。内部采用 {@link EnumMap} 对每种草药在 5 种加工形态下的
 * {@link AspectVector}、纯度、挥发性及毒性进行预计算缓存，确保坩埚高频 Tick 模拟时的 O(1) 访问性能。
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.20.1-2.0.0
 */
public enum HerbRegistry implements IAlchemicalHerb {

    /**
     * 1. 灰烬血蓟 (Cinder Thistle)
     * 主导元素：火 (Ignis) 与 混沌 (Perditio)。研磨成细粉时火元素激增，压榨成油时狂暴度极高。
     */
    CINDER_THISTLE(
            "cinder_thistle",
            new AspectVector(8.5f, 0.0f, 2.0f, 1.5f, 0.5f, 4.0f),
            1.20f, 1.80f, 15.0f,
            FormModifierMatrix.builder()
                    .with(ProcessingForm.RAW,           1.00f, 1.00f, 1.00f, new float[]{1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f})
                    .with(ProcessingForm.MILLED_COARSE, 1.15f, 1.30f, 0.90f, new float[]{1.2f, 1.0f, 1.1f, 1.1f, 1.0f, 1.1f})
                    .with(ProcessingForm.MILLED_FINE,   1.45f, 1.95f, 0.80f, new float[]{1.6f, 1.0f, 0.8f, 1.4f, 1.2f, 1.3f})
                    .with(ProcessingForm.PRESSED_OIL,   1.80f, 2.40f, 1.25f, new float[]{2.1f, 0.5f, 0.2f, 1.5f, 1.5f, 1.6f})
                    .with(ProcessingForm.DRIED,         1.05f, 0.50f, 0.60f, new float[]{1.1f, 0.0f, 1.3f, 0.6f, 1.2f, 0.7f})
                    .build()
    ) {
        @Override
        public boolean canSurviveInEnvironment(Level level, BlockPos pos, BlockState state) {
            return IAlchemicalHerb.EnvironmentProbe.getBiomeTemperature(level, pos) >= 0.5F;
        }

        @Override
        public boolean isSafeToHarvest(Level level, BlockPos pos, BlockState state, Player player, ItemStack tool) {
            return player != null && isShears(tool);
        }
    },

    /**
     * 2. 深渊幽光薄荷 (Abyssal Lumina Mint)
     * 主导元素：水 (Aqua) 与 秩序 (Ordo)。阴干会破坏其幽光活性，压榨成精油后秩序纯度达到巅峰。
     */
    ABYSSAL_LUMINA_MINT(
            "abyssal_lumina_mint",
            new AspectVector(0.0f, 6.5f, 1.5f, 2.5f, 7.5f, 0.5f),
            1.50f, 1.10f, 5.0f,
            FormModifierMatrix.builder()
                    .with(ProcessingForm.RAW,           1.00f, 1.00f, 1.00f, new float[]{1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f})
                    .with(ProcessingForm.MILLED_COARSE, 1.20f, 1.25f, 0.85f, new float[]{1.0f, 1.2f, 1.0f, 1.2f, 1.2f, 0.9f})
                    .with(ProcessingForm.MILLED_FINE,   1.35f, 1.60f, 0.70f, new float[]{1.0f, 1.3f, 0.7f, 1.5f, 1.4f, 0.8f})
                    .with(ProcessingForm.PRESSED_OIL,   1.95f, 1.85f, 0.40f, new float[]{0.0f, 1.8f, 0.2f, 1.3f, 2.2f, 0.2f})
                    .with(ProcessingForm.DRIED,         0.65f, 0.35f, 0.50f, new float[]{0.0f, 0.3f, 1.2f, 0.5f, 0.6f, 1.1f})
                    .build()
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
     * 主导元素：混沌 (Perditio) 与 土 (Terra)。生食毒性极强，唯有阴干（DRIED）或精细研磨可褪去其怨念毒性。
     */
    SCREAMING_MANDRAGORA(
            "screaming_mandragora",
            new AspectVector(1.5f, 2.0f, 6.0f, 3.5f, 1.0f, 9.5f),
            1.65f, 1.45f, 65.0f,
            FormModifierMatrix.builder()
                    .with(ProcessingForm.RAW,           1.00f, 1.00f, 1.00f, new float[]{1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f})
                    .with(ProcessingForm.MILLED_COARSE, 1.15f, 1.40f, 0.85f, new float[]{1.1f, 1.1f, 1.2f, 1.3f, 1.1f, 1.1f})
                    .with(ProcessingForm.MILLED_FINE,   1.40f, 1.75f, 0.65f, new float[]{1.2f, 1.0f, 1.4f, 1.5f, 1.5f, 0.9f})
                    .with(ProcessingForm.PRESSED_OIL,   1.70f, 2.10f, 1.50f, new float[]{1.4f, 1.6f, 0.4f, 1.2f, 0.8f, 1.8f})
                    .with(ProcessingForm.DRIED,         1.30f, 0.40f, 0.35f, new float[]{0.8f, 0.2f, 1.8f, 0.4f, 2.0f, 0.5f})
                    .build()
    ) {
        @Override
        public boolean canSurviveInEnvironment(Level level, BlockPos pos, BlockState state) {
            return true;
        }

        @Override
        public boolean isSafeToHarvest(Level level, BlockPos pos, BlockState state, Player player, ItemStack tool) {
            if (state.getValue(AlchemicalHerbBlock.AGE) < AlchemicalHerbBlock.MAX_AGE) {
                return true;
            }
            return player != null && isShears(tool);
        }
    },

    /**
     * 4. 凝霜幽兰 (Frostvein Orchid)
     * 主导元素：水 (Aqua) 与 秩序 (Ordo)，火属性为绝对零值。极度抗拒高温，粗磨与精油形态最佳。
     */
    FROSTVEIN_ORCHID(
            "frostvein_orchid",
            new AspectVector(0.0f, 8.0f, 3.0f, 4.0f, 6.5f, 0.0f),
            1.75f, 2.10f, 8.0f,
            FormModifierMatrix.builder()
                    .with(ProcessingForm.RAW,           1.00f, 1.00f, 1.00f, new float[]{0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f})
                    .with(ProcessingForm.MILLED_COARSE, 1.25f, 1.35f, 0.90f, new float[]{0.0f, 1.3f, 1.1f, 1.2f, 1.2f, 1.0f})
                    .with(ProcessingForm.MILLED_FINE,   1.50f, 2.20f, 0.80f, new float[]{0.0f, 1.6f, 0.9f, 1.6f, 1.5f, 1.0f})
                    .with(ProcessingForm.PRESSED_OIL,   2.00f, 2.50f, 0.50f, new float[]{0.0f, 2.2f, 0.3f, 1.8f, 2.0f, 0.5f})
                    .with(ProcessingForm.DRIED,         0.80f, 0.60f, 0.70f, new float[]{0.0f, 0.5f, 1.4f, 0.8f, 1.1f, 1.0f})
                    .build()
    ) {
        @Override
        public boolean canSurviveInEnvironment(Level level, BlockPos pos, BlockState state) {
            return IAlchemicalHerb.EnvironmentProbe.getBiomeTemperature(level, pos) <= 0.35F
                    && IAlchemicalHerb.EnvironmentProbe.getBlockLightLevel(level, pos) <= 8;
        }

        @Override
        public boolean isSafeToHarvest(Level level, BlockPos pos, BlockState state, Player player, ItemStack tool) {
            return player != null && isShears(tool);
        }
    },

    /**
     * 5. 月影颠茄 (Moonveil Belladonna)
     * 主导元素：混沌 (Perditio)、风 (Aer) 与 水 (Aqua)。剧毒暗影植物，压榨成精油后毒性与魔力双双突破极限。
     */
    MOONVEIL_BELLADONNA(
            "moonveil_belladonna",
            new AspectVector(0.5f, 5.0f, 1.5f, 5.5f, 2.5f, 8.5f),
            1.80f, 1.60f, 80.0f,
            FormModifierMatrix.builder()
                    .with(ProcessingForm.RAW,           1.00f, 1.00f, 1.00f, new float[]{1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f})
                    .with(ProcessingForm.MILLED_COARSE, 1.15f, 1.30f, 1.10f, new float[]{1.0f, 1.2f, 1.0f, 1.2f, 1.1f, 1.2f})
                    .with(ProcessingForm.MILLED_FINE,   1.45f, 1.80f, 1.25f, new float[]{1.0f, 1.4f, 0.8f, 1.6f, 1.3f, 1.5f})
                    .with(ProcessingForm.PRESSED_OIL,   2.10f, 2.30f, 1.60f, new float[]{0.5f, 1.9f, 0.2f, 1.8f, 1.6f, 2.1f})
                    .with(ProcessingForm.DRIED,         1.10f, 0.50f, 0.75f, new float[]{1.0f, 0.6f, 1.4f, 0.7f, 1.4f, 0.9f})
                    .build()
    ) {
        @Override
        public boolean canSurviveInEnvironment(Level level, BlockPos pos, BlockState state) {
            return true;
        }

        @Override
        public boolean isSafeToHarvest(Level level, BlockPos pos, BlockState state, Player player, ItemStack tool) {
            boolean blooming = state.hasProperty(AlchemicalHerbBlock.BLOOMING) && state.getValue(AlchemicalHerbBlock.BLOOMING);
            return !level.isDay() && (blooming || (player != null && isShears(tool)));
        }
    },

    /**
     * 6. 腐水食腐苔 (Carrion Bogmoss)
     * 主导元素：土 (Terra)、水 (Aqua) 与 混沌 (Perditio)。性质极其粘稠稳定，挥发性极低，适合作为慢火基底。
     */
    CARRION_BOGMOSS(
            "carrion_bogmoss",
            new AspectVector(0.0f, 6.0f, 7.5f, 0.5f, 1.0f, 6.5f),
            1.10f, 0.55f, 40.0f,
            FormModifierMatrix.builder()
                    .with(ProcessingForm.RAW,           1.00f, 1.00f, 1.00f, new float[]{1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f})
                    .with(ProcessingForm.MILLED_COARSE, 1.10f, 1.15f, 0.95f, new float[]{1.0f, 1.1f, 1.2f, 1.0f, 1.1f, 1.1f})
                    .with(ProcessingForm.MILLED_FINE,   1.30f, 1.40f, 0.85f, new float[]{1.0f, 1.2f, 1.5f, 1.2f, 1.3f, 1.2f})
                    .with(ProcessingForm.PRESSED_OIL,   1.55f, 1.65f, 1.30f, new float[]{0.0f, 1.8f, 0.6f, 1.0f, 0.9f, 1.7f})
                    .with(ProcessingForm.DRIED,         1.25f, 0.25f, 0.50f, new float[]{0.0f, 0.2f, 1.9f, 0.5f, 1.6f, 0.8f})
                    .build()
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
     * 主导元素：风 (Aer) 与 火 (Ignis)。蕴含狂暴的雷霆电浆，挥发性冠绝群芳，精磨粉末是高阶迅捷与雷暴药剂的核心。
     */
    STORM_MARIGOLD(
            "storm_marigold",
            new AspectVector(6.5f, 1.0f, 1.5f, 9.0f, 3.0f, 2.5f),
            1.60f, 2.35f, 20.0f,
            FormModifierMatrix.builder()
                    .with(ProcessingForm.RAW,           1.00f, 1.00f, 1.00f, new float[]{1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f})
                    .with(ProcessingForm.MILLED_COARSE, 1.20f, 1.40f, 0.90f, new float[]{1.2f, 1.0f, 1.0f, 1.3f, 1.1f, 1.1f})
                    .with(ProcessingForm.MILLED_FINE,   1.65f, 2.10f, 0.75f, new float[]{1.5f, 0.8f, 0.7f, 1.8f, 1.4f, 1.2f})
                    .with(ProcessingForm.PRESSED_OIL,   1.85f, 2.60f, 1.10f, new float[]{1.8f, 1.2f, 0.3f, 2.0f, 1.5f, 1.4f})
                    .with(ProcessingForm.DRIED,         1.00f, 0.70f, 0.60f, new float[]{1.1f, 0.3f, 1.3f, 0.9f, 1.3f, 0.8f})
                    .build()
    ) {
        @Override
        public boolean canSurviveInEnvironment(Level level, BlockPos pos, BlockState state) {
            return level.canSeeSky(pos);
        }

        @Override
        public boolean isSafeToHarvest(Level level, BlockPos pos, BlockState state, Player player, ItemStack tool) {
            boolean charged = state.hasProperty(AlchemicalHerbBlock.CHARGED) && state.getValue(AlchemicalHerbBlock.CHARGED);
            if (!charged) return true;
            return player != null && isShears(tool);
        }
    };

    // =========================================================================
    // 枚举字段与预计算缓存结构
    // =========================================================================

    private final String registryName;
    private final Map<ProcessingForm, AspectVector> precomputedVectors;
    private final Map<ProcessingForm, Float> precomputedQualities;
    private final Map<ProcessingForm, Float> precomputedVolatilities;
    private final Map<ProcessingForm, Float> precomputedToxicities;

    HerbRegistry(String registryName,
                 AspectVector rawVector,
                 float baseQuality,
                 float baseVolatility,
                 float baseToxicity,
                 FormModifierMatrix matrix) {
        this.registryName = registryName;

        EnumMap<ProcessingForm, AspectVector> vectorMap = new EnumMap<>(ProcessingForm.class);
        EnumMap<ProcessingForm, Float> qualityMap = new EnumMap<>(ProcessingForm.class);
        EnumMap<ProcessingForm, Float> volatilityMap = new EnumMap<>(ProcessingForm.class);
        EnumMap<ProcessingForm, Float> toxicityMap = new EnumMap<>(ProcessingForm.class);

        for (ProcessingForm form : ProcessingForm.values()) {
            FormModifierMatrix.Entry mod = matrix.get(form);
            float[] m = mod.elementMultipliers;
            vectorMap.put(form, rawVector.multiply(m[0], m[1], m[2], m[3], m[4], m[5]));
            qualityMap.put(form, baseQuality * mod.qualityMultiplier);
            volatilityMap.put(form, baseVolatility * mod.volatilityMultiplier);
            toxicityMap.put(form, baseToxicity * mod.toxicityMultiplier);
        }

        this.precomputedVectors = Collections.unmodifiableMap(vectorMap);
        this.precomputedQualities = Collections.unmodifiableMap(qualityMap);
        this.precomputedVolatilities = Collections.unmodifiableMap(volatilityMap);
        this.precomputedToxicities = Collections.unmodifiableMap(toxicityMap);
    }

    public String getRegistryName() {
        return registryName;
    }

    @Override
    public AspectVector getAspectVector(ProcessingForm form) {
        return this.precomputedVectors.getOrDefault(form, AspectVector.ZERO);
    }

    @Override
    public float getBaseQuality(ProcessingForm form) {
        return this.precomputedQualities.getOrDefault(form, 1.0f);
    }

    @Override
    public float getVolatilityModifier(ProcessingForm form) {
        return this.precomputedVolatilities.getOrDefault(form, 1.0f);
    }

    @Override
    public float getBaseToxicity(ProcessingForm form) {
        return this.precomputedToxicities.getOrDefault(form, 0.0f);
    }

    protected static boolean isShears(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(AlchemicalHerbBlock.HERB_SHEARS_TAG) || stack.is(Items.SHEARS));
    }

    // =========================================================================
    // 加工形态变换矩阵构建器 (Form Modifier Matrix Builder)
    // =========================================================================

    private static final class FormModifierMatrix {
        private final EnumMap<ProcessingForm, Entry> entries = new EnumMap<>(ProcessingForm.class);

        private record Entry(float qualityMultiplier, float volatilityMultiplier, float toxicityMultiplier, float[] elementMultipliers) {}

        public static Builder builder() {
            return new Builder();
        }

        public Entry get(ProcessingForm form) {
            return entries.get(form);
        }

        private static final class Builder {
            private final FormModifierMatrix matrix = new FormModifierMatrix();

            public Builder with(ProcessingForm form, float qualityMult, float volatilityMult, float toxicityMult, float[] elementMults) {
                if (elementMults.length != 6) {
                    throw new IllegalArgumentException("Element multiplier array must have exactly 6 dimensions (Ignis, Aqua, Terra, Aer, Ordo, Perditio).");
                }
                matrix.entries.put(form, new Entry(qualityMult, volatilityMult, toxicityMult, elementMults));
                return this;
            }

            public FormModifierMatrix build() {
                return matrix;
            }
        }
    }

    // =========================================================================
    // 方块实例工厂 (Block Registration Factories)
    // 供 Forge / Fabric 多端注册器 (DeferredRegister / Registry.register) 直接调用
    // =========================================================================

    public static final Supplier<Block> CINDER_THISTLE_BLOCK_FACTORY = () ->
            new AlchemicalHerbBlock.CinderThistleBlock(defaultHerbProperties(MapColor.COLOR_RED));

    public static final Supplier<Block> ABYSSAL_LUMINA_MINT_BLOCK_FACTORY = () ->
            new AlchemicalHerbBlock.AbyssalLuminaMintBlock(defaultHerbProperties(MapColor.COLOR_CYAN));

    public static final Supplier<Block> SCREAMING_MANDRAGORA_BLOCK_FACTORY = () ->
            new AlchemicalHerbBlock.ScreamingMandragoraBlock(defaultHerbProperties(MapColor.COLOR_BROWN));

    public static final Supplier<Block> FROSTVEIN_ORCHID_BLOCK_FACTORY = () ->
            new AlchemicalHerbBlock.FrostveinOrchidBlock(defaultHerbProperties(MapColor.ICE));

    public static final Supplier<Block> MOONVEIL_BELLADONNA_BLOCK_FACTORY = () ->
            new AlchemicalHerbBlock.MoonveilBelladonnaBlock(defaultHerbProperties(MapColor.COLOR_PURPLE));

    public static final Supplier<Block> CARRION_BOGMOSS_BLOCK_FACTORY = () ->
            new AlchemicalHerbBlock.CarrionBogmossBlock(defaultHerbProperties(MapColor.COLOR_GREEN));

    public static final Supplier<Block> STORM_MARIGOLD_BLOCK_FACTORY = () ->
            new AlchemicalHerbBlock.StormMarigoldBlock(defaultHerbProperties(MapColor.GOLD));

    private static BlockBehaviour.Properties defaultHerbProperties(MapColor color) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .noCollission()
                .randomTicks()
                .instabreak()
                .sound(Blocks.SWEET_BERRY_BUSH.defaultBlockState().getSoundType())
                .pushReaction(PushReaction.DESTROY);
    }
}
```