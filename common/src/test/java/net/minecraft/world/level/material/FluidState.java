package net.minecraft.world.level.material;

public class FluidState {
    private final Fluid fluid;

    public FluidState(Fluid fluid) {
        this.fluid = fluid;
    }

    public Fluid getType() {
        return fluid != null ? fluid : Fluids.EMPTY;
    }
}
