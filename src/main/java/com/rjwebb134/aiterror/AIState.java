package com.rjwebb134.aiterror;

public final class AIState {
    private final double playerY;
    private final float playerHealth;
    private final boolean onGround;

    public AIState(double playerY, float playerHealth, boolean onGround) {
        this.playerY = playerY;
        this.playerHealth = playerHealth;
        this.onGround = onGround;
    }

    public double getPlayerY() {
        return playerY;
    }

    public float getPlayerHealth() {
        return playerHealth;
    }

    public boolean isOnGround() {
        return onGround;
    }
}
