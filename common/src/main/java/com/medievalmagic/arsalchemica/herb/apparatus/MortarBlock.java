package com.medievalmagic.arsalchemica.herb.apparatus;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * 黑曜石研磨石臼方块（加固版）。
 * 完全事件驱动 (Zero-Tick Overhead)，杜绝刷物品与交互冲突。
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public class MortarBlock extends BaseEntityBlock {

    private static final VoxelShape SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 7.0D, 13.0D);

    public MortarBlock(Properties properties) {
        super(properties);
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
        if (!(level.getBlockEntity(pos) instanceof MortarBlockEntity mortar)) return InteractionResult.PASS;

        ItemStack heldStack = player.getItemInHand(hand);

        // 1. 手持研磨杵 (Pestle)：执行研磨
        if (isPestle(heldStack)) {
            if (!mortar.isEmpty()) {
                if (!level.isClientSide) {
                    mortar.performGrind(player, heldStack);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            return InteractionResult.CONSUME;
        }

        // 2. 潜行 + 空手 或 普通空手交互：取出当前石臼内的物品
        if (heldStack.isEmpty()) {
            if (!mortar.isEmpty()) {
                if (!level.isClientSide) {
                    ItemStack extracted = mortar.extractItem();
                    if (!player.getInventory().add(extracted)) {
                        player.drop(extracted, false);
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            return InteractionResult.PASS;
        }

        // 3. 持有可研磨草药：放入石臼
        if (mortar.isEmpty() && mortar.canAccept(heldStack)) {
            if (!level.isClientSide) {
                ItemStack toInsert = heldStack.split(1);
                mortar.insertItem(toInsert);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    private boolean isPestle(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String name = stack.getItem().getDescriptionId().toLowerCase(Locale.ROOT);
        return name.contains("pestle") || stack.is(Items.STICK);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof MortarBlockEntity mortar && !mortar.isEmpty()) {
                ItemStack extracted = mortar.extractItem(); // 原子提取清空，杜绝二次破坏刷物品
                if (!extracted.isEmpty()) {
                    Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, extracted);
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MortarBlockEntity(pos, state);
    }
}
