package com.evandev.reliable_advancements.config;

import com.evandev.reliable_advancements.client.config.ConfigScreens;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ReliableAdvancementsModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (!ConfigScreens.isAvailable()) {
            return screen -> null;
        }
        return Config::createConfigScreen;
    }
}
