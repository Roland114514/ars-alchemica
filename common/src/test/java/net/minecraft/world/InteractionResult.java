package net.minecraft.world;

public enum InteractionResult {
    SUCCESS, CONSUME, PASS, FAIL;

    public boolean consumesAction() {
        return this == SUCCESS || this == CONSUME;
    }

    public static InteractionResult sidedSuccess(boolean isClient) {
        return isClient ? SUCCESS : CONSUME;
    }
}
