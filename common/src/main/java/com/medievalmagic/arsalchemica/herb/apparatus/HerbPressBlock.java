package com.medievalmagic.arsalchemica.herb.apparatus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
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
 * 杠杆活塞压榨台方块（加固版）。
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public class HerbPressBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty PRESSED = BooleanProperty.create("pressed");
    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D);

    public HerbPressBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PRESSED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PRESSED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof HerbPressBlockEntity press)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);

        // 1. 放入空玻璃小瓶
        if (held.is(Items.GLASS_BOTTLE) && press.canInsertVial()) {
            if (!level.isClientSide) {
                press.insertVial(held.split(1));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // 2. 放入新鲜草药 (RAW)
        if (press.canInsertHerb(held)) {
            if (!level.isClientSide) {
                press.insertHerb(held.split(1));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // 3. 潜行 + 空手：撤回尚未压榨的原料
        if (player.isShiftKeyDown() && held.isEmpty()) {
            if (!level.isClientSide) {
                ItemStack returned = press.extractAnyRemaining();
                if (!returned.isEmpty() && !player.getInventory().add(returned)) {
                    player.drop(returned, false);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // 4. 空手交互：取出产物精油或拉动杠杆
        if (held.isEmpty()) {
            if (press.hasOutputOil()) {
                if (!level.isClientSide) {
                    ItemStack oil = press.extractVialSlot();
                    if (!player.getInventory().add(oil)) player.drop(oil, false);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }

            if (press.canActivatePress()) {
                if (!level.isClientSide) {
                    press.activateLeverPress(state);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }

            // 无可压榨物料时，取出残留物
            if (!level.isClientSide) {
                ItemStack returned = press.extractAnyRemaining();
                if (!returned.isEmpty() && !player.getInventory().add(returned)) {
                    player.drop(returned, false);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof HerbPressBlockEntity press) {
                ItemStack herb = press.extractAnyRemaining();
                ItemStack vial = press.extractVialSlot();
                if (!herb.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, herb);
                if (!vial.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, vial);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HerbPressBlockEntity(pos, state);
    }
}
