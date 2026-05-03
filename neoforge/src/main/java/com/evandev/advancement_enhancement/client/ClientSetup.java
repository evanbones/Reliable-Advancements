package com.evandev.advancement_enhancement.client;

import com.evandev.advancement_enhancement.client.config.ClothConfigScreen;
import com.evandev.advancement_enhancement.config.ModConfig;
import com.evandev.advancement_enhancement.handler.GuiOpenHandler;
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