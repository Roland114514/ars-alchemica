package net.minecraft.world.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.function.Consumer;

public class ItemStack {
    public static final ItemStack EMPTY = new ItemStack((Item) null, 0);

    private final Item item;
    private int count;
    private CompoundTag tag;

    public ItemStack(Item item) {
        this(item, 1);
    }

    public ItemStack(Item item, int count) {
        this.item = item;
        this.count = count;
    }

    public static ItemStack of(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) return EMPTY;
        String id = tag.getString("id");
        int count = tag.getInt("Count");
        ItemStack stack = new ItemStack(new Item(id), count > 0 ? count : 1);
        if (tag.contains("tag")) {
            stack.tag = tag.getCompound("tag");
        }
        return stack;
    }

    public CompoundTag save(CompoundTag target) {
        if (item != null) {
            target.putString("id", item.getDescriptionId());
            target.putInt("Count", count);
            if (tag != null) target.put("tag", tag);
        }
        return target;
    }

    public boolean isEmpty() {
        return this == EMPTY || count <= 0 || item == null;
    }

    public Item getItem() { return item; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    public CompoundTag getTag() { return tag; }
    public boolean hasTag() { return tag != null && !tag.isEmpty(); }

    public CompoundTag getOrCreateTag() {
        if (tag == null) tag = new CompoundTag();
        return tag;
    }

    public void shrink(int amount) {
        this.count = Math.max(0, this.count - amount);
    }

    public void grow(int amount) {
        this.count += amount;
    }

    public ItemStack copyWithCount(int count) {
        if (isEmpty()) return EMPTY;
        ItemStack copy = new ItemStack(this.item, count);
        if (this.tag != null) {
            copy.tag = this.tag.copy();
        }
        return copy;
    }

    public ItemStack split(int amount) {
        int take = Math.min(amount, this.count);
        this.count -= take;
        ItemStack res = copyWithCount(take);
        if (this.tag != null) res.tag = this.tag;
        return res;
    }

    public boolean is(Item other) {
        return this.item == other;
    }

    public CompoundTag getTagElement(String key) {
        if (tag == null) return null;
        return tag.contains(key) ? tag.getCompound(key) : null;
    }

    public CompoundTag getOrCreateTagElement(String key) {
        if (tag == null) tag = new CompoundTag();
        if (!tag.contains(key)) tag.put(key, new CompoundTag());
        return tag.getCompound(key);
    }

    public boolean isDamageableItem() {
        return false;
    }

    public void hurtAndBreak(int amount, Player player, Consumer<Player> onBreak) {
    }

    public Component getHoverName() {
        return item != null ? item.getName(this) : Component.literal("Air");
    }
}
