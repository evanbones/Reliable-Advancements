package com.evandev.advancement_enhancement;

import com.evandev.advancement_enhancement.client.config.ClothConfigScreen;
import com.evandev.advancement_enhancement.config.ModConfig;
import com.evandev.advancement_enhancement.handler.GuiOpenHandler;
import com.evandev.advancement_enhancement.network.NeoForgeNetworkHandler;
import com.evandev.advancement_enhancement.reference.Constants;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Constants.MOD_ID)
public class AdvancementEnhancement {
    public AdvancementEnhancement(ModContainer container, IEventBus modEventBus) {
        modEventBus.addListener(NeoForgeNetworkHandler::register);

        // Safely isolate client-only initialization
        if (FMLLoader.getDist() == Dist.CLIENT) {
            ClientSetup.init(container);
        }
    }

    private static class ClientSetup {
        private static void init(ModContainer container) {
            ModConfig.load();
            NeoForge.EVENT_BUS.register(GuiOpenHandler.instance);

            container.registerExtensionPoint(IConfigScreenFactory.class, (client, parent) -> {
                return ClothConfigScreen.create(parent, ModConfig::save);
            });
        }
    }
}