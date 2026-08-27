package com.evandev.reliable_advancements.gui;

import com.evandev.reliable_advancements.advancements.AdvancementDisplayInfo;
import com.evandev.reliable_advancements.api.IAdvancementEntryGui;
import com.evandev.reliable_advancements.api.event.IAdvancementDrawConnectionsEvent;
import com.evandev.reliable_advancements.client.ClientRewardTracker;
import com.evandev.reliable_advancements.config.ModConfig;
import com.evandev.reliable_advancements.gui.screens.EnhancedAdvancementsScreen;
import com.evandev.reliable_advancements.platform.Services;
import com.evandev.reliable_advancements.reference.Resources;
import com.evandev.reliable_advancements.util.ConnectionRouter;
import com.evandev.reliable_advancements.util.CriterionGrid;
import com.evandev.reliable_advancements.util.PersistentData;
import com.evandev.reliable_advancements.util.RenderUtil;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.advancements.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementWidgetType;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.Collections;
import java.util.List;

public class EnhancedAdvancementWidget implements IAdvancementEntryGui {
    public static final int ADVANCEMENT_SIZE = 26;
    private static final ResourceLocation TITLE_BOX_SPRITE = ResourceLocation.withDefaultNamespace("advancements/title_box");
    private static final int CORNER_SIZE = 10;
    private static final int WIDGET_WIDTH = 256;
    private static final int WIDGET_HEIGHT = 26;
    private static final int TITLE_SIZE = 32;
    private static final int ICON_SIZE = 26;
    public final AdvancementDisplayInfo enhancedDisplayInfo;
    private final EnhancedAdvancementTab advancementTabGui;
    private final AdvancementNode advancementNode;
    private final DisplayInfo displayInfo;
    private final String title;
    private final Minecraft minecraft;
    private final List<EnhancedAdvancementWidget> children = Lists.newArrayList();
    public AdvancementProgress advancementProgress;
    protected int x, y;
    private int width;
    private List<FormattedCharSequence> description;
    private CriterionGrid criterionGrid;
    private EnhancedAdvancementWidget parent;
    private float hoverAnim = 0.0f;
    /**
     * Side of this widget that its incoming connection arrives at, used so outgoing connections can
     * leave through the opposite side and the chain reads as flowing through the widget.
     */
    private ConnectionRouter.Side incomingSide = ConnectionRouter.Side.NONE;

    public EnhancedAdvancementWidget(EnhancedAdvancementTab advancementTabGui, Minecraft mc, AdvancementNode advancementNode, DisplayInfo displayInfo) {
        this.advancementTabGui = advancementTabGui;
        this.advancementNode = advancementNode;
        this.enhancedDisplayInfo = advancementTabGui.getDisplayInfo(this.advancementNode);
        this.displayInfo = displayInfo;
        this.minecraft = mc;
        this.title = displayInfo.getTitle().getString(163);
        this.x = this.enhancedDisplayInfo.getPosX() != null ? this.enhancedDisplayInfo.getPosX() : Mth.floor(displayInfo.getX() * 32.0F);
        this.y = this.enhancedDisplayInfo.getPosY() != null ? this.enhancedDisplayInfo.getPosY() : Mth.floor(displayInfo.getY() * 27.0F);
        if (PersistentData.hasSavedPosition(this.advancementNode.holder())) {
            PersistentData.loadSavedPosition(this.advancementNode.holder(), this);
        } else {
            PersistentData.setMemoryPosition(this.advancementNode.holder().id(), this.x, this.y);
        }
        this.refreshHover();
    }

    private void refreshHover() {
        Minecraft mc = this.minecraft;
        int k = 0;
        if (this.advancementNode.advancement().requirements().size() > 1) {
            int strLengthRequirementCount = String.valueOf(this.advancementNode.advancement().requirements().size()).length();
            k = mc.font.width("  ") + mc.font.width("0") * strLengthRequirementCount * 2 + mc.font.width("/");
        }
        int titleWidth = 29 + mc.font.width(this.title) + k;
        EnhancedAdvancementsScreen screen = advancementTabGui.getScreen();
        this.criterionGrid = CriterionGrid.findOptimalCriterionGrid(this.advancementNode.holder(), this.advancementNode.advancement(), advancementProgress, screen.width / 2, mc.font);
        int maxWidth;

        if (!ModConfig.get().requiresShift || Screen.hasShiftDown()) {
            maxWidth = Math.max(titleWidth, this.criterionGrid.width);
        } else {
            maxWidth = titleWidth;
        }
        this.description = Language.getInstance().getVisualOrder(
                this.findOptimalLines(ComponentUtils.mergeStyles(
                        displayInfo.getDescription().copy(),
                        Style.EMPTY.withColor(displayInfo.getType().getChatColor())
                ), maxWidth));

        for (FormattedCharSequence line : this.description) {
            maxWidth = Math.max(maxWidth, mc.font.width(line));
        }

        this.width = maxWidth + 8;
    }

