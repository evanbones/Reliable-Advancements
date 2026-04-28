package com.evandev.advancement_enhancement;

import com.evandev.advancement_enhancement.network.FabricNetworkHandler;
import net.fabricmc.api.ModInitializer;

public class AdvancementEnhancement implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricNetworkHandler.registerPayloads();
    }
}