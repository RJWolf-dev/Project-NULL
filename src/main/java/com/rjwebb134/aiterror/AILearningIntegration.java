package com.rjwebb134.aiterror;

import com.rjwebb134.aiterror.data.FirestoreManager;
import com.rjwebb134.aiterror.data.PlayerLearningData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Integration layer between AIManager and Firestore
 * Handles persistent storage of player learning data
 * 
 * This class enhances the AIManager by:
 * 1. Storing every decision and scare attempt
 * 2. Building long-term behavior profiles
 * 3. Improving AI effectiveness over time per player
 */
public class AILearningIntegration {
    private static final Logger LOGGER = LoggerFactory.getLogger(AILearningIntegration.class);
    private final AIManager aiManager;
    
    public AILearningIntegration(AIManager aiManager) {
        this.aiManager = aiManager;
    }
    
    /**
     * Called when analyzing a player to fetch and apply learned data
     */
    public AIPlan analyzeWithLearning(World world, PlayerEntity player) {
        if (world == null || player == null) {
            return AIPlan.NO_ACTION;
        }
        
        // Get the base AI analysis
        AIPlan basePlan = aiManager.analyze(world, player);
        
        // If Firestore is ready, enhance decision with learned data
        if (FirestoreManager.isInitialized()) {
            PlayerLearningData learningData = FirestoreManager.getOrCreatePlayerData(player);
            
            // Analyze the current state
            AIState currentState = buildStateFromPlayer(player);
            
            // Compare with historical patterns
            AIPlan enhancedPlan = enhancePlanWithLearning(basePlan, learningData, currentState);
            
            // Log the decision
            logDecision(player, basePlan, enhancedPlan, learningData);
            
            return enhancedPlan;
        }
        
        return basePlan;
    }
    
    /**
     * Record the result of a scare attempt in Firestore
     */
    public void recordScareOutcome(PlayerEntity player, AIPlan action, boolean successful) {
        if (!FirestoreManager.isInitialized()) {
            return;
        }
        
        try {
            PlayerLearningData data = FirestoreManager.getOrCreatePlayerData(player);
            
            // Record the scare attempt
            data.recordScareAttempt(successful);
            
            // Update fear factors based on action type
            updateFearFactors(data, action, successful);
            
            // Store behavior pattern
            String actionType = action.toString().toLowerCase();
            data.updateBehaviorPattern("lastAction", actionType);
            data.updateBehaviorPattern("lastActionSuccess", successful);
            data.updateBehaviorPattern("playerY", player.getY());
            data.updateBehaviorPattern("playerHealth", player.getHealth());
            
            // Save to Firestore
            FirestoreManager.savePlayerData(data);
            
            LOGGER.info("[AI Learning] {} - {} action was {}", 
                    player.getName().getString(),
                    actionType,
                    successful ? "SUCCESSFUL" : "FAILED");
            
        } catch (Exception e) {
            LOGGER.error("Error recording scare outcome: {}", e.getMessage());
        }
    }
    
    /**
     * Update fear factors based on scare type and outcome
     */
    private void updateFearFactors(PlayerLearningData data, AIPlan action, boolean successful) {
        float changeAmount = successful ? 0.15f : -0.10f;
        
        switch (action) {
            case SPAWN_ZOMBIE -> {
                float currentZombieFear = data.getFearFactor("zombie");
                data.updateFearFactor("zombie", currentZombieFear + changeAmount);
            }
            case PLACE_COBWEB -> {
                float currentCobwebFear = data.getFearFactor("environment");
                data.updateFearFactor("environment", currentCobwebFear + changeAmount);
            }
            case SEND_WARNING -> {
                float currentWarningFear = data.getFearFactor("warning");
                data.updateFearFactor("warning", currentWarningFear + changeAmount);
            }
            case NO_ACTION, UNSET -> {
                // No fear factor change
            }
        }
    }
    
    /**
     * Enhance the base AI plan with learned player patterns
     */
    private AIPlan enhancePlanWithLearning(AIPlan basePlan, PlayerLearningData learningData, AIState currentState) {
        if (learningData.getScareAttempts() < 3) {
            // Not enough data yet, use base plan
            return basePlan;
        }
        
        // If success rate is low, vary tactics
        if (learningData.getSuccessRate() < 0.40f) {
            return varyTactics(basePlan);
        }
        
        // If success rate is high, double down on working tactics
        if (learningData.getSuccessRate() > 0.70f) {
            return intensifyWorking(basePlan, learningData);
        }
        
        return basePlan;
    }
    
