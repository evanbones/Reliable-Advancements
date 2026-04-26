package com.evandev.better_advancements.network;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class FabricNetworkHandler implements ModInitializer {
    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(EditAdvancementPayload.TYPE, EditAdvancementPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(EditAdvancementPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                if (context.server().getPlayerList().isOp(context.player().getGameProfile())) {
                    ServerAdvancementEditor.saveAdvancementEdit(context.server(), payload);
                }
            });
        });
    }
}