package com.evandev.better_advancements.client.config;


import com.evandev.better_advancements.advancements.BetterDisplayInfo;
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
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.better_advancements.title"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("config.better_advancements.category.general"));

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.better_advancements.doFade"), BetterAdvancementTab.doFade)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.better_advancements.doFade.tooltip"))
                .setSaveConsumer(newValue -> BetterAdvancementTab.doFade = newValue)
                .build());

        general.addEntry(entryBuilder.startDropdownMenu(Component.translatable("config.better_advancements.criteriaDetail"),
                        CriterionGrid.detailLevel, CriteriaDetail::fromName, o -> Component.translatable("config.better_advancements.criteriaDetail." + o.getName().toLowerCase()))
                .setSelections(CriteriaDetail.valuesAsList())
                .setDefaultValue(CriteriaDetail.SPOILER)
                .setTooltip(Component.translatable("config.better_advancements.criteriaDetail.tooltip"))
                .setSaveConsumer(newValue -> CriterionGrid.detailLevel = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.better_advancements.requiresShift"), CriterionGrid.requiresShift)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.better_advancements.requiresShift.tooltip"))
                .setSaveConsumer(newValue -> CriterionGrid.requiresShift = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.better_advancements.addToInventory"), BetterAdvancementsScreenButton.addToInventory)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.better_advancements.addToInventory.tooltip"))
                .setSaveConsumer(newValue -> BetterAdvancementsScreenButton.addToInventory = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.better_advancements.showDebugCoordinates"), BetterAdvancementsScreen.showDebugCoordinates)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.better_advancements.showDebugCoordinates.tooltip"))
                .setSaveConsumer(newValue -> BetterAdvancementsScreen.showDebugCoordinates = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.better_advancements.orderTabsAlphabetically"), BetterAdvancementsScreen.orderTabsAlphabetically)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.better_advancements.orderTabsAlphabetically.tooltip"))
                .setSaveConsumer(newValue -> BetterAdvancementsScreen.orderTabsAlphabetically = newValue)
                .build());

        general.addEntry(entryBuilder.startIntSlider(Component.translatable("config.better_advancements.uiScaling"), BetterAdvancementsScreen.uiScaling, 1, 100)
                .setDefaultValue(100)
                .setTooltip(Component.translatable("config.better_advancements.uiScaling.tooltip"))
                .setSaveConsumer(newValue -> BetterAdvancementsScreen.uiScaling = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.better_advancements.onlyUseAboveAdvancementTabs"), BetterAdvancementTabType.onlyUseAbove)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.better_advancements.onlyUseAboveAdvancementTabs.tooltip"))
                .setSaveConsumer(newValue -> BetterAdvancementTabType.onlyUseAbove = newValue)
                .build());

        ConfigCategory editing = builder.getOrCreateCategory(Component.translatable("config.better_advancements.category.editing"));

        editing.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.better_advancements.freeformLayoutEditing"), BetterAdvancementsScreen.freeformLayoutEditing)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.better_advancements.freeformLayoutEditing.tooltip"))
                .setSaveConsumer(newValue -> BetterAdvancementsScreen.freeformLayoutEditing = newValue)
                .build());

        editing.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.better_advancements.enableEditMode"), BetterAdvancementsScreen.enableEditMode)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.better_advancements.enableEditMode.tooltip"))
                .setSaveConsumer(newValue -> BetterAdvancementsScreen.enableEditMode = newValue)
                .build());

        ConfigCategory visuals = builder.getOrCreateCategory(Component.translatable("config.better_advancements.category.visuals"));

        visuals.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.better_advancements.drawArrows"), BetterAdvancementWidget.drawArrows)
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.better_advancements.drawArrows.tooltip"))
                .setSaveConsumer(newValue -> BetterAdvancementWidget.drawArrows = newValue)
                .build());

        visuals.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.better_advancements.drawDirectLines"), BetterDisplayInfo.defaultDrawDirectLines)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.better_advancements.drawDirectLines.tooltip"))
                .setSaveConsumer(newValue -> BetterDisplayInfo.defaultDrawDirectLines = newValue)
                .build());

        visuals.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.better_advancements.hideLines"), BetterDisplayInfo.defaultHideLines)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.better_advancements.hideLines.tooltip"))
                .setSaveConsumer(newValue -> BetterDisplayInfo.defaultHideLines = newValue)
                .build());

        visuals.addEntry(entryBuilder.startAlphaColorField(Component.translatable("config.better_advancements.defaultCompletedLineColor"), BetterDisplayInfo.defaultCompletedLineColor)
                .setDefaultValue(ColorHelper.RGB("#FFFFFF"))
                .setTooltip(Component.translatable("config.better_advancements.defaultCompletedLineColor.tooltip"))
                .setSaveConsumer(newValue -> BetterDisplayInfo.defaultCompletedLineColor = newValue)
                .build());

        visuals.addEntry(entryBuilder.startAlphaColorField(Component.translatable("config.better_advancements.defaultUncompletedLineColor"), BetterDisplayInfo.defaultUncompletedLineColor)
                .setDefaultValue(ColorHelper.RGB("#FFFFFF"))
                .setTooltip(Component.translatable("config.better_advancements.defaultUncompletedLineColor.tooltip"))
                .setSaveConsumer(newValue -> BetterDisplayInfo.defaultUncompletedLineColor = newValue)
                .build());

        visuals.addEntry(entryBuilder.startAlphaColorField(Component.translatable("config.better_advancements.defaultCompletedIconColor"), BetterDisplayInfo.defaultCompletedIconColor)
                .setDefaultValue(ColorHelper.RGB(BetterDisplayInfo.defaultMinecraftCompletedIconColor))
                .setTooltip(Component.translatable("config.better_advancements.defaultCompletedIconColor.tooltip"))
                .setSaveConsumer(newValue -> BetterDisplayInfo.defaultCompletedIconColor = newValue)
                .build());

        visuals.addEntry(entryBuilder.startAlphaColorField(Component.translatable("config.better_advancements.defaultUncompletedIconColor"), BetterDisplayInfo.defaultUncompletedIconColor)
                .setDefaultValue(ColorHelper.RGB(BetterDisplayInfo.defaultMinecraftUncompletedIconColor))
                .setTooltip(Component.translatable("config.better_advancements.defaultUncompletedIconColor.tooltip"))
                .setSaveConsumer(newValue -> BetterDisplayInfo.defaultUncompletedIconColor = newValue)
                .build());

        visuals.addEntry(entryBuilder.startAlphaColorField(Component.translatable("config.better_advancements.defaultCompletedTitleColor"), BetterDisplayInfo.defaultCompletedTitleColor)
                .setDefaultValue(ColorHelper.RGB(BetterDisplayInfo.defaultMinecraftCompletedTitleColor))
                .setTooltip(Component.translatable("config.better_advancements.defaultCompletedTitleColor.tooltip"))
                .setSaveConsumer(newValue -> BetterDisplayInfo.defaultCompletedTitleColor = newValue)
                .build());

        visuals.addEntry(entryBuilder.startAlphaColorField(Component.translatable("config.better_advancements.defaultUncompletedTitleColor"), BetterDisplayInfo.defaultUncompletedTitleColor)
                .setDefaultValue(ColorHelper.RGB(BetterDisplayInfo.defaultMinecraftUncompletedTitleColor))
                .setTooltip(Component.translatable("config.better_advancements.defaultUncompletedTitleColor.tooltip"))
                .setSaveConsumer(newValue -> BetterDisplayInfo.defaultUncompletedTitleColor = newValue)
                .build());

        builder.setSavingRunnable(platformSaveAction);

        return builder.build();
    }
}