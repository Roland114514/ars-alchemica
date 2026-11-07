package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.function.ToIntFunction;

public class Block {
    public static final int UPDATE_ALL = 3;
    public static final int UPDATE_CLIENTS = 2;

    protected final StateDefinition<Block, BlockState> stateDefinition;
    private BlockState defaultBlockState;

    public Block(Properties properties) {
        this.stateDefinition = new StateDefinition<>(new BlockState(this));
        this.defaultBlockState = this.stateDefinition.any();
    }

    public static VoxelShape box(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new VoxelShape();
    }

    public BlockState defaultBlockState() {
        return defaultBlockState;
    }

    protected void registerDefaultState(BlockState state) {
        this.defaultBlockState = state;
    }

    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return new VoxelShape();
    }

    @Nullable
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState();
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    }

    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }

    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
    }

    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
    }

    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
    }

    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    public static class Properties {
        public static Properties of() { return new Properties(); }
        public Properties randomTicks() { return this; }
        public Properties noCollission() { return this; }
        public Properties lightLevel(ToIntFunction<BlockState> light) { return this; }
    }
}
