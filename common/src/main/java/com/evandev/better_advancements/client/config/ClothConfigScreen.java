package com.evandev.better_advancements.client.config;

import com.evandev.better_advancements.advancements.BetterDisplayInfo;
import com.evandev.better_advancements.config.ModConfig;
import com.evandev.better_advancements.gui.*;
import com.evandev.better_advancements.util.ColorHelper;
import com.evandev.better_advancements.util.CriteriaDetail;
import com.evandev.better_advancements.util.CriterionGrid;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ClothConfigScreen {

    public static Screen create(Screen parent, Runnable platformSaveAction) {
        ModConfig defaults = new ModConfig();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.better_advancements.title"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("config.better_advancements.category.general"));

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.better_advancements.doFade"), BetterAdvancementTab.doFade)
                .setDefaultValue(defaults.doFade)
                .setTooltip(Component.translatable("config.better_advancements.doFade.tooltip"))
                .setSaveConsumer(newValue -> BetterAdvancementTab.doFade = newValue)
                .build());

        general.addEntry(entryBuilder.startDropdownMenu(Component.translatable("config.better_advancements.criteriaDetail"),
                        CriterionGrid.detailLevel, CriteriaDetail::fromName, o -> Component.translatable("config.better_advancements.criteriaDetail." + o.getName().toLowerCase()))
                .setSelections(CriteriaDetail.valuesAsList())
                .setDefaultValue(CriteriaDetail.fromName(defaults.criteriaDetail))
                .setTooltip(Component.translatable("config.better_advancements.criteriaDetail.tooltip"))
                .setSaveConsumer(newValue -> CriterionGrid.detailLevel = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.better_advancements.requiresShift"), CriterionGrid.requiresShift)
                .setDefaultValue(defaults.requiresShift)
                .setTooltip(Component.translatable("config.better_advancements.requiresShift.tooltip"))
                .setSaveConsumer(newValue -> CriterionGrid.requiresShift = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.better_advancements.addToInventory"), BetterAdvancementsScreenButton.addToInventory)
                .setDefaultValue(defaults.addToInventory)
                .setTooltip(Component.translatable("config.better_advancements.addToInventory.tooltip"))
                .setSaveConsumer(newValue -> BetterAdvancementsScreenButton.addToInventory = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.better_advancements.showDebugCoordinates"), BetterAdvancementsScreen.showDebugCoordinates)
                .setDefaultValue(defaults.showDebugCoordinates)
                .setTooltip(Component.translatable("config.better_advancements.showDebugCoordinates.tooltip"))
                .setSaveConsumer(newValue -> BetterAdvancementsScreen.showDebugCoordinates = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.better_advancements.orderTabsAlphabetically"), BetterAdvancementsScreen.orderTabsAlphabetically)
                .setDefaultValue(defaults.orderTabsAlphabetically)
                .setTooltip(Component.translatable("config.better_advancements.orderTabsAlphabetically.tooltip"))
                .setSaveConsumer(newValue -> BetterAdvancementsScreen.orderTabsAlphabetically = newValue)
                .build());

        general.addEntry(entryBuilder.startIntSlider(Component.translatable("config.better_advancements.uiScaling"), BetterAdvancementsScreen.uiScaling, 1, 100)
                .setDefaultValue(defaults.uiScaling)
                .setTooltip(Component.translatable("config.better_advancements.uiScaling.tooltip"))
                .setSaveConsumer(newValue -> BetterAdvancementsScreen.uiScaling = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.better_advancements.onlyUseAboveAdvancementTabs"), BetterAdvancementTabType.onlyUseAbove)
                .setDefaultValue(defaults.onlyUseAboveAdvancementTabs)
                .setTooltip(Component.translatable("config.better_advancements.onlyUseAboveAdvancementTabs.tooltip"))
                .setSaveConsumer(newValue -> BetterAdvancementTabType.onlyUseAbove = newValue)
                .build());

        ConfigCategory editing = builder.getOrCreateCategory(Component.translatable("config.better_advancements.category.editing"));

        editing.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.better_advancements.freeformLayoutEditing"), BetterAdvancementsScreen.freeformLayoutEditing)
                .setDefaultValue(defaults.freeformLayoutEditing)
                .setTooltip(Component.translatable("config.better_advancements.freeformLayoutEditing.tooltip"))
                .setSaveConsumer(newValue -> BetterAdvancementsScreen.freeformLayoutEditing = newValue)
                .build());

        editing.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.better_advancements.enableEditMode"), BetterAdvancementsScreen.enableEditMode)
                .setDefaultValue(defaults.enableEditMode)
                .setTooltip(Component.translatable("config.better_advancements.enableEditMode.tooltip"))
                .setSaveConsumer(newValue -> BetterAdvancementsScreen.enableEditMode = newValue)
                .build());

        ConfigCategory visuals = builder.getOrCreateCategory(Component.translatable("config.better_advancements.category.visuals"));

        visuals.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.better_advancements.drawArrows"), BetterAdvancementWidget.drawArrows)
                .setDefaultValue(defaults.drawArrows)
                .setTooltip(Component.translatable("config.better_advancements.drawArrows.tooltip"))
                .setSaveConsumer(newValue -> BetterAdvancementWidget.drawArrows = newValue)
                .build());

        visuals.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.better_advancements.drawDirectLines"), BetterDisplayInfo.defaultDrawDirectLines)
                .setDefaultValue(defaults.defaultDrawDirectLines)
                .setTooltip(Component.translatable("config.better_advancements.drawDirectLines.tooltip"))
                .setSaveConsumer(newValue -> BetterDisplayInfo.defaultDrawDirectLines = newValue)
                .build());

        visuals.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.better_advancements.hideLines"), BetterDisplayInfo.defaultHideLines)
                .setDefaultValue(defaults.defaultHideLines)
                .setTooltip(Component.translatable("config.better_advancements.hideLines.tooltip"))
                .setSaveConsumer(newValue -> BetterDisplayInfo.defaultHideLines = newValue)
                .build());

        visuals.addEntry(entryBuilder.startAlphaColorField(Component.translatable("config.better_advancements.defaultCompletedLineColor"), BetterDisplayInfo.defaultCompletedLineColor)
                .setDefaultValue(ColorHelper.RGB(defaults.defaultCompletedLineColor))
                .setTooltip(Component.translatable("config.better_advancements.defaultCompletedLineColor.tooltip"))
                .setSaveConsumer(newValue -> BetterDisplayInfo.defaultCompletedLineColor = newValue)
                .build());

        visuals.addEntry(entryBuilder.startAlphaColorField(Component.translatable("config.better_advancements.defaultUncompletedLineColor"), BetterDisplayInfo.defaultUncompletedLineColor)
                .setDefaultValue(ColorHelper.RGB(defaults.defaultUncompletedLineColor))
                .setTooltip(Component.translatable("config.better_advancements.defaultUncompletedLineColor.tooltip"))
                .setSaveConsumer(newValue -> BetterDisplayInfo.defaultUncompletedLineColor = newValue)
                .build());

        visuals.addEntry(entryBuilder.startAlphaColorField(Component.translatable("config.better_advancements.defaultCompletedIconColor"), BetterDisplayInfo.defaultCompletedIconColor)
                .setDefaultValue(ColorHelper.RGB(defaults.defaultCompletedIconColor))
                .setTooltip(Component.translatable("config.better_advancements.defaultCompletedIconColor.tooltip"))
                .setSaveConsumer(newValue -> BetterDisplayInfo.defaultCompletedIconColor = newValue)
                .build());

        visuals.addEntry(entryBuilder.startAlphaColorField(Component.translatable("config.better_advancements.defaultUncompletedIconColor"), BetterDisplayInfo.defaultUncompletedIconColor)
                .setDefaultValue(ColorHelper.RGB(defaults.defaultUncompletedIconColor))
                .setTooltip(Component.translatable("config.better_advancements.defaultUncompletedIconColor.tooltip"))
                .setSaveConsumer(newValue -> BetterDisplayInfo.defaultUncompletedIconColor = newValue)
                .build());

        visuals.addEntry(entryBuilder.startAlphaColorField(Component.translatable("config.better_advancements.defaultCompletedTitleColor"), BetterDisplayInfo.defaultCompletedTitleColor)
                .setDefaultValue(ColorHelper.RGB(defaults.defaultCompletedTitleColor))
                .setTooltip(Component.translatable("config.better_advancements.defaultCompletedTitleColor.tooltip"))
                .setSaveConsumer(newValue -> BetterDisplayInfo.defaultCompletedTitleColor = newValue)
                .build());

        visuals.addEntry(entryBuilder.startAlphaColorField(Component.translatable("config.better_advancements.defaultUncompletedTitleColor"), BetterDisplayInfo.defaultUncompletedTitleColor)
                .setDefaultValue(ColorHelper.RGB(defaults.defaultUncompletedTitleColor))
                .setTooltip(Component.translatable("config.better_advancements.defaultUncompletedTitleColor.tooltip"))
                .setSaveConsumer(newValue -> BetterDisplayInfo.defaultUncompletedTitleColor = newValue)
                .build());

        builder.setSavingRunnable(platformSaveAction);

        return builder.build();
    }
}