package com.evandev.advancement_enhancement.handler;

import com.evandev.advancement_enhancement.config.InventoryButtonStyle;
import com.evandev.advancement_enhancement.config.ModConfig;
import com.evandev.advancement_enhancement.gui.button.AdvancementsScreenButton;
import com.evandev.advancement_enhancement.gui.screens.EnhancedAdvancementsScreen;
import com.evandev.advancement_enhancement.util.AdvancementComparer;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GuiOpenHandler {
    public static final GuiOpenHandler instance = new GuiOpenHandler();

    private GuiOpenHandler() {

    }

    @SubscribeEvent
    public void onGuiOpen(ScreenEvent.Opening event) {
        if (event.getScreen() instanceof AdvancementsScreen) {
            event.setCanceled(true);
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(new EnhancedAdvancementsScreen(mc.player.connection.getAdvancements()));
        }
    }

    @SubscribeEvent
    public void onGuiOpened(final ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof InventoryScreen) {
            if (ModConfig.get().addToInventory) {
                InventoryScreen guiInventory = (InventoryScreen) event.getScreen();

                int x = guiInventory.getGuiLeft();
                int y = guiInventory.getGuiTop();

                if (ModConfig.get().inventoryButtonStyle == InventoryButtonStyle.BUTTON) {
                    x += 126;
                    y += 61;
                } else {
                    x += guiInventory.getXSize();
                }

                event.addListener(new AdvancementsScreenButton(x, y, Component.literal("BA")));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH) // put on HIGH to be before Triumph sorting, giving them priority
    public void onGuiAboutToOpen(final ScreenEvent.Init.Pre event) {
        if (event.getScreen() instanceof EnhancedAdvancementsScreen) {
            if (ModConfig.get().orderTabsAlphabetically) {
                Minecraft mc = Minecraft.getInstance();
                ClientAdvancements clientAdvancements = mc.player.connection.getAdvancements();
                AdvancementTree advancementTree = clientAdvancements.getTree();
                Set<AdvancementNode> roots = (Set<AdvancementNode>) advancementTree.roots();

                List<String> advancementLocations = roots.stream().sorted(AdvancementComparer.sortByTitle()).map(n -> n.holder().id().toString()).toList();

                List<AdvancementNode> advancements = new ArrayList<>(roots);
                roots.clear();

                for (String location : advancementLocations) {
                    for (AdvancementNode advancement : advancements) {
                        if (advancement.holder().id().toString().equals(location)) {
                            roots.add(advancement);
                        }
                    }
                }
            }
        }
    }
}
