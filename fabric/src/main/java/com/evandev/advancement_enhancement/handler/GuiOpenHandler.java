package com.evandev.advancement_enhancement.handler;

import com.evandev.advancement_enhancement.gui.AdvancementsScreenButton;
import com.evandev.advancement_enhancement.gui.InventoryButtonStyle;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

public class GuiOpenHandler implements ScreenEvents.AfterInit {
    public static final GuiOpenHandler instance = new GuiOpenHandler();

    private GuiOpenHandler() {

    }

    public void registerEventHandlers() {
        ScreenEvents.AFTER_INIT.register(this);
    }

    @Override
    public void afterInit(Minecraft minecraft, Screen screen, int scaledWidth, int scaledHeight) {
        if (screen instanceof InventoryScreen) {
            if (AdvancementsScreenButton.addToInventory) {
                InventoryScreen inventoryScreen = (InventoryScreen) screen;

                int x = inventoryScreen.leftPos;
                int y = inventoryScreen.topPos;

                if (AdvancementsScreenButton.style == InventoryButtonStyle.BUTTON) {
                    x += 126;
                    y += 61;
                } else {
                    x += inventoryScreen.imageWidth;
                }

                Screens.getButtons(screen).add(new AdvancementsScreenButton(x, y, Component.literal("BA")));
            }
        }
    }
}