    private List<FormattedText> findOptimalLines(Component line, int width) {
        if (line.getString().isEmpty()) {
            return Collections.emptyList();
        } else {
            StringSplitter stringsplitter = this.minecraft.font.getSplitter();
            List<FormattedText> list = stringsplitter.splitLines(line, width, Style.EMPTY);
            if (list.size() > 1) {
                width = Math.max(width, advancementTabGui.getScreen().internalWidth / 4);
                list = stringsplitter.splitLines(line, width, Style.EMPTY);
            }
            while (list.size() > 5 && width < WIDGET_WIDTH * 1.5 && width < advancementTabGui.getScreen().internalWidth / 2.5) {
                width += width / 4;
                list = stringsplitter.splitLines(line, width, Style.EMPTY);
            }
            return list;
        }
    }

    private EnhancedAdvancementWidget getFirstVisibleParent(AdvancementNode advancement) {
        do {
            advancement = advancement.parent();
        } while (advancement != null && advancement.advancement().display().isEmpty());

        if (advancement != null && advancement.advancement().display().isPresent()) {
            return this.advancementTabGui.getWidget(advancement.holder());
        } else {
            return null;
        }
    }

    public boolean shouldRender() {
        if (EnhancedAdvancementsScreen.canEdit()) return true;
        if (this.advancementProgress != null && this.advancementProgress.isDone()) return true;
        if (this.displayInfo.isHidden()) return false;

        if (ModConfig.get().discoveryMode) {
            if (this.parent == null) return true;
            boolean parentCompleted = this.parent.advancementProgress != null && this.parent.advancementProgress.isDone();

            boolean parentClaimed = !ModConfig.get().requireRewardClaiming || ClientRewardTracker.isClaimed(this.parent.getAdvancement().holder().id());

            return parentCompleted && parentClaimed;
        }
        return true;
    }

    public void drawConnectivity(GuiGraphics guiGraphics, int scrollX, int scrollY, boolean drawInside) {
        // Check if connections should be drawn at all
        if (this.shouldRender() && (EnhancedAdvancementsScreen.canEdit() || !this.enhancedDisplayInfo.hideLines())) {
            // Draw connection to parent
            if (this.parent != null && this.parent.shouldRender()) {
                this.drawConnection(guiGraphics, this.parent, scrollX, scrollY, drawInside);
            }
            // Create and post event to get extra connections
            IAdvancementDrawConnectionsEvent event = Services.PLATFORM.getEventHelper().postAdvancementDrawConnectionsEvent(this.advancementNode);
            // Draw extra connections from event
            for (AdvancementHolder parent : event.getExtraConnections()) {
                final EnhancedAdvancementWidget parentGui = this.advancementTabGui.getWidget(parent);
                if (parentGui != null && parentGui.shouldRender()) {
                    this.drawConnection(guiGraphics, parentGui, scrollX, scrollY, drawInside);
                }
            }
        }
        // Draw child connections
        for (EnhancedAdvancementWidget advancementWidget : this.children) {
            advancementWidget.drawConnectivity(guiGraphics, scrollX, scrollY, drawInside);
        }
    }

