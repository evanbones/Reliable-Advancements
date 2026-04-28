package com.evandev.advancement_enhancement.client;

import com.evandev.advancement_enhancement.config.ModConfig;
import com.evandev.advancement_enhancement.handler.GuiOpenHandler;
import com.evandev.advancement_enhancement.network.FabricNetworkHandler;
import net.fabricmc.api.ClientModInitializer;

public class AdvancementEnhancementClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModConfig.load();
        GuiOpenHandler.instance.registerEventHandlers();
        FabricNetworkHandler.registerClientReceivers();
    }
}