package net.minecraft.world.phys;

import net.minecraft.core.BlockPos;

public class AABB {
    private final double minX, minY, minZ, maxX, maxY, maxZ;

    public AABB(BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
    }

    public AABB(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public AABB inflate(double radius) {
        return new AABB(minX - radius, minY - radius, minZ - radius, maxX + radius, maxY + radius, maxZ + radius);
    }
}
