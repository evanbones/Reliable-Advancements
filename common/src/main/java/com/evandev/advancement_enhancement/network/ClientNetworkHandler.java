package com.evandev.advancement_enhancement.network;

import com.evandev.advancement_enhancement.gui.screens.AdvancementEditorScreen;
import com.evandev.advancement_enhancement.gui.screens.EnhancedAdvancementsScreen;
import com.evandev.advancement_enhancement.gui.screens.TabEditorScreen;
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
                    mc.player.displayClientMessage(Component.literal("Copied advancement: " + payload.advancementId()), false);
                }
                return;
            }
            if (mainScreen.selectedTab != null) {
                mainScreen.selectedTab.getWidgets().values().stream()
                        .filter(w -> w.getAdvancement().holder().id().equals(payload.advancementId()))
                        .findFirst().ifPresent(widget -> mc.setScreen(new AdvancementEditorScreen(
                                mainScreen, payload.advancementId(), false, widget.getX(), widget.getY(), payload.initialTab(), payload.jsonPayload()
                        )));
            }
        }
    }
}