package com.evandev.reliable_advancements.handler;

import com.evandev.reliable_advancements.config.InventoryButtonStyle;
import com.evandev.reliable_advancements.config.ModConfig;
import com.evandev.reliable_advancements.gui.button.AdvancementsScreenButton;
import com.evandev.reliable_advancements.mixin.AbstractContainerScreenAccessor;
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
        if (screen instanceof InventoryScreen inventoryScreen) {
            if (ModConfig.get().addToInventory) {

                AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) inventoryScreen;
                Screens.getButtons(screen).add(new AdvancementsScreenButton(
                        () -> {
                            int currentX = accessor.getLeftPos();
                            return ModConfig.get().inventoryButtonStyle == InventoryButtonStyle.BUTTON
                                    ? currentX + 126
                                    : currentX + accessor.getImageWidth();
                        },
                        () -> {
                            int currentY = accessor.getTopPos();
                            return ModConfig.get().inventoryButtonStyle == InventoryButtonStyle.BUTTON
                                    ? currentY + 61
                                    : currentY;
                        },
                        Component.literal("BA")
                ));
            }
        }
    }
}
