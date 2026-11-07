package net.minecraft.world.item.context;

import net.minecraft.core.Direction;

public class BlockPlaceContext {
    private final Direction clickedFace;
    private final Direction horizontalDirection;

    public BlockPlaceContext(Direction clickedFace, Direction horizontalDirection) {
        this.clickedFace = clickedFace;
        this.horizontalDirection = horizontalDirection;
    }

    public Direction getClickedFace() {
        return clickedFace;
    }

    public Direction getHorizontalDirection() {
        return horizontalDirection;
    }
}
