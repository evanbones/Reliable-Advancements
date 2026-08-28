package com.evandev.reliable_advancements.client.config;

import com.evandev.reliable_advancements.config.InventoryButtonStyle;
import com.evandev.reliable_advancements.config.ModConfig;
import com.evandev.reliable_advancements.gui.screens.EnhancedAdvancementsScreen;
import com.evandev.reliable_advancements.network.RequestAdvancementJsonPayload;
import com.evandev.reliable_advancements.platform.Services;
import com.evandev.reliable_advancements.reference.Constants;
import com.evandev.reliable_advancements.util.ColorHelper;
import com.evandev.reliable_advancements.util.CriteriaDetail;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.awt.Color;

public class YaclConfigScreen {

    public static Screen create(Screen parent, Runnable platformSaveAction) {
        ModConfig defaults = new ModConfig();
        ModConfig config = ModConfig.get();

        YetAnotherConfigLib.Builder builder = YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("config.reliable_advancements.title"))
                .save(() -> {
                    ModConfig.save();
                    if (platformSaveAction != null) {
                        platformSaveAction.run();
                    }
                });

        ConfigCategory.Builder generalCategory = ConfigCategory.createBuilder()
                .name(Component.translatable("config.reliable_advancements.category.general"));

        generalCategory.option(Option.<Boolean>createBuilder()
                .name(Component.translatable("config.reliable_advancements.doFade"))
                .description(OptionDescription.of(Component.translatable("config.reliable_advancements.doFade.tooltip")))
                .binding(defaults.doFade, () -> config.doFade, val -> config.doFade = val)
                .controller(TickBoxControllerBuilder::create)
                .build());

        generalCategory.option(Option.<CriteriaDetail>createBuilder()
                .name(Component.translatable("config.reliable_advancements.criteriaDetail"))
                .description(OptionDescription.of(Component.translatable("config.reliable_advancements.criteriaDetail.tooltip")))
                .binding(CriteriaDetail.fromName(defaults.criteriaDetail),
                        () -> CriteriaDetail.fromName(config.criteriaDetail),
                        val -> config.criteriaDetail = val.getName())
                .controller(opt -> EnumControllerBuilder.create(opt)
                        .enumClass(CriteriaDetail.class)
                        .formatValue(val -> Component.translatable("config.reliable_advancements.criteriaDetail." + val.getName().toLowerCase())))
                .build());

        generalCategory.option(Option.<Boolean>createBuilder()
                .name(Component.translatable("config.reliable_advancements.requiresShift"))
                .description(OptionDescription.of(Component.translatable("config.reliable_advancements.requiresShift.tooltip")))
                .binding(defaults.requiresShift, () -> config.requiresShift, val -> config.requiresShift = val)
                .controller(TickBoxControllerBuilder::create)
                .build());

        generalCategory.option(Option.<Boolean>createBuilder()
                .name(Component.translatable("config.reliable_advancements.addToInventory"))
                .description(OptionDescription.of(Component.translatable("config.reliable_advancements.addToInventory.tooltip")))
                .binding(defaults.addToInventory, () -> config.addToInventory, val -> config.addToInventory = val)
                .controller(TickBoxControllerBuilder::create)
                .build());

        generalCategory.option(Option.<Boolean>createBuilder()
                .name(Component.translatable("config.reliable_advancements.showDebugCoordinates"))
                .description(OptionDescription.of(Component.translatable("config.reliable_advancements.showDebugCoordinates.tooltip")))
                .binding(defaults.showDebugCoordinates, () -> config.showDebugCoordinates, val -> config.showDebugCoordinates = val)
                .controller(TickBoxControllerBuilder::create)
                .build());

        generalCategory.option(Option.<Boolean>createBuilder()
                .name(Component.translatable("config.reliable_advancements.orderTabsAlphabetically"))
                .description(OptionDescription.of(Component.translatable("config.reliable_advancements.orderTabsAlphabetically.tooltip")))
                .binding(defaults.orderTabsAlphabetically, () -> config.orderTabsAlphabetically, val -> config.orderTabsAlphabetically = val)
                .controller(TickBoxControllerBuilder::create)
                .build());

