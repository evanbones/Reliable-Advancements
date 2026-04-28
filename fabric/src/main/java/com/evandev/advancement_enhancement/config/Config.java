package com.evandev.advancement_enhancement.config;

import com.evandev.advancement_enhancement.client.config.ClothConfigScreen;
import net.minecraft.client.gui.screens.Screen;

public class Config {
    public static Screen createConfigScreen(Screen parent) {
        return ClothConfigScreen.create(parent, ModConfig::save);
    }
}