package com.rjwebb134.aiterror;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainMod implements ModInitializer {
    public static final String MODID = "aiterror";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    private AIManager aiManager;

    @Override
    public void onInitialize() {
        LOGGER.info("AI Terror mod initializing");
        aiManager = new AIManager();
        aiManager.initialize();
    }
}
