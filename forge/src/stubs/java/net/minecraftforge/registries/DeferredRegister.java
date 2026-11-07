package net.minecraftforge.registries;

import net.minecraftforge.eventbus.api.IEventBus;
import java.util.function.Supplier;

public class DeferredRegister<T> {
    private final IForgeRegistry<T> registry;
    private final String modId;

    private DeferredRegister(IForgeRegistry<T> registry, String modId) {
        this.registry = registry;
        this.modId = modId;
    }

    public static <B> DeferredRegister<B> create(IForgeRegistry<B> reg, String modId) {
        return new DeferredRegister<>(reg, modId);
    }

    public <I extends T> RegistryObject<I> register(String name, Supplier<? extends I> sup) {
        return new RegistryObject<>(sup);
    }

    public void register(IEventBus bus) {
    }
}
