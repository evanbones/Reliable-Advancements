package com.evandev.better_advancements.client;

import com.evandev.better_advancements.config.ModConfig;
import com.evandev.better_advancements.handler.GuiOpenHandler;
import com.evandev.better_advancements.network.FabricNetworkHandler;
import net.fabricmc.api.ClientModInitializer;

public class BetterAdvancementsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModConfig.load();
        GuiOpenHandler.instance.registerEventHandlers();
        FabricNetworkHandler.registerClientReceivers();
    }
}