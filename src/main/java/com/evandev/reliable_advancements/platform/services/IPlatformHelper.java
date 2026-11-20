package com.evandev.reliable_advancements.platform.services;

import com.evandev.reliable_advancements.network.*;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

public interface IPlatformHelper {

    String getPlatformName();

    boolean isModLoaded(String modId);

    boolean isDevelopmentEnvironment();

    default String getEnvironmentName() {
        return isDevelopmentEnvironment() ? "development" : "production";
    }

    Path getConfigDirectory();

    boolean isPhysicalClient();

    IEventHelper getEventHelper();

    boolean canSendAdvancementEdit();

    void sendAdvancementEdit(EditAdvancementPayload payload);

    void sendAdvancementJsonRequest(RequestAdvancementJsonPayload payload);

    void sendLinkAdvancement(LinkAdvancementPayload payload);

    void sendAdvancementJsonToClient(ServerPlayer player, AdvancementJsonPayload payload);

    void sendClaimReward(ClaimRewardPayload payload);

    void sendClaimedRewardsSync(ServerPlayer player, SyncClaimedRewardsPayload payload);

    void sendRequestFullTree();

    void sendTabAction(TabActionPayload payload);

    void sendAdvancementBatch(AdvancementBatchPayload payload);

    void sendTabsToClient(ServerPlayer player, SyncTabsPayload payload);

    boolean sendSyncRequest(RequestSyncPayload payload);

    void sendSyncComplete(ServerPlayer player, SyncCompletePayload payload);
}