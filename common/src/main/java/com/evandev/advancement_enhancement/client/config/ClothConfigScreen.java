package com.evandev.advancement_enhancement.client.config;

import com.evandev.advancement_enhancement.config.InventoryButtonStyle;
import com.evandev.advancement_enhancement.config.ModConfig;
import com.evandev.advancement_enhancement.gui.screens.EnhancedAdvancementsScreen;
import com.evandev.advancement_enhancement.network.RequestAdvancementJsonPayload;
import com.evandev.advancement_enhancement.platform.Services;
import com.evandev.advancement_enhancement.reference.Constants;
import com.evandev.advancement_enhancement.util.ColorHelper;
import com.evandev.advancement_enhancement.util.CriteriaDetail;
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
                .setTitle(Component.translatable("config.advancement_enhancement.title"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("config.advancement_enhancement.category.general"));
        ModConfig config = ModConfig.get();

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.advancement_enhancement.doFade"), config.doFade)
                .setDefaultValue(defaults.doFade)
                .setTooltip(Component.translatable("config.advancement_enhancement.doFade.tooltip"))
                .setSaveConsumer(newValue -> config.doFade = newValue)
                .build());

        general.addEntry(entryBuilder.startDropdownMenu(Component.translatable("config.advancement_enhancement.criteriaDetail"),
                        CriteriaDetail.fromName(config.criteriaDetail),
                        CriteriaDetail::fromName,
                        o -> Component.translatable("config.advancement_enhancement.criteriaDetail." + o.getName().toLowerCase()))
                .setSelections(CriteriaDetail.valuesAsList())
                .setDefaultValue(CriteriaDetail.fromName(defaults.criteriaDetail))
                .setTooltip(Component.translatable("config.advancement_enhancement.criteriaDetail.tooltip"))
                .setSaveConsumer(newValue -> config.criteriaDetail = newValue.getName())
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.advancement_enhancement.requiresShift"), config.requiresShift)
                .setDefaultValue(defaults.requiresShift)
                .setTooltip(Component.translatable("config.advancement_enhancement.requiresShift.tooltip"))
                .setSaveConsumer(newValue -> config.requiresShift = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.advancement_enhancement.addToInventory"), config.addToInventory)
                .setDefaultValue(defaults.addToInventory)
                .setTooltip(Component.translatable("config.advancement_enhancement.addToInventory.tooltip"))
                .setSaveConsumer(newValue -> config.addToInventory = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.advancement_enhancement.showDebugCoordinates"), config.showDebugCoordinates)
                .setDefaultValue(defaults.showDebugCoordinates)
                .setTooltip(Component.translatable("config.advancement_enhancement.showDebugCoordinates.tooltip"))
                .setSaveConsumer(newValue -> config.showDebugCoordinates = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.advancement_enhancement.orderTabsAlphabetically"), config.orderTabsAlphabetically)
                .setDefaultValue(defaults.orderTabsAlphabetically)
                .setTooltip(Component.translatable("config.advancement_enhancement.orderTabsAlphabetically.tooltip"))
                .setSaveConsumer(newValue -> config.orderTabsAlphabetically = newValue)
                .build());

        general.addEntry(entryBuilder.startIntSlider(Component.translatable("config.advancement_enhancement.uiScaling"), config.uiScaling, 1, 100)
                .setDefaultValue(defaults.uiScaling)
                .setTooltip(Component.translatable("config.advancement_enhancement.uiScaling.tooltip"))
                .setSaveConsumer(newValue -> config.uiScaling = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.advancement_enhancement.onlyUseAboveAdvancementTabs"), config.onlyUseAboveAdvancementTabs)
                .setDefaultValue(defaults.onlyUseAboveAdvancementTabs)
                .setTooltip(Component.translatable("config.advancement_enhancement.onlyUseAboveAdvancementTabs.tooltip"))
                .setSaveConsumer(newValue -> config.onlyUseAboveAdvancementTabs = newValue)
                .build());

        general.addEntry(entryBuilder.startEnumSelector(Component.translatable("config.advancement_enhancement.inventoryButtonStyle"),
                        InventoryButtonStyle.class, config.inventoryButtonStyle)
                .setDefaultValue(defaults.inventoryButtonStyle)
                .setTooltip(Component.translatable("config.advancement_enhancement.inventoryButtonStyle.tooltip"))
                .setSaveConsumer(newValue -> config.inventoryButtonStyle = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.advancement_enhancement.enableButtonTooltip"), config.enableButtonTooltip)
                .setDefaultValue(defaults.enableButtonTooltip)
                .setTooltip(Component.translatable("config.advancement_enhancement.enableButtonTooltip.tooltip"))
                .setSaveConsumer(newValue -> config.enableButtonTooltip = newValue)
                .build());

        general.addEntry(entryBuilder.startIntField(Component.translatable("config.advancement_enhancement.inventoryButtonOffsetX"), config.inventoryButtonOffsetX)
                .setDefaultValue(defaults.inventoryButtonOffsetX)
                .setTooltip(Component.translatable("config.advancement_enhancement.inventoryButtonOffsetX.tooltip"))
                .setSaveConsumer(newValue -> config.inventoryButtonOffsetX = newValue)
                .build());

        general.addEntry(entryBuilder.startIntField(Component.translatable("config.advancement_enhancement.inventoryButtonOffsetY"), config.inventoryButtonOffsetY)
                .setDefaultValue(defaults.inventoryButtonOffsetY)
                .setTooltip(Component.translatable("config.advancement_enhancement.inventoryButtonOffsetY.tooltip"))
                .setSaveConsumer(newValue -> config.inventoryButtonOffsetY = newValue)
                .build());

        general.addEntry(entryBuilder.startStrField(Component.translatable("config.advancement_enhancement.customInventoryButtonTexture"), config.customInventoryButtonTexture)
                .setDefaultValue(defaults.customInventoryButtonTexture)
                .setTooltip(Component.translatable("config.advancement_enhancement.customInventoryButtonTexture.tooltip"))
                .setSaveConsumer(newValue -> config.customInventoryButtonTexture = newValue)
                .build());

        general.addEntry(entryBuilder.startStrField(Component.translatable("config.advancement_enhancement.customInventoryButtonTextureHovered"), config.customInventoryButtonTextureHovered)
                .setDefaultValue(defaults.customInventoryButtonTextureHovered)
                .setTooltip(Component.translatable("config.advancement_enhancement.customInventoryButtonTextureHovered.tooltip"))
                .setSaveConsumer(newValue -> config.customInventoryButtonTextureHovered = newValue)
                .build());

        general.addEntry(entryBuilder.startStrField(Component.translatable("config.advancement_enhancement.customInventoryButtonIcon"), config.customInventoryButtonIcon)
                .setDefaultValue(defaults.customInventoryButtonIcon)
                .setTooltip(Component.translatable("config.advancement_enhancement.customInventoryButtonIcon.tooltip"))
                .setSaveConsumer(newValue -> config.customInventoryButtonIcon = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.advancement_enhancement.discoveryMode"), config.discoveryMode)
                .setDefaultValue(defaults.discoveryMode)
                .setTooltip(Component.translatable("config.advancement_enhancement.discoveryMode.tooltip"))
                .setSaveConsumer(newValue -> config.discoveryMode = newValue)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.advancement_enhancement.requireRewardClaiming"), config.requireRewardClaiming)
                .setDefaultValue(defaults.requireRewardClaiming)
                .setTooltip(Component.translatable("config.advancement_enhancement.requireRewardClaiming.tooltip"))
                .setSaveConsumer(newValue -> config.requireRewardClaiming = newValue)
                .build());

        ConfigCategory editing = builder.getOrCreateCategory(Component.translatable("config.advancement_enhancement.category.editing"));

        editing.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.advancement_enhancement.enableEditMode"), config.enableEditMode)
                .setDefaultValue(defaults.enableEditMode)
                .setTooltip(Component.translatable("config.advancement_enhancement.enableEditMode.tooltip"))
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

        editing.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.advancement_enhancement.showTooltipsInEditMode"), config.showTooltipsInEditMode)
                .setDefaultValue(defaults.showTooltipsInEditMode)
                .setTooltip(Component.translatable("config.advancement_enhancement.showTooltipsInEditMode.tooltip"))
                .setSaveConsumer(newValue -> config.showTooltipsInEditMode = newValue)
                .build());

        editing.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.advancement_enhancement.showEditModeButton"), config.showEditModeButton)
                .setDefaultValue(defaults.showEditModeButton)
                .setTooltip(Component.translatable("config.advancement_enhancement.showEditModeButton.tooltip"))
                .setSaveConsumer(newValue -> config.showEditModeButton = newValue)
                .build());

        ConfigCategory visuals = builder.getOrCreateCategory(Component.translatable("config.advancement_enhancement.category.visuals"));

        visuals.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.advancement_enhancement.drawArrows"), config.drawArrows)
                .setDefaultValue(defaults.drawArrows)
                .setTooltip(Component.translatable("config.advancement_enhancement.drawArrows.tooltip"))
                .setSaveConsumer(newValue -> config.drawArrows = newValue)
                .build());

        visuals.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.advancement_enhancement.drawDirectLines"), config.defaultDrawDirectLines)
                .setDefaultValue(defaults.defaultDrawDirectLines)
                .setTooltip(Component.translatable("config.advancement_enhancement.drawDirectLines.tooltip"))
                .setSaveConsumer(newValue -> config.defaultDrawDirectLines = newValue)
                .build());

        visuals.addEntry(entryBuilder.startBooleanToggle(Component.translatable("config.advancement_enhancement.hideLines"), config.defaultHideLines)
                .setDefaultValue(defaults.defaultHideLines)
                .setTooltip(Component.translatable("config.advancement_enhancement.hideLines.tooltip"))
                .setSaveConsumer(newValue -> config.defaultHideLines = newValue)
                .build());

        visuals.addEntry(entryBuilder.startAlphaColorField(Component.translatable("config.advancement_enhancement.defaultCompletedLineColor"), ColorHelper.RGB(config.defaultCompletedLineColor))
                .setDefaultValue(ColorHelper.RGB(defaults.defaultCompletedLineColor))
                .setTooltip(Component.translatable("config.advancement_enhancement.defaultCompletedLineColor.tooltip"))
                .setSaveConsumer(newValue -> config.defaultCompletedLineColor = ColorHelper.asRGBString(newValue))
                .build());

        visuals.addEntry(entryBuilder.startAlphaColorField(Component.translatable("config.advancement_enhancement.defaultUncompletedLineColor"), ColorHelper.RGB(config.defaultUncompletedLineColor))
                .setDefaultValue(ColorHelper.RGB(defaults.defaultUncompletedLineColor))
                .setTooltip(Component.translatable("config.advancement_enhancement.defaultUncompletedLineColor.tooltip"))
                .setSaveConsumer(newValue -> config.defaultUncompletedLineColor = ColorHelper.asRGBString(newValue))
                .build());

        visuals.addEntry(entryBuilder.startAlphaColorField(Component.translatable("config.advancement_enhancement.defaultCompletedIconColor"), ColorHelper.RGB(config.defaultCompletedIconColor))
                .setDefaultValue(ColorHelper.RGB(defaults.defaultCompletedIconColor))
                .setTooltip(Component.translatable("config.advancement_enhancement.defaultCompletedIconColor.tooltip"))
                .setSaveConsumer(newValue -> config.defaultCompletedIconColor = ColorHelper.asRGBString(newValue))
                .build());

        visuals.addEntry(entryBuilder.startAlphaColorField(Component.translatable("config.advancement_enhancement.defaultUncompletedIconColor"), ColorHelper.RGB(config.defaultUncompletedIconColor))
                .setDefaultValue(ColorHelper.RGB(defaults.defaultUncompletedIconColor))
                .setTooltip(Component.translatable("config.advancement_enhancement.defaultUncompletedIconColor.tooltip"))
                .setSaveConsumer(newValue -> config.defaultUncompletedIconColor = ColorHelper.asRGBString(newValue))
                .build());

        visuals.addEntry(entryBuilder.startAlphaColorField(Component.translatable("config.advancement_enhancement.defaultCompletedTitleColor"), ColorHelper.RGB(config.defaultCompletedTitleColor))
                .setDefaultValue(ColorHelper.RGB(defaults.defaultCompletedTitleColor))
                .setTooltip(Component.translatable("config.advancement_enhancement.defaultCompletedTitleColor.tooltip"))
                .setSaveConsumer(newValue -> config.defaultCompletedTitleColor = ColorHelper.asRGBString(newValue))
                .build());

        visuals.addEntry(entryBuilder.startAlphaColorField(Component.translatable("config.advancement_enhancement.defaultUncompletedTitleColor"), ColorHelper.RGB(config.defaultUncompletedTitleColor))
                .setDefaultValue(ColorHelper.RGB(defaults.defaultUncompletedTitleColor))
                .setTooltip(Component.translatable("config.advancement_enhancement.defaultUncompletedTitleColor.tooltip"))
                .setSaveConsumer(newValue -> config.defaultUncompletedTitleColor = ColorHelper.asRGBString(newValue))
                .build());

        builder.setSavingRunnable(platformSaveAction);

        return builder.build();
    }
}