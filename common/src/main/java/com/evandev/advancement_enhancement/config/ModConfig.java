package com.evandev.advancement_enhancement.config;

import com.evandev.advancement_enhancement.advancements.AdvancementDisplayInfo;
import com.evandev.advancement_enhancement.gui.*;
import com.evandev.advancement_enhancement.gui.button.AdvancementsScreenButton;
import com.evandev.advancement_enhancement.gui.button.InventoryButtonStyle;
import com.evandev.advancement_enhancement.gui.screens.EnhancedAdvancementsScreen;
import com.evandev.advancement_enhancement.platform.Services;
import com.evandev.advancement_enhancement.reference.Constants;
import com.evandev.advancement_enhancement.util.ColorHelper;
import com.evandev.advancement_enhancement.util.CriteriaDetail;
import com.evandev.advancement_enhancement.util.CriterionGrid;
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

    // --- Visuals ---
    public boolean drawArrows = false;
    public boolean defaultDrawDirectLines = false;
    public boolean defaultHideLines = false;

    // --- Colors ---
    public String defaultCompletedLineColor = "#FFFFFF";
    public String defaultUncompletedLineColor = "#FFFFFF";
    public String defaultCompletedIconColor = AdvancementDisplayInfo.defaultMinecraftCompletedIconColor;
    public String defaultUncompletedIconColor = AdvancementDisplayInfo.defaultMinecraftUncompletedIconColor;
    public String defaultCompletedTitleColor = AdvancementDisplayInfo.defaultMinecraftCompletedTitleColor;
    public String defaultUncompletedTitleColor = AdvancementDisplayInfo.defaultMinecraftUncompletedTitleColor;

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

        pushToStatics();
        save();
    }

    public static void save() {
        pullFromStatics();

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            Constants.LOG.error("Failed to save " + Constants.MOD_ID + ".json.", e);
        }
    }

    public static void pushToStatics() {
        // General
        EnhancedAdvancementTab.doFade = INSTANCE.doFade;
        CriterionGrid.detailLevel = CriteriaDetail.fromName(INSTANCE.criteriaDetail);
        CriterionGrid.requiresShift = INSTANCE.requiresShift;
        AdvancementsScreenButton.addToInventory = INSTANCE.addToInventory;
        AdvancementsScreenButton.enableButtonTooltip = INSTANCE.enableButtonTooltip;
        EnhancedAdvancementsScreen.showDebugCoordinates = INSTANCE.showDebugCoordinates;
        EnhancedAdvancementsScreen.orderTabsAlphabetically = INSTANCE.orderTabsAlphabetically;
        EnhancedAdvancementsScreen.uiScaling = INSTANCE.uiScaling;
        EnhancedAdvancementTabType.onlyUseAbove = INSTANCE.onlyUseAboveAdvancementTabs;
        EnhancedAdvancementsScreen.discoveryMode = INSTANCE.discoveryMode;
        EnhancedAdvancementsScreen.requireRewardClaiming = INSTANCE.requireRewardClaiming;
        AdvancementsScreenButton.style = INSTANCE.inventoryButtonStyle;
        AdvancementsScreenButton.offsetX = INSTANCE.inventoryButtonOffsetX;
        AdvancementsScreenButton.offsetY = INSTANCE.inventoryButtonOffsetY;
        AdvancementsScreenButton.customTexture = INSTANCE.customInventoryButtonTexture;
        AdvancementsScreenButton.customTextureHovered = INSTANCE.customInventoryButtonTextureHovered;
        AdvancementsScreenButton.customIcon = INSTANCE.customInventoryButtonIcon;

        // Editing
        EnhancedAdvancementsScreen.enableEditMode = INSTANCE.enableEditMode;
        EnhancedAdvancementsScreen.showTooltipsInEditMode = INSTANCE.showTooltipsInEditMode;

        // Visuals
        EnhancedAdvancementWidget.drawArrows = INSTANCE.drawArrows;
        AdvancementDisplayInfo.defaultDrawDirectLines = INSTANCE.defaultDrawDirectLines;
        AdvancementDisplayInfo.defaultHideLines = INSTANCE.defaultHideLines;

        // Colors
        AdvancementDisplayInfo.defaultCompletedLineColor = ColorHelper.RGB(INSTANCE.defaultCompletedLineColor);
        AdvancementDisplayInfo.defaultUncompletedLineColor = ColorHelper.RGB(INSTANCE.defaultUncompletedLineColor);
        AdvancementDisplayInfo.defaultCompletedIconColor = ColorHelper.RGB(INSTANCE.defaultCompletedIconColor);
        AdvancementDisplayInfo.defaultUncompletedIconColor = ColorHelper.RGB(INSTANCE.defaultUncompletedIconColor);
        AdvancementDisplayInfo.defaultCompletedTitleColor = ColorHelper.RGB(INSTANCE.defaultCompletedTitleColor);
        AdvancementDisplayInfo.defaultUncompletedTitleColor = ColorHelper.RGB(INSTANCE.defaultUncompletedTitleColor);
    }

    public static void pullFromStatics() {
        // General
        INSTANCE.doFade = EnhancedAdvancementTab.doFade;
        INSTANCE.criteriaDetail = CriterionGrid.detailLevel.getName();
        INSTANCE.requiresShift = CriterionGrid.requiresShift;
        INSTANCE.addToInventory = AdvancementsScreenButton.addToInventory;
        INSTANCE.showDebugCoordinates = EnhancedAdvancementsScreen.showDebugCoordinates;
        INSTANCE.orderTabsAlphabetically = EnhancedAdvancementsScreen.orderTabsAlphabetically;
        INSTANCE.uiScaling = EnhancedAdvancementsScreen.uiScaling;
        INSTANCE.requireRewardClaiming = EnhancedAdvancementsScreen.requireRewardClaiming;
        INSTANCE.discoveryMode = EnhancedAdvancementsScreen.discoveryMode;
        INSTANCE.onlyUseAboveAdvancementTabs = EnhancedAdvancementTabType.onlyUseAbove;
        INSTANCE.inventoryButtonStyle = AdvancementsScreenButton.style;
        INSTANCE.enableButtonTooltip = AdvancementsScreenButton.enableButtonTooltip;
        INSTANCE.inventoryButtonOffsetX = AdvancementsScreenButton.offsetX;
        INSTANCE.inventoryButtonOffsetY = AdvancementsScreenButton.offsetY;
        INSTANCE.customInventoryButtonTexture = AdvancementsScreenButton.customTexture;
        INSTANCE.customInventoryButtonTextureHovered = AdvancementsScreenButton.customTextureHovered;
        INSTANCE.customInventoryButtonIcon = AdvancementsScreenButton.customIcon;

        // Editing
        INSTANCE.enableEditMode = EnhancedAdvancementsScreen.enableEditMode;
        INSTANCE.showTooltipsInEditMode = EnhancedAdvancementsScreen.showTooltipsInEditMode;

        // Visuals
        INSTANCE.drawArrows = EnhancedAdvancementWidget.drawArrows;
        INSTANCE.defaultDrawDirectLines = AdvancementDisplayInfo.defaultDrawDirectLines;
        INSTANCE.defaultHideLines = AdvancementDisplayInfo.defaultHideLines;

        // Colors
        INSTANCE.defaultCompletedLineColor = ColorHelper.asRGBString(AdvancementDisplayInfo.defaultCompletedLineColor);
        INSTANCE.defaultUncompletedLineColor = ColorHelper.asRGBString(AdvancementDisplayInfo.defaultUncompletedLineColor);
        INSTANCE.defaultCompletedIconColor = ColorHelper.asRGBString(AdvancementDisplayInfo.defaultCompletedIconColor);
        INSTANCE.defaultUncompletedIconColor = ColorHelper.asRGBString(AdvancementDisplayInfo.defaultUncompletedIconColor);
        INSTANCE.defaultCompletedTitleColor = ColorHelper.asRGBString(AdvancementDisplayInfo.defaultCompletedTitleColor);
        INSTANCE.defaultUncompletedTitleColor = ColorHelper.asRGBString(AdvancementDisplayInfo.defaultUncompletedTitleColor);
    }
}