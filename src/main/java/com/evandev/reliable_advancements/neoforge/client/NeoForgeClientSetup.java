package com.evandev.reliable_advancements.neoforge.client;

//? if neoforge {
import com.evandev.reliable_advancements.client.config.ConfigScreens;
import com.evandev.reliable_advancements.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

public class NeoForgeClientSetup {
    public static void init(ModContainer container) {
        ModConfig.load();
        NeoForge.EVENT_BUS.register(NeoForgeGuiOpenHandler.instance);

        if (ConfigScreens.isAvailable()) {
            container.registerExtensionPoint(IConfigScreenFactory.class, (client, parent) -> ConfigScreens.create(parent, ModConfig::save));
        }
    }
}
//?}
