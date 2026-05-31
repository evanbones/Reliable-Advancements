package com.evandev.reliable_advancements.client;

import com.evandev.reliable_advancements.config.ModConfig;
import com.evandev.reliable_advancements.handler.GuiOpenHandler;
import com.evandev.reliable_advancements.network.FabricNetworkHandler;
import net.fabricmc.api.ClientModInitializer;

public class ReliableAdvancementsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModConfig.load();
        GuiOpenHandler.instance.registerEventHandlers();
        FabricNetworkHandler.registerClientReceivers();
    }
}