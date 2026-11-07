package net.minecraft.core;

public enum Direction {
    DOWN(0, -1, 0, Axis.Y),
    UP(0, 1, 0, Axis.Y),
    NORTH(0, 0, -1, Axis.Z),
    SOUTH(0, 0, 1, Axis.Z),
    WEST(-1, 0, 0, Axis.X),
    EAST(1, 0, 0, Axis.X);

    public enum Axis {
        X, Y, Z;
        public boolean isHorizontal() { return this == X || this == Z; }
    }

    private final int stepX, stepY, stepZ;
    private final Axis axis;

    Direction(int x, int y, int z, Axis axis) {
        this.stepX = x;
        this.stepY = y;
        this.stepZ = z;
        this.axis = axis;
    }

    public int getStepX() { return stepX; }
    public int getStepY() { return stepY; }
    public int getStepZ() { return stepZ; }
    public Axis getAxis() { return axis; }
    public Direction getOpposite() {
        return switch (this) {
            case DOWN -> UP;
            case UP -> DOWN;
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case WEST -> EAST;
            case EAST -> WEST;
        };
    }
}
