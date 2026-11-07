package net.minecraft.world;

public enum InteractionResult {
    SUCCESS, CONSUME, PASS, FAIL;

    public static InteractionResult sidedSuccess(boolean isClient) {
        return isClient ? SUCCESS : CONSUME;
    }
}
