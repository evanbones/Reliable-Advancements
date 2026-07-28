package com.evandev.reliable_advancements.network;

import com.evandev.reliable_advancements.client.ClientRewardTracker;
import com.evandev.reliable_advancements.gui.screens.AdvancementEditorScreen;
import com.evandev.reliable_advancements.gui.screens.EnhancedAdvancementsScreen;
import com.evandev.reliable_advancements.gui.screens.TabEditorScreen;
import net.minecraft.client.Minecraft;

public class ClientNetworkHandler {
    public static void handleAdvancementJson(AdvancementJsonPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() instanceof EnhancedAdvancementsScreen mainScreen) {
            if ("TabProperties".equals(payload.initialTab())) {
                if (mainScreen.selectedTab != null && mainScreen.selectedTab.getRootNode().holder().id().equals(payload.advancementId())) {
                    mc.setScreenAndShow(new TabEditorScreen(mainScreen, mainScreen.selectedTab, payload.jsonPayload()));
                }
                return;
            }
            if ("Copy".equals(payload.initialTab())) {
                EnhancedAdvancementsScreen.clipboardJson = payload.jsonPayload();
                EnhancedAdvancementsScreen.clipboardId = payload.advancementId();
                return;
            }
            if (mainScreen.selectedTab != null) {
                mainScreen.selectedTab.getWidgets().values().stream()
                        .filter(w -> w.getAdvancement().holder().id().equals(payload.advancementId()))
                        .findFirst().ifPresent(widget -> mc.setScreenAndShow(new AdvancementEditorScreen(
                                mainScreen, payload.advancementId(), false, widget.getX(), widget.getY(), payload.initialTab(), payload.jsonPayload()
                        )));
            }
        }
    }

    public static void handleSyncClaimedRewards(SyncClaimedRewardsPayload payload) {
        ClientRewardTracker.CLAIMED.clear();
        ClientRewardTracker.CLAIMED.addAll(payload.claimedIds());
    }
}