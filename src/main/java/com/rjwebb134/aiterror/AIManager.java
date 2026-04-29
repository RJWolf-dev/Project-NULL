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

        switch (currentPlan) {
            case SPAWN_ZOMBIE -> {
                player.sendMessage(Text.literal("An AI terror lurks nearby..."), false);
                // Spawn a zombie near the player
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
    }

    public AIPlan getCurrentPlan() {
        return currentPlan;
    }
}