    /**
     * Vary tactics if current approach isn't working
     */
    private AIPlan varyTactics(AIPlan currentPlan) {
        return switch (currentPlan) {
            case SPAWN_ZOMBIE -> AIPlan.PLACE_COBWEB; // Try something different
            case PLACE_COBWEB -> AIPlan.SEND_WARNING;
            case SEND_WARNING -> AIPlan.SPAWN_ZOMBIE;
            default -> currentPlan;
        };
    }
    
    /**
     * Intensify tactics that are working
     */
    private AIPlan intensifyWorking(AIPlan currentPlan, PlayerLearningData learningData) {
        // Check which fear factor is highest
        float zombieFear = learningData.getFearFactor("zombie");
        float envFear = learningData.getFearFactor("environment");
        float warningFear = learningData.getFearFactor("warning");
        
        if (zombieFear > envFear && zombieFear > warningFear && zombieFear > 0.65f) {
            return AIPlan.SPAWN_ZOMBIE;
        } else if (envFear > zombieFear && envFear > warningFear && envFear > 0.65f) {
            return AIPlan.PLACE_COBWEB;
        } else if (warningFear > zombieFear && warningFear > envFear && warningFear > 0.65f) {
            return AIPlan.SEND_WARNING;
        }
        
        return currentPlan;
    }
    
    /**
     * Record audio/speech feedback to learning profile
     */
    public void recordAudioFeedback(PlayerEntity player, double scareScore, String transcript) {
        if (!FirestoreManager.isInitialized()) {
            return;
        }
        
        try {
            PlayerLearningData data = FirestoreManager.getOrCreatePlayerData(player);
            
            // Update fear patterns based on what the player said
            if (transcript != null && !transcript.isEmpty()) {
                analyzeTranscript(data, transcript);
            }
            
            // Update overall scare effectiveness
            if (scareScore > 0.7f) {
                data.recordScareAttempt(true);
            } else {
                data.recordScareAttempt(false);
            }
            
            FirestoreManager.savePlayerData(data);
            
        } catch (Exception e) {
            LOGGER.error("Error recording audio feedback: {}", e.getMessage());
        }
    }
    
    /**
     * Analyze player speech to identify fears
     */
    private void analyzeTranscript(PlayerLearningData data, String transcript) {
        String lower = transcript.toLowerCase();
        
        if (lower.contains("zombie") || lower.contains("monster")) {
            data.updateFearFactor("zombie", data.getFearFactor("zombie") + 0.1f);
        }
        if (lower.contains("dark") || lower.contains("cave") || lower.contains("scary")) {
            data.updateFearFactor("environment", data.getFearFactor("environment") + 0.1f);
        }
        if (lower.contains("help") || lower.contains("please") || lower.contains("scared")) {
            data.updateFearFactor("warning", data.getFearFactor("warning") + 0.1f);
        }
    }
    
    /**
     * Get player profile for debugging/analysis
     */
    public PlayerLearningData getPlayerProfile(PlayerEntity player) {
        if (!FirestoreManager.isInitialized()) {
            return null;
        }
        
        return FirestoreManager.getPlayerData(player.getUuidAsString());
    }
    
    /**
     * Build AI state from current player
     */
    private AIState buildStateFromPlayer(PlayerEntity player) {
        boolean moving = player.getVelocity().horizontalLengthSquared() > 0.01D;
        boolean sprinting = player.isSprinting() || (moving && player.forwardSpeed > 0.0F);
        boolean jumping = !player.isOnGround() && player.getVelocity().y > 0.0D;
        
        return new AIState(
                player.getY(),
                player.getHealth(),
                player.isOnGround(),
                sprinting,
                moving,
                jumping,
                false,
                0,
                0.0D,
                0.0D,
                ""
        );
    }
    
    /**
     * Log decision making for debugging
     */
    private void logDecision(PlayerEntity player, AIPlan basePlan, AIPlan enhancedPlan, PlayerLearningData learningData) {
        if (basePlan != enhancedPlan) {
            LOGGER.info("[AI Learning] {} - Enhanced decision: {} → {} (attempts: {}, success: {:.1f}%)",
                    player.getName().getString(),
                    basePlan,
                    enhancedPlan,
                    learningData.getScareAttempts(),
                    learningData.getSuccessRate() * 100);
        }
    }
}
