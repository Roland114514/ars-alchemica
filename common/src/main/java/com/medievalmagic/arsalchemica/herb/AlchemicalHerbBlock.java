package com.medievalmagic.arsalchemica.herb;

import com.medievalmagic.arsalchemica.herb.apparatus.ProcessedHerbItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
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
 * 炼金草药方块核心基类及 7 大特种衍生草药实现（加固版）。
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public abstract class AlchemicalHerbBlock extends BushBlock {

    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    public static final int MAX_AGE = 3;

    public static final BooleanProperty BLOOMING = BooleanProperty.create("blooming");
    public static final BooleanProperty CHARGED = BooleanProperty.create("charged");

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

    protected boolean isUsingShears(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.SHEARS);
    }

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

    protected void onUnsafeHarvest(ServerLevel level, BlockPos pos, BlockState state, Player player, ItemStack tool) {}

    // =========================================================================
    // 1. 灰烬血蓟 (Cinder Thistle)
    // =========================================================================
    public static class CinderThistleBlock extends AlchemicalHerbBlock {
        public CinderThistleBlock(Properties properties) {
            super(properties, () -> HerbRegistry.CINDER_THISTLE);
        }

        @Override
        public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            if (entity instanceof LivingEntity living && !entity.fireImmune()) {
                living.setSecondsOnFire(4);
                living.hurt(level.damageSources().inFire(), 1.5F + state.getValue(AGE) * 0.5F);
            }
            super.entityInside(state, level, pos, entity);
        }

        @Override
        protected void onUnsafeHarvest(ServerLevel level, BlockPos pos, BlockState state, Player player, ItemStack tool) {
            player.setSecondsOnFire(6);
            player.hurt(level.damageSources().onFire(), 4.0F);
            level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 0.8F);
            level.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 15, 0.2, 0.2, 0.2, 0.1);
        }

        @Override
        public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (!level.hasChunkAt(pos)) return;
            int age = state.getValue(AGE);
            if (age < MAX_AGE && random.nextInt(5) == 0) {
                level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
            }
        }
    }

    // =========================================================================
    // 2. 深渊幽光薄荷 (Abyssal Lumina Mint)
    // =========================================================================
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
            if (!level.hasChunkAt(pos)) return;
            int light = IAlchemicalHerb.EnvironmentProbe.getCombinedLightLevel(level, pos);
            int age = state.getValue(AGE);

            if (light > 7) {
                if (age > 0) {
                    level.setBlock(pos, state.setValue(AGE, age - 1), Block.UPDATE_CLIENTS);
                    level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 5, 0.1, 0.1, 0.1, 0.01);
                } else {
                    level.destroyBlock(pos, false);
                }
            } else if (light <= 4 && age < MAX_AGE && random.nextInt(4) == 0) {
                level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
            }
        }
    }

    // =========================================================================
    // 3. 哀嚎曼陀罗 (Screaming Mandragora)
    // =========================================================================
    public static class ScreamingMandragoraBlock extends AlchemicalHerbBlock {
        private static final double SCREAM_RADIUS = 8.0D;

        public ScreamingMandragoraBlock(Properties properties) {
            super(properties, () -> HerbRegistry.SCREAMING_MANDRAGORA);
        }

        @Override
        protected void onUnsafeHarvest(ServerLevel level, BlockPos pos, BlockState state, Player player, ItemStack tool) {
            if (!isMaxAge(state)) return;

            level.playSound(null, pos, SoundEvents.GHAST_SCREAM, SoundSource.HOSTILE, 2.0F, 0.7F);
            level.sendParticles(ParticleTypes.SONIC_BOOM, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.0, 0.0, 0.0, 0.0);

            AABB affectBox = new AABB(pos).inflate(SCREAM_RADIUS);
            List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, affectBox);
            for (LivingEntity victim : victims) {
                victim.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 240, 1)); // 12秒反胃
                victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 160, 0)); // 8秒失明
                victim.hurt(level.damageSources().magic(), 3.0F);
            }
        }

        @Override
        public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (!level.hasChunkAt(pos)) return;
            int age = state.getValue(AGE);
            if (age < MAX_AGE && random.nextInt(6) == 0) {
                level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
            }
        }
    }

    // =========================================================================
    // 4. 凝霜幽兰 (Frostvein Orchid)
    // =========================================================================
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
            if (!level.hasChunkAt(pos)) return;
            float temp = IAlchemicalHerb.EnvironmentProbe.getBiomeTemperature(level, pos);
            int blockLight = IAlchemicalHerb.EnvironmentProbe.getBlockLightLevel(level, pos);

            if (temp > 0.35F || blockLight > 8) {
                melt(level, pos);
                return;
            }

            int age = state.getValue(AGE);
            if (age < MAX_AGE && random.nextInt(5) == 0) {
                level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
            }
        }

        protected void melt(ServerLevel level, BlockPos pos) {
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.5F);
            level.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 20, 0.2, 0.1, 0.2, 0.05);
            level.setBlockAndUpdate(pos, IceBlock.meltsInto());
        }
    }

    // =========================================================================
    // 5. 月影颠茄 (Moonveil Belladonna)
    // =========================================================================
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
            if (!level.hasChunkAt(pos)) return;
            boolean canSeeSky = level.canSeeSky(pos);
            boolean isSyzygy = IAlchemicalHerb.EnvironmentProbe.isSyzygyNight(level);
            boolean currentlyBlooming = state.getValue(BLOOMING);

            boolean shouldBloom = canSeeSky && isSyzygy && isMaxAge(state);
            if (currentlyBlooming != shouldBloom) {
                state = state.setValue(BLOOMING, shouldBloom);
                level.setBlock(pos, state, Block.UPDATE_CLIENTS);
            }

            if (!level.isDay() && canSeeSky && state.getValue(AGE) < MAX_AGE && random.nextInt(4) == 0) {
                int nextAge = state.getValue(AGE) + 1;
                boolean bloomOnMature = (nextAge == MAX_AGE) && isSyzygy;
                level.setBlock(pos, state.setValue(AGE, nextAge).setValue(BLOOMING, bloomOnMature), Block.UPDATE_CLIENTS);
            }
        }

        @Override
        protected void onUnsafeHarvest(ServerLevel level, BlockPos pos, BlockState state, Player player, ItemStack tool) {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 160, 1));
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
            level.sendParticles(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 25, 0.3, 0.3, 0.3, 0.02);
        }
    }

    // =========================================================================
    // 6. 腐水食腐苔 (Carrion Bogmoss)
    // =========================================================================
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
            boolean isStillWater = fluidState.getType() == Fluids.WATER && fluidAbove.getType() == Fluids.EMPTY;
            return isStillWater || state.is(Blocks.MUD) || state.is(Blocks.MUDDY_MANGROVE_ROOTS);
        }

        @Override
        public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            if (!level.isClientSide() && entity instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            }
            super.entityInside(state, level, pos, entity);
        }

        @Override
        public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (!level.hasChunkAt(pos)) return;
            int age = state.getValue(AGE);
            if (age < MAX_AGE && random.nextInt(4) == 0) {
                level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
            }
        }
    }

    // =========================================================================
    // 7. 避雷金盏花 (Storm Marigold)
    // =========================================================================
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
            if (!level.hasChunkAt(pos)) return;
            boolean thundering = level.isThundering() && level.canSeeSky(pos);
            boolean currentlyCharged = state.getValue(CHARGED);

            if (thundering != currentlyCharged) {
                state = state.setValue(CHARGED, thundering);
                level.setBlock(pos, state, Block.UPDATE_CLIENTS);
            }

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
    }
}
