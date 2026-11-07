package net.minecraft.server.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class ServerLevel extends Level {
    @Override public BlockState getBlockState(BlockPos pos) { return null; }
    @Override public FluidState getFluidState(BlockPos pos) { return null; }
    @Override public BlockEntity getBlockEntity(BlockPos pos) { return null; }

    public <T extends ParticleOptions> int sendParticles(T type, double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
        return count;
    }
}
