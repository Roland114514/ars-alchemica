package net.minecraft.world.level.block.state.properties;

public class IntegerProperty extends Property<Integer> {
    private final int min, max;

    protected IntegerProperty(String name, int min, int max) {
        super(name);
        this.min = min;
        this.max = max;
    }

    public static IntegerProperty create(String name, int min, int max) {
        return new IntegerProperty(name, min, max);
    }
}
