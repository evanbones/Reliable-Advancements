package com.evandev.advancement_enhancement.platform;

import com.evandev.advancement_enhancement.network.*;
import com.evandev.advancement_enhancement.platform.services.IAdvancementVisitor;
import com.evandev.advancement_enhancement.platform.services.IEventHelper;
import com.evandev.advancement_enhancement.platform.services.IPlatformHelper;
import com.evandev.advancement_enhancement.reference.Constants;
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
    private final FabricAdvancementVisitor advancementVisitor = new FabricAdvancementVisitor();

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
    public IAdvancementVisitor getAdvancementVisitor() {
        return advancementVisitor;
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
    public void sendResetTab(ResetTabPayload payload) {
        if (ClientPlayNetworking.canSend(ResetTabPayload.TYPE)) {
            ClientPlayNetworking.send(payload);
        }
    }
}