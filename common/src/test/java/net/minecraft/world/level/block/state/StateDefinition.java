package net.minecraft.world.level.block.state;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.List;

public class StateDefinition<O, S extends StateHolder<O, S>> {
    private final S any;

    public StateDefinition(S any) {
        this.any = any;
    }

    public S any() {
        return any;
    }

    public static class Builder<O, S> {
        private final List<Property<?>> properties = new ArrayList<>();

        public Builder<O, S> add(Property<?>... props) {
            for (Property<?> p : props) properties.add(p);
            return this;
        }
    }
}
