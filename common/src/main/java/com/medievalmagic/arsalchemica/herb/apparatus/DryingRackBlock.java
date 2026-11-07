package com.medievalmagic.arsalchemica.herb.apparatus;

import com.medievalmagic.arsalchemica.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 通风风干架方块 (支持墙面挂载与横梁悬吊)（加固版）。
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public class DryingRackBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty CEILING_MOUNTED = BooleanProperty.create("ceiling");

    private static final VoxelShape SHAPE_CEILING = Block.box(2.0D, 10.0D, 2.0D, 14.0D, 16.0D, 14.0D);
    private static final VoxelShape SHAPE_NORTH = Block.box(1.0D, 8.0D, 11.0D, 15.0D, 15.0D, 16.0D);
    private static final VoxelShape SHAPE_SOUTH = Block.box(1.0D, 8.0D, 0.0D, 15.0D, 15.0D, 5.0D);
    private static final VoxelShape SHAPE_WEST = Block.box(11.0D, 8.0D, 1.0D, 16.0D, 15.0D, 15.0D);
    private static final VoxelShape SHAPE_EAST = Block.box(0.0D, 8.0D, 1.0D, 5.0D, 15.0D, 15.0D);

    public DryingRackBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(CEILING_MOUNTED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CEILING_MOUNTED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        if (clickedFace == Direction.DOWN) {
            return this.defaultBlockState()
                    .setValue(FACING, context.getHorizontalDirection().getOpposite())
                    .setValue(CEILING_MOUNTED, true);
        } else if (clickedFace.getAxis().isHorizontal()) {
            return this.defaultBlockState()
                    .setValue(FACING, clickedFace)
                    .setValue(CEILING_MOUNTED, false);
        }
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(CEILING_MOUNTED, true);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(CEILING_MOUNTED)) return SHAPE_CEILING;
        return switch (state.getValue(FACING)) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            case EAST -> SHAPE_EAST;
            default -> SHAPE_CEILING;
        };
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof DryingRackBlockEntity rack)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);

        // 1. 悬挂新鲜草药
        if (rack.canHangHerb(held)) {
            if (!level.isClientSide() && rack.hangHerb(held.split(1))) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        // 2. 空手取下草药
        if (held.isEmpty()) {
            if (!level.isClientSide()) {
                ItemStack retrieved = rack.takeHerb();
                if (!retrieved.isEmpty()) {
                    if (!player.getInventory().add(retrieved)) player.drop(retrieved, false);
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof DryingRackBlockEntity rack) {
                ItemStack item;
                while (!(item = rack.takeHerb()).isEmpty()) {
                    Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, item);
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DryingRackBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ModBlockEntities.getDryingRackType(), DryingRackBlockEntity::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actualType, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker) {
        return expectedType == actualType ? (BlockEntityTicker<A>) ticker : null;
    }
}
