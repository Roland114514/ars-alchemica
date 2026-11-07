package net.minecraftforge.registries;

import java.util.function.Supplier;

public class RegistryObject<T> implements Supplier<T> {
    private final Supplier<? extends T> supplier;
    private T value;

    public RegistryObject(Supplier<? extends T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public T get() {
        if (value == null && supplier != null) {
            value = supplier.get();
        }
        return value;
    }
}
