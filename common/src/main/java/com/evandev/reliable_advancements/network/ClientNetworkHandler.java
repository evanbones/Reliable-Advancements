package com.evandev.reliable_advancements.network;

import com.evandev.reliable_advancements.client.ClientRewardTracker;
import com.evandev.reliable_advancements.gui.EnhancedAdvancementTab;
import com.evandev.reliable_advancements.gui.EnhancedAdvancementWidget;
import com.evandev.reliable_advancements.gui.screens.AdvancementEditorScreen;
import com.evandev.reliable_advancements.gui.screens.EnhancedAdvancementsScreen;
import com.evandev.reliable_advancements.gui.screens.TabEditorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class ClientNetworkHandler {
    public static void handleAdvancementJson(AdvancementJsonPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof EnhancedAdvancementsScreen mainScreen) {
            if ("TabProperties".equals(payload.initialTab())) {
                if (mainScreen.selectedTab != null && mainScreen.selectedTab.getRootNode().holder().id().equals(payload.advancementId())) {
                    mc.setScreen(new TabEditorScreen(mainScreen, mainScreen.selectedTab, payload.jsonPayload()));
                }
                return;
            }
            if ("Copy".equals(payload.initialTab())) {
                EnhancedAdvancementsScreen.clipboardJson = payload.jsonPayload();
                EnhancedAdvancementsScreen.clipboardId = payload.advancementId();
                if (mc.player != null) {
                    mc.player.sendSystemMessage(Component.literal("Copied advancement: " + payload.advancementId()));
                }
                return;
            }
            EnhancedAdvancementWidget targetWidget = null;
            if (mainScreen.selectedTab != null) {
                for (EnhancedAdvancementWidget w : mainScreen.selectedTab.getWidgets().values()) {
                    if (w.getAdvancement().holder().id().equals(payload.advancementId())) {
                        targetWidget = w;
                        break;
                    }
                }
            }
            if (targetWidget == null) {
                for (EnhancedAdvancementTab tab : mainScreen.getTabs().values()) {
                    for (EnhancedAdvancementWidget w : tab.getWidgets().values()) {
                        if (w.getAdvancement().holder().id().equals(payload.advancementId())) {
                            targetWidget = w;
                            break;
                        }
                    }
                    if (targetWidget != null) break;
                }
            }
            int x = targetWidget != null ? targetWidget.getX() : 0;
            int y = targetWidget != null ? targetWidget.getY() : 0;
            mc.setScreen(new AdvancementEditorScreen(
                    mainScreen, payload.advancementId(), false, x, y, payload.initialTab(), payload.jsonPayload()
            ));
        }
    }

    public static void handleSyncClaimedRewards(SyncClaimedRewardsPayload payload) {
        ClientRewardTracker.CLAIMED.clear();
        ClientRewardTracker.CLAIMED.addAll(payload.claimedIds());
    }
}