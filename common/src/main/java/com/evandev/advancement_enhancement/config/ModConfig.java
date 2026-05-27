package com.evandev.advancement_enhancement.config;

import com.evandev.advancement_enhancement.platform.Services;
import com.evandev.advancement_enhancement.reference.Constants;
import com.evandev.advancement_enhancement.util.CriteriaDetail;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = Services.PLATFORM.getConfigDirectory().resolve(Constants.MOD_ID + ".json").toFile();
    private static ModConfig INSTANCE;

    // --- General / UI Settings ---
    public boolean doFade = false;
    public String criteriaDetail = CriteriaDetail.DEFAULT.getName();
    public boolean requiresShift = true;
    public boolean addToInventory = true;
    public boolean showDebugCoordinates = false;
    public boolean orderTabsAlphabetically = false;
    public int uiScaling = 100;
    public boolean onlyUseAboveAdvancementTabs = false;
    public boolean discoveryMode = false;
    public boolean requireRewardClaiming = false;

    // --- Inventory Button ---
    public InventoryButtonStyle inventoryButtonStyle = InventoryButtonStyle.TAB;
    public boolean enableButtonTooltip = false;
    public int inventoryButtonOffsetX = 0;
    public int inventoryButtonOffsetY = 0;
    public String customInventoryButtonTexture = "advancement_enhancement:textures/gui/inventory_button.png";
    public String customInventoryButtonTextureHovered = "advancement_enhancement:textures/gui/inventory_button_highlighted.png";
    public String customInventoryButtonIcon = "minecraft:book";

    // --- Editing ---
    public boolean enableEditMode = false;
    public boolean showTooltipsInEditMode = false;
    public boolean showEditModeButton = true;

    // --- Visuals ---
    public boolean drawArrows = false;
    public boolean defaultDrawDirectLines = false;
    public boolean defaultHideLines = false;

    // --- Colors ---
    public String defaultCompletedLineColor = "#FFFFFF";
    public String defaultUncompletedLineColor = "#FFFFFF";
    public String defaultCompletedIconColor = "#DBA213";
    public String defaultUncompletedIconColor = "#FFFFFF";
    public String defaultCompletedTitleColor = "#DBA213";
    public String defaultUncompletedTitleColor = "#0489C1";

    public static ModConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                INSTANCE = GSON.fromJson(reader, ModConfig.class);
            } catch (Exception e) {
                Constants.LOG.error("Failed to load " + Constants.MOD_ID + ".json, generating default.", e);
                INSTANCE = new ModConfig();
            }
        } else {
            INSTANCE = new ModConfig();
        }
        save();
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            Constants.LOG.error("Failed to save " + Constants.MOD_ID + ".json.", e);
        }
    }
}