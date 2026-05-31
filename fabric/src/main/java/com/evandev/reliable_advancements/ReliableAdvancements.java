package com.evandev.reliable_advancements;

import com.evandev.reliable_advancements.network.FabricNetworkHandler;
import com.evandev.reliable_advancements.util.RewardTrackerData;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class ReliableAdvancements implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricNetworkHandler.registerPayloads();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            RewardTrackerData.get(server).syncToPlayer(handler.player);
        });
    }
}