        generalCategory.option(Option.<Integer>createBuilder()
                .name(Component.translatable("config.reliable_advancements.uiScaling"))
                .description(OptionDescription.of(Component.translatable("config.reliable_advancements.uiScaling.tooltip")))
                .binding(defaults.uiScaling, () -> config.uiScaling, val -> config.uiScaling = val)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(1, 100).step(1))
                .build());

        generalCategory.option(Option.<Boolean>createBuilder()
                .name(Component.translatable("config.reliable_advancements.onlyUseAboveAdvancementTabs"))
                .description(OptionDescription.of(Component.translatable("config.reliable_advancements.onlyUseAboveAdvancementTabs.tooltip")))
                .binding(defaults.onlyUseAboveAdvancementTabs, () -> config.onlyUseAboveAdvancementTabs, val -> config.onlyUseAboveAdvancementTabs = val)
                .controller(TickBoxControllerBuilder::create)
                .build());

        generalCategory.option(Option.<InventoryButtonStyle>createBuilder()
                .name(Component.translatable("config.reliable_advancements.inventoryButtonStyle"))
                .description(OptionDescription.of(Component.translatable("config.reliable_advancements.inventoryButtonStyle.tooltip")))
                .binding(defaults.inventoryButtonStyle, () -> config.inventoryButtonStyle, val -> config.inventoryButtonStyle = val)
                .controller(opt -> EnumControllerBuilder.create(opt).enumClass(InventoryButtonStyle.class))
                .build());

        generalCategory.option(Option.<Boolean>createBuilder()
                .name(Component.translatable("config.reliable_advancements.enableButtonTooltip"))
                .description(OptionDescription.of(Component.translatable("config.reliable_advancements.enableButtonTooltip.tooltip")))
                .binding(defaults.enableButtonTooltip, () -> config.enableButtonTooltip, val -> config.enableButtonTooltip = val)
                .controller(TickBoxControllerBuilder::create)
                .build());

        generalCategory.option(Option.<Integer>createBuilder()
                .name(Component.translatable("config.reliable_advancements.inventoryButtonOffsetX"))
                .description(OptionDescription.of(Component.translatable("config.reliable_advancements.inventoryButtonOffsetX.tooltip")))
                .binding(defaults.inventoryButtonOffsetX, () -> config.inventoryButtonOffsetX, val -> config.inventoryButtonOffsetX = val)
                .controller(IntegerFieldControllerBuilder::create)
                .build());

        generalCategory.option(Option.<Integer>createBuilder()
                .name(Component.translatable("config.reliable_advancements.inventoryButtonOffsetY"))
                .description(OptionDescription.of(Component.translatable("config.reliable_advancements.inventoryButtonOffsetY.tooltip")))
                .binding(defaults.inventoryButtonOffsetY, () -> config.inventoryButtonOffsetY, val -> config.inventoryButtonOffsetY = val)
                .controller(IntegerFieldControllerBuilder::create)
                .build());

        generalCategory.option(Option.<String>createBuilder()
                .name(Component.translatable("config.reliable_advancements.customInventoryButtonTexture"))
                .description(OptionDescription.of(Component.translatable("config.reliable_advancements.customInventoryButtonTexture.tooltip")))
                .binding(defaults.customInventoryButtonTexture, () -> config.customInventoryButtonTexture, val -> config.customInventoryButtonTexture = val)
                .controller(StringControllerBuilder::create)
                .build());

        generalCategory.option(Option.<String>createBuilder()
                .name(Component.translatable("config.reliable_advancements.customInventoryButtonTextureHovered"))
                .description(OptionDescription.of(Component.translatable("config.reliable_advancements.customInventoryButtonTextureHovered.tooltip")))
                .binding(defaults.customInventoryButtonTextureHovered, () -> config.customInventoryButtonTextureHovered, val -> config.customInventoryButtonTextureHovered = val)
                .controller(StringControllerBuilder::create)
                .build());

        generalCategory.option(Option.<String>createBuilder()
                .name(Component.translatable("config.reliable_advancements.customInventoryButtonIcon"))
                .description(OptionDescription.of(Component.translatable("config.reliable_advancements.customInventoryButtonIcon.tooltip")))
                .binding(defaults.customInventoryButtonIcon, () -> config.customInventoryButtonIcon, val -> config.customInventoryButtonIcon = val)
                .controller(StringControllerBuilder::create)
                .build());

        generalCategory.option(Option.<Boolean>createBuilder()
                .name(Component.translatable("config.reliable_advancements.unclampedScrolling"))
                .description(OptionDescription.of(Component.translatable("config.reliable_advancements.unclampedScrolling.tooltip")))
                .binding(defaults.unclampedScrolling, () -> config.unclampedScrolling, val -> config.unclampedScrolling = val)
                .controller(TickBoxControllerBuilder::create)
                .build());

        generalCategory.option(Option.<Integer>createBuilder()
                .name(Component.translatable("config.reliable_advancements.visibilityDepth"))
                .description(OptionDescription.of(Component.translatable("config.reliable_advancements.visibilityDepth.tooltip")))
                .binding(defaults.visibilityDepth, () -> config.visibilityDepth, val -> config.visibilityDepth = val)
                .controller(opt -> IntegerFieldControllerBuilder.create(opt).min(-1))
                .build());

        generalCategory.option(Option.<Boolean>createBuilder()
                .name(Component.translatable("config.reliable_advancements.requireRewardClaiming"))
                .description(OptionDescription.of(Component.translatable("config.reliable_advancements.requireRewardClaiming.tooltip")))
                .binding(defaults.requireRewardClaiming, () -> config.requireRewardClaiming, val -> config.requireRewardClaiming = val)
                .controller(TickBoxControllerBuilder::create)
                .build());

        ConfigCategory.Builder editingCategory = ConfigCategory.createBuilder()
                .name(Component.translatable("config.reliable_advancements.category.editing"));

        editingCategory.option(Option.<Boolean>createBuilder()
                .name(Component.translatable("config.reliable_advancements.enableEditMode"))
                .description(OptionDescription.of(Component.translatable("config.reliable_advancements.enableEditMode.tooltip")))
                .binding(defaults.enableEditMode, () -> config.enableEditMode, newValue -> {
                    boolean previous = config.enableEditMode;
                    config.enableEditMode = newValue;

                    if (previous && !newValue) {
                        EnhancedAdvancementsScreen.clientHasFullTree = false;
                        Services.PLATFORM.sendAdvancementJsonRequest(new RequestAdvancementJsonPayload(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "resync"), "Resync"));
                    } else if (!previous && newValue) {
                        EnhancedAdvancementsScreen.clientHasFullTree = true;
                        Services.PLATFORM.sendRequestFullTree();
                    }
                })
                .controller(TickBoxControllerBuilder::create)
                .build());

        editingCategory.option(Option.<Boolean>createBuilder()
                .name(Component.translatable("config.reliable_advancements.showTooltipsInEditMode"))
                .description(OptionDescription.of(Component.translatable("config.reliable_advancements.showTooltipsInEditMode.tooltip")))
                .binding(defaults.showTooltipsInEditMode, () -> config.showTooltipsInEditMode, val -> config.showTooltipsInEditMode = val)
                .controller(TickBoxControllerBuilder::create)
                .build());

        editingCategory.option(Option.<Boolean>createBuilder()
                .name(Component.translatable("config.reliable_advancements.showEditModeButton"))
                .description(OptionDescription.of(Component.translatable("config.reliable_advancements.showEditModeButton.tooltip")))
                .binding(defaults.showEditModeButton, () -> config.showEditModeButton, val -> config.showEditModeButton = val)
                .controller(TickBoxControllerBuilder::create)
                .build());

        editingCategory.option(Option.<Boolean>createBuilder()
                .name(Component.translatable("config.reliable_advancements.storeAdvancementEditsGlobally"))
                .description(OptionDescription.of(Component.translatable("config.reliable_advancements.storeAdvancementEditsGlobally.tooltip")))
                .binding(defaults.storeAdvancementEditsGlobally, () -> config.storeAdvancementEditsGlobally, val -> config.storeAdvancementEditsGlobally = val)
                .controller(TickBoxControllerBuilder::create)
                .build());

        editingCategory.option(Option.<Boolean>createBuilder()
                .name(Component.translatable("config.reliable_advancements.storeAdvancementEditsAsDatapack"))
                .description(OptionDescription.of(Component.translatable("config.reliable_advancements.storeAdvancementEditsAsDatapack.tooltip")))
                .binding(defaults.storeAdvancementEditsAsDatapack, () -> config.storeAdvancementEditsAsDatapack, val -> config.storeAdvancementEditsAsDatapack = val)
                .controller(TickBoxControllerBuilder::create)
                .build());

        ConfigCategory.Builder visualsCategory = ConfigCategory.createBuilder()
                .name(Component.translatable("config.reliable_advancements.category.visuals"));

        visualsCategory.option(Option.<Boolean>createBuilder()
                .name(Component.translatable("config.reliable_advancements.blurBackground"))
                .description(OptionDescription.of(Component.translatable("config.reliable_advancements.blurBackground.tooltip")))
                .binding(defaults.blurBackground, () -> config.blurBackground, val -> config.blurBackground = val)
                .controller(TickBoxControllerBuilder::create)
                .build());

        visualsCategory.option(Option.<Integer>createBuilder()
                .name(Component.translatable("config.reliable_advancements.blurBackgroundOpacity"))
                .description(OptionDescription.of(Component.translatable("config.reliable_advancements.blurBackgroundOpacity.tooltip")))
                .binding(defaults.blurBackgroundOpacity, () -> config.blurBackgroundOpacity, val -> config.blurBackgroundOpacity = val)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(0, 100).step(1))
                .build());

        visualsCategory.option(Option.<Boolean>createBuilder()
                .name(Component.translatable("config.reliable_advancements.drawArrows"))
                .description(OptionDescription.of(Component.translatable("config.reliable_advancements.drawArrows.tooltip")))
                .binding(defaults.drawArrows, () -> config.drawArrows, val -> config.drawArrows = val)
                .controller(TickBoxControllerBuilder::create)
                .build());

        visualsCategory.option(Option.<Boolean>createBuilder()
                .name(Component.translatable("config.reliable_advancements.drawDirectLines"))
                .description(OptionDescription.of(Component.translatable("config.reliable_advancements.drawDirectLines.tooltip")))
                .binding(defaults.defaultDrawDirectLines, () -> config.defaultDrawDirectLines, val -> config.defaultDrawDirectLines = val)
                .controller(TickBoxControllerBuilder::create)
                .build());

        visualsCategory.option(Option.<Boolean>createBuilder()
                .name(Component.translatable("config.reliable_advancements.hideLines"))
                .description(OptionDescription.of(Component.translatable("config.reliable_advancements.hideLines.tooltip")))
                .binding(defaults.defaultHideLines, () -> config.defaultHideLines, val -> config.defaultHideLines = val)
                .controller(TickBoxControllerBuilder::create)
                .build());

        visualsCategory.option(Option.<Color>createBuilder()
                .name(Component.translatable("config.reliable_advancements.defaultCompletedLineColor"))
                .description(OptionDescription.of(Component.translatable("config.reliable_advancements.defaultCompletedLineColor.tooltip")))
                .binding(new Color(ColorHelper.RGB(defaults.defaultCompletedLineColor)),
                        () -> new Color(ColorHelper.RGB(config.defaultCompletedLineColor)),
                        val -> config.defaultCompletedLineColor = ColorHelper.asRGBString(val.getRGB()))
                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(false))
                .build());

        visualsCategory.option(Option.<Color>createBuilder()
                .name(Component.translatable("config.reliable_advancements.defaultUncompletedLineColor"))
                .description(OptionDescription.of(Component.translatable("config.reliable_advancements.defaultUncompletedLineColor.tooltip")))
                .binding(new Color(ColorHelper.RGB(defaults.defaultUncompletedLineColor)),
                        () -> new Color(ColorHelper.RGB(config.defaultUncompletedLineColor)),
                        val -> config.defaultUncompletedLineColor = ColorHelper.asRGBString(val.getRGB()))
                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(false))
                .build());

        visualsCategory.option(Option.<Color>createBuilder()
                .name(Component.translatable("config.reliable_advancements.defaultCompletedIconColor"))
                .description(OptionDescription.of(Component.translatable("config.reliable_advancements.defaultCompletedIconColor.tooltip")))
                .binding(new Color(ColorHelper.RGB(defaults.defaultCompletedIconColor)),
                        () -> new Color(ColorHelper.RGB(config.defaultCompletedIconColor)),
                        val -> config.defaultCompletedIconColor = ColorHelper.asRGBString(val.getRGB()))
                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(false))
                .build());

        visualsCategory.option(Option.<Color>createBuilder()
                .name(Component.translatable("config.reliable_advancements.defaultUncompletedIconColor"))
                .description(OptionDescription.of(Component.translatable("config.reliable_advancements.defaultUncompletedIconColor.tooltip")))
                .binding(new Color(ColorHelper.RGB(defaults.defaultUncompletedIconColor)),
                        () -> new Color(ColorHelper.RGB(config.defaultUncompletedIconColor)),
                        val -> config.defaultUncompletedIconColor = ColorHelper.asRGBString(val.getRGB()))
                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(false))
                .build());

        visualsCategory.option(Option.<Color>createBuilder()
                .name(Component.translatable("config.reliable_advancements.defaultCompletedTitleColor"))
                .description(OptionDescription.of(Component.translatable("config.reliable_advancements.defaultCompletedTitleColor.tooltip")))
                .binding(new Color(ColorHelper.RGB(defaults.defaultCompletedTitleColor)),
                        () -> new Color(ColorHelper.RGB(config.defaultCompletedTitleColor)),
                        val -> config.defaultCompletedTitleColor = ColorHelper.asRGBString(val.getRGB()))
                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(false))
                .build());

        visualsCategory.option(Option.<Color>createBuilder()
                .name(Component.translatable("config.reliable_advancements.defaultUncompletedTitleColor"))
                .description(OptionDescription.of(Component.translatable("config.reliable_advancements.defaultUncompletedTitleColor.tooltip")))
                .binding(new Color(ColorHelper.RGB(defaults.defaultUncompletedTitleColor)),
                        () -> new Color(ColorHelper.RGB(config.defaultUncompletedTitleColor)),
                        val -> config.defaultUncompletedTitleColor = ColorHelper.asRGBString(val.getRGB()))
                .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(false))
                .build());

        return builder
                .category(generalCategory.build())
                .category(editingCategory.build())
                .category(visualsCategory.build())
                .build()
                .generateScreen(parent);
    }
}
