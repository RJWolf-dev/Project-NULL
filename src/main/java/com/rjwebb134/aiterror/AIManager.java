package com.rjwebb134.aiterror;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AIManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AIManager.class);
    private final Map<UUID, AIPlan> playerPlans = new HashMap<>();

    public void initialize() {
        playerPlans.clear();
        LOGGER.info("AI manager initialized");
    }

    public AIPlan analyze(World world, PlayerEntity player) {
        if (world == null || player == null) {
            LOGGER.warn("Cannot analyze: world or player is null");
            currentPlan = AIPlan.NO_ACTION;
            return currentPlan;
        }

        AIState state = new AIState(player.getY(), player.getHealth(), player.isOnGround());
        // Phase 1 & 2: Analyze state and Formulate a plan
        AIPlan plan = analyze(state);
        playerPlans.put(player.getUuid(), plan);
        LOGGER.info("AI selected plan {} for player {} at y={} health={}", plan, player.getName().getString(), state.getPlayerY(), state.getPlayerHealth());
        return currentPlan;
    }

    // Decision logic to determine the best way to "terrorize" the player
    public AIPlan analyze(AIState state) {
        if (state == null) {
            return AIPlan.NO_ACTION;
        }

        if (state.getPlayerY() < 40) {
            return AIPlan.SPAWN_ZOMBIE;
        }

        if (state.getPlayerHealth() < 10.0f) {
            return AIPlan.SEND_WARNING;
        }

        if (state.isOnGround()) {
            return AIPlan.PLACE_COBWEB;
        }

        return AIPlan.SEND_WARNING;
    }

    public void interact(World world, PlayerEntity player) {
        if (world == null || player == null) {
            LOGGER.warn("Cannot execute interaction: world or player is null");
            return;
        }

        if (world.isClient) return;
        ServerWorld serverWorld = (ServerWorld) world;
        BlockPos pos = player.getBlockPos();
        AIPlan currentPlan = playerPlans.getOrDefault(player.getUuid(), AIPlan.NO_ACTION);

        // Phase 3: Execute the formulated plan
        switch (currentPlan) {
            case SPAWN_ZOMBIE -> {
                player.sendMessage(Text.literal("An AI terror lurks nearby..."), false);
                // Spawn a zombie near the player using the 1.20.1 compatible method
                EntityType.ZOMBIE.spawn(serverWorld, pos.add(3, 0, 3), SpawnReason.EVENT);
                LOGGER.info("Executed SPAWN_ZOMBIE interaction");
            }
            case SEND_WARNING -> {
                player.sendMessage(Text.literal("The AI has analyzed your world and is watching."), false);
                LOGGER.info("Executed SEND_WARNING interaction");
            }
            case PLACE_COBWEB -> {
                player.sendMessage(Text.literal("The world feels heavy..."), false);
                // Place cobwebs in a cross pattern around the player
                BlockPos[] offsets = {
                    pos.north(), pos.south(), pos.east(), pos.west()
                };
                for (BlockPos p : offsets) {
                    if (serverWorld.getBlockState(p).isAir()) {
                        serverWorld.setBlockState(p, Blocks.COBWEB.getDefaultState());
                    }
                }
                LOGGER.info("Executed PLACE_COBWEB interaction");
            }
            case NO_ACTION, UNSET -> {
                LOGGER.info("No AI action taken");
            }
        }

        // Reset the plan after execution to avoid unintended repetition
        playerPlans.remove(player.getUuid());
    }

    public AIPlan getCurrentPlan(PlayerEntity player) {
        return playerPlans.getOrDefault(player.getUuid(), AIPlan.NO_ACTION);
    }
}
