package com.rjwebb134.aiterror;

public final class AIState {
    private final double playerY;
    private final float playerHealth;
    private final boolean onGround;
    private final boolean sprinting;
    private final boolean moving;
    private final boolean jumping;
    private final boolean lookingAtAi;
    private final int consecutiveActionCount;

    public AIState(double playerY, float playerHealth, boolean onGround) {
        this(playerY, playerHealth, onGround, false, false, false, false, 0);
    }

    public AIState(double playerY, float playerHealth, boolean onGround, boolean sprinting, boolean moving,
                   boolean jumping, boolean lookingAtAi, int consecutiveActionCount) {
        this.playerY = playerY;
        this.playerHealth = playerHealth;
        this.onGround = onGround;
        this.sprinting = sprinting;
        this.moving = moving;
        this.jumping = jumping;
        this.lookingAtAi = lookingAtAi;
        this.consecutiveActionCount = consecutiveActionCount;
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

    public boolean isSprinting() {
        return sprinting;
    }

    public boolean isMoving() {
        return moving;
    }

    public boolean isJumping() {
        return jumping;
    }

    public boolean isLookingAtAi() {
        return lookingAtAi;
    }

    public int getConsecutiveActionCount() {
        return consecutiveActionCount;
    }
}
