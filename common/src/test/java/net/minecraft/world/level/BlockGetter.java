package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public interface BlockGetter {
    BlockState getBlockState(BlockPos pos);
    FluidState getFluidState(BlockPos pos);
    BlockEntity getBlockEntity(BlockPos pos);
}
