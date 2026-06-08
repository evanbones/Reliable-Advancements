package com.evandev.reliable_advancements.handler;

import com.evandev.reliable_advancements.config.InventoryButtonStyle;
import com.evandev.reliable_advancements.config.ModConfig;
import com.evandev.reliable_advancements.gui.button.AdvancementsScreenButton;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public class GuiOpenHandler implements ScreenEvents.AfterInit {
    public static final GuiOpenHandler instance = new GuiOpenHandler();

    private GuiOpenHandler() {
    }

    public void registerEventHandlers() {
        ScreenEvents.AFTER_INIT.register(this);
    }

    @Override
    public void afterInit(@NonNull Minecraft minecraft, @NonNull Screen screen, int scaledWidth, int scaledHeight) {
        if (screen instanceof InventoryScreen inventoryScreen) {
            if (ModConfig.get().addToInventory) {

                Screens.getWidgets(screen).add(new AdvancementsScreenButton(
                        () -> {
                            int currentX = inventoryScreen.leftPos;
                            return ModConfig.get().inventoryButtonStyle == InventoryButtonStyle.BUTTON
                                    ? currentX + 126
                                    : currentX + inventoryScreen.imageWidth;
                        },
                        () -> {
                            int currentY = inventoryScreen.topPos;
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
