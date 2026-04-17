package com.rjwebb134.aiterror;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void analyzeHighHealthAboveGroundReturnsWarning() {
        AIManager aiManager = new AIManager();
        aiManager.initialize();

        AIState safeState = new AIState(80.0, 18.0f, true);
        AIPlan plan = aiManager.analyze(safeState);

        assertEquals(AIPlan.SEND_WARNING, plan);
    }
}
