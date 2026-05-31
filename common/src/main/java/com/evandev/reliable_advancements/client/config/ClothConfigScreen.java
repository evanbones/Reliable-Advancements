package com.evandev.reliable_advancements.client.config;

import com.evandev.reliable_advancements.config.InventoryButtonStyle;
import com.evandev.reliable_advancements.config.ModConfig;
import com.evandev.reliable_advancements.gui.screens.EnhancedAdvancementsScreen;
import com.evandev.reliable_advancements.network.RequestAdvancementJsonPayload;
import com.evandev.reliable_advancements.platform.Services;
import com.evandev.reliable_advancements.reference.Constants;
import com.evandev.reliable_advancements.util.ColorHelper;
import com.evandev.reliable_advancements.util.CriteriaDetail;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ClothConfigScreen {

    public static Screen create(Screen parent, Runnable platformSaveAction) {
        ModConfig defaults = new ModConfig();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.reliable_advancements.title"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("config.reliable_advancements.category.general"));
        ModConfig config = ModConfig.get();

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_advancements.doFade"), config.doFade)
                .setDefaultValue(defaults.doFade)
                .setTooltip(Component.translatable("config.reliable_advancements.doFade.tooltip"))
                .setSaveConsumer(newValue -> config.doFade = newValue)
                .build());

        general.addEntry(entryBuilder.startDropdownMenu(Component.translatable("config.reliable_advancements.criteriaDetail"),
                        CriteriaDetail.fromName(config.criteriaDetail),
                        CriteriaDetail::fromName,
                        o -> Component.translatable("config.reliable_advancements.criteriaDetail." + o.getName().toLowerCase()))
                .setSelections(CriteriaDetail.valuesAsList())
                .setDefaultValue(CriteriaDetail.fromName(defaults.criteriaDetail))
                .setTooltip(Component.translatable("config.reliable_advancements.criteriaDetail.tooltip"))
                .setSaveConsumer(newValue -> config.criteriaDetail = newValue.getName())
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_advancements.requiresShift"), config.requiresShift)
                .setDefaultValue(defaults.requiresShift)
                .setTooltip(Component.translatable("config.reliable_advancements.requiresShift.tooltip"))
                .setSaveConsumer(newValue -> config.requiresShift = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_advancements.addToInventory"), config.addToInventory)
                .setDefaultValue(defaults.addToInventory)
                .setTooltip(Component.translatable("config.reliable_advancements.addToInventory.tooltip"))
                .setSaveConsumer(newValue -> config.addToInventory = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_advancements.showDebugCoordinates"), config.showDebugCoordinates)
                .setDefaultValue(defaults.showDebugCoordinates)
                .setTooltip(Component.translatable("config.reliable_advancements.showDebugCoordinates.tooltip"))
                .setSaveConsumer(newValue -> config.showDebugCoordinates = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_advancements.orderTabsAlphabetically"), config.orderTabsAlphabetically)
                .setDefaultValue(defaults.orderTabsAlphabetically)
                .setTooltip(Component.translatable("config.reliable_advancements.orderTabsAlphabetically.tooltip"))
                .setSaveConsumer(newValue -> config.orderTabsAlphabetically = newValue)
                .build());

        general.addEntry(entryBuilder.startIntSlider(Component.translatable("config.reliable_advancements.uiScaling"), config.uiScaling, 1, 100)
                .setDefaultValue(defaults.uiScaling)
                .setTooltip(Component.translatable("config.reliable_advancements.uiScaling.tooltip"))
                .setSaveConsumer(newValue -> config.uiScaling = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_advancements.onlyUseAboveAdvancementTabs"), config.onlyUseAboveAdvancementTabs)
                .setDefaultValue(defaults.onlyUseAboveAdvancementTabs)
                .setTooltip(Component.translatable("config.reliable_advancements.onlyUseAboveAdvancementTabs.tooltip"))
                .setSaveConsumer(newValue -> config.onlyUseAboveAdvancementTabs = newValue)
                .build());

        general.addEntry(entryBuilder.startEnumSelector(Component.translatable("config.reliable_advancements.inventoryButtonStyle"),
                        InventoryButtonStyle.class, config.inventoryButtonStyle)
                .setDefaultValue(defaults.inventoryButtonStyle)
                .setTooltip(Component.translatable("config.reliable_advancements.inventoryButtonStyle.tooltip"))
                .setSaveConsumer(newValue -> config.inventoryButtonStyle = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_advancements.enableButtonTooltip"), config.enableButtonTooltip)
                .setDefaultValue(defaults.enableButtonTooltip)
                .setTooltip(Component.translatable("config.reliable_advancements.enableButtonTooltip.tooltip"))
                .setSaveConsumer(newValue -> config.enableButtonTooltip = newValue)
                .build());

        general.addEntry(entryBuilder.startIntField(Component.translatable("config.reliable_advancements.inventoryButtonOffsetX"), config.inventoryButtonOffsetX)
                .setDefaultValue(defaults.inventoryButtonOffsetX)
                .setTooltip(Component.translatable("config.reliable_advancements.inventoryButtonOffsetX.tooltip"))
                .setSaveConsumer(newValue -> config.inventoryButtonOffsetX = newValue)
                .build());

        general.addEntry(entryBuilder.startIntField(Component.translatable("config.reliable_advancements.inventoryButtonOffsetY"), config.inventoryButtonOffsetY)
                .setDefaultValue(defaults.inventoryButtonOffsetY)
                .setTooltip(Component.translatable("config.reliable_advancements.inventoryButtonOffsetY.tooltip"))
                .setSaveConsumer(newValue -> config.inventoryButtonOffsetY = newValue)
                .build());

        general.addEntry(entryBuilder.startStrField(Component.translatable("config.reliable_advancements.customInventoryButtonTexture"), config.customInventoryButtonTexture)
                .setDefaultValue(defaults.customInventoryButtonTexture)
                .setTooltip(Component.translatable("config.reliable_advancements.customInventoryButtonTexture.tooltip"))
                .setSaveConsumer(newValue -> config.customInventoryButtonTexture = newValue)
                .build());

        general.addEntry(entryBuilder.startStrField(Component.translatable("config.reliable_advancements.customInventoryButtonTextureHovered"), config.customInventoryButtonTextureHovered)
                .setDefaultValue(defaults.customInventoryButtonTextureHovered)
                .setTooltip(Component.translatable("config.reliable_advancements.customInventoryButtonTextureHovered.tooltip"))
                .setSaveConsumer(newValue -> config.customInventoryButtonTextureHovered = newValue)
                .build());

        general.addEntry(entryBuilder.startStrField(Component.translatable("config.reliable_advancements.customInventoryButtonIcon"), config.customInventoryButtonIcon)
                .setDefaultValue(defaults.customInventoryButtonIcon)
                .setTooltip(Component.translatable("config.reliable_advancements.customInventoryButtonIcon.tooltip"))
                .setSaveConsumer(newValue -> config.customInventoryButtonIcon = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_advancements.discoveryMode"), config.discoveryMode)
                .setDefaultValue(defaults.discoveryMode)
                .setTooltip(Component.translatable("config.reliable_advancements.discoveryMode.tooltip"))
                .setSaveConsumer(newValue -> config.discoveryMode = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_advancements.requireRewardClaiming"), config.requireRewardClaiming)
                .setDefaultValue(defaults.requireRewardClaiming)
                .setTooltip(Component.translatable("config.reliable_advancements.requireRewardClaiming.tooltip"))
                .setSaveConsumer(newValue -> config.requireRewardClaiming = newValue)
                .build());

        ConfigCategory editing = builder.getOrCreateCategory(Component.translatable("config.reliable_advancements.category.editing"));

        editing.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_advancements.enableEditMode"), config.enableEditMode)
                .setDefaultValue(defaults.enableEditMode)
                .setTooltip(Component.translatable("config.reliable_advancements.enableEditMode.tooltip"))
                .setSaveConsumer(newValue -> {
                    boolean previous = config.enableEditMode;
                    config.enableEditMode = newValue;

                    if (previous && !newValue) {
                        EnhancedAdvancementsScreen.clientHasFullTree = false;
                        Services.PLATFORM.sendAdvancementJsonRequest(new RequestAdvancementJsonPayload(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "resync"), "Resync"));
                    } else if (!previous && newValue) {
                        EnhancedAdvancementsScreen.clientHasFullTree = true;
                        Services.PLATFORM.sendRequestFullTree();
                    }
                })
                .build());

        editing.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_advancements.showTooltipsInEditMode"), config.showTooltipsInEditMode)
                .setDefaultValue(defaults.showTooltipsInEditMode)
                .setTooltip(Component.translatable("config.reliable_advancements.showTooltipsInEditMode.tooltip"))
                .setSaveConsumer(newValue -> config.showTooltipsInEditMode = newValue)
                .build());

        editing.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_advancements.showEditModeButton"), config.showEditModeButton)
                .setDefaultValue(defaults.showEditModeButton)
                .setTooltip(Component.translatable("config.reliable_advancements.showEditModeButton.tooltip"))
                .setSaveConsumer(newValue -> config.showEditModeButton = newValue)
                .build());

        ConfigCategory visuals = builder.getOrCreateCategory(Component.translatable("config.reliable_advancements.category.visuals"));

        visuals.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_advancements.blurBackground"), config.blurBackground)
                .setDefaultValue(defaults.blurBackground)
                .setTooltip(Component.translatable("config.reliable_advancements.blurBackground.tooltip"))
                .setSaveConsumer(newValue -> config.blurBackground = newValue)
                .build());

        visuals.addEntry(entryBuilder.startIntSlider(Component.translatable("config.reliable_advancements.blurBackgroundOpacity"), config.blurBackgroundOpacity, 0, 100)
                .setDefaultValue(defaults.blurBackgroundOpacity)
                .setTooltip(Component.translatable("config.reliable_advancements.blurBackgroundOpacity.tooltip"))
                .setSaveConsumer(newValue -> config.blurBackgroundOpacity = newValue)
                .build());

        visuals.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_advancements.drawArrows"), config.drawArrows)
                .setDefaultValue(defaults.drawArrows)
                .setTooltip(Component.translatable("config.reliable_advancements.drawArrows.tooltip"))
                .setSaveConsumer(newValue -> config.drawArrows = newValue)
                .build());

        visuals.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_advancements.drawDirectLines"), config.defaultDrawDirectLines)
                .setDefaultValue(defaults.defaultDrawDirectLines)
                .setTooltip(Component.translatable("config.reliable_advancements.drawDirectLines.tooltip"))
                .setSaveConsumer(newValue -> config.defaultDrawDirectLines = newValue)
                .build());

        visuals.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.reliable_advancements.hideLines"), config.defaultHideLines)
                .setDefaultValue(defaults.defaultHideLines)
                .setTooltip(Component.translatable("config.reliable_advancements.hideLines.tooltip"))
                .setSaveConsumer(newValue -> config.defaultHideLines = newValue)
                .build());

        visuals.addEntry(entryBuilder.startAlphaColorField(Component.translatable("config.reliable_advancements.defaultCompletedLineColor"), ColorHelper.RGB(config.defaultCompletedLineColor))
                .setDefaultValue(ColorHelper.RGB(defaults.defaultCompletedLineColor))
                .setTooltip(Component.translatable("config.reliable_advancements.defaultCompletedLineColor.tooltip"))
                .setSaveConsumer(newValue -> config.defaultCompletedLineColor = ColorHelper.asRGBString(newValue))
                .build());

        visuals.addEntry(entryBuilder.startAlphaColorField(Component.translatable("config.reliable_advancements.defaultUncompletedLineColor"), ColorHelper.RGB(config.defaultUncompletedLineColor))
                .setDefaultValue(ColorHelper.RGB(defaults.defaultUncompletedLineColor))
                .setTooltip(Component.translatable("config.reliable_advancements.defaultUncompletedLineColor.tooltip"))
                .setSaveConsumer(newValue -> config.defaultUncompletedLineColor = ColorHelper.asRGBString(newValue))
                .build());

        visuals.addEntry(entryBuilder.startAlphaColorField(Component.translatable("config.reliable_advancements.defaultCompletedIconColor"), ColorHelper.RGB(config.defaultCompletedIconColor))
                .setDefaultValue(ColorHelper.RGB(defaults.defaultCompletedIconColor))
                .setTooltip(Component.translatable("config.reliable_advancements.defaultCompletedIconColor.tooltip"))
                .setSaveConsumer(newValue -> config.defaultCompletedIconColor = ColorHelper.asRGBString(newValue))
                .build());

        visuals.addEntry(entryBuilder.startAlphaColorField(Component.translatable("config.reliable_advancements.defaultUncompletedIconColor"), ColorHelper.RGB(config.defaultUncompletedIconColor))
                .setDefaultValue(ColorHelper.RGB(defaults.defaultUncompletedIconColor))
                .setTooltip(Component.translatable("config.reliable_advancements.defaultUncompletedIconColor.tooltip"))
                .setSaveConsumer(newValue -> config.defaultUncompletedIconColor = ColorHelper.asRGBString(newValue))
                .build());

        visuals.addEntry(entryBuilder.startAlphaColorField(Component.translatable("config.reliable_advancements.defaultCompletedTitleColor"), ColorHelper.RGB(config.defaultCompletedTitleColor))
                .setDefaultValue(ColorHelper.RGB(defaults.defaultCompletedTitleColor))
                .setTooltip(Component.translatable("config.reliable_advancements.defaultCompletedTitleColor.tooltip"))
                .setSaveConsumer(newValue -> config.defaultCompletedTitleColor = ColorHelper.asRGBString(newValue))
                .build());

        visuals.addEntry(entryBuilder.startAlphaColorField(Component.translatable("config.reliable_advancements.defaultUncompletedTitleColor"), ColorHelper.RGB(config.defaultUncompletedTitleColor))
                .setDefaultValue(ColorHelper.RGB(defaults.defaultUncompletedTitleColor))
                .setTooltip(Component.translatable("config.reliable_advancements.defaultUncompletedTitleColor.tooltip"))
                .setSaveConsumer(newValue -> config.defaultUncompletedTitleColor = ColorHelper.asRGBString(newValue))
                .build());

        builder.setSavingRunnable(platformSaveAction);

        return builder.build();
    }
}