    /**
     * Draws connection line between this advancement and the advancement supplied in parent.
     */
    public void drawConnection(GuiGraphics guiGraphics, EnhancedAdvancementWidget parent, int scrollX, int scrollY, boolean drawInside) {
        boolean parentCompleted = parent.advancementProgress != null && parent.advancementProgress.isDone();
        boolean parentClaimed = !ModConfig.get().requireRewardClaiming || ClientRewardTracker.isClaimed(parent.getAdvancement().holder().id());

        boolean thisCompleted = this.advancementProgress != null && this.advancementProgress.isDone();
        boolean thisClaimed = !ModConfig.get().requireRewardClaiming || ClientRewardTracker.isClaimed(this.advancementNode.holder().id());

        int innerLineColor;
        int borderLineColor = 0xFF000000;

        if (ModConfig.get().requireRewardClaiming) {
            if (parentClaimed) {
                if (thisClaimed) {
                    innerLineColor = enhancedDisplayInfo.getCompletedLineColor();
                } else {
                    innerLineColor = 0xFF00FF00;
                }
            } else {
                innerLineColor = 0xFF444444;
            }
        } else {
            innerLineColor = thisCompleted ? enhancedDisplayInfo.getCompletedLineColor() : enhancedDisplayInfo.getUnCompletedLineColor();
        }

        int startX = scrollX + parent.x + ADVANCEMENT_SIZE / 2 + 3;
        int startY = scrollY + parent.y + ADVANCEMENT_SIZE / 2;
        int endX = scrollX + this.x + ADVANCEMENT_SIZE / 2 + 3;
        int endY = scrollY + this.y + ADVANCEMENT_SIZE / 2;

        boolean goalFrame = this.displayInfo.getType() == AdvancementType.GOAL;

        if (this.enhancedDisplayInfo.drawDirectLines()) {
            if (parent == this.parent) {
                this.incomingSide = ConnectionRouter.Side.NONE;
            }

            if (drawInside) {
                RenderUtil.drawRect(guiGraphics, endX - 1, endY - 1, startX - 1, startY - 1, 3, borderLineColor);
            } else {
                RenderUtil.drawRect(guiGraphics, endX, endY, startX, startY, 1, innerLineColor);

                float dx = endX - startX;
                float dy = endY - startY;

                if (ModConfig.get().drawArrows && Math.sqrt(dx * dx + dy * dy) > ADVANCEMENT_SIZE) {
                    RenderUtil.drawDiagonalArrow(guiGraphics, endX, endY, dx, dy, goalFrame, innerLineColor);
                }
            }
        } else {
            ConnectionRouter.Route route = ConnectionRouter.route(startX, startY, endX, endY, parent.incomingSide);
            if (parent == this.parent) {
                this.incomingSide = route.entrySide();
            }

            int thickness = drawInside ? 1 : 0;
            int color = drawInside ? borderLineColor : innerLineColor;

            RenderUtil.line(guiGraphics, route.startX(), route.startY(), route.startAnchorX(), route.startAnchorY(), thickness, color);
            RenderUtil.line(guiGraphics, route.startAnchorX(), route.startAnchorY(), route.endAnchorX(), route.endAnchorY(), thickness, color);
            RenderUtil.line(guiGraphics, route.endAnchorX(), route.endAnchorY(), route.endX(), route.endY(), thickness, color);

            if (!drawInside && ModConfig.get().drawArrows && route.shouldShowArrow()) {
                RenderUtil.drawArrow(guiGraphics, route.endX(), route.endY(), route.endAnchorX(), route.endAnchorY(),
                        route.verticalAnchors(), goalFrame, innerLineColor);
            }
        }
    }

