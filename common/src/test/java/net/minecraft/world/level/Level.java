package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public abstract class Level implements LevelReader {
    public boolean isClientSide = false;
    private long gameTime = 0L;
    private final DamageSources damageSources = new DamageSources();
    private final List<Entity> entities = new ArrayList<>();

    public boolean isClientSide() { return isClientSide; }
    public long getGameTime() { return gameTime; }
    public void setGameTime(long time) { this.gameTime = time; }
    public DamageSources damageSources() { return damageSources; }

    public int getMoonPhase() { return 0; }
    public boolean isDay() { return true; }
    public boolean isThundering() { return false; }
    public boolean isRainingAt(BlockPos pos) { return false; }

    @Override public Holder<Biome> getBiome(BlockPos pos) { return Holder.direct(new Biome(0.8F)); }
    @Override public int getMaxLocalRawBrightness(BlockPos pos) { return 15; }
    @Override public int getBrightness(LightLayer layer, BlockPos pos) { return 15; }
    @Override public boolean canSeeSky(BlockPos pos) { return true; }
    @Override public boolean hasChunkAt(BlockPos pos) { return true; }

    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return null;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return Fluids.EMPTY.defaultFluidState();
    }

    public boolean setBlock(BlockPos pos, BlockState state, int flags) { return true; }
    public boolean setBlockAndUpdate(BlockPos pos, BlockState state) { return setBlock(pos, state, 3); }
    public boolean destroyBlock(BlockPos pos, boolean drop) { return true; }

    public void playSound(Player player, BlockPos pos, SoundEvent sound, SoundSource source, float volume, float pitch) {}
    public void sendBlockUpdated(BlockPos pos, BlockState oldState, BlockState newState, int flags) {}

    public boolean addFreshEntity(Entity entity) {
        if (entity != null) {
            entity.level = this;
            entities.add(entity);
            return true;
        }
        return false;
    }

    public List<Entity> getEntities() {
        return entities;
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> List<T> getEntitiesOfClass(Class<T> clazz, AABB aabb) {
        List<T> list = new ArrayList<>();
        for (Entity e : entities) {
            if (clazz.isInstance(e)) {
                list.add((T) e);
            }
        }
        return list;
    }
}
