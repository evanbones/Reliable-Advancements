package com.evandev.better_advancements;

import com.evandev.better_advancements.client.config.ClothConfigScreen;
import com.evandev.better_advancements.config.Config;
import com.evandev.better_advancements.config.ConfigValues;
import com.evandev.better_advancements.handler.GuiOpenHandler;
import com.evandev.better_advancements.reference.Constants;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Objects;

@Mod(Constants.MOD_ID)
public class EvenBetterAdvancements {
    public EvenBetterAdvancements(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT);
        Objects.requireNonNull(container.getEventBus()).register(Config.instance);
        NeoForge.EVENT_BUS.register(GuiOpenHandler.instance);

        container.registerExtensionPoint(IConfigScreenFactory.class, (client, parent) -> {
            return ClothConfigScreen.create(parent, () -> {
                ConfigValues.updateToModConfigSpec();
                Config.CLIENT.save();
            });
        });
    }
}