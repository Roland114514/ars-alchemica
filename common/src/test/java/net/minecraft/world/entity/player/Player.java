package net.minecraft.world.entity.player;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class Player extends LivingEntity {
    private final Inventory inventory = new Inventory();

    public Inventory getInventory() { return inventory; }
    public ItemStack getItemInHand(InteractionHand hand) { return ItemStack.EMPTY; }
    public InteractionHand getUsedItemHand() { return InteractionHand.MAIN_HAND; }
    public boolean isShiftKeyDown() { return false; }
    public void drop(ItemStack stack, boolean includeName) {}
    public void broadcastBreakEvent(InteractionHand hand) {}
}