    public void draw(GuiGraphics guiGraphics, int scrollX, int scrollY, double unzoomedX, double unzoomedY) {
        boolean isHovered = EnhancedAdvancementsScreen.canEdit() && !ModConfig.get().showTooltipsInEditMode && this.isMouseOver(scrollX, scrollY, unzoomedX, unzoomedY);
        if (isHovered) {
            hoverAnim = Math.min(1.0f, hoverAnim + 0.15f);
        } else {
            hoverAnim = Math.max(0.0f, hoverAnim - 0.15f);
        }

        if (this.shouldRender()) {
            boolean isCompleted = this.advancementProgress != null && this.advancementProgress.isDone();
            boolean isClaimed = !ModConfig.get().requireRewardClaiming || ClientRewardTracker.isClaimed(this.advancementNode.holder().id());

            AdvancementWidgetType advancementState;
            boolean isDimmed = false;

            if (ModConfig.get().requireRewardClaiming) {
                if (isCompleted && !isClaimed) {
                    advancementState = AdvancementWidgetType.OBTAINED;
                } else if (isCompleted && isClaimed) {
                    advancementState = AdvancementWidgetType.UNOBTAINED;
                } else {
                    advancementState = AdvancementWidgetType.UNOBTAINED;
                    isDimmed = true;
                }
            } else {
                advancementState = isCompleted ? AdvancementWidgetType.OBTAINED : AdvancementWidgetType.UNOBTAINED;
            }

            int baseColor = enhancedDisplayInfo.getIconColor(advancementState);
            if (hoverAnim > 0.0f) {
                int r = (baseColor >> 16) & 255;
                int g = (baseColor >> 8) & 255;
                int b = baseColor & 255;

                // Blend upwards of 40% towards white when fully hovered
                r = (int) (r + (255 - r) * hoverAnim * 0.4f);
                g = (int) (g + (255 - g) * hoverAnim * 0.4f);
                b = (int) (b + (255 - b) * hoverAnim * 0.4f);
                baseColor = 0xFF000000 | (r << 16) | (g << 8) | b;
            }

            RenderUtil.setColor(baseColor);
            RenderSystem.enableBlend();
            guiGraphics.pose().pushPose();

            if (isDimmed) {
                RenderSystem.setShaderColor(0.5F, 0.5F, 0.5F, 1.0F);
            }

            float scale = 1.0f + (hoverAnim * 0.1f);
            float centerX = scrollX + this.x + 3 + ICON_SIZE / 2.0f;
            float centerY = scrollY + this.y + ICON_SIZE / 2.0f;
            guiGraphics.pose().translate(centerX, centerY, 0);
            guiGraphics.pose().scale(scale, scale, 1.0f);
            guiGraphics.pose().translate(-centerX, -centerY, 0);

            guiGraphics.blitSprite(advancementState.frameSprite(this.displayInfo.getType()), scrollX + this.x + 3, scrollY + this.y, ICON_SIZE, ICON_SIZE);

            if (isDimmed) {
                RenderSystem.setShaderColor(0.25F, 0.25F, 0.25F, 1.0F);
            } else {
                RenderUtil.setColor(enhancedDisplayInfo.defaultIconColor());
            }

            guiGraphics.renderFakeItem(this.displayInfo.getIcon(), scrollX + this.x + 8, scrollY + this.y + 5);

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            guiGraphics.pose().popPose();
        }

        for (EnhancedAdvancementWidget advancementWidget : this.children) {
            advancementWidget.draw(guiGraphics, scrollX, scrollY, unzoomedX, unzoomedY);
        }
    }

    public void getAdvancementProgress(AdvancementProgress advancementProgressIn) {
        this.advancementProgress = advancementProgressIn;
        this.refreshHover();
    }

    public void addGuiAdvancement(EnhancedAdvancementWidget advancementEntryScreen) {
        this.children.add(advancementEntryScreen);
    }

