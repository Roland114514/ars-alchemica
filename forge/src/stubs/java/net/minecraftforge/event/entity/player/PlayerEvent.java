package net.minecraftforge.event.entity.player;

import net.minecraft.world.entity.player.Player;

public class PlayerEvent {
    public static class PlayerRespawnEvent {
        public final Player player;
        public PlayerRespawnEvent(Player player) {
            this.player = player;
        }
    }
}
