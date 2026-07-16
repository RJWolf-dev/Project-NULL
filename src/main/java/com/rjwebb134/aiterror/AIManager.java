package com.rjwebb134.aiterror;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class AIManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AIManager.class);
    private final Map<UUID, AIPlan> playerPlans = new HashMap<>();
    private final Map<UUID, AIState> lastAnalyzedStates = new HashMap<>();
    private final Map<UUID, Integer> behaviorStreaks = new HashMap<>();
    private final Map<UUID, AudioMemory> audioMemories = new HashMap<>();

    public void initialize() {
        playerPlans.clear();
        lastAnalyzedStates.clear();
        behaviorStreaks.clear();
        audioMemories.clear();
        LOGGER.info("AI manager initialized");
    }

    public AIPlan analyze(World world, PlayerEntity player) {
        if (world == null || player == null) {
            LOGGER.warn("Cannot analyze: world or player is null");
            return AIPlan.NO_ACTION;
        }

        AIState state = buildState(player);
        AIPlan plan = analyze(state, player.getUuid());
        playerPlans.put(player.getUuid(), plan);
        lastAnalyzedStates.put(player.getUuid(), state);
        LOGGER.info("AI selected plan {} for player {} at y={} health={} moving={} sprinting={} streak={} audioScore={} speechScore={}",
                plan,
                player.getName().getString(),
                state.getPlayerY(),
                state.getPlayerHealth(),
                state.isMoving(),
                state.isSprinting(),
                state.getConsecutiveActionCount(),
                state.getAudioScareScore(),
                state.getSpeechInsightScore());
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

        AudioMemory memory = audioMemories.get(player.getUuid());
        return new AIState(player.getY(), player.getHealth(), player.isOnGround(), sprinting, moving, jumping,
                false, streak, memory != null ? memory.getLastScareScore() : 0.0D,
                memory != null ? memory.getInsightScore() : 0.0D,
                memory != null ? memory.getLastTranscript() : "");
    }

    private boolean isSameBehavior(AIState previousState, boolean moving, boolean sprinting, boolean jumping) {
        return previousState.isMoving() == moving
                && previousState.isSprinting() == sprinting
                && previousState.isJumping() == jumping;
    }

    // Decision logic to determine the best way to "terrorize" the player
    public AIPlan analyze(AIState state) {
        return analyze(state, null);
    }

    public AIPlan analyze(AIState state, UUID playerId) {
        if (state == null) {
            return AIPlan.NO_ACTION;
        }

        AudioMemory memory = playerId != null ? audioMemories.get(playerId) : null;
        double scareScore = memory != null ? memory.getLastScareScore() : state.getAudioScareScore();
        double speechInsight = memory != null ? memory.getInsightScore() : state.getSpeechInsightScore();

        if (state.getPlayerY() < 40) {
            return AIPlan.SPAWN_ZOMBIE;
        }

        if (state.getPlayerHealth() < 10.0f) {
            return AIPlan.SEND_WARNING;
        }

        if (scareScore >= 0.75D) {
            return AIPlan.SPAWN_ZOMBIE;
        }

        if (speechInsight >= 0.65D) {
            AIPlan speechBasedPlan = inferPlanFromSpeech(memory);
            if (speechBasedPlan != null) {
                return speechBasedPlan;
            }
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

    public void processMicrophoneFeedback(PlayerEntity player, double scareScore, String transcript) {
        if (player == null) {
            return;
        }
        rememberAudioObservation(player.getUuid(), scareScore, transcript);
    }

    public void rememberAudioObservation(UUID playerId, double scareScore, String transcript) {
        if (playerId == null) {
            return;
        }

        AudioMemory memory = audioMemories.computeIfAbsent(playerId, ignored -> new AudioMemory());
        memory.update(scareScore, transcript);
        LOGGER.info("Recorded audio feedback for player {} with scareScore={} transcript='{}'",
                playerId,
                memory.getLastScareScore(),
                memory.getLastTranscript());
    }

    public void rememberSpeech(PlayerEntity player, String transcript) {
        if (player == null) {
            return;
        }
        rememberSpeech(player.getUuid(), transcript);
    }

    public void rememberSpeech(UUID playerId, String transcript) {
        if (playerId == null) {
            return;
        }

        AudioMemory memory = audioMemories.computeIfAbsent(playerId, ignored -> new AudioMemory());
        memory.update(0.0D, transcript);
    }

    private AIPlan inferPlanFromSpeech(AudioMemory memory) {
        if (memory == null) {
            return null;
        }

        if (memory.containsKeyword("zombie") || memory.containsKeyword("monster") || memory.containsKeyword("ghost")) {
            return AIPlan.SPAWN_ZOMBIE;
        }

        if (memory.containsKeyword("dark") || memory.containsKeyword("cave") || memory.containsKeyword("spider")) {
            return AIPlan.PLACE_COBWEB;
        }

        if (memory.containsKeyword("help") || memory.containsKeyword("please") || memory.containsKeyword("scared")) {
            return AIPlan.SEND_WARNING;
        }

        return null;
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

    private static final class AudioMemory {
        private double lastScareScore = 0.0D;
        private String lastTranscript = "";
        private final Map<String, Integer> keywordScores = new HashMap<>();

        void update(double scareScore, String transcript) {
            double clampedScareScore = Math.max(0.0D, Math.min(1.0D, scareScore));
            if (clampedScareScore > this.lastScareScore) {
                this.lastScareScore = clampedScareScore;
            }

            if (transcript != null && !transcript.isBlank()) {
                this.lastTranscript = transcript;
                String normalized = transcript.toLowerCase(Locale.ROOT);
                for (String token : normalized.split("[^a-z0-9]+")) {
                    if (token.isBlank()) {
                        continue;
                    }
                    keywordScores.merge(token, 1, Integer::sum);
                }
            }
        }

        double getLastScareScore() {
            return lastScareScore;
        }

        String getLastTranscript() {
            return lastTranscript;
        }

        double getInsightScore() {
            if (keywordScores.isEmpty()) {
                return 0.0D;
            }

            double score = 0.0D;
            for (Map.Entry<String, Integer> entry : keywordScores.entrySet()) {
                double weight = switch (entry.getKey()) {
                    case "zombie", "monster", "ghost" -> 0.45D;
                    case "dark", "cave", "spider" -> 0.35D;
                    case "scared", "help", "please", "run" -> 0.25D;
                    default -> 0.1D;
                };
                score += weight * Math.min(3, entry.getValue());
            }
            return Math.min(1.0D, score / 2.0D);
        }

        boolean containsKeyword(String keyword) {
            return keyword != null && keywordScores.containsKey(keyword.toLowerCase(Locale.ROOT));
        }
    }
}
