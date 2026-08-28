package com.evandev.reliable_advancements.handler;

import com.evandev.reliable_advancements.config.InventoryButtonStyle;
import com.evandev.reliable_advancements.config.ModConfig;
import com.evandev.reliable_advancements.gui.button.AdvancementsScreenButton;
import com.evandev.reliable_advancements.gui.screens.EnhancedAdvancementsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

public class GuiOpenHandler {
    public static final GuiOpenHandler instance = new GuiOpenHandler();

    private GuiOpenHandler() {
    }

    @SubscribeEvent
    public void onGuiOpen(ScreenEvent.Opening event) {
        if (event.getScreen() instanceof AdvancementsScreen) {
            event.setCanceled(true);
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(new EnhancedAdvancementsScreen(mc.player.connection.getAdvancements(), mc.screen));
        }
    }

    @SubscribeEvent
    public void onGuiOpened(final ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof InventoryScreen guiInventory) {
            if (ModConfig.get().addToInventory) {

                event.addListener(new AdvancementsScreenButton(
                        () -> {
                            int currentX = guiInventory.getGuiLeft();
                            return ModConfig.get().inventoryButtonStyle == InventoryButtonStyle.BUTTON
                                    ? currentX + 126
                                    : currentX + guiInventory.getXSize();
                        },
                        () -> {
                            int currentY = guiInventory.getGuiTop();
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
