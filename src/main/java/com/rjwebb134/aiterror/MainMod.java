package com.rjwebb134.aiterror;

import com.rjwebb134.aiterror.data.FirestoreManager;
import com.rjwebb134.aiterror.data.PlayerLearningData;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main mod entry point with integrated Firestore support
 * Handles mod initialization and server lifecycle events
 */
public class MainMod implements ModInitializer {
    public static final String MODID = "aiterror";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    private AIManager aiManager;

    @Override
    public void onInitialize() {
        LOGGER.info("AI Terror mod initializing");
        
        // Initialize Firestore first
        FirestoreManager.initialize();
        
        // Initialize AI Manager
        aiManager = new AIManager();
        aiManager.initialize();
        
        // Register server lifecycle events
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("[AI Terror Mod] Server started - Firestore ready");
        });
        
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("[AI Terror Mod] Server stopping - cleaning up Firestore");
            FirestoreManager.shutdown();
        });
    }
    
    public AIManager getAIManager() {
        return aiManager;
    }
}
