package com.evandev.reliable_advancements.client;

import com.evandev.reliable_advancements.client.config.ClothConfigScreen;
import com.evandev.reliable_advancements.config.ModConfig;
import com.evandev.reliable_advancements.handler.GuiOpenHandler;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

public class ClientSetup {
    public static void init(ModContainer container) {
        ModConfig.load();
        NeoForge.EVENT_BUS.register(GuiOpenHandler.instance);

        container.registerExtensionPoint(IConfigScreenFactory.class, (client, parent) -> {
            return ClothConfigScreen.create(parent, ModConfig::save);
        });
    }
}