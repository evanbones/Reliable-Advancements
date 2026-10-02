package com.evandev.reliable_advancements.client.config;

import com.evandev.reliable_advancements.platform.Services;
import com.evandev.reliable_advancements.reference.Constants;
import net.minecraft.client.gui.screens.Screen;

public final class ConfigScreens {

    public static final String YACL_MOD_ID = "yet_another_config_lib_v3";

    private ConfigScreens() {
    }

    public static boolean isAvailable() {
        return Services.PLATFORM.isModLoaded(YACL_MOD_ID);
    }

    public static Screen create(Screen parent, Runnable platformSaveAction) {
        if (!isAvailable()) return parent;
        try {
            return YaclConfigScreen.create(parent, platformSaveAction);
        } catch (LinkageError | RuntimeException e) {
            Constants.LOG.error("Failed to build the config screen", e);
            return parent;
        }
    }
}
