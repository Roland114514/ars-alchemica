package net.minecraft.world.item;

public interface TooltipFlag {
    TooltipFlag NORMAL = Default.NORMAL;
    TooltipFlag ADVANCED = Default.ADVANCED;

    enum Default implements TooltipFlag {
        NORMAL, ADVANCED;
    }
}
