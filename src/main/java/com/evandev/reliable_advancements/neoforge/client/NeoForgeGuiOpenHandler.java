package com.evandev.reliable_advancements.neoforge.client;

//? if neoforge {
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

public class NeoForgeGuiOpenHandler {
    public static final NeoForgeGuiOpenHandler instance = new NeoForgeGuiOpenHandler();

    private NeoForgeGuiOpenHandler() {
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
                            int currentX = guiInventory.leftPos;
                            return ModConfig.get().inventoryButtonStyle == InventoryButtonStyle.BUTTON
                                    ? currentX + 126
                                    : currentX + guiInventory.imageWidth;
                        },
                        () -> {
                            int currentY = guiInventory.topPos;
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
//?}
