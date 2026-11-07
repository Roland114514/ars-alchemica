package net.minecraft.world.level.block.state;

import java.util.function.ToIntFunction;

public abstract class BlockBehaviour {

    public static class Properties {
        public static Properties of() { return new Properties(); }
        public Properties randomTicks() { return this; }
        public Properties noCollission() { return this; }
        public Properties lightLevel(ToIntFunction<BlockState> light) { return this; }
        public Properties instabreak() { return this; }
        public Properties sound(Object sound) { return this; }
        public Properties noOcclusion() { return this; }
    }
}
