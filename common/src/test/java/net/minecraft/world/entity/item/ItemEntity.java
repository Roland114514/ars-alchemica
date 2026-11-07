package net.minecraft.world.entity.item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public class ItemEntity extends Entity {
    private ItemStack item;
    private boolean alive = true;

    public ItemEntity(ItemStack item) {
        this.item = item != null ? item : ItemStack.EMPTY;
    }

    public ItemEntity(net.minecraft.world.level.Level level, double x, double y, double z, ItemStack item) {
        this(item);
        this.level = level;
    }

    public ItemStack getItem() {
        return item;
    }

    public void setItem(ItemStack item) {
        this.item = item != null ? item : ItemStack.EMPTY;
    }

    public boolean isAlive() {
        return alive && !item.isEmpty();
    }

    public void discard() {
        this.alive = false;
        this.item = ItemStack.EMPTY;
    }
}