    public void drawHover(GuiGraphics guiGraphics, int scrollX, int scrollY, int left, int top) {
        if (EnhancedAdvancementsScreen.canEdit() && !ModConfig.get().showTooltipsInEditMode) {
            return;
        }

        this.refreshHover();
        boolean drawLeft = left + scrollX + this.x + this.width + ADVANCEMENT_SIZE >= this.advancementTabGui.getScreen().internalWidth;
        String s = this.advancementProgress == null || this.advancementProgress.getProgressText() == null ? null : this.advancementProgress.getProgressText().getString();
        int i = s == null ? 0 : this.minecraft.font.width(s);
        boolean drawTop;

        if (!ModConfig.get().requiresShift || Screen.hasShiftDown()) {
            if (this.criterionGrid.height < this.advancementTabGui.getScreen().height) {
                drawTop = top + scrollY + this.y + this.description.size() * this.minecraft.font.lineHeight + this.criterionGrid.height + 50 >= this.advancementTabGui.getScreen().height;
            } else {
                // Always draw on the bottom if the grid is larger than the screen
                drawTop = false;
            }
        } else {
            drawTop = top + scrollY + this.y + this.description.size() * this.minecraft.font.lineHeight + 50 >= this.advancementTabGui.getScreen().height;
        }

        float percentageObtained = this.advancementProgress == null ? 0.0F : this.advancementProgress.getPercent();
        int j = Mth.floor(percentageObtained * (float) this.width);
        AdvancementWidgetType stateTitleLeft;
        AdvancementWidgetType stateTitleRight;
        AdvancementWidgetType stateIcon;

        if (percentageObtained >= 1.0F) {
            j = this.width / 2;
            stateTitleLeft = AdvancementWidgetType.OBTAINED;
            stateTitleRight = AdvancementWidgetType.OBTAINED;
            stateIcon = AdvancementWidgetType.OBTAINED;
        } else if (j < 2) {
            j = this.width / 2;
            stateTitleLeft = AdvancementWidgetType.UNOBTAINED;
            stateTitleRight = AdvancementWidgetType.UNOBTAINED;
            stateIcon = AdvancementWidgetType.UNOBTAINED;
        } else if (j > this.width - 2) {
            j = this.width / 2;
            stateTitleLeft = AdvancementWidgetType.OBTAINED;
            stateTitleRight = AdvancementWidgetType.OBTAINED;
            stateIcon = AdvancementWidgetType.UNOBTAINED;
        } else {
            stateTitleLeft = AdvancementWidgetType.OBTAINED;
            stateTitleRight = AdvancementWidgetType.UNOBTAINED;
            stateIcon = AdvancementWidgetType.UNOBTAINED;
        }

        int k = this.width - j;
        RenderSystem.enableBlend();
        int drawY = scrollY + this.y;
        int drawX;

        if (drawLeft) {
            drawX = scrollX + this.x - this.width + ADVANCEMENT_SIZE + 6;
        } else {
            drawX = scrollX + this.x;
        }
        int boxHeight;

        if (!ModConfig.get().requiresShift || Screen.hasShiftDown()) {
            boxHeight = TITLE_SIZE + this.description.size() * this.minecraft.font.lineHeight + this.criterionGrid.height;
        } else {
            boxHeight = TITLE_SIZE + this.description.size() * this.minecraft.font.lineHeight;
        }

        if (!this.description.isEmpty()) {
            if (drawTop) {
                guiGraphics.blitSprite(TITLE_BOX_SPRITE, drawX, drawY + ADVANCEMENT_SIZE - boxHeight, this.width, boxHeight);
            } else {
                guiGraphics.blitSprite(TITLE_BOX_SPRITE, drawX, drawY, this.width, boxHeight);
            }
        }

        RenderUtil.setColor(enhancedDisplayInfo.getTitleColor(stateTitleLeft));
        guiGraphics.blitSprite(stateTitleLeft.boxSprite(), 200, 26, 0, 0, drawX, drawY, j, WIDGET_HEIGHT);
        RenderUtil.setColor(enhancedDisplayInfo.getTitleColor(stateTitleRight));
        guiGraphics.blitSprite(stateTitleRight.boxSprite(), 200, 26, 200 - k, 0, drawX + j, drawY, k, WIDGET_HEIGHT);

        RenderUtil.setColor(enhancedDisplayInfo.getIconColor(stateIcon));
        guiGraphics.blitSprite(stateIcon.frameSprite(this.displayInfo.getType()), scrollX + this.x + 3, scrollY + this.y, ICON_SIZE, ICON_SIZE);
        RenderUtil.setColor(enhancedDisplayInfo.defaultIconColor());

        if (drawLeft) {
            guiGraphics.drawString(this.minecraft.font, this.title, drawX + 5, scrollY + this.y + 9, -1);

            if (s != null) {
                guiGraphics.drawString(this.minecraft.font, s, scrollX + this.x - i, scrollY + this.y + 9, -1);
            }
        } else {
            guiGraphics.drawString(this.minecraft.font, this.title, scrollX + this.x + 32, scrollY + this.y + 9, -1);

            if (s != null) {
                guiGraphics.drawString(this.minecraft.font, s, scrollX + this.x + this.width - i - 5, scrollY + this.y + 9, -1);
            }
        }

        int yOffset;
        if (drawTop) {
            yOffset = drawY + 26 - boxHeight + 7;
        } else {
            yOffset = scrollY + this.y + 9 + 17;
        }
        for (int k1 = 0; k1 < this.description.size(); ++k1) {
            guiGraphics.drawString(this.minecraft.font, this.description.get(k1), drawX + 5, yOffset + k1 * this.minecraft.font.lineHeight, -5592406, false);
        }
        if (this.criterionGrid != null && !ModConfig.get().requiresShift || Screen.hasShiftDown()) {
            int xOffset = drawX + 5;
            yOffset += this.description.size() * this.minecraft.font.lineHeight;
            for (int colIndex = 0; colIndex < this.criterionGrid.columns.size(); colIndex++) {
                CriterionGrid.Column col = this.criterionGrid.columns.get(colIndex);
                for (int rowIndex = 0; rowIndex < col.cells().size(); rowIndex++) {
                    guiGraphics.drawString(this.minecraft.font, col.cells().get(rowIndex), xOffset, yOffset + rowIndex * this.minecraft.font.lineHeight, -5592406, false);
                }
                xOffset += col.width();
            }
        }

        guiGraphics.renderFakeItem(this.displayInfo.getIcon(), scrollX + this.x + 8, scrollY + this.y + 5);
    }

