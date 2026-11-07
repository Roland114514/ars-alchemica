package net.minecraft.world.level.block;

import net.minecraft.world.level.block.state.BlockState;

public class IceBlock extends Block {
    public IceBlock(Properties properties) {
        super(properties);
    }

    public static BlockState meltsInto() {
        return Blocks.WATER_BLOCK.defaultBlockState();
    }
}
