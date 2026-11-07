package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntity {
    protected final BlockEntityType<?> type;
    protected final BlockPos worldPosition;
    protected BlockState blockState;
    protected Level level;

    public BlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        this.type = type;
        this.worldPosition = pos;
        this.blockState = state;
    }

    public BlockEntityType<?> getType() { return type; }
    public BlockPos getBlockPos() { return worldPosition; }
    public BlockState getBlockState() { return blockState; }
    public Level getLevel() { return level; }
    public void setLevel(Level level) { this.level = level; }

    public void setChanged() {}

    protected void saveAdditional(CompoundTag tag) {}
    public void load(CompoundTag tag) {}
    public CompoundTag getUpdateTag() { return new CompoundTag(); }

    @org.jetbrains.annotations.Nullable
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return null;
    }
}
