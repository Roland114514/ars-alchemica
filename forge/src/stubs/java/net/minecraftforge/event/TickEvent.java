package net.minecraftforge.event;

import net.minecraft.world.entity.player.Player;

public class TickEvent {
    public enum Phase {
        START, END
    }

    public static class PlayerTickEvent {
        public final Player player;
        public final Phase phase;

        public PlayerTickEvent(Player player, Phase phase) {
            this.player = player;
            this.phase = phase;
        }
    }
}
