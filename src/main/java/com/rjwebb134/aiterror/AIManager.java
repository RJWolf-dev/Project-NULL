package com.rjwebb134.aiterror;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AIManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AIManager.class);
    private AIPlan currentPlan = AIPlan.NO_ACTION;

    public void initialize() {
        currentPlan = AIPlan.NO_ACTION;
        LOGGER.info("AI manager initialized");
    }

    public AIPlan analyze(World world, PlayerEntity player) {
        if (world == null || player == null) {
            LOGGER.warn("Cannot analyze: world or player is null");
            currentPlan = AIPlan.NO_ACTION;
            return currentPlan;
        }

        AIState state = new AIState(player.getY(), player.getHealth(), player.isOnGround());
        currentPlan = analyze(state);
        LOGGER.info("AI selected plan {} for player at y={} health={}", currentPlan, state.getPlayerY(), state.getPlayerHealth());
        return currentPlan;
    }

    public AIPlan analyze(AIState state) {
        if (state == null) {
            return AIPlan.NO_ACTION;
        }

        if (state.getPlayerHealth() < 10.0f) {
            return AIPlan.SEND_WARNING;
        }

        if (state.getPlayerY() < 40) {
            return AIPlan.SPAWN_ZOMBIE;
        }

        return AIPlan.SEND_WARNING;
    }

    public void interact(World world, PlayerEntity player) {
        if (world == null || player == null) {
            LOGGER.warn("Cannot execute interaction: world or player is null");
            return;
        }

        switch (currentPlan) {
            case SPAWN_ZOMBIE -> {
                player.sendMessage(Text.literal("An AI terror lurks nearby..."), false);
                LOGGER.info("Executed SPAWN_ZOMBIE interaction");
            }
            case SEND_WARNING -> {
                player.sendMessage(Text.literal("The AI has analyzed your world and is watching."), false);
                LOGGER.info("Executed SEND_WARNING interaction");
            }
            case NO_ACTION, UNSET -> {
                LOGGER.info("No AI action taken");
            }
        }
    }

    public AIPlan getCurrentPlan() {
        return currentPlan;
    }
}
