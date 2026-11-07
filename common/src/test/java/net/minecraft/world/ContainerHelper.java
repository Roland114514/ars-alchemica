package net.minecraft.world;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class ContainerHelper {
    public static CompoundTag saveAllItems(CompoundTag tag, NonNullList<ItemStack> items) {
        return tag;
    }

    public static void loadAllItems(CompoundTag tag, NonNullList<ItemStack> items) {
    }
}
