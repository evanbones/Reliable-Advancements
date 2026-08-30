package com.evandev.reliable_advancements.platform;

import com.evandev.reliable_advancements.network.*;
import com.evandev.reliable_advancements.platform.services.IEventHelper;
import com.evandev.reliable_advancements.platform.services.IPlatformHelper;
import com.evandev.reliable_advancements.reference.Constants;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

public class FabricPlatformHelper implements IPlatformHelper {
    private final FabricEventHelper eventHelper = new FabricEventHelper();

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public boolean isPhysicalClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    @Override
    public IEventHelper getEventHelper() {
        return eventHelper;
    }

    @Override
    public boolean canSendAdvancementEdit() {
        return ClientPlayNetworking.canSend(EditAdvancementPayload.TYPE);
    }

    @Override
    public void sendAdvancementEdit(EditAdvancementPayload payload) {
        if (canSendAdvancementEdit()) {
            ClientPlayNetworking.send(payload);
        }
    }

    @Override
    public void sendAdvancementJsonRequest(RequestAdvancementJsonPayload payload) {
        if (ClientPlayNetworking.canSend(RequestAdvancementJsonPayload.TYPE)) {
            ClientPlayNetworking.send(payload);
        } else {
            Minecraft.getInstance().player.displayClientMessage(
                    Component.literal("§cThis server does not have " + Constants.MOD_NAME + " installed. Editing is disabled."), false
            );
        }
    }

    @Override
    public void sendLinkAdvancement(LinkAdvancementPayload payload) {
        if (ClientPlayNetworking.canSend(LinkAdvancementPayload.TYPE)) {
            ClientPlayNetworking.send(payload);
        }
    }

    @Override
    public void sendAdvancementJsonToClient(ServerPlayer player, AdvancementJsonPayload payload) {
        if (ServerPlayNetworking.canSend(player, AdvancementJsonPayload.TYPE)) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    @Override
    public void sendClaimReward(ClaimRewardPayload payload) {
        if (ClientPlayNetworking.canSend(ClaimRewardPayload.TYPE)) {
            ClientPlayNetworking.send(payload);
        }
    }

    @Override
    public void sendClaimedRewardsSync(ServerPlayer player, SyncClaimedRewardsPayload payload) {
        if (ServerPlayNetworking.canSend(player, SyncClaimedRewardsPayload.TYPE)) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    @Override
    public void sendRequestFullTree() {
        if (ClientPlayNetworking.canSend(RequestFullTreePayload.TYPE)) {
            ClientPlayNetworking.send(new RequestFullTreePayload());
        }
    }

    @Override
    public void sendTabAction(TabActionPayload payload) {
        if (ClientPlayNetworking.canSend(TabActionPayload.TYPE)) {
            ClientPlayNetworking.send(payload);
        }
    }

    @Override
    public void sendAdvancementBatch(AdvancementBatchPayload payload) {
        if (ClientPlayNetworking.canSend(AdvancementBatchPayload.TYPE)) {
            ClientPlayNetworking.send(payload);
        }
    }

    @Override
    public void sendTabsToClient(ServerPlayer player, SyncTabsPayload payload) {
        if (ServerPlayNetworking.canSend(player, SyncTabsPayload.TYPE)) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    @Override
    public boolean sendSyncRequest(RequestSyncPayload payload) {
        if (!ClientPlayNetworking.canSend(RequestSyncPayload.TYPE)) return false;
        ClientPlayNetworking.send(payload);
        return true;
    }

    @Override
    public void sendSyncComplete(ServerPlayer player, SyncCompletePayload payload) {
        if (ServerPlayNetworking.canSend(player, SyncCompletePayload.TYPE)) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}