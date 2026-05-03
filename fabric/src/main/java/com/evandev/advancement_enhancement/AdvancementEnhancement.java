package com.evandev.advancement_enhancement;

import com.evandev.advancement_enhancement.network.FabricNetworkHandler;
import com.evandev.advancement_enhancement.util.RewardTrackerData;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class AdvancementEnhancement implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricNetworkHandler.registerPayloads();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            RewardTrackerData.get(server).syncToPlayer(handler.player);
        });
    }
}