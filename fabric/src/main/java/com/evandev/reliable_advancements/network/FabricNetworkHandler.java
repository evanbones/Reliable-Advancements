package com.evandev.reliable_advancements.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class FabricNetworkHandler {
    public static void registerPayloads() {
        PayloadTypeRegistry.playC2S().register(EditAdvancementPayload.TYPE, EditAdvancementPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(RequestAdvancementJsonPayload.TYPE, RequestAdvancementJsonPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(LinkAdvancementPayload.TYPE, LinkAdvancementPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ClaimRewardPayload.TYPE, ClaimRewardPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(RequestFullTreePayload.TYPE, RequestFullTreePayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ResetTabPayload.TYPE, ResetTabPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(AdvancementJsonPayload.TYPE, AdvancementJsonPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncClaimedRewardsPayload.TYPE, SyncClaimedRewardsPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(EditAdvancementPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> ServerAdvancementEditor.saveAdvancementEdit(context.server(), context.player(), payload));
        });

        ServerPlayNetworking.registerGlobalReceiver(RequestAdvancementJsonPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> ServerAdvancementEditor.handleJsonRequest(context.server(), context.player(), payload));
        });

        ServerPlayNetworking.registerGlobalReceiver(LinkAdvancementPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> ServerAdvancementEditor.handleLinkAdvancement(context.server(), context.player(), payload));
        });

        ServerPlayNetworking.registerGlobalReceiver(ClaimRewardPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> ServerAdvancementEditor.handleRewardClaim(context.server(), context.player(), payload));
        });

        ServerPlayNetworking.registerGlobalReceiver(RequestFullTreePayload.TYPE, (payload, context) -> {
            context.server().execute(() -> ServerAdvancementEditor.handleRequestFullTree(context.server(), context.player()));
        });

        ServerPlayNetworking.registerGlobalReceiver(ResetTabPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> ServerAdvancementEditor.handleResetTab(context.server(), context.player(), payload));
        });
    }

    public static void registerClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(AdvancementJsonPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> ClientNetworkHandler.handleAdvancementJson(payload));
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncClaimedRewardsPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> ClientNetworkHandler.handleSyncClaimedRewards(payload));
        });
    }
}