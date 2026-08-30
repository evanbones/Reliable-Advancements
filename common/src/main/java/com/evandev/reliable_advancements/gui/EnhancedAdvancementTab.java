package com.evandev.reliable_advancements.gui;

import com.evandev.reliable_advancements.advancements.AdvancementDisplayInfo;
import com.evandev.reliable_advancements.advancements.AdvancementDisplayInfoRegistry;
import com.evandev.reliable_advancements.advancements.IMultiParentNode;
import com.evandev.reliable_advancements.config.ModConfig;
import com.evandev.reliable_advancements.gui.screens.EnhancedAdvancementsScreen;
import com.evandev.reliable_advancements.reference.Constants;
import com.evandev.reliable_advancements.tabs.ResolvedTab;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class EnhancedAdvancementTab {
    public static final Map<ResourceLocation, Tuple<Integer, Integer>> scrollHistory = Maps.newLinkedHashMap();

    public final List<BackgroundRule> backgroundRules = new ArrayList<>();
    protected final Map<ResourceLocation, EnhancedAdvancementWidget> widgets = Maps.newLinkedHashMap();

    private final Minecraft minecraft;
    private final EnhancedAdvancementsScreen screen;
    private final AdvancementDisplayInfoRegistry displayInfos = new AdvancementDisplayInfoRegistry();

    private final ResolvedTab definition;
    private final EnhancedAdvancementTabType type;
    private final int index;
    public int scrollX;
    public int scrollY;
    private int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
    private int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
    private float fade;
    private boolean centered;

    protected EnhancedAdvancementTab(Minecraft mc, EnhancedAdvancementsScreen screen, EnhancedAdvancementTabType type,
                                     int index, ResolvedTab definition) {
        this.minecraft = mc;
        this.screen = screen;
        this.type = type;
        this.index = index;
        this.definition = definition;
        parseBackgroundRules(definition.backgroundRules());
    }

    public static @Nullable EnhancedAdvancementTab create(Minecraft mc, EnhancedAdvancementsScreen screen, int index,
                                                          ResolvedTab definition, int width, int height) {
        EnhancedAdvancementTabType type = EnhancedAdvancementTabType.getTabType(width, height, index);
        return type == null ? null : new EnhancedAdvancementTab(mc, screen, type, index, definition);
    }

    public ResourceLocation getId() {
        return this.definition.id();
    }

    public ResolvedTab getDefinition() {
        return this.definition;
    }

    public @Nullable AdvancementNode getPrimaryRoot() {
        return this.definition.primaryRoot();
    }

    public Component getTitle() {
        return this.definition.title();
    }

    public void parseBackgroundRules(String jsonStr) {
        this.backgroundRules.clear();
        if (jsonStr == null || jsonStr.isEmpty()) return;
        try {
            JsonArray arr = JsonParser.parseString(jsonStr).getAsJsonArray();
            for (JsonElement el : arr) {
                this.backgroundRules.add(BackgroundRule.fromJson(el.getAsJsonObject()));
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to parse background rules for tab {}", getId(), e);
        }
    }

    public Map<ResourceLocation, EnhancedAdvancementWidget> getWidgets() {
        return this.widgets;
    }

    public void drawTab(GuiGraphics guiGraphics, int left, int top, int width, int height, boolean selected) {
        this.type.draw(guiGraphics, left, top, width, height, selected, this.index);
    }

    public void drawIcon(GuiGraphics guiGraphics, int left, int top, int width, int height) {
        this.type.drawIcon(guiGraphics, left, top, width, height, this.index, this.definition.icon());
    }

    public void drawContents(GuiGraphics guiGraphics, int left, int top, int width, int height, double mouseX, double mouseY) {
        float zoom = this.screen.getZoom();
        int scaledWidth = (int) (width / zoom);
        int scaledHeight = (int) (height / zoom);

        double unzoomedX = (mouseX - left) / zoom;
        double unzoomedY = (mouseY - top) / zoom;

        if (!this.centered) {
            centreOnContent(scaledWidth, scaledHeight);
            this.centered = true;
        }

        guiGraphics.enableScissor(left, top, left + width, top + height);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(left, top, 0);
        guiGraphics.pose().scale(zoom, zoom, 1.0F);

        ResourceLocation defaultRes = this.definition.background();
        boolean isStatic = this.definition.staticBackground();
        int configuredW = this.definition.bgWidth();
        int configuredH = this.definition.bgHeight();

        if (isStatic && configuredW == 0 && configuredH == 0) {
            guiGraphics.blit(defaultRes, 0, 0, 0.0F, 0.0F, scaledWidth, scaledHeight, scaledWidth, scaledHeight);
        } else {
            int texW = configuredW > 0 ? configuredW : 16;
            int texH = configuredH > 0 ? configuredH : 16;

            int i = isStatic ? 0 : this.scrollX % texW;
            int j = isStatic ? 0 : this.scrollY % texH;

            Random random = new Random();

            for (int k = -1; k <= 1 + scaledWidth / texW; k++) {
                for (int l = -1; l <= 1 + scaledHeight / texH; l++) {
                    ResourceLocation texToDraw = defaultRes;

                    if (!this.backgroundRules.isEmpty() && !isStatic) {
                        int absoluteX = i + (texW * k) - this.scrollX;
                        int absoluteY = j + (texH * l) - this.scrollY;
                        int cellX = Math.floorDiv(absoluteX, texW);
                        int cellY = Math.floorDiv(absoluteY, texH);

                        random.setSeed((long) cellX * 3129871L ^ (long) cellY * 116129781L);

                        int j4 = cellY + random.nextInt(4) - random.nextInt(4);
                        float randomChance = random.nextFloat();

                        for (BackgroundRule rule : this.backgroundRules) {
                            int yToCheck = rule.absoluteY ? cellY : j4;

                            if (yToCheck >= rule.minY && yToCheck <= rule.maxY && randomChance <= rule.chance) {
                                texToDraw = rule.texture;
                                break;
                            }
                        }
                    }

                    guiGraphics.blit(texToDraw, i + texW * k, j + texH * l, 0.0F, 0.0F, texW, texH, texW, texH);
                }
            }
        }

        if (ModConfig.get().blurBackground) {
            int alpha = (int) (ModConfig.get().blurBackgroundOpacity / 100.0f * 255.0f);
            guiGraphics.fill(0, 0, scaledWidth, scaledHeight, alpha << 24);
        }

        for (EnhancedAdvancementWidget widget : this.widgets.values()) {
            widget.drawConnectivity(guiGraphics, this.scrollX, this.scrollY, true);
        }
        for (EnhancedAdvancementWidget widget : this.widgets.values()) {
            widget.drawConnectivity(guiGraphics, this.scrollX, this.scrollY, false);
        }
        for (EnhancedAdvancementWidget widget : this.widgets.values()) {
            widget.draw(guiGraphics, this.scrollX, this.scrollY, unzoomedX, unzoomedY);
        }

        for (EnhancedAdvancementWidget advancementWidget : this.widgets.values()) {
            if (EnhancedAdvancementsScreen.selectedWidgets.contains(advancementWidget)) {
                guiGraphics.fill(advancementWidget.getX() + this.scrollX + 1, advancementWidget.getY() + this.scrollY - 2,
                        advancementWidget.getX() + this.scrollX + 31, advancementWidget.getY() + this.scrollY + 28, 0x6600FF00);
            }
        }
        guiGraphics.pose().popPose();
        guiGraphics.disableScissor();
    }

    private void centreOnContent(int scaledWidth, int scaledHeight) {
        AdvancementNode primary = getPrimaryRoot();
        EnhancedAdvancementWidget anchor = primary != null ? getWidget(primary.holder().id()) : null;

        int targetX;
        int targetY;
        if (anchor != null) {
            targetX = anchor.getX() + EnhancedAdvancementWidget.ADVANCEMENT_SIZE / 2;
            targetY = anchor.getY() + EnhancedAdvancementWidget.ADVANCEMENT_SIZE / 2;
        } else if (!this.widgets.isEmpty()) {
            recalculateBounds();
            targetX = (this.minX + this.maxX) / 2;
            targetY = (this.minY + this.maxY) / 2;
        } else {
            targetX = EnhancedAdvancementWidget.ADVANCEMENT_SIZE / 2;
            targetY = EnhancedAdvancementWidget.ADVANCEMENT_SIZE / 2;
        }

        this.scrollX = (scaledWidth / 2) - targetX;
        this.scrollY = (scaledHeight / 2) - targetY;
    }

    public void setCentered(boolean centered) {
        this.centered = centered;
    }

    public void drawToolTips(GuiGraphics guiGraphics, int mouseX, int mouseY, int left, int top, int width, int height) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0D, 0.0D, -200.0D);
        guiGraphics.fill(0, 0, width, height, Mth.floor(this.fade * 255.0F) << 24);
        boolean flag = false;

        float zoom = this.screen.getZoom();
        double scaledMouseX = mouseX / zoom;
        double scaledMouseY = mouseY / zoom;

        if (mouseX > 0 && mouseX < width && mouseY > 0 && mouseY < height) {
            for (EnhancedAdvancementWidget advancementWidget : this.widgets.values()) {
                if (advancementWidget.isMouseOver(this.scrollX, this.scrollY, scaledMouseX, scaledMouseY)) {
                    flag = true;
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().scale(zoom, zoom, 1.0F);
                    advancementWidget.drawHover(guiGraphics, this.scrollX, this.scrollY, (int) (left / zoom), (int) (top / zoom));
                    guiGraphics.pose().popPose();
                    break;
                }
            }
        }

        guiGraphics.pose().popPose();

        if (ModConfig.get().doFade && flag) {
            this.fade = Mth.clamp(this.fade + 0.02F, 0.0F, 0.3F);
        } else {
            this.fade = Mth.clamp(this.fade - 0.04F, 0.0F, 1.0F);
        }
    }

    public boolean isMouseOver(int left, int top, int width, int height, double mouseX, double mouseY) {
        return this.type.isMouseOver(left, top, width, height, this.index, mouseX, mouseY);
    }

    public void scroll(double scrollX, double scrollY, int width, int height) {
        if (ModConfig.get().unclampedScrolling || this.widgets.isEmpty()) {
            this.scrollX = (int) Math.round(this.scrollX + scrollX);
            this.scrollY = (int) Math.round(this.scrollY + scrollY);
            return;
        }

        float zoom = this.screen.getZoom();
        int marginX = (int) (width / zoom) / 2;
        int marginY = (int) (height / zoom) / 2;

        this.scrollX = (int) Math.round(Mth.clamp(this.scrollX + scrollX, -(this.maxX - marginX), -this.minX + marginX));
        this.scrollY = (int) Math.round(Mth.clamp(this.scrollY + scrollY, -(this.maxY - marginY), -this.minY + marginY));
    }

    public void addAdvancement(AdvancementNode advancementNode) {
        Optional<DisplayInfo> optional = advancementNode.advancement().display();
        optional.ifPresent(displayInfo -> this.addWidget(new EnhancedAdvancementWidget(this, this.minecraft, advancementNode, displayInfo),
                advancementNode.holder().id()));
    }

    public void recalculateBounds() {
        this.minX = Integer.MAX_VALUE;
        this.maxX = Integer.MIN_VALUE;
        this.minY = Integer.MAX_VALUE;
        this.maxY = Integer.MIN_VALUE;
        for (EnhancedAdvancementWidget widget : this.widgets.values()) {
            expandBounds(widget);
        }
    }

    private void expandBounds(EnhancedAdvancementWidget widget) {
        this.minX = Math.min(this.minX, widget.getX());
        this.maxX = Math.max(this.maxX, widget.getX() + 28);
        this.minY = Math.min(this.minY, widget.getY());
        this.maxY = Math.max(this.maxY, widget.getY() + 27);
    }

    public void addWidget(EnhancedAdvancementWidget widget, ResourceLocation id) {
        widget.setTab(this);
        this.widgets.put(id, widget);
        expandBounds(widget);
    }

    public void linkWidgets() {
        for (EnhancedAdvancementWidget widget : this.widgets.values()) {
            widget.unlinkAll();
        }
        for (EnhancedAdvancementWidget widget : this.widgets.values()) {
            for (AdvancementNode parent : IMultiParentNode.getParents(widget.getAdvancement())) {
                if (parent != null) {
                    widget.link(this.widgets.get(parent.holder().id()));
                }
            }
        }
    }

    public EnhancedAdvancementWidget getWidget(ResourceLocation id) {
        return id == null ? null : this.widgets.get(id);
    }

    public EnhancedAdvancementsScreen getScreen() {
        return this.screen;
    }

    public AdvancementDisplayInfo getDisplayInfo(AdvancementNode advancementNode) {
        return displayInfos.get(advancementNode.holder());
    }

    public void storeScroll() {
        if (this.centered) {
            scrollHistory.put(getId(), new Tuple<>(scrollX, scrollY));
        }
    }

    public void loadScroll() {
        Tuple<Integer, Integer> scroll = scrollHistory.get(getId());
        if (scroll != null) {
            this.centered = true;
            this.scrollX = scroll.getA();
            this.scrollY = scroll.getB();
        }
    }

    public static class BackgroundRule {
        public int minY = Integer.MIN_VALUE;
        public int maxY = Integer.MAX_VALUE;
        public float chance = 1.0f;
        public boolean absoluteY = false;
        public ResourceLocation texture;

        public static BackgroundRule fromJson(JsonObject json) {
            BackgroundRule rule = new BackgroundRule();
            if (json.has("min_y")) rule.minY = json.get("min_y").getAsInt();
            if (json.has("max_y")) rule.maxY = json.get("max_y").getAsInt();
            if (json.has("chance")) rule.chance = json.get("chance").getAsFloat();
            if (json.has("absolute_y")) rule.absoluteY = json.get("absolute_y").getAsBoolean();
            if (json.has("texture")) rule.texture = ResourceLocation.parse(json.get("texture").getAsString());
            return rule;
        }
    }
}
