package com.evandev.reliable_advancements.neoforge.platform;

//? if neoforge {
import com.evandev.reliable_advancements.network.*;
import com.evandev.reliable_advancements.platform.services.IEventHelper;
import com.evandev.reliable_advancements.platform.services.IPlatformHelper;
import com.evandev.reliable_advancements.reference.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
//? if >=26.1 {
/*import net.neoforged.neoforge.client.network.ClientPacketDistributor;
*///?}

import java.nio.file.Path;

public class NeoForgePlatformHelper implements IPlatformHelper {
    private final NeoForgeEventHelper eventHelper = new NeoForgeEventHelper();

    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        //? if <26.1 {
        return !FMLLoader.isProduction();
        //?} else {
        /*return !FMLLoader.getCurrent().isProduction();
        *///?}
    }

    @Override
    public Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public boolean isPhysicalClient() {
        //? if <26.1 {
        return FMLLoader.getDist() == Dist.CLIENT;
        //?} else {
        /*return FMLLoader.getCurrent().getDist() == Dist.CLIENT;
        *///?}
    }

    @Override
    public IEventHelper getEventHelper() {
        return eventHelper;
    }

    @Override
    public boolean canSendAdvancementEdit() {
        var connection = Minecraft.getInstance().getConnection();
        return connection != null && connection.hasChannel(EditAdvancementPayload.TYPE);
    }

    @Override
    public void sendAdvancementEdit(EditAdvancementPayload payload) {
        if (canSendAdvancementEdit()) {
            sendToServer(payload);
        }
    }

    @Override
    public void sendAdvancementJsonRequest(RequestAdvancementJsonPayload payload) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null && connection.hasChannel(RequestAdvancementJsonPayload.TYPE)) {
            sendToServer(payload);
        } else {
            ClientNetworkHandler.showMessage(Component.literal("§cThis server does not have " + Constants.MOD_NAME + " installed. Editing is disabled."));
        }
    }

    @Override
    public void sendLinkAdvancement(LinkAdvancementPayload payload) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null && connection.hasChannel(LinkAdvancementPayload.TYPE)) {
            sendToServer(payload);
        }
    }

    @Override
    public void sendAdvancementJsonToClient(ServerPlayer player, AdvancementJsonPayload payload) {
        if (player.connection.hasChannel(AdvancementJsonPayload.TYPE)) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    @Override
    public void sendClaimReward(ClaimRewardPayload payload) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null && connection.hasChannel(ClaimRewardPayload.TYPE)) {
            sendToServer(payload);
        }
    }

    @Override
    public void sendClaimedRewardsSync(ServerPlayer player, SyncClaimedRewardsPayload payload) {
        if (player.connection.hasChannel(SyncClaimedRewardsPayload.TYPE)) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    @Override
    public void sendRequestFullTree() {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null && connection.hasChannel(RequestFullTreePayload.TYPE)) {
            sendToServer(new RequestFullTreePayload());
        }
    }

    @Override
    public void sendTabAction(TabActionPayload payload) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null && connection.hasChannel(TabActionPayload.TYPE)) {
            sendToServer(payload);
        }
    }

    @Override
    public void sendAdvancementBatch(AdvancementBatchPayload payload) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null && connection.hasChannel(AdvancementBatchPayload.TYPE)) {
            sendToServer(payload);
        }
    }

    @Override
    public void sendTabsToClient(ServerPlayer player, SyncTabsPayload payload) {
        if (player.connection.hasChannel(SyncTabsPayload.TYPE)) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    @Override
    public boolean sendSyncRequest(RequestSyncPayload payload) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null || !connection.hasChannel(RequestSyncPayload.TYPE)) return false;
        sendToServer(payload);
        return true;
    }

    @Override
    public void sendSyncComplete(ServerPlayer player, SyncCompletePayload payload) {
        if (player.connection.hasChannel(SyncCompletePayload.TYPE)) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    private static void sendToServer(CustomPacketPayload payload) {
        //? if <26.1 {
        PacketDistributor.sendToServer(payload);
        //?} else {
        /*ClientPacketDistributor.sendToServer(payload);
        *///?}
    }
}
//?}
