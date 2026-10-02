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
import net.minecraft.resources.ResourceLocation;

import java.awt.Color;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class YaclConfigScreen {

    private YaclConfigScreen() {
    }

    public static Screen create(Screen parent, Runnable platformSaveAction) {
        ModConfig defaults = new ModConfig();
        ModConfig config = ModConfig.get();

        ConfigCategory general = ConfigCategory.createBuilder()
                .name(Component.translatable("config.reliable_advancements.category.general"))
                .option(bool("doFade", defaults.doFade, () -> config.doFade, v -> config.doFade = v))
                .option(Option.<CriteriaDetail>createBuilder()
                        .name(name("criteriaDetail"))
                        .description(tooltip("criteriaDetail"))
                        .binding(CriteriaDetail.fromName(defaults.criteriaDetail),
                                () -> CriteriaDetail.fromName(config.criteriaDetail),
                                v -> config.criteriaDetail = v.getName())
                        .controller(opt -> EnumControllerBuilder.create(opt)
                                .enumClass(CriteriaDetail.class)
                                .formatValue(v -> name("criteriaDetail." + v.getName().toLowerCase())))
                        .build())
                .option(bool("requiresShift", defaults.requiresShift, () -> config.requiresShift, v -> config.requiresShift = v))
                .option(bool("addToInventory", defaults.addToInventory, () -> config.addToInventory, v -> config.addToInventory = v))
                .option(bool("showDebugCoordinates", defaults.showDebugCoordinates, () -> config.showDebugCoordinates, v -> config.showDebugCoordinates = v))
                .option(bool("orderTabsAlphabetically", defaults.orderTabsAlphabetically, () -> config.orderTabsAlphabetically, v -> config.orderTabsAlphabetically = v))
                .option(slider("uiScaling", 1, 100, defaults.uiScaling, () -> config.uiScaling, v -> config.uiScaling = v))
                .option(bool("onlyUseAboveAdvancementTabs", defaults.onlyUseAboveAdvancementTabs, () -> config.onlyUseAboveAdvancementTabs, v -> config.onlyUseAboveAdvancementTabs = v))
                .option(Option.<InventoryButtonStyle>createBuilder()
                        .name(name("inventoryButtonStyle"))
                        .description(tooltip("inventoryButtonStyle"))
                        .binding(defaults.inventoryButtonStyle, () -> config.inventoryButtonStyle, v -> config.inventoryButtonStyle = v)
                        .controller(opt -> EnumControllerBuilder.create(opt)
                                .enumClass(InventoryButtonStyle.class)
                                .formatValue(v -> name("inventoryButtonStyle." + v.name().toLowerCase())))
                        .build())
                .option(bool("enableButtonTooltip", defaults.enableButtonTooltip, () -> config.enableButtonTooltip, v -> config.enableButtonTooltip = v))
                .option(intField("inventoryButtonOffsetX", defaults.inventoryButtonOffsetX, () -> config.inventoryButtonOffsetX, v -> config.inventoryButtonOffsetX = v))
                .option(intField("inventoryButtonOffsetY", defaults.inventoryButtonOffsetY, () -> config.inventoryButtonOffsetY, v -> config.inventoryButtonOffsetY = v))
                .option(string("customInventoryButtonTexture", defaults.customInventoryButtonTexture, () -> config.customInventoryButtonTexture, v -> config.customInventoryButtonTexture = v))
                .option(string("customInventoryButtonTextureHovered", defaults.customInventoryButtonTextureHovered, () -> config.customInventoryButtonTextureHovered, v -> config.customInventoryButtonTextureHovered = v))
                .option(string("customInventoryButtonIcon", defaults.customInventoryButtonIcon, () -> config.customInventoryButtonIcon, v -> config.customInventoryButtonIcon = v))
                .option(bool("discoveryMode", defaults.discoveryMode, () -> config.discoveryMode, v -> config.discoveryMode = v))
                .option(bool("requireRewardClaiming", defaults.requireRewardClaiming, () -> config.requireRewardClaiming, v -> config.requireRewardClaiming = v))
                .build();

        ConfigCategory editing = ConfigCategory.createBuilder()
                .name(Component.translatable("config.reliable_advancements.category.editing"))
                .option(bool("enableEditMode", defaults.enableEditMode, () -> config.enableEditMode, newValue -> {
                    boolean previous = config.enableEditMode;
                    config.enableEditMode = newValue;

                    if (previous && !newValue) {
                        EnhancedAdvancementsScreen.clientHasFullTree = false;
                        Services.PLATFORM.sendAdvancementJsonRequest(new RequestAdvancementJsonPayload(new ResourceLocation(Constants.MOD_ID, "resync"), "Resync"));
                    } else if (!previous && newValue) {
                        EnhancedAdvancementsScreen.clientHasFullTree = true;
                        Services.PLATFORM.sendRequestFullTree();
                    }
                }))
                .option(bool("showTooltipsInEditMode", defaults.showTooltipsInEditMode, () -> config.showTooltipsInEditMode, v -> config.showTooltipsInEditMode = v))
                .option(bool("showEditModeButton", defaults.showEditModeButton, () -> config.showEditModeButton, v -> config.showEditModeButton = v))
                .build();

        ConfigCategory visuals = ConfigCategory.createBuilder()
                .name(Component.translatable("config.reliable_advancements.category.visuals"))
                .option(bool("blurBackground", defaults.blurBackground, () -> config.blurBackground, v -> config.blurBackground = v))
                .option(slider("blurBackgroundOpacity", 0, 100, defaults.blurBackgroundOpacity, () -> config.blurBackgroundOpacity, v -> config.blurBackgroundOpacity = v))
                .option(bool("drawArrows", defaults.drawArrows, () -> config.drawArrows, v -> config.drawArrows = v))
                .option(bool("drawDirectLines", defaults.defaultDrawDirectLines, () -> config.defaultDrawDirectLines, v -> config.defaultDrawDirectLines = v))
                .option(bool("hideLines", defaults.defaultHideLines, () -> config.defaultHideLines, v -> config.defaultHideLines = v))
                .option(color("defaultCompletedLineColor", defaults.defaultCompletedLineColor, () -> config.defaultCompletedLineColor, v -> config.defaultCompletedLineColor = v))
                .option(color("defaultUncompletedLineColor", defaults.defaultUncompletedLineColor, () -> config.defaultUncompletedLineColor, v -> config.defaultUncompletedLineColor = v))
                .option(color("defaultCompletedIconColor", defaults.defaultCompletedIconColor, () -> config.defaultCompletedIconColor, v -> config.defaultCompletedIconColor = v))
                .option(color("defaultUncompletedIconColor", defaults.defaultUncompletedIconColor, () -> config.defaultUncompletedIconColor, v -> config.defaultUncompletedIconColor = v))
                .option(color("defaultCompletedTitleColor", defaults.defaultCompletedTitleColor, () -> config.defaultCompletedTitleColor, v -> config.defaultCompletedTitleColor = v))
                .option(color("defaultUncompletedTitleColor", defaults.defaultUncompletedTitleColor, () -> config.defaultUncompletedTitleColor, v -> config.defaultUncompletedTitleColor = v))
                .build();

        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("config.reliable_advancements.title"))
                .category(general)
                .category(editing)
                .category(visuals)
                .save(platformSaveAction)
                .build()
                .generateScreen(parent);
    }

    private static Component name(String key) {
        return Component.translatable("config.reliable_advancements." + key);
    }

    private static OptionDescription tooltip(String key) {
        return OptionDescription.of(Component.translatable("config.reliable_advancements." + key + ".tooltip"));
    }

    private static Option<Boolean> bool(String key, boolean def, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return Option.<Boolean>createBuilder()
                .name(name(key))
                .description(tooltip(key))
                .binding(def, getter, setter)
                .controller(TickBoxControllerBuilder::create)
                .build();
    }

    private static Option<Integer> slider(String key, int min, int max, int def, Supplier<Integer> getter, Consumer<Integer> setter) {
        return Option.<Integer>createBuilder()
                .name(name(key))
                .description(tooltip(key))
                .binding(def, getter, setter)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(min, max).step(1))
                .build();
    }

    private static Option<Integer> intField(String key, int def, Supplier<Integer> getter, Consumer<Integer> setter) {
        return Option.<Integer>createBuilder()
                .name(name(key))
                .description(tooltip(key))
                .binding(def, getter, setter)
                .controller(IntegerFieldControllerBuilder::create)
                .build();
    }

    private static Option<String> string(String key, String def, Supplier<String> getter, Consumer<String> setter) {
        return Option.<String>createBuilder()
                .name(name(key))
                .description(tooltip(key))
                .binding(def, getter, setter)
                .controller(StringControllerBuilder::create)
                .build();
    }

    private static Option<Color> color(String key, String def, Supplier<String> getter, Consumer<String> setter) {
        return Option.<Color>createBuilder()
                .name(name(key))
                .description(tooltip(key))
                .binding(toColor(def), () -> toColor(getter.get()), v -> setter.accept(ColorHelper.asRGBString(v.getRGB())))
                .controller(ColorControllerBuilder::create)
                .build();
    }

    private static Color toColor(String hex) {
        try {
            return new Color(ColorHelper.RGB(hex) & 0xFFFFFF);
        } catch (IllegalArgumentException e) {
            return Color.WHITE;
        }
    }
}
