package net.minecraft.core.particles;

import net.minecraft.world.item.ItemStack;

public class ItemParticleOption implements ParticleOptions {
    private final ParticleType<ItemParticleOption> type;
    private final ItemStack itemStack;

    public ItemParticleOption(ParticleType<ItemParticleOption> type, ItemStack itemStack) {
        this.type = type;
        this.itemStack = itemStack;
    }
}
