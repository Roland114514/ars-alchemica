package net.minecraft.world.level.block;

import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class CampfireBlock extends Block {
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public CampfireBlock(Properties properties) {
        super(properties);
    }
}
