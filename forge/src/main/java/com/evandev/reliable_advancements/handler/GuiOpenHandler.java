package com.evandev.reliable_advancements.handler;

import com.evandev.reliable_advancements.config.InventoryButtonStyle;
import com.evandev.reliable_advancements.config.ModConfig;
import com.evandev.reliable_advancements.gui.button.AdvancementsScreenButton;
import com.evandev.reliable_advancements.gui.screens.EnhancedAdvancementsScreen;
import com.evandev.reliable_advancements.mixin.AbstractContainerScreenAccessor;
import com.evandev.reliable_advancements.mixin.AdvancementListAccessor;
import com.evandev.reliable_advancements.util.AdvancementComparer;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

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
        if (event.getScreen() instanceof InventoryScreen guiInventory) {
            if (ModConfig.get().addToInventory) {
                AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) guiInventory;
                event.addListener(new AdvancementsScreenButton(
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

    @SubscribeEvent(priority = EventPriority.HIGH) // put on HIGH to be before Triumph sorting, giving them priority
    public void onGuiAboutToOpen(final ScreenEvent.Init.Pre event) {
        if (event.getScreen() instanceof EnhancedAdvancementsScreen) {
            if (ModConfig.get().orderTabsAlphabetically) {
                Minecraft mc = Minecraft.getInstance();
                ClientAdvancements clientAdvancements = mc.player.connection.getAdvancements();
                AdvancementList advancementList = clientAdvancements.getAdvancements();
                Set<Advancement> roots = ((AdvancementListAccessor) advancementList).getRoots();

                List<String> advancementLocations = roots.stream().sorted(AdvancementComparer.sortByTitle()).map(n -> n.getId().toString()).toList();

                List<Advancement> advancements = new ArrayList<>(roots);
                roots.clear();

                for (String location : advancementLocations) {
                    for (Advancement advancement : advancements) {
                        if (advancement.getId().toString().equals(location)) {
                            roots.add(advancement);
                        }
                    }
                }
            }
        }
    }
}
