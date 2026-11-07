package net.minecraftforge.registries;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ForgeRegistries {
    public static final IForgeRegistry<Block> BLOCKS = new IForgeRegistry<>() {};
    public static final IForgeRegistry<Item> ITEMS = new IForgeRegistry<>() {};
    public static final IForgeRegistry<BlockEntityType<?>> BLOCK_ENTITY_TYPES = new IForgeRegistry<>() {};
    public static final IForgeRegistry<MobEffect> MOB_EFFECTS = new IForgeRegistry<>() {};
}
