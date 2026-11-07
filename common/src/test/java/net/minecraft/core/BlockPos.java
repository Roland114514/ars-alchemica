package net.minecraft.core;

public class BlockPos {
    public static final BlockPos ZERO = new BlockPos(0, 0, 0);

    private final int x, y, z;

    public BlockPos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }

    public BlockPos above() { return new BlockPos(x, y + 1, z); }
    public BlockPos above(int n) { return new BlockPos(x, y + n, z); }
    public BlockPos below() { return new BlockPos(x, y - 1, z); }
    public BlockPos below(int n) { return new BlockPos(x, y - n, z); }
    public BlockPos relative(Direction dir) { return new BlockPos(x + dir.getStepX(), y + dir.getStepY(), z + dir.getStepZ()); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockPos bp)) return false;
        return x == bp.x && y == bp.y && z == bp.z;
    }

    @Override
    public int hashCode() {
        return (y + z * 31) * 31 + x;
    }
}
