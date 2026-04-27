package com.evandev.better_advancements.network;

import com.evandev.better_advancements.gui.BetterAdvancementWidget;
import com.evandev.better_advancements.gui.screens.AdvancementEditorScreen;
import com.evandev.better_advancements.gui.screens.BetterAdvancementsScreen;
import net.minecraft.client.Minecraft;

public class ClientNetworkHandler {
    public static void handleAdvancementJson(AdvancementJsonPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof BetterAdvancementsScreen mainScreen) {
            if (mainScreen.selectedTab != null) {
                mainScreen.selectedTab.getWidgets().values().stream()
                        .filter(w -> w.getAdvancement().holder().id().equals(payload.advancementId()))
                        .findFirst().ifPresent(widget -> mc.setScreen(new AdvancementEditorScreen(mainScreen, widget, payload.initialTab(), payload.jsonPayload())));

            }
        }
    }
}