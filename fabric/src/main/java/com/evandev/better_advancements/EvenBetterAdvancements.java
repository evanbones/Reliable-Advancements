package com.evandev.better_advancements;

import com.evandev.better_advancements.config.ConfigFileHandler;
import com.evandev.better_advancements.handler.GuiOpenHandler;
import com.evandev.better_advancements.reference.Constants;
import net.fabricmc.api.ClientModInitializer;

public class EvenBetterAdvancements implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Constants.LOG.info("Loaded");
        ConfigFileHandler.readFromConfig();
        GuiOpenHandler.instance.registerEventHandlers();
    }
}