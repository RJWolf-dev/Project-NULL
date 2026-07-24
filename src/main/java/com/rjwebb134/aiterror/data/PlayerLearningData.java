package com.rjwebb134.aiterror.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents AI learning data for a player
 * Stores behavior patterns, fear factors, and scare effectiveness metrics
 */
public class PlayerLearningData {
    private String playerId;
    private String playerName;
    private long lastUpdated;
    private Map<String, Object> behaviorPatterns;
    private Map<String, Float> fearFactors;
    private int scareAttempts;
    private float successRate;
    
    /**
     * Default constructor for Firestore deserialization
     */
    public PlayerLearningData() {
        this.behaviorPatterns = new HashMap<>();
        this.fearFactors = new HashMap<>();
        this.scareAttempts = 0;
        this.successRate = 0.0f;
    }
    
    /**
     * Constructor with player info
     */
    public PlayerLearningData(String playerId, String playerName) {
        this();
        this.playerId = playerId;
        this.playerName = playerName;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    // ==================== Getters & Setters ====================
    
    public String getPlayerId() {
        return playerId;
    }
    
    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    
    public long getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
    
    public Map<String, Object> getBehaviorPatterns() {
        return behaviorPatterns;
    }
    
    public void setBehaviorPatterns(Map<String, Object> patterns) {
        this.behaviorPatterns = patterns;
    }
    
    public Map<String, Float> getFearFactors() {
        return fearFactors;
    }
    
    public void setFearFactors(Map<String, Float> factors) {
        this.fearFactors = factors;
    }
    
    public int getScareAttempts() {
        return scareAttempts;
    }
    
    public void setScareAttempts(int attempts) {
        this.scareAttempts = attempts;
    }
    
    public float getSuccessRate() {
        return successRate;
    }
    
    public void setSuccessRate(float rate) {
        this.successRate = rate;
    }
    
    // ==================== Update Methods ====================
    
    /**
     * Record a scare attempt and update success rate
     */
    public void recordScareAttempt(boolean successful) {
        this.scareAttempts++;
        if (successful) {
            this.successRate = (this.successRate * (this.scareAttempts - 1) + 1) / this.scareAttempts;
        } else {
            this.successRate = (this.successRate * (this.scareAttempts - 1)) / this.scareAttempts;
        }
        this.lastUpdated = System.currentTimeMillis();
    }
    
    /**
     * Update a behavior pattern observed in the player
     */
    public void updateBehaviorPattern(String pattern, Object value) {
        this.behaviorPatterns.put(pattern, value);
        this.lastUpdated = System.currentTimeMillis();
    }
    
    /**
     * Update fear factor for a specific scare type (0.0 to 1.0)
     */
    public void updateFearFactor(String factor, float value) {
        // Clamp value between 0.0 and 1.0
        float clampedValue = Math.max(0.0f, Math.min(1.0f, value));
        this.fearFactors.put(factor, clampedValue);
        this.lastUpdated = System.currentTimeMillis();
    }
    
    /**
     * Get fear factor for specific scare type, default to 0.5 if not found
     */
    public float getFearFactor(String factor) {
        return this.fearFactors.getOrDefault(factor, 0.5f);
    }
    
    /**
     * Get behavior pattern, returns null if not found
     */
    public Object getBehaviorPattern(String pattern) {
        return this.behaviorPatterns.get(pattern);
    }
}
