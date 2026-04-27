package com.evandev.better_advancements;

import com.evandev.better_advancements.config.ModConfig;
import com.evandev.better_advancements.handler.GuiOpenHandler;
import com.evandev.better_advancements.reference.Constants;
import net.fabricmc.api.ClientModInitializer;

public class EvenBetterAdvancements implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Constants.LOG.info("Loaded");
        ModConfig.load();
        GuiOpenHandler.instance.registerEventHandlers();
    }
}