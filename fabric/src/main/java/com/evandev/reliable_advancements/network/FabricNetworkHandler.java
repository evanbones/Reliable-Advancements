package com.evandev.reliable_advancements.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class FabricNetworkHandler {
    public static void registerPayloads() {
        ServerPlayNetworking.registerGlobalReceiver(EditAdvancementPayload.ID, (server, player, handler, buf, responseSender) -> {
            EditAdvancementPayload payload = new EditAdvancementPayload(buf);
            server.execute(() -> ServerAdvancementEditor.saveAdvancementEdit(server, player, payload));
        });

        ServerPlayNetworking.registerGlobalReceiver(RequestAdvancementJsonPayload.ID, (server, player, handler, buf, responseSender) -> {
            RequestAdvancementJsonPayload payload = new RequestAdvancementJsonPayload(buf);
            server.execute(() -> ServerAdvancementEditor.handleJsonRequest(server, player, payload));
        });

        ServerPlayNetworking.registerGlobalReceiver(LinkAdvancementPayload.ID, (server, player, handler, buf, responseSender) -> {
            LinkAdvancementPayload payload = new LinkAdvancementPayload(buf);
            server.execute(() -> ServerAdvancementEditor.handleLinkAdvancement(server, player, payload));
        });

        ServerPlayNetworking.registerGlobalReceiver(ClaimRewardPayload.ID, (server, player, handler, buf, responseSender) -> {
            ClaimRewardPayload payload = new ClaimRewardPayload(buf);
            server.execute(() -> ServerAdvancementEditor.handleRewardClaim(server, player, payload));
        });

        ServerPlayNetworking.registerGlobalReceiver(RequestFullTreePayload.ID, (server, player, handler, buf, responseSender) -> {
            new RequestFullTreePayload(buf);
            server.execute(() -> ServerAdvancementEditor.handleRequestFullTree(server, player));
        });

        ServerPlayNetworking.registerGlobalReceiver(ResetTabPayload.ID, (server, player, handler, buf, responseSender) -> {
            ResetTabPayload payload = new ResetTabPayload(buf);
            server.execute(() -> ServerAdvancementEditor.handleResetTab(server, player, payload));
        });
    }

    public static void registerClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(AdvancementJsonPayload.ID, (client, handler, buf, responseSender) -> {
            AdvancementJsonPayload payload = new AdvancementJsonPayload(buf);
            client.execute(() -> ClientNetworkHandler.handleAdvancementJson(payload));
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncClaimedRewardsPayload.ID, (client, handler, buf, responseSender) -> {
            SyncClaimedRewardsPayload payload = new SyncClaimedRewardsPayload(buf);
            client.execute(() -> ClientNetworkHandler.handleSyncClaimedRewards(payload));
        });
    }
}