package com.evandev.reliable_advancements.platform;

import com.evandev.reliable_advancements.network.*;
import com.evandev.reliable_advancements.platform.services.IEventHelper;
import com.evandev.reliable_advancements.platform.services.IPlatformHelper;
import com.evandev.reliable_advancements.reference.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;

import java.nio.file.Path;

public class ForgePlatformHelper implements IPlatformHelper {
    private final ForgeEventHelper eventHelper = new ForgeEventHelper();

    @Override
    public String getPlatformName() {
        return "Forge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }

    @Override
    public Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public boolean isPhysicalClient() {
        return FMLLoader.getDist() == Dist.CLIENT;
    }

    @Override
    public IEventHelper getEventHelper() {
        return eventHelper;
    }

    @Override
    public boolean canSendAdvancementEdit() {
        return Minecraft.getInstance().getConnection() != null;
    }

    @Override
    public void sendAdvancementEdit(EditAdvancementPayload payload) {
        if (canSendAdvancementEdit()) {
            ForgeNetworkHandler.CHANNEL.sendToServer(payload);
        }
    }

    @Override
    public void sendAdvancementJsonRequest(RequestAdvancementJsonPayload payload) {
        if (Minecraft.getInstance().getConnection() != null) {
            ForgeNetworkHandler.CHANNEL.sendToServer(payload);
        } else {
            Minecraft.getInstance().player.displayClientMessage(
                    Component.literal("§cThis server does not have " + Constants.MOD_NAME + " installed. Editing is disabled."), false
            );
        }
    }

    @Override
    public void sendLinkAdvancement(LinkAdvancementPayload payload) {
        if (Minecraft.getInstance().getConnection() != null) {
            ForgeNetworkHandler.CHANNEL.sendToServer(payload);
        }
    }

    @Override
    public void sendAdvancementJsonToClient(ServerPlayer player, AdvancementJsonPayload payload) {
        ForgeNetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }

    @Override
    public void sendClaimReward(ClaimRewardPayload payload) {
        if (Minecraft.getInstance().getConnection() != null) {
            ForgeNetworkHandler.CHANNEL.sendToServer(payload);
        }
    }

    @Override
    public void sendClaimedRewardsSync(ServerPlayer player, SyncClaimedRewardsPayload payload) {
        ForgeNetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }

    @Override
    public void sendRequestFullTree() {
        if (Minecraft.getInstance().getConnection() != null) {
            ForgeNetworkHandler.CHANNEL.sendToServer(new RequestFullTreePayload());
        }
    }

    @Override
    public void sendResetTab(ResetTabPayload payload) {
        if (Minecraft.getInstance().getConnection() != null) {
            ForgeNetworkHandler.CHANNEL.sendToServer(payload);
        }
    }
}