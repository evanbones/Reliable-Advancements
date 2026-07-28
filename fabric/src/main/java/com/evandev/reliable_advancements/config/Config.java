package com.evandev.reliable_advancements.config;

import com.evandev.reliable_advancements.client.config.YaclConfigScreen;
import net.minecraft.client.gui.screens.Screen;

public class Config {
    public static Screen createConfigScreen(Screen parent) {
        return YaclConfigScreen.create(parent, ModConfig::save);
    }
}