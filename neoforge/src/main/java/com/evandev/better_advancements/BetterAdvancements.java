package com.evandev.better_advancements;

import com.evandev.better_advancements.client.config.ClothConfigScreen;
import com.evandev.better_advancements.config.ModConfig;
import com.evandev.better_advancements.handler.GuiOpenHandler;
import com.evandev.better_advancements.network.NeoForgeNetworkHandler;
import com.evandev.better_advancements.reference.Constants;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Constants.MOD_ID)
public class BetterAdvancements {
    public BetterAdvancements(ModContainer container, IEventBus modEventBus) {
        ModConfig.load();
        NeoForge.EVENT_BUS.register(GuiOpenHandler.instance);
        modEventBus.addListener(NeoForgeNetworkHandler::register);

        container.registerExtensionPoint(IConfigScreenFactory.class, (client, parent) -> {
            return ClothConfigScreen.create(parent, ModConfig::save);
        });
    }
}