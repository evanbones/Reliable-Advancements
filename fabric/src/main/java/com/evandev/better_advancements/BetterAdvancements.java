package com.evandev.better_advancements;

import com.evandev.better_advancements.network.FabricNetworkHandler;
import net.fabricmc.api.ModInitializer;

public class BetterAdvancements implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricNetworkHandler.registerPayloads();
    }
}