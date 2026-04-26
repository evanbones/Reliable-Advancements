package com.evandev.better_advancements.config;

import com.evandev.better_advancements.client.config.ClothConfigScreen;
import net.minecraft.client.gui.screens.Screen;

public class Config {
    public static Screen createConfigScreen(Screen parent) {
        return ClothConfigScreen.create(parent, ConfigFileHandler::writeToConfig);
    }
}