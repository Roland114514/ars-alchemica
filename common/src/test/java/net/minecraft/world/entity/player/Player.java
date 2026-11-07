package net.minecraft.world.entity.player;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class Player extends LivingEntity {
    private final Inventory inventory = new Inventory();
    private final Abilities abilities = new Abilities();
    private ItemStack mainHandItem = ItemStack.EMPTY;
    private ItemStack offHandItem = ItemStack.EMPTY;
    private Component lastClientMessage = null;
    private boolean spectator = false;

    public Inventory getInventory() { return inventory; }
    public Abilities getAbilities() { return abilities; }
    public boolean isCreative() { return abilities.instabuild; }
    public boolean isSpectator() { return spectator; }
    public void setSpectator(boolean spectator) { this.spectator = spectator; }

    public ItemStack getItemInHand(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? mainHandItem : offHandItem;
    }

    public void setItemInHand(InteractionHand hand, ItemStack stack) {
        if (hand == InteractionHand.MAIN_HAND) {
            this.mainHandItem = stack != null ? stack : ItemStack.EMPTY;
        } else {
            this.offHandItem = stack != null ? stack : ItemStack.EMPTY;
        }
    }

    public InteractionHand getUsedItemHand() { return InteractionHand.MAIN_HAND; }
    public boolean isShiftKeyDown() { return false; }
    public void drop(ItemStack stack, boolean includeName) {}
    public void broadcastBreakEvent(InteractionHand hand) {}

    public void startUsingItem(InteractionHand hand) {}
    public void awardStat(Object stat) {}

    public void displayClientMessage(Component message, boolean overlay) {
        this.lastClientMessage = message;
    }

    public Component getLastClientMessage() {
        return lastClientMessage;
    }
}
