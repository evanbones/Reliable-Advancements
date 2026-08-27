package com.evandev.reliable_advancements.config;

import com.evandev.reliable_advancements.platform.Services;
import com.evandev.reliable_advancements.reference.Constants;
import com.evandev.reliable_advancements.util.CriteriaDetail;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = Services.PLATFORM.getConfigDirectory().resolve(Constants.MOD_ID + ".json").toFile();
    private static ModConfig INSTANCE;

    public boolean doFade = false;
    public String criteriaDetail = CriteriaDetail.DEFAULT.getName();
    public boolean requiresShift = true;
    public boolean addToInventory = true;
    public boolean showDebugCoordinates = false;
    public boolean orderTabsAlphabetically = false;
    public List<String> tabSortOrder = new ArrayList<>();
    public boolean unclampedScrolling = false;
    public int uiScaling = 100;
    public boolean onlyUseAboveAdvancementTabs = false;
    public boolean discoveryMode = false;
    public boolean requireRewardClaiming = false;
    public int editorSidebarWidth = 130;

    public InventoryButtonStyle inventoryButtonStyle = InventoryButtonStyle.TAB;
    public boolean enableButtonTooltip = false;
    public int inventoryButtonOffsetX = 0;
    public int inventoryButtonOffsetY = 0;
    public String customInventoryButtonTexture = "reliable_advancements:textures/gui/inventory_button.png";
    public String customInventoryButtonTextureHovered = "reliable_advancements:textures/gui/inventory_button_highlighted.png";
    public String customInventoryButtonIcon = "minecraft:book";

    public boolean enableEditMode = false;
    public boolean showTooltipsInEditMode = false;
    public boolean showEditModeButton = true;
    public boolean storeAdvancementEditsGlobally = true;
    public boolean storeAdvancementEditsAsDatapack = false;

    public boolean drawArrows = false;
    public boolean defaultDrawDirectLines = false;
    public boolean defaultHideLines = false;
    public boolean blurBackground = false;
    public int blurBackgroundOpacity = 66;

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
