package net.minecraft.world.level.material;

public class Fluid {
    private final FluidState defaultFluidState = new FluidState(this);

    public FluidState defaultFluidState() {
        return defaultFluidState;
    }
}
