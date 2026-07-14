package com.rjwebb134.aiterror;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AIManagerTest {
    @Test
    void analyzeLowHealthReturnsWarning() {
        AIManager aiManager = new AIManager();
        aiManager.initialize();

        AIState lowHealthState = new AIState(70.0, 5.0f, true);
        AIPlan plan = aiManager.analyze(lowHealthState);

        assertEquals(AIPlan.SEND_WARNING, plan);
    }

    @Test
    void analyzeDeepCaveReturnsZombieSpawn() {
        AIManager aiManager = new AIManager();
        aiManager.initialize();

        AIState deepState = new AIState(20.0, 18.0f, false);
        AIPlan plan = aiManager.analyze(deepState);

        assertEquals(AIPlan.SPAWN_ZOMBIE, plan);
    }

    @Test
    void analyzeHighHealthOnGroundReturnsCobweb() {
        AIManager aiManager = new AIManager();
        aiManager.initialize();

        AIState safeState = new AIState(80.0, 18.0f, true);
        AIPlan plan = aiManager.analyze(safeState);

        assertEquals(AIPlan.PLACE_COBWEB, plan);
    }

    @Test
    void analyzeSprintingPlayerReturnsCobweb() {
        AIManager aiManager = new AIManager();
        aiManager.initialize();

        AIState sprintingState = new AIState(80.0, 18.0f, true, true, false, false, false, 2);
        AIPlan plan = aiManager.analyze(sprintingState);

        assertEquals(AIPlan.PLACE_COBWEB, plan);
    }

    @Test
    void analyzeNullStateReturnsNoAction() {
        AIManager aiManager = new AIManager();
        aiManager.initialize();

        AIPlan plan = aiManager.analyze((AIState) null);

        assertEquals(AIPlan.NO_ACTION, plan);
    }

    @Test
    void analyzeBoundaryConditionYEqualsThreshold() {
        AIManager aiManager = new AIManager();
        aiManager.initialize();

        // Test Y = 40 (boundary)
        AIState boundaryState = new AIState(40.0, 15.0f, false);
        AIPlan plan = aiManager.analyze(boundaryState);

        // Y < 40 triggers zombie spawn, so Y=40 should not trigger it
        assertEquals(AIPlan.SEND_WARNING, plan);
    }

    @Test
    void analyzeBoundaryConditionHealthEqualsThreshold() {
        AIManager aiManager = new AIManager();
        aiManager.initialize();

        // Test health = 10.0 (boundary)
        AIState boundaryState = new AIState(50.0, 10.0f, true);
        AIPlan plan = aiManager.analyze(boundaryState);

        // Health < 10 triggers warning, so health=10 should not trigger it
        assertEquals(AIPlan.PLACE_COBWEB, plan);
    }

    @Test
    void analyzeMultipleConditionsFollowsPriority() {
        AIManager aiManager = new AIManager();
        aiManager.initialize();

        // Deep AND low health - should prioritize zombie spawn (checked first)
        AIState criticalState = new AIState(20.0, 5.0f, true);
        AIPlan plan = aiManager.analyze(criticalState);

        assertEquals(AIPlan.SPAWN_ZOMBIE, plan);
    }

    @Test
    void initializeResetsState() {
        AIManager aiManager = new AIManager();
        aiManager.initialize();

        // Perform analysis
        AIState state = new AIState(50.0, 15.0f, true);
        aiManager.analyze(state);

        // Re-initialize
        aiManager.initialize();

        assertEquals(AIPlan.NO_ACTION, aiManager.analyze((AIState) null));
    }
}
