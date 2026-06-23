package com.evandev.reliable_advancements.platform.services;

import com.evandev.reliable_advancements.network.*;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

public interface IPlatformHelper {

    /**
     * Gets the name of the current platform
     *
     * @return The name of the current platform.
     */
    String getPlatformName();

    /**
     * Checks if a mod with the given id is loaded.
     *
     * @param modId The mod to check if it is loaded.
     * @return True if the mod is loaded, false otherwise.
     */
    boolean isModLoaded(String modId);

    /**
     * Check if the game is currently in a development environment.
     *
     * @return True if in a development environment, false otherwise.
     */
    boolean isDevelopmentEnvironment();

    /**
     * Gets the name of the environment type as a string.
     *
     * @return The name of the environment type.
     */
    default String getEnvironmentName() {
        return isDevelopmentEnvironment() ? "development" : "production";
    }

    /**
     * Gets the configuration directory for the current platform.
     *
     * @return The path to the config directory.
     */
    Path getConfigDirectory();

    /**
     * Checks if the code is running on the physical client.
     *
     * @return True if on the client, false if on a dedicated server.
     */
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

    void sendResetTab(ResetTabPayload payload);
}