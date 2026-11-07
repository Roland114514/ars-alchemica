package net.minecraft.world.entity.player;

import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class Inventory {
    private final List<ItemStack> items = new ArrayList<>();

    public boolean add(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            items.add(stack.copy());
            return true;
        }
        return false;
    }

    public boolean contains(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return items.stream().anyMatch(i -> i.getItem() == stack.getItem());
    }

    public List<ItemStack> getItems() {
        return items;
    }
}
