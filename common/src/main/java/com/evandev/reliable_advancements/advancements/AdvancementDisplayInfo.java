package com.evandev.reliable_advancements.advancements;

import com.evandev.reliable_advancements.api.IDisplayInfo;
import com.evandev.reliable_advancements.config.ModConfig;
import com.evandev.reliable_advancements.util.ColorHelper;
import com.google.gson.JsonObject;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.screens.advancements.AdvancementWidgetType;
import net.minecraft.resources.Identifier;

public class AdvancementDisplayInfo implements IDisplayInfo {
    public static final String defaultMinecraftCompletedIconColor = "#DBA213", defaultMinecraftUncompletedIconColor = "#FFFFFF";
    public static final String defaultMinecraftCompletedTitleColor = "#DBA213", defaultMinecraftUncompletedTitleColor = "#0489C1";
    private static final int minecraftCompletedIconColor = ColorHelper.RGB(defaultMinecraftCompletedIconColor);
    private static final int minecraftUncompletedIconColor = ColorHelper.RGB(defaultMinecraftUncompletedIconColor);
    private static final int minecraftCompletedTitleColor = ColorHelper.RGB(defaultMinecraftCompletedTitleColor);
    private static final int minecraftUncompletedTitleColor = ColorHelper.RGB(defaultMinecraftUncompletedTitleColor);
    private static final int WHITE = ColorHelper.RGB(1F, 1F, 1F);
    private static int cachedDefaultCompletedIconColor;
    private static int cachedDefaultCompletedTitleColor;
    private static int cachedDefaultUncompletedIconColor;
    private static int cachedDefaultUncompletedTitleColor;
    private static int cachedDefaultUncompletedLineColor;
    private static int cachedDefaultCompletedLineColor;
    private static ModConfig lastConfig;
    private final Identifier id;
    private int completedIconColor, unCompletedIconColor;
    private int completedTitleColor, unCompletedTitleColor;
    private boolean drawDirectLines;
    private int completedLineColor, unCompletedLineColor;
    private Integer posX, posY;
    private boolean hideLines;
    private boolean allowDragging;

    public AdvancementDisplayInfo(AdvancementHolder advancementHolder) {
        this(advancementHolder.id());
        Advancement advancement = advancementHolder.value();

        if (advancement.display().isPresent()) {
            DisplayInfo displayInfo = advancement.display().get();
            if (displayInfo instanceof IDisplayInfo) {
                parseIDisplayInfo((IDisplayInfo) displayInfo);
            }
        }
    }

    public AdvancementDisplayInfo(Identifier id) {
        this.id = id;
        this.defaults();
    }

    public AdvancementDisplayInfo(Identifier id, JsonObject displayJson) {
        this(id);
        if (displayJson != null) {
            this.parseDisplayJson(displayJson);
        }
    }

    private static void ensureDefaultColorsCached() {
        ModConfig config = ModConfig.get();
        if (lastConfig != config) {
            lastConfig = config;
            cachedDefaultCompletedIconColor = ColorHelper.RGB(config.defaultCompletedIconColor);
            cachedDefaultCompletedTitleColor = ColorHelper.RGB(config.defaultCompletedTitleColor);
            cachedDefaultUncompletedIconColor = ColorHelper.RGB(config.defaultUncompletedIconColor);
            cachedDefaultUncompletedTitleColor = ColorHelper.RGB(config.defaultUncompletedTitleColor);
            cachedDefaultUncompletedLineColor = ColorHelper.RGB(config.defaultUncompletedLineColor);
            cachedDefaultCompletedLineColor = ColorHelper.RGB(config.defaultCompletedLineColor);
        }
    }

    private void defaults() {
        ensureDefaultColorsCached();
        this.completedIconColor = cachedDefaultCompletedIconColor;
        this.completedTitleColor = cachedDefaultCompletedTitleColor;
        this.unCompletedIconColor = cachedDefaultUncompletedIconColor;
        this.unCompletedTitleColor = cachedDefaultUncompletedTitleColor;
        this.drawDirectLines = ModConfig.get().defaultDrawDirectLines;
        this.unCompletedLineColor = cachedDefaultUncompletedLineColor;
        this.completedLineColor = cachedDefaultCompletedLineColor;
        this.posX = null;
        this.posY = null;
        this.hideLines = ModConfig.get().defaultHideLines;
        this.allowDragging = false;
    }

