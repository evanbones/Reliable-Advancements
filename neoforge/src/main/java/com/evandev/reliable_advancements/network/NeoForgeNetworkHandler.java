package com.evandev.reliable_advancements.network;

import com.evandev.reliable_advancements.reference.Constants;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NeoForgeNetworkHandler {
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Constants.MOD_ID).optional();

        registrar.playToServer(EditAdvancementPayload.TYPE, EditAdvancementPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> ServerAdvancementEditor.saveAdvancementEdit(context.player().getServer(), (ServerPlayer) context.player(), payload));
        });

        registrar.playToServer(RequestAdvancementJsonPayload.TYPE, RequestAdvancementJsonPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> ServerAdvancementEditor.handleJsonRequest(context.player().getServer(), (ServerPlayer) context.player(), payload));
        });

        registrar.playToServer(LinkAdvancementPayload.TYPE, LinkAdvancementPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> ServerAdvancementEditor.handleLinkAdvancement(context.player().getServer(), (ServerPlayer) context.player(), payload));
        });

        registrar.playToServer(ClaimRewardPayload.TYPE, ClaimRewardPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> ServerAdvancementEditor.handleRewardClaim(context.player().getServer(), (ServerPlayer) context.player(), payload));
        });

        registrar.playToServer(RequestFullTreePayload.TYPE, RequestFullTreePayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> ServerAdvancementEditor.handleRequestFullTree(context.player().getServer(), (ServerPlayer) context.player()));
        });

        registrar.playToServer(TabActionPayload.TYPE, TabActionPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> ServerAdvancementEditor.handleTabAction(context.player().getServer(), (ServerPlayer) context.player(), payload));
        });

        registrar.playToServer(AdvancementBatchPayload.TYPE, AdvancementBatchPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> ServerAdvancementEditor.handleAdvancementBatch(context.player().getServer(), (ServerPlayer) context.player(), payload));
        });

        registrar.playToServer(RequestSyncPayload.TYPE, RequestSyncPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> ServerAdvancementEditor.handleSyncRequest((ServerPlayer) context.player(), payload));
        });

        registrar.playToClient(AdvancementJsonPayload.TYPE, AdvancementJsonPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> ClientNetworkHandler.handleAdvancementJson(payload));
        });

        registrar.playToClient(SyncClaimedRewardsPayload.TYPE, SyncClaimedRewardsPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> ClientNetworkHandler.handleSyncClaimedRewards(payload));
        });

        registrar.playToClient(SyncTabsPayload.TYPE, SyncTabsPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> ClientNetworkHandler.handleSyncTabs(payload));
        });

        registrar.playToClient(SyncCompletePayload.TYPE, SyncCompletePayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> ClientNetworkHandler.handleSyncComplete(payload));
        });
    }
}