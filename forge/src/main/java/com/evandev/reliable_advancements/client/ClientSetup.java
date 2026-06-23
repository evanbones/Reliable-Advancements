package com.evandev.reliable_advancements.client;

import com.evandev.reliable_advancements.client.config.ClothConfigScreen;
import com.evandev.reliable_advancements.config.ModConfig;
import com.evandev.reliable_advancements.handler.GuiOpenHandler;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;

public class ClientSetup {
    public static void init() {
        ModConfig.load();
        MinecraftForge.EVENT_BUS.register(GuiOpenHandler.instance);

        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () ->
                new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> ClothConfigScreen.create(parent, ModConfig::save))
        );
    }
}