    private void parseDisplayJson(JsonObject displayJson) {
        if (displayJson.has("completed_icon_color")) {
            this.completedIconColor = ColorHelper.RGB(displayJson.get("completed_icon_color").getAsString());
        }
        if (displayJson.has("uncompleted_icon_color")) {
            this.unCompletedIconColor = ColorHelper.RGB(displayJson.get("uncompleted_icon_color").getAsString());
        }
        if (displayJson.has("completed_title_color")) {
            this.completedTitleColor = ColorHelper.RGB(displayJson.get("completed_title_color").getAsString());
        }
        if (displayJson.has("uncompleted_title_color")) {
            this.unCompletedTitleColor = ColorHelper.RGB(displayJson.get("uncompleted_title_color").getAsString());
        }
        if (displayJson.has("draw_direct_lines")) {
            this.drawDirectLines = displayJson.get("draw_direct_lines").getAsBoolean();
        }
        if (displayJson.has("completed_line_color")) {
            this.completedLineColor = ColorHelper.RGB(displayJson.get("completed_line_color").getAsString());
        }
        if (displayJson.has("uncompleted_line_color")) {
            this.unCompletedLineColor = ColorHelper.RGB(displayJson.get("uncompleted_line_color").getAsString());
        }
        if (displayJson.has("pos_x")) {
            this.posX = displayJson.get("pos_x").getAsInt();
        }
        if (displayJson.has("pos_y")) {
            this.posY = displayJson.get("pos_y").getAsInt();
        }
        if (displayJson.has("hide_lines")) {
            this.hideLines = displayJson.get("hide_lines").getAsBoolean();
        }
    }

    private void parseIDisplayInfo(IDisplayInfo displayInfo) {
        if (displayInfo.getCompletedIconColor() != -1) {
            this.completedIconColor = displayInfo.getCompletedIconColor();
        }
        if (displayInfo.getUnCompletedIconColor() != -1) {
            this.unCompletedIconColor = displayInfo.getUnCompletedIconColor();
        }
        if (displayInfo.getCompletedTitleColor() != -1) {
            this.completedTitleColor = displayInfo.getCompletedTitleColor();
        }
        if (displayInfo.getUnCompletedTitleColor() != -1) {
            this.unCompletedTitleColor = displayInfo.getUnCompletedTitleColor();
        }
        if (displayInfo.drawDirectLines() != null) {
            this.drawDirectLines = displayInfo.drawDirectLines();
        }
        if (displayInfo.getCompletedLineColor() != -1) {
            this.completedLineColor = displayInfo.getCompletedLineColor();
        }
        if (displayInfo.getUnCompletedLineColor() != -1) {
            this.unCompletedLineColor = displayInfo.getUnCompletedLineColor();
        }
        if (displayInfo.getPosX() != null) {
            this.posX = displayInfo.getPosX();
        }
        if (displayInfo.getPosY() != null) {
            this.posY = displayInfo.getPosY();
        }
        if (displayInfo.hideLines() != null) {
            this.hideLines = displayInfo.hideLines();
        }
        this.allowDragging = displayInfo.allowDragging();
    }

    public Identifier getId() {
        return this.id;
    }

    public int getCompletedIconColor() {
        return this.completedIconColor;
    }

    public int getUnCompletedIconColor() {
        return this.unCompletedIconColor;
    }

    public int getCompletedTitleColor() {
        return this.completedTitleColor;
    }

    public int getUnCompletedTitleColor() {
        return this.unCompletedTitleColor;
    }

    public Boolean drawDirectLines() {
        return this.drawDirectLines;
    }

    public int getCompletedLineColor() {
        return this.completedLineColor;
    }

    public int getUnCompletedLineColor() {
        return this.unCompletedLineColor;
    }

    public Integer getPosX() {
        return this.posX;
    }

    public Integer getPosY() {
        return this.posY;
    }

    public Boolean hideLines() {
        return this.hideLines;
    }

    public boolean allowDragging() {
        return this.allowDragging;
    }

    public boolean hasCustomIconColor() {
        return this.completedIconColor != minecraftCompletedIconColor || this.unCompletedIconColor != minecraftUncompletedIconColor;
    }

    public boolean hasCustomTitleColor() {
        return this.completedTitleColor != minecraftCompletedTitleColor || this.unCompletedTitleColor != minecraftUncompletedTitleColor;
    }

    public int getIconYMultiplier(AdvancementWidgetType state) {
        if (hasCustomIconColor()) {
            return 2;
        }
        return state == AdvancementWidgetType.OBTAINED ? 0 : 1;
    }

    public int getIconColor(AdvancementWidgetType state) {
        if (!hasCustomIconColor()) {
            return WHITE;
        }
        return state == AdvancementWidgetType.OBTAINED ? getCompletedIconColor() : getUnCompletedIconColor();
    }

    public int defaultIconColor() {
        return WHITE;
    }

    public int getTitleYMultiplier(AdvancementWidgetType state) {
        if (hasCustomTitleColor()) {
            return 3;
        }
        return state == AdvancementWidgetType.OBTAINED ? 0 : 1;
    }

    public int getTitleColor(AdvancementWidgetType state) {
        if (!hasCustomTitleColor()) {
            return WHITE;
        }
        return state == AdvancementWidgetType.OBTAINED ? getCompletedTitleColor() : getUnCompletedTitleColor();
    }
}