    protected void render9Sprite(GuiGraphics guiGraphics, int x, int y, int width, int height, int textureHeight, int textureWidth, int textureDistance, int textureX, int textureY) {
        // Top left corner
        guiGraphics.blit(Resources.Gui.WIDGETS, x, y, textureX, textureY, textureHeight, textureHeight);
        // Top side
        RenderUtil.renderRepeating(Resources.Gui.WIDGETS, guiGraphics, x + textureHeight, y, width - textureHeight - textureHeight, textureHeight, textureX + textureHeight, textureY, textureWidth - textureHeight - textureHeight, textureDistance);
        // Top right corner
        guiGraphics.blit(Resources.Gui.WIDGETS, x + width - textureHeight, y, textureX + textureWidth - textureHeight, textureY, textureHeight, textureHeight);
        // Bottom left corner
        guiGraphics.blit(Resources.Gui.WIDGETS, x, y + height - textureHeight, textureX, textureY + textureDistance - textureHeight, textureHeight, textureHeight);
        // Bottom side
        RenderUtil.renderRepeating(Resources.Gui.WIDGETS, guiGraphics, x + textureHeight, y + height - textureHeight, width - textureHeight - textureHeight, textureHeight, textureX + textureHeight, textureY + textureDistance - textureHeight, textureWidth - textureHeight - textureHeight, textureDistance);
        // Bottom right corner
        guiGraphics.blit(Resources.Gui.WIDGETS, x + width - textureHeight, y + height - textureHeight, textureX + textureWidth - textureHeight, textureY + textureDistance - textureHeight, textureHeight, textureHeight);
        // Left side
        RenderUtil.renderRepeating(Resources.Gui.WIDGETS, guiGraphics, x, y + textureHeight, textureHeight, height - textureHeight - textureHeight, textureX, textureY + textureHeight, textureWidth, textureDistance - textureHeight - textureHeight);
        // Center
        RenderUtil.renderRepeating(Resources.Gui.WIDGETS, guiGraphics, x + textureHeight, y + textureHeight, width - textureHeight - textureHeight, height - textureHeight - textureHeight, textureX + textureHeight, textureY + textureHeight, textureWidth - textureHeight - textureHeight, textureDistance - textureHeight - textureHeight);
        // Right side
        RenderUtil.renderRepeating(Resources.Gui.WIDGETS, guiGraphics, x + width - textureHeight, y + textureHeight, textureHeight, height - textureHeight - textureHeight, textureX + textureWidth - textureHeight, textureY + textureHeight, textureWidth, textureDistance - textureHeight - textureHeight);
    }

    public boolean isMouseOver(double scrollX, double scrollY, double mouseX, double mouseY) {
        if (this.shouldRender()) {
            double left = scrollX + this.x + 3;
            double right = left + ADVANCEMENT_SIZE;
            double top = scrollY + this.y;
            double bottom = top + ADVANCEMENT_SIZE;
            return mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom;
        } else {
            return false;
        }
    }

    public void attachToParent() {
        if (this.parent == null && advancementNode.advancement().parent().isPresent()) {
            this.parent = this.getFirstVisibleParent(advancementNode);

            if (this.parent != null) {
                this.parent.addGuiAdvancement(this);
            }
        }
    }

    @Override
    public int getY() {
        return this.y;
    }

    public void setY(int y) {
        this.y = y;
    }

    @Override
    public int getX() {
        return this.x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public EnhancedAdvancementWidget getParent() {
        return this.parent;
    }

    public void setParent(EnhancedAdvancementWidget parent) {
        this.parent = parent;
    }

    public List<EnhancedAdvancementWidget> getChildren() {
        return this.children;
    }

    @Override
    public AdvancementNode getAdvancement() {
        return this.advancementNode;
    }
}