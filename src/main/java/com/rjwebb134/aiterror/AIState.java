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
    private final double audioScareScore;
    private final double speechInsightScore;
    private final String spokenObservation;

    public AIState(double playerY, float playerHealth, boolean onGround) {
        this(playerY, playerHealth, onGround, false, false, false, false, 0, 0.0D, 0.0D, "");
    }

    public AIState(double playerY, float playerHealth, boolean onGround, boolean sprinting, boolean moving,
                   boolean jumping, boolean lookingAtAi, int consecutiveActionCount) {
        this(playerY, playerHealth, onGround, sprinting, moving, jumping, lookingAtAi, consecutiveActionCount,
                0.0D, 0.0D, "");
    }

    public AIState(double playerY, float playerHealth, boolean onGround, boolean sprinting, boolean moving,
                   boolean jumping, boolean lookingAtAi, int consecutiveActionCount, double audioScareScore,
                   double speechInsightScore, String spokenObservation) {
        this.playerY = playerY;
        this.playerHealth = playerHealth;
        this.onGround = onGround;
        this.sprinting = sprinting;
        this.moving = moving;
        this.jumping = jumping;
        this.lookingAtAi = lookingAtAi;
        this.consecutiveActionCount = consecutiveActionCount;
        this.audioScareScore = audioScareScore;
        this.speechInsightScore = speechInsightScore;
        this.spokenObservation = spokenObservation == null ? "" : spokenObservation;
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

    public double getAudioScareScore() {
        return audioScareScore;
    }

    public double getSpeechInsightScore() {
        return speechInsightScore;
    }

    public String getSpokenObservation() {
        return spokenObservation;
    }
}
