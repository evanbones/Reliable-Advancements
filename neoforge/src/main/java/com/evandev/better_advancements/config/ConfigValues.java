package com.evandev.better_advancements.config;

import com.evandev.better_advancements.advancements.BetterDisplayInfo;
import com.evandev.better_advancements.gui.*;
import com.evandev.better_advancements.util.ColorHelper;
import com.evandev.better_advancements.util.CriteriaDetail;
import com.evandev.better_advancements.util.CriterionGrid;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ConfigValues {

    public static ModConfigSpec.ConfigValue<String> defaultUncompletedIconColor;
    public static ModConfigSpec.ConfigValue<String> defaultUncompletedTitleColor;
    public static ModConfigSpec.ConfigValue<String> defaultCompletedIconColor;
    public static ModConfigSpec.ConfigValue<String> defaultCompletedTitleColor;

    public static ModConfigSpec.BooleanValue doFade;
    public static ModConfigSpec.BooleanValue showDebugCoordinates;
    public static ModConfigSpec.BooleanValue orderTabsAlphabetically;
    public static ModConfigSpec.IntValue uiScaling;

    public static ModConfigSpec.ConfigValue<String> detailLevel;
    public static ModConfigSpec.BooleanValue requiresShift;
    public static ModConfigSpec.BooleanValue addToInventory;

    public static ModConfigSpec.BooleanValue drawDirectLines;
    public static ModConfigSpec.BooleanValue hideLines;
    public static ModConfigSpec.ConfigValue<String> defaultCompletedLineColor;
    public static ModConfigSpec.ConfigValue<String> defaultUncompletedLineColor;

    public static ModConfigSpec.BooleanValue onlyUseAboveAdvancementTabs;

    public static ModConfigSpec.BooleanValue freeformLayoutEditing;
    public static ModConfigSpec.BooleanValue enableEditMode;
    public static ModConfigSpec.BooleanValue drawArrows;

    public static ModConfigSpec build() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("general");
        doFade = builder.define("doAdvancementsBackgroundFade", false);
        showDebugCoordinates = builder.define("showDebugCoordinates", false);
        orderTabsAlphabetically = builder.define("orderTabsAlphabetically", false);
        uiScaling = builder.comment("Values below 50% might give odd results, use on own risk ;)").defineInRange("uiScaling", 100, 1, 100);
        detailLevel = builder.comment(CriteriaDetail.comments()).defineInList("criteriaDetail", CriteriaDetail.SPOILER.getName(), CriteriaDetail.names());
        requiresShift = builder.define("criteriaDetailRequiresShift", true);
        addToInventory = builder.define("addInventoryButton", true);
        onlyUseAboveAdvancementTabs = builder.define("onlyUseAboveAdvancementTabs", false);
        builder.pop();

        builder.push("visuals");
        drawDirectLines = builder.define("drawDirectLines", false);
        hideLines = builder.define("hideLines", false);
        defaultCompletedLineColor = builder.define("defaultCompletedLineColor", "#FFFFFF");
        defaultUncompletedLineColor = builder.define("defaultUncompletedLineColor", "#FFFFFF");
        defaultUncompletedIconColor = builder.define("defaultUncompletedIconColor", BetterDisplayInfo.defaultMinecraftUncompletedIconColor);
        defaultUncompletedTitleColor = builder.define("defaultUncompletedTitleColor", BetterDisplayInfo.defaultMinecraftUncompletedTitleColor);
        defaultCompletedIconColor = builder.define("defaultCompletedIconColor", BetterDisplayInfo.defaultMinecraftCompletedIconColor);
        defaultCompletedTitleColor = builder.define("defaultCompletedTitleColor", BetterDisplayInfo.defaultMinecraftCompletedTitleColor);
        drawArrows = builder.comment("Draw arrows at the end of orthogonal connection lines").define("drawArrows", true);
        builder.pop();

        builder.push("editing");
        freeformLayoutEditing = builder.comment("Drag advancements to arrange layouts persistently").define("freeformLayoutEditing", false);
        enableEditMode = builder.comment("Enable edit mode where you can directly edit advancement info in-game").define("enableEditMode", false);
        builder.pop();

        return builder.build();
    }

    public static void pushChanges() {
        BetterDisplayInfo.defaultUncompletedIconColor = ColorHelper.RGB(defaultUncompletedIconColor.get());
        BetterDisplayInfo.defaultUncompletedTitleColor = ColorHelper.RGB(defaultUncompletedTitleColor.get());
        BetterDisplayInfo.defaultCompletedIconColor = ColorHelper.RGB(defaultCompletedIconColor.get());
        BetterDisplayInfo.defaultCompletedTitleColor = ColorHelper.RGB(defaultCompletedTitleColor.get());

        BetterAdvancementTab.doFade = doFade.get();
        BetterAdvancementsScreen.showDebugCoordinates = showDebugCoordinates.get();
        BetterAdvancementsScreen.orderTabsAlphabetically = orderTabsAlphabetically.get();
        BetterAdvancementsScreen.uiScaling = uiScaling.get();

        CriterionGrid.detailLevel = CriteriaDetail.fromName(detailLevel.get());
        CriterionGrid.requiresShift = requiresShift.get();
        BetterAdvancementsScreenButton.addToInventory = addToInventory.get();

        BetterDisplayInfo.defaultDrawDirectLines = drawDirectLines.get();
        BetterDisplayInfo.defaultHideLines = hideLines.get();
        BetterDisplayInfo.defaultCompletedLineColor = ColorHelper.RGB(defaultCompletedLineColor.get());
        BetterDisplayInfo.defaultUncompletedLineColor = ColorHelper.RGB(defaultUncompletedLineColor.get());

        BetterAdvancementTabType.onlyUseAbove = onlyUseAboveAdvancementTabs.get();

        BetterAdvancementsScreen.freeformLayoutEditing = freeformLayoutEditing.get();
        BetterAdvancementsScreen.enableEditMode = enableEditMode.get();
        BetterAdvancementWidget.drawArrows = drawArrows.get();
    }

    public static void updateToModConfigSpec() {
        defaultUncompletedIconColor.set(ColorHelper.asRGBString(BetterDisplayInfo.defaultUncompletedIconColor));
        defaultUncompletedTitleColor.set(ColorHelper.asRGBString(BetterDisplayInfo.defaultUncompletedTitleColor));
        defaultCompletedIconColor.set(ColorHelper.asRGBString(BetterDisplayInfo.defaultCompletedIconColor));
        defaultCompletedTitleColor.set(ColorHelper.asRGBString(BetterDisplayInfo.defaultCompletedTitleColor));

        doFade.set(BetterAdvancementTab.doFade);
        showDebugCoordinates.set(BetterAdvancementsScreen.showDebugCoordinates);
        orderTabsAlphabetically.set(BetterAdvancementsScreen.orderTabsAlphabetically);
        uiScaling.set(BetterAdvancementsScreen.uiScaling);

        detailLevel.set(CriterionGrid.detailLevel.getName());
        requiresShift.set(CriterionGrid.requiresShift);
        addToInventory.set(BetterAdvancementsScreenButton.addToInventory);

        drawDirectLines.set(BetterDisplayInfo.defaultDrawDirectLines);
        hideLines.set(BetterDisplayInfo.defaultHideLines);
        defaultCompletedLineColor.set(ColorHelper.asRGBString(BetterDisplayInfo.defaultCompletedLineColor));
        defaultUncompletedLineColor.set(ColorHelper.asRGBString(BetterDisplayInfo.defaultUncompletedLineColor));

        onlyUseAboveAdvancementTabs.set(BetterAdvancementTabType.onlyUseAbove);

        freeformLayoutEditing.set(BetterAdvancementsScreen.freeformLayoutEditing);
        enableEditMode.set(BetterAdvancementsScreen.enableEditMode);
        drawArrows.set(BetterAdvancementWidget.drawArrows);
    }
}