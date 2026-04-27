package com.evandev.better_advancements.config;

import com.evandev.better_advancements.advancements.BetterDisplayInfo;
import com.evandev.better_advancements.gui.*;
import com.evandev.better_advancements.gui.screens.BetterAdvancementsScreen;
import com.evandev.better_advancements.platform.Services;
import com.evandev.better_advancements.reference.Constants;
import com.evandev.better_advancements.util.ColorHelper;
import com.evandev.better_advancements.util.CriteriaDetail;
import com.evandev.better_advancements.util.CriterionGrid;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = Services.PLATFORM.getConfigDirectory().resolve("better_advancements.json").toFile();
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

    // --- Editing ---
    public boolean enableEditMode = false;

    // --- Visuals ---
    public boolean drawArrows = false;
    public boolean defaultDrawDirectLines = false;
    public boolean defaultHideLines = false;

    // --- Colors ---
    public String defaultCompletedLineColor = "#FFFFFF";
    public String defaultUncompletedLineColor = "#FFFFFF";
    public String defaultCompletedIconColor = BetterDisplayInfo.defaultMinecraftCompletedIconColor;
    public String defaultUncompletedIconColor = BetterDisplayInfo.defaultMinecraftUncompletedIconColor;
    public String defaultCompletedTitleColor = BetterDisplayInfo.defaultMinecraftCompletedTitleColor;
    public String defaultUncompletedTitleColor = BetterDisplayInfo.defaultMinecraftUncompletedTitleColor;

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
                Constants.LOG.error("Failed to load better_advancements.json, generating default.", e);
                INSTANCE = new ModConfig();
            }
        } else {
            INSTANCE = new ModConfig();
        }

        save();
        pushToStatics();
    }

    public static void save() {
        pullFromStatics();

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            Constants.LOG.error("Failed to save better_advancements.json", e);
        }
    }

    public static void pushToStatics() {
        // General
        BetterAdvancementTab.doFade = INSTANCE.doFade;
        CriterionGrid.detailLevel = CriteriaDetail.fromName(INSTANCE.criteriaDetail);
        CriterionGrid.requiresShift = INSTANCE.requiresShift;
        BetterAdvancementsScreenButton.addToInventory = INSTANCE.addToInventory;
        BetterAdvancementsScreen.showDebugCoordinates = INSTANCE.showDebugCoordinates;
        BetterAdvancementsScreen.orderTabsAlphabetically = INSTANCE.orderTabsAlphabetically;
        BetterAdvancementsScreen.uiScaling = INSTANCE.uiScaling;
        BetterAdvancementTabType.onlyUseAbove = INSTANCE.onlyUseAboveAdvancementTabs;

        // Editing
        BetterAdvancementsScreen.enableEditMode = INSTANCE.enableEditMode;

        // Visuals
        BetterAdvancementWidget.drawArrows = INSTANCE.drawArrows;
        BetterDisplayInfo.defaultDrawDirectLines = INSTANCE.defaultDrawDirectLines;
        BetterDisplayInfo.defaultHideLines = INSTANCE.defaultHideLines;

        // Colors
        BetterDisplayInfo.defaultCompletedLineColor = ColorHelper.RGB(INSTANCE.defaultCompletedLineColor);
        BetterDisplayInfo.defaultUncompletedLineColor = ColorHelper.RGB(INSTANCE.defaultUncompletedLineColor);
        BetterDisplayInfo.defaultCompletedIconColor = ColorHelper.RGB(INSTANCE.defaultCompletedIconColor);
        BetterDisplayInfo.defaultUncompletedIconColor = ColorHelper.RGB(INSTANCE.defaultUncompletedIconColor);
        BetterDisplayInfo.defaultCompletedTitleColor = ColorHelper.RGB(INSTANCE.defaultCompletedTitleColor);
        BetterDisplayInfo.defaultUncompletedTitleColor = ColorHelper.RGB(INSTANCE.defaultUncompletedTitleColor);
    }

    public static void pullFromStatics() {
        // General
        INSTANCE.doFade = BetterAdvancementTab.doFade;
        INSTANCE.criteriaDetail = CriterionGrid.detailLevel.getName();
        INSTANCE.requiresShift = CriterionGrid.requiresShift;
        INSTANCE.addToInventory = BetterAdvancementsScreenButton.addToInventory;
        INSTANCE.showDebugCoordinates = BetterAdvancementsScreen.showDebugCoordinates;
        INSTANCE.orderTabsAlphabetically = BetterAdvancementsScreen.orderTabsAlphabetically;
        INSTANCE.uiScaling = BetterAdvancementsScreen.uiScaling;
        INSTANCE.onlyUseAboveAdvancementTabs = BetterAdvancementTabType.onlyUseAbove;

        // Editing
        INSTANCE.enableEditMode = BetterAdvancementsScreen.enableEditMode;

        // Visuals
        INSTANCE.drawArrows = BetterAdvancementWidget.drawArrows;
        INSTANCE.defaultDrawDirectLines = BetterDisplayInfo.defaultDrawDirectLines;
        INSTANCE.defaultHideLines = BetterDisplayInfo.defaultHideLines;

        // Colors
        INSTANCE.defaultCompletedLineColor = ColorHelper.asRGBString(BetterDisplayInfo.defaultCompletedLineColor);
        INSTANCE.defaultUncompletedLineColor = ColorHelper.asRGBString(BetterDisplayInfo.defaultUncompletedLineColor);
        INSTANCE.defaultCompletedIconColor = ColorHelper.asRGBString(BetterDisplayInfo.defaultCompletedIconColor);
        INSTANCE.defaultUncompletedIconColor = ColorHelper.asRGBString(BetterDisplayInfo.defaultUncompletedIconColor);
        INSTANCE.defaultCompletedTitleColor = ColorHelper.asRGBString(BetterDisplayInfo.defaultCompletedTitleColor);
        INSTANCE.defaultUncompletedTitleColor = ColorHelper.asRGBString(BetterDisplayInfo.defaultUncompletedTitleColor);
    }
}