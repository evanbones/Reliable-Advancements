package com.evandev.reliable_advancements.client;

import com.evandev.reliable_advancements.client.config.ConfigScreens;
import com.evandev.reliable_advancements.config.ModConfig;
import com.evandev.reliable_advancements.handler.GuiOpenHandler;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

public class ClientSetup {
    public static void init(ModContainer container) {
        ModConfig.load();
        NeoForge.EVENT_BUS.register(GuiOpenHandler.instance);

        if (ConfigScreens.isAvailable()) {
            container.registerExtensionPoint(IConfigScreenFactory.class, (client, parent) -> ConfigScreens.create(parent, ModConfig::save));
        }
    }
}