package com.evandev.reliable_advancements.fabric.compat;

//? if fabric {
/*import com.evandev.reliable_advancements.client.config.ConfigScreens;
import com.evandev.reliable_advancements.config.ModConfig;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (!ConfigScreens.isAvailable()) {
            return screen -> null;
        }
        return parent -> ConfigScreens.create(parent, ModConfig::save);
    }
}
*///?}
