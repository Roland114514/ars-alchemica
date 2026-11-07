package net.minecraft.world.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Item {
    private final String descriptionId;

    public Item(Properties properties) {
        this.descriptionId = "item.ars_alchemica.default";
    }

    public Item(String id) {
        this.descriptionId = id;
    }

    public String getDescriptionId() {
        return descriptionId;
    }

    public Component getName(ItemStack stack) {
        return Component.literal(descriptionId);
    }

    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
    }

    public static class Properties {
    }
}
