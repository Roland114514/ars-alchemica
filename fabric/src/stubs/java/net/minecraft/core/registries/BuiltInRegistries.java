package net.minecraft.core.registries;

import net.minecraft.core.Registry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class BuiltInRegistries {
    public static final Registry<Block> BLOCK = new Registry<>() {};
    public static final Registry<Item> ITEM = new Registry<>() {};
    public static final Registry<BlockEntityType<?>> BLOCK_ENTITY_TYPE = new Registry<>() {};
    public static final Registry<MobEffect> MOB_EFFECT = new Registry<>() {};
}
