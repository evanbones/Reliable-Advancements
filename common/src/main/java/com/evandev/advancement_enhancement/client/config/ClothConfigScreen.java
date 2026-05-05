package com.evandev.advancement_enhancement.client.config;

import com.evandev.advancement_enhancement.advancements.AdvancementDisplayInfo;
import com.evandev.advancement_enhancement.config.ModConfig;
import com.evandev.advancement_enhancement.gui.*;
import com.evandev.advancement_enhancement.gui.button.AdvancementsScreenButton;
import com.evandev.advancement_enhancement.gui.button.InventoryButtonStyle;
import com.evandev.advancement_enhancement.gui.screens.EnhancedAdvancementsScreen;
import com.evandev.advancement_enhancement.util.ColorHelper;
import com.evandev.advancement_enhancement.util.CriteriaDetail;
import com.evandev.advancement_enhancement.util.CriterionGrid;
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
                .setTitle(Component.translatable("config.advancement_enhancement.title"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("config.advancement_enhancement.category.general"));

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.advancement_enhancement.doFade"), EnhancedAdvancementTab.doFade)
                .setDefaultValue(defaults.doFade)
                .setTooltip(Component.translatable("config.advancement_enhancement.doFade.tooltip"))
                .setSaveConsumer(newValue -> EnhancedAdvancementTab.doFade = newValue)
                .build());

        general.addEntry(entryBuilder.startDropdownMenu(Component.translatable("config.advancement_enhancement.criteriaDetail"),
                        CriterionGrid.detailLevel, CriteriaDetail::fromName, o -> Component.translatable("config.advancement_enhancement.criteriaDetail." + o.getName().toLowerCase()))
                .setSelections(CriteriaDetail.valuesAsList())
                .setDefaultValue(CriteriaDetail.fromName(defaults.criteriaDetail))
                .setTooltip(Component.translatable("config.advancement_enhancement.criteriaDetail.tooltip"))
                .setSaveConsumer(newValue -> CriterionGrid.detailLevel = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.advancement_enhancement.requiresShift"), CriterionGrid.requiresShift)
                .setDefaultValue(defaults.requiresShift)
                .setTooltip(Component.translatable("config.advancement_enhancement.requiresShift.tooltip"))
                .setSaveConsumer(newValue -> CriterionGrid.requiresShift = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.advancement_enhancement.addToInventory"), AdvancementsScreenButton.addToInventory)
                .setDefaultValue(defaults.addToInventory)
                .setTooltip(Component.translatable("config.advancement_enhancement.addToInventory.tooltip"))
                .setSaveConsumer(newValue -> AdvancementsScreenButton.addToInventory = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.advancement_enhancement.showDebugCoordinates"), EnhancedAdvancementsScreen.showDebugCoordinates)
                .setDefaultValue(defaults.showDebugCoordinates)
                .setTooltip(Component.translatable("config.advancement_enhancement.showDebugCoordinates.tooltip"))
                .setSaveConsumer(newValue -> EnhancedAdvancementsScreen.showDebugCoordinates = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.advancement_enhancement.orderTabsAlphabetically"), EnhancedAdvancementsScreen.orderTabsAlphabetically)
                .setDefaultValue(defaults.orderTabsAlphabetically)
                .setTooltip(Component.translatable("config.advancement_enhancement.orderTabsAlphabetically.tooltip"))
                .setSaveConsumer(newValue -> EnhancedAdvancementsScreen.orderTabsAlphabetically = newValue)
                .build());

        general.addEntry(entryBuilder.startIntSlider(Component.translatable("config.advancement_enhancement.uiScaling"), EnhancedAdvancementsScreen.uiScaling, 1, 100)
                .setDefaultValue(defaults.uiScaling)
                .setTooltip(Component.translatable("config.advancement_enhancement.uiScaling.tooltip"))
                .setSaveConsumer(newValue -> EnhancedAdvancementsScreen.uiScaling = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.advancement_enhancement.onlyUseAboveAdvancementTabs"), EnhancedAdvancementTabType.onlyUseAbove)
                .setDefaultValue(defaults.onlyUseAboveAdvancementTabs)
                .setTooltip(Component.translatable("config.advancement_enhancement.onlyUseAboveAdvancementTabs.tooltip"))
                .setSaveConsumer(newValue -> EnhancedAdvancementTabType.onlyUseAbove = newValue)
                .build());

        general.addEntry(entryBuilder.startEnumSelector(Component.translatable("config.advancement_enhancement.inventoryButtonStyle"),
                        InventoryButtonStyle.class, AdvancementsScreenButton.style)
                .setDefaultValue(defaults.inventoryButtonStyle)
                .setTooltip(Component.translatable("config.advancement_enhancement.inventoryButtonStyle.tooltip"))
                .setSaveConsumer(newValue -> AdvancementsScreenButton.style = newValue)
                .build());

        general.addEntry(entryBuilder.startIntField(Component.translatable("config.advancement_enhancement.inventoryButtonOffsetX"), AdvancementsScreenButton.offsetX)
                .setDefaultValue(defaults.inventoryButtonOffsetX)
                .setTooltip(Component.translatable("config.advancement_enhancement.inventoryButtonOffsetX.tooltip"))
                .setSaveConsumer(newValue -> AdvancementsScreenButton.offsetX = newValue)
                .build());

        general.addEntry(entryBuilder.startIntField(Component.translatable("config.advancement_enhancement.inventoryButtonOffsetY"), AdvancementsScreenButton.offsetY)
                .setDefaultValue(defaults.inventoryButtonOffsetY)
                .setTooltip(Component.translatable("config.advancement_enhancement.inventoryButtonOffsetY.tooltip"))
                .setSaveConsumer(newValue -> AdvancementsScreenButton.offsetY = newValue)
                .build());

        general.addEntry(entryBuilder.startStrField(Component.translatable("config.advancement_enhancement.customInventoryButtonTexture"), AdvancementsScreenButton.customTexture)
                .setDefaultValue(defaults.customInventoryButtonTexture)
                .setTooltip(Component.translatable("config.advancement_enhancement.customInventoryButtonTexture.tooltip"))
                .setSaveConsumer(newValue -> AdvancementsScreenButton.customTexture = newValue)
                .build());

        general.addEntry(entryBuilder.startStrField(Component.translatable("config.advancement_enhancement.customInventoryButtonTextureHovered"), AdvancementsScreenButton.customTextureHovered)
                .setDefaultValue(defaults.customInventoryButtonTextureHovered)
                .setTooltip(Component.translatable("config.advancement_enhancement.customInventoryButtonTextureHovered.tooltip"))
                .setSaveConsumer(newValue -> AdvancementsScreenButton.customTextureHovered = newValue)
                .build());

        general.addEntry(entryBuilder.startStrField(Component.translatable("config.advancement_enhancement.customInventoryButtonIcon"), AdvancementsScreenButton.customIcon)
                .setDefaultValue(defaults.customInventoryButtonIcon)
                .setTooltip(Component.translatable("config.advancement_enhancement.customInventoryButtonIcon.tooltip"))
                .setSaveConsumer(newValue -> AdvancementsScreenButton.customIcon = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.advancement_enhancement.discoveryMode"), EnhancedAdvancementsScreen.discoveryMode)
                .setDefaultValue(defaults.discoveryMode)
                .setTooltip(Component.translatable("config.advancement_enhancement.discoveryMode.tooltip"))
                .setSaveConsumer(newValue -> EnhancedAdvancementsScreen.discoveryMode = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.advancement_enhancement.requireRewardClaiming"), EnhancedAdvancementsScreen.requireRewardClaiming)
                .setDefaultValue(defaults.requireRewardClaiming)
                .setTooltip(Component.translatable("config.advancement_enhancement.requireRewardClaiming.tooltip"))
                .setSaveConsumer(newValue -> EnhancedAdvancementsScreen.requireRewardClaiming = newValue)
                .build());

        ConfigCategory editing = builder.getOrCreateCategory(Component.translatable("config.advancement_enhancement.category.editing"));

        editing.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.advancement_enhancement.enableEditMode"), EnhancedAdvancementsScreen.enableEditMode)
                .setDefaultValue(defaults.enableEditMode)
                .setTooltip(Component.translatable("config.advancement_enhancement.enableEditMode.tooltip"))
                .setSaveConsumer(newValue -> EnhancedAdvancementsScreen.enableEditMode = newValue)
                .build());

        ConfigCategory visuals = builder.getOrCreateCategory(Component.translatable("config.advancement_enhancement.category.visuals"));

        visuals.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.advancement_enhancement.drawArrows"), EnhancedAdvancementWidget.drawArrows)
                .setDefaultValue(defaults.drawArrows)
                .setTooltip(Component.translatable("config.advancement_enhancement.drawArrows.tooltip"))
                .setSaveConsumer(newValue -> EnhancedAdvancementWidget.drawArrows = newValue)
                .build());

        visuals.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.advancement_enhancement.drawDirectLines"), AdvancementDisplayInfo.defaultDrawDirectLines)
                .setDefaultValue(defaults.defaultDrawDirectLines)
                .setTooltip(Component.translatable("config.advancement_enhancement.drawDirectLines.tooltip"))
                .setSaveConsumer(newValue -> AdvancementDisplayInfo.defaultDrawDirectLines = newValue)
                .build());

        visuals.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.advancement_enhancement.hideLines"), AdvancementDisplayInfo.defaultHideLines)
                .setDefaultValue(defaults.defaultHideLines)
                .setTooltip(Component.translatable("config.advancement_enhancement.hideLines.tooltip"))
                .setSaveConsumer(newValue -> AdvancementDisplayInfo.defaultHideLines = newValue)
                .build());

        visuals.addEntry(entryBuilder.startAlphaColorField(Component.translatable("config.advancement_enhancement.defaultCompletedLineColor"), AdvancementDisplayInfo.defaultCompletedLineColor)
                .setDefaultValue(ColorHelper.RGB(defaults.defaultCompletedLineColor))
                .setTooltip(Component.translatable("config.advancement_enhancement.defaultCompletedLineColor.tooltip"))
                .setSaveConsumer(newValue -> AdvancementDisplayInfo.defaultCompletedLineColor = newValue)
                .build());

        visuals.addEntry(entryBuilder.startAlphaColorField(Component.translatable("config.advancement_enhancement.defaultUncompletedLineColor"), AdvancementDisplayInfo.defaultUncompletedLineColor)
                .setDefaultValue(ColorHelper.RGB(defaults.defaultUncompletedLineColor))
                .setTooltip(Component.translatable("config.advancement_enhancement.defaultUncompletedLineColor.tooltip"))
                .setSaveConsumer(newValue -> AdvancementDisplayInfo.defaultUncompletedLineColor = newValue)
                .build());

        visuals.addEntry(entryBuilder.startAlphaColorField(Component.translatable("config.advancement_enhancement.defaultCompletedIconColor"), AdvancementDisplayInfo.defaultCompletedIconColor)
                .setDefaultValue(ColorHelper.RGB(defaults.defaultCompletedIconColor))
                .setTooltip(Component.translatable("config.advancement_enhancement.defaultCompletedIconColor.tooltip"))
                .setSaveConsumer(newValue -> AdvancementDisplayInfo.defaultCompletedIconColor = newValue)
                .build());

        visuals.addEntry(entryBuilder.startAlphaColorField(Component.translatable("config.advancement_enhancement.defaultUncompletedIconColor"), AdvancementDisplayInfo.defaultUncompletedIconColor)
                .setDefaultValue(ColorHelper.RGB(defaults.defaultUncompletedIconColor))
                .setTooltip(Component.translatable("config.advancement_enhancement.defaultUncompletedIconColor.tooltip"))
                .setSaveConsumer(newValue -> AdvancementDisplayInfo.defaultUncompletedIconColor = newValue)
                .build());

        visuals.addEntry(entryBuilder.startAlphaColorField(Component.translatable("config.advancement_enhancement.defaultCompletedTitleColor"), AdvancementDisplayInfo.defaultCompletedTitleColor)
                .setDefaultValue(ColorHelper.RGB(defaults.defaultCompletedTitleColor))
                .setTooltip(Component.translatable("config.advancement_enhancement.defaultCompletedTitleColor.tooltip"))
                .setSaveConsumer(newValue -> AdvancementDisplayInfo.defaultCompletedTitleColor = newValue)
                .build());

        visuals.addEntry(entryBuilder.startAlphaColorField(Component.translatable("config.advancement_enhancement.defaultUncompletedTitleColor"), AdvancementDisplayInfo.defaultUncompletedTitleColor)
                .setDefaultValue(ColorHelper.RGB(defaults.defaultUncompletedTitleColor))
                .setTooltip(Component.translatable("config.advancement_enhancement.defaultUncompletedTitleColor.tooltip"))
                .setSaveConsumer(newValue -> AdvancementDisplayInfo.defaultUncompletedTitleColor = newValue)
                .build());

        builder.setSavingRunnable(platformSaveAction);

        return builder.build();
    }
}