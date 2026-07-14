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
    private final Map<UUID, AIState> lastAnalyzedStates = new HashMap<>();
    private final Map<UUID, Integer> behaviorStreaks = new HashMap<>();

    public void initialize() {
        playerPlans.clear();
        lastAnalyzedStates.clear();
        behaviorStreaks.clear();
        LOGGER.info("AI manager initialized");
    }

    public AIPlan analyze(World world, PlayerEntity player) {
        if (world == null || player == null) {
            LOGGER.warn("Cannot analyze: world or player is null");
            return AIPlan.NO_ACTION;
        }

        AIState state = buildState(player);
        AIPlan plan = analyze(state);
        playerPlans.put(player.getUuid(), plan);
        lastAnalyzedStates.put(player.getUuid(), state);
        LOGGER.info("AI selected plan {} for player {} at y={} health={} moving={} sprinting={} streak={}",
                plan,
                player.getName().getString(),
                state.getPlayerY(),
                state.getPlayerHealth(),
                state.isMoving(),
                state.isSprinting(),
                state.getConsecutiveActionCount());
        return plan;
    }

    private AIState buildState(PlayerEntity player) {
        boolean moving = player.getVelocity().horizontalLengthSquared() > 0.01D;
        boolean sprinting = player.isSprinting() || (moving && player.forwardSpeed > 0.0F);
        boolean jumping = !player.isOnGround() && player.getVelocity().y > 0.0D;

        AIState previousState = lastAnalyzedStates.get(player.getUuid());
        int streak = 0;
        if (previousState != null && isSameBehavior(previousState, moving, sprinting, jumping)) {
            streak = behaviorStreaks.getOrDefault(player.getUuid(), 0) + 1;
        }
        behaviorStreaks.put(player.getUuid(), streak);

        return new AIState(player.getY(), player.getHealth(), player.isOnGround(), sprinting, moving, jumping, false, streak);
    }

    private boolean isSameBehavior(AIState previousState, boolean moving, boolean sprinting, boolean jumping) {
        return previousState.isMoving() == moving
                && previousState.isSprinting() == sprinting
                && previousState.isJumping() == jumping;
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

        if (state.isSprinting() || state.getConsecutiveActionCount() >= 2) {
            return AIPlan.PLACE_COBWEB;
        }

        if (state.isMoving() || state.isJumping()) {
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
        AIState state = lastAnalyzedStates.get(player.getUuid());

        // Phase 3: Execute the formulated plan
        switch (currentPlan) {
            case SPAWN_ZOMBIE -> {
                int spawnCount = state != null && state.isSprinting() ? 2 : 1;
                player.sendMessage(Text.literal("The AI has learned your panic and sends a stalker closer."), false);
                for (int i = 0; i < spawnCount; i++) {
                    EntityType.ZOMBIE.spawn(serverWorld, pos.add(2 + i, 0, 2 + i), SpawnReason.EVENT);
                }
                LOGGER.info("Executed SPAWN_ZOMBIE interaction with {} zombies", spawnCount);
            }
            case SEND_WARNING -> {
                String message = state != null && state.isSprinting()
                        ? "The AI has analyzed your frantic movements and is watching."
                        : "The AI has analyzed your world and is watching.";
                player.sendMessage(Text.literal(message), false);
                LOGGER.info("Executed SEND_WARNING interaction");
            }
            case PLACE_COBWEB -> {
                int spread = state != null && state.isSprinting() ? 2 : 1;
                player.sendMessage(Text.literal("The world feels heavy..."), false);
                BlockPos[] offsets = {
                        pos.north(spread), pos.south(spread), pos.east(spread), pos.west(spread)
                };
                for (BlockPos p : offsets) {
                    if (serverWorld.getBlockState(p).isAir()) {
                        serverWorld.setBlockState(p, Blocks.COBWEB.getDefaultState());
                    }
                }
                LOGGER.info("Executed PLACE_COBWEB interaction with spread {}", spread);
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
