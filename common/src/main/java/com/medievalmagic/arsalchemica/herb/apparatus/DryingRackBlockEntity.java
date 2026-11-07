package com.medievalmagic.arsalchemica.herb.apparatus;

import com.medievalmagic.arsalchemica.herb.HerbRegistry;
import com.medievalmagic.arsalchemica.herb.IAlchemicalHerb.ProcessingForm;
import com.medievalmagic.arsalchemica.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 通风风干架实体（加固版）。
 * <p>
 * 遵循 Zero-Lag 相位惰性 Tick 铁律：
 * 1. 每 20 ticks (1秒) 仅计算一次风干进度；
 * 2. 真实中世纪热力学仿真：淋雨受潮退化、下方营火/热源加速风干；
 * 3. 纯净原子物品提取，杜绝二次破坏与并发刷物品。
 * </p>
 *
 * @author Ars Alchemica Architecture Team
 * @version 1.0.0
 */
public class DryingRackBlockEntity extends BlockEntity {

    public static final int SLOT_COUNT = 4;
    public static final int REQUIRED_DRYING_TICKS = 600; // 30 秒基础风干完成时间

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final int[] dryingProgress = new int[SLOT_COUNT];

    public DryingRackBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.getDryingRackType(), pos, state);
    }

    public DryingRackBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean canHangHerb(ItemStack stack) {
        if (!(stack.getItem() instanceof ProcessedHerbItem herbItem)) return false;
        if (herbItem.isSlag(stack) || herbItem.getForm(stack) != ProcessingForm.RAW) return false;

        for (ItemStack slot : items) {
            if (slot.isEmpty()) return true;
        }
        return false;
    }

    public boolean hangHerb(ItemStack stack) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, stack.copyWithCount(1));
                dryingProgress[i] = 0;
                sync();
                return true;
            }
        }
        return false;
    }

    public ItemStack takeHerb() {
        // 优先取下已风干完成的
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty() && dryingProgress[i] >= REQUIRED_DRYING_TICKS) {
                items.set(i, ItemStack.EMPTY);
                dryingProgress[i] = 0;
                sync();
                return stack;
            }
        }
        // 其次取下任意未风干的
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                items.set(i, ItemStack.EMPTY);
                dryingProgress[i] = 0;
                sync();
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * 服务端周期 Tick：严格采用相位取模 (Staggered Ticking) 避免集中卡顿。
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, DryingRackBlockEntity rack) {
        rack.tick(level, pos, state);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if ((level.getGameTime() + pos.hashCode()) % 20 != 0) {
            return;
        }

        // 1. 露天淋雨检测：回潮惩罚
        boolean raining = level.isRainingAt(pos.above()) || level.isRainingAt(pos);
        if (raining) {
            boolean changed = false;
            for (int i = 0; i < SLOT_COUNT; i++) {
                if (!items.get(i).isEmpty() && dryingProgress[i] > 0) {
                    dryingProgress[i] = Math.max(0, dryingProgress[i] - 10);
                    changed = true;
                }
            }
            if (changed) sync();
            return;
        }

        // 2. 热源检测：下方有营火、火、岩浆块时显著加速风干
        int rateBonus = 20; // 基础 20 ticks / 秒
        if (isHeatSource(level.getBlockState(pos.below(1))) || isHeatSource(level.getBlockState(pos.below(2)))) {
            rateBonus += 20; // 2x 烘烤加速
        }

        boolean changed = false;
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ProcessedHerbItem herbItem) {
                if (herbItem.getForm(stack) == ProcessingForm.RAW) {
                    dryingProgress[i] += rateBonus;
                    if (dryingProgress[i] >= REQUIRED_DRYING_TICKS) {
                        // 蜕变为阴干形态 (DRIED)，继承原有基础品质并赋予 1.15x 通风增益
                        HerbRegistry herb = herbItem.getHerb(stack);
                        double quality = herbItem.getQuality(stack) * 1.15;
                        items.set(i, ProcessedHerbItem.create(herbItem, herb, ProcessingForm.DRIED, quality));
                        changed = true;
                    }
                }
            }
        }

        if (changed) {
            sync();
        }
    }

    private static boolean isHeatSource(BlockState state) {
        if (state == null) return false;
        if (state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)) {
            return state.getOptionalValue(CampfireBlock.LIT).orElse(false);
        }
        return state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.LAVA);
    }

    public NonNullList<ItemStack> getItems() { return items; }
    public int[] getDryingProgress() { return dryingProgress; }

    private void sync() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        tag.putIntArray("Progress", dryingProgress);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ContainerHelper.loadAllItems(tag, items);
        if (tag.contains("Progress")) {
            int[] arr = tag.getIntArray("Progress");
            System.arraycopy(arr, 0, dryingProgress, 0, Math.min(arr.length, SLOT_COUNT));
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
