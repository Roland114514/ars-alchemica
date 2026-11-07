package net.minecraft.world.phys.shapes;

public class Shapes {
    public static final VoxelShape EMPTY = new VoxelShape();
    public static final VoxelShape BLOCK = new VoxelShape();

    public static VoxelShape box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return new VoxelShape();
    }

    public static VoxelShape or(VoxelShape a, VoxelShape b) {
        return new VoxelShape();
    }

    public static VoxelShape or(VoxelShape first, VoxelShape... others) {
        return new VoxelShape();
    }
}
