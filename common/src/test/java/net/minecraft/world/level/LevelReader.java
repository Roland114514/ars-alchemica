package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

public interface LevelReader extends BlockGetter {
    Holder<Biome> getBiome(BlockPos pos);
    int getMaxLocalRawBrightness(BlockPos pos);
    int getBrightness(LightLayer layer, BlockPos pos);
    boolean canSeeSky(BlockPos pos);
    boolean hasChunkAt(BlockPos pos);
}
