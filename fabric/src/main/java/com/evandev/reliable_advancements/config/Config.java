package com.evandev.reliable_advancements.config;

import com.evandev.reliable_advancements.client.config.ConfigScreens;
import net.minecraft.client.gui.screens.Screen;

public class Config {
    public static Screen createConfigScreen(Screen parent) {
        return ConfigScreens.create(parent, ModConfig::save);
    }
}