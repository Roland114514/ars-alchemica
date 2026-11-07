package net.minecraftforge.eventbus.api;

public interface IEventBus {
    void register(Object target);
    void addListener(Object consumer);
}
