package com.evandev.better_advancements.config;

import com.evandev.better_advancements.reference.Constants;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    public static final ModConfigSpec CLIENT = ConfigValues.build();
    public static Config instance = new Config();

    private Config() {
    }

    @SubscribeEvent
    public void onLoad(final ModConfigEvent.Loading configEvent) {
        if (configEvent.getConfig().getModId().equals(Constants.MOD_ID)) {
            Constants.LOG.debug("Loaded {} config file {}", Constants.MOD_ID, configEvent.getConfig().getFileName());
            ConfigValues.pushChanges();
        }
    }

    @SubscribeEvent
    public void onFileChange(final ModConfigEvent.Reloading configEvent) {
        if (configEvent.getConfig().getModId().equals(Constants.MOD_ID)) {
            Constants.LOG.debug("Reloaded {} config file {}", Constants.MOD_ID, configEvent.getConfig().getFileName());
            ConfigValues.pushChanges();
        }
    }
}