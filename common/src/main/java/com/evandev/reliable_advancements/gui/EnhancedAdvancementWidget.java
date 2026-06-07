package com.evandev.reliable_advancements.gui;

import com.evandev.reliable_advancements.advancements.AdvancementDisplayInfo;
import com.evandev.reliable_advancements.api.IAdvancementEntryGui;
import com.evandev.reliable_advancements.api.event.IAdvancementDrawConnectionsEvent;
import com.evandev.reliable_advancements.client.ClientRewardTracker;
import com.evandev.reliable_advancements.config.ModConfig;
import com.evandev.reliable_advancements.gui.screens.EnhancedAdvancementsScreen;
import com.evandev.reliable_advancements.platform.Services;
import com.evandev.reliable_advancements.reference.Resources;
import com.evandev.reliable_advancements.util.CriterionGrid;
import com.evandev.reliable_advancements.util.PersistentData;
import com.evandev.reliable_advancements.util.RenderUtil;
import com.google.common.collect.Lists;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.advancements.AdvancementWidgetType;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.Collections;
import java.util.List;

public class EnhancedAdvancementWidget implements IAdvancementEntryGui {
    public static final int ADVANCEMENT_SIZE = 26;
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

        if (!ModConfig.get().requiresShift || EnhancedAdvancementsScreen.hasShiftDown()) {
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

    public void drawConnectivity(GuiGraphicsExtractor guiGraphicsExtractor, int scrollX, int scrollY, boolean drawInside) {
        if (this.shouldRender() && (EnhancedAdvancementsScreen.canEdit() || !this.enhancedDisplayInfo.hideLines())) {
            if (this.parent != null && this.parent.shouldRender()) {
                this.drawConnection(guiGraphicsExtractor, this.parent, scrollX, scrollY, drawInside);
            }
            IAdvancementDrawConnectionsEvent event = Services.PLATFORM.getEventHelper().postAdvancementDrawConnectionsEvent(this.advancementNode);
            for (AdvancementHolder parent : event.getExtraConnections()) {
                final EnhancedAdvancementWidget parentGui = this.advancementTabGui.getWidget(parent);
                if (parentGui != null && parentGui.shouldRender()) {
                    this.drawConnection(guiGraphicsExtractor, parentGui, scrollX, scrollY, drawInside);
                }
            }
        }
        for (EnhancedAdvancementWidget advancementWidget : this.children) {
            advancementWidget.drawConnectivity(guiGraphicsExtractor, scrollX, scrollY, drawInside);
        }
    }

    public void drawConnection(GuiGraphicsExtractor guiGraphicsExtractor, EnhancedAdvancementWidget parent, int scrollX, int scrollY, boolean drawInside) {
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

        if (this.enhancedDisplayInfo.drawDirectLines()) {
            int x1 = Math.round(scrollX + this.x + ADVANCEMENT_SIZE / 2.0F + 3.0F);
            int y1 = Math.round(scrollY + this.y + ADVANCEMENT_SIZE / 2.0F);
            int x2 = Math.round(scrollX + parent.x + ADVANCEMENT_SIZE / 2.0F + 3.0F);
            int y2 = Math.round(scrollY + parent.y + ADVANCEMENT_SIZE / 2.0F);

            boolean perpendicular = x1 == x2 || y1 == y2;

            if (!perpendicular) {
                if (drawInside) {
                    RenderUtil.drawRect(guiGraphicsExtractor, x1 - 1, y1 - 1, x2 - 1, y2 - 1, 3, borderLineColor);
                } else {
                    RenderUtil.drawRect(guiGraphicsExtractor, x1, y1, x2, y2, 1, innerLineColor);
                }

                if (ModConfig.get().drawArrows && !drawInside) {
                    float dx = x1 - x2;
                    float dy = y1 - y2;
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);

                    if (distance > ADVANCEMENT_SIZE) {
                        float offsetX, offsetY;

                        if (this.displayInfo.getType() == net.minecraft.advancements.AdvancementType.GOAL) {
                            float radius = ADVANCEMENT_SIZE / 2.0F + 5.0F;
                            offsetX = (dx / distance) * radius;
                            offsetY = (dy / distance) * radius;
                        } else {
                            float maxAxis = Math.max(Math.abs(dx), Math.abs(dy));
                            float radius = ADVANCEMENT_SIZE / 2.0F + 4.0F;
                            offsetX = (dx / maxAxis) * radius;
                            offsetY = (dy / maxAxis) * radius;
                        }

                        float arrowX = x1 - offsetX;
                        float arrowY = y1 - offsetY;
                        float angle = (float) Math.atan2(dy, dx);

                        RenderUtil.drawRotatedArrow(guiGraphicsExtractor, arrowX, arrowY, angle, innerLineColor);
                    }
                }
            } else {
                int startX = parent.getX() + ADVANCEMENT_SIZE / 2 + scrollX + 3;
                int startY = parent.getY() + ADVANCEMENT_SIZE / 2 + scrollY;

                int endX = this.getX() + ADVANCEMENT_SIZE / 2 + scrollX + 3;
                int endY = this.getY() + ADVANCEMENT_SIZE / 2 + scrollY;

                int diffX = Math.abs(endX - startX);
                int diffY = Math.abs(endY - startY);

                boolean verticalAnchors = diffX > diffY;
                if (diffX < ADVANCEMENT_SIZE) verticalAnchors = true;
                else if (diffY < ADVANCEMENT_SIZE) verticalAnchors = false;

                int MAX_BEND_DISTANCE = 64;
                int endAnchorX = verticalAnchors ? endX : endX - Math.min((endX - startX) / 2, MAX_BEND_DISTANCE);
                int endAnchorY = !verticalAnchors ? endY : endY - Math.min((endY - startY) / 2, MAX_BEND_DISTANCE);

                int startAnchorX = verticalAnchors ? startX : endAnchorX;
                int startAnchorY = verticalAnchors ? startY : endAnchorY;

                int thickness = drawInside ? 1 : 0;
                int color = drawInside ? borderLineColor : innerLineColor;

                RenderUtil.line(guiGraphicsExtractor, startX, startY, startAnchorX, startAnchorY, thickness, color);
                RenderUtil.line(guiGraphicsExtractor, startAnchorX, startAnchorY, endAnchorX, endAnchorY, thickness, color);
                RenderUtil.line(guiGraphicsExtractor, endAnchorX, endAnchorY, endX, endY, thickness, color);

                boolean showArrow = (verticalAnchors ? diffY : diffX) > 15;

                if (!drawInside && showArrow && ModConfig.get().drawArrows) {
                    int edgeDistanceX = ADVANCEMENT_SIZE / 2 + 3;
                    int edgeDistanceY = ADVANCEMENT_SIZE / 2 + 3;
                    if (this.displayInfo.getType() == net.minecraft.advancements.AdvancementType.GOAL) {
                        edgeDistanceX += 2;
                        edgeDistanceY += 2;
                    }
                    RenderUtil.drawArrow(guiGraphicsExtractor, endX, endY, endAnchorX, endAnchorY, verticalAnchors, edgeDistanceX, edgeDistanceY, innerLineColor);
                }
            }
        } else {
            int startX = scrollX + parent.x + ADVANCEMENT_SIZE / 2;
            int endXHalf = scrollX + parent.x + ADVANCEMENT_SIZE + 6;
            int startY = scrollY + parent.y + ADVANCEMENT_SIZE / 2;
            int endX = scrollX + this.x + ADVANCEMENT_SIZE / 2;
            int endY = scrollY + this.y + ADVANCEMENT_SIZE / 2;

            if (drawInside) {
                guiGraphicsExtractor.horizontalLine(endXHalf, startX, startY - 1, borderLineColor);
                guiGraphicsExtractor.horizontalLine(endXHalf + 1, startX, startY, borderLineColor);
                guiGraphicsExtractor.horizontalLine(endXHalf, startX, startY + 1, borderLineColor);
                guiGraphicsExtractor.horizontalLine(endX, endXHalf - 1, endY - 1, borderLineColor);
                guiGraphicsExtractor.horizontalLine(endX, endXHalf - 1, endY, borderLineColor);
                guiGraphicsExtractor.horizontalLine(endX, endXHalf - 1, endY + 1, borderLineColor);
                guiGraphicsExtractor.verticalLine(endXHalf - 1, endY, startY, borderLineColor);
                guiGraphicsExtractor.verticalLine(endXHalf + 1, endY, startY, borderLineColor);
            } else {
                guiGraphicsExtractor.horizontalLine(endXHalf, startX, startY, innerLineColor);
                guiGraphicsExtractor.horizontalLine(endX, endXHalf, endY, innerLineColor);
                guiGraphicsExtractor.verticalLine(endXHalf, endY, startY, innerLineColor);

                if (ModConfig.get().drawArrows) {
                    int edgeDistanceX = ADVANCEMENT_SIZE / 2 + 3;
                    int edgeDistanceY = ADVANCEMENT_SIZE / 2 + 3;
                    if (this.displayInfo.getType() == net.minecraft.advancements.AdvancementType.GOAL) {
                        edgeDistanceX += 2;
                        edgeDistanceY += 2;
                    }
                    RenderUtil.drawArrow(guiGraphicsExtractor, endX, endY, endXHalf, endY, false, edgeDistanceX, edgeDistanceY, innerLineColor);
                }
            }
        }
    }

    public void draw(GuiGraphicsExtractor guiGraphicsExtractor, int scrollX, int scrollY, double unzoomedX, double unzoomedY) {
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

                r = (int) (r + (255 - r) * hoverAnim * 0.4f);
                g = (int) (g + (255 - g) * hoverAnim * 0.4f);
                b = (int) (b + (255 - b) * hoverAnim * 0.4f);
                baseColor = 0xFF000000 | (r << 16) | (g << 8) | b;
            }

            int finalColor = isDimmed ? ARGB.multiply(baseColor, 0xFF808080) : baseColor;

            guiGraphicsExtractor.pose().pushMatrix();

            float scale = 1.0f + (hoverAnim * 0.1f);
            float centerX = scrollX + this.x + 3 + ICON_SIZE / 2.0f;
            float centerY = scrollY + this.y + ICON_SIZE / 2.0f;
            guiGraphicsExtractor.pose().translate(centerX, centerY);
            guiGraphicsExtractor.pose().scale(scale, scale);
            guiGraphicsExtractor.pose().translate(-centerX, -centerY);

            guiGraphicsExtractor.blitSprite(RenderPipelines.GUI_TEXTURED, advancementState.frameSprite(this.displayInfo.getType()), scrollX + this.x + 3, scrollY + this.y, ICON_SIZE, ICON_SIZE, finalColor);

            guiGraphicsExtractor.fakeItem(this.displayInfo.getIcon().create(), scrollX + this.x + 8, scrollY + this.y + 5);

            if (isDimmed) {
                guiGraphicsExtractor.fill(RenderPipelines.GUI, scrollX + this.x + 8, scrollY + this.y + 5, scrollX + this.x + 24, scrollY + this.y + 21, 0x80000000);
            }

            guiGraphicsExtractor.pose().popMatrix();
        }

        for (EnhancedAdvancementWidget advancementWidget : this.children) {
            advancementWidget.draw(guiGraphicsExtractor, scrollX, scrollY, unzoomedX, unzoomedY);
        }
    }

    public void getAdvancementProgress(AdvancementProgress advancementProgressIn) {
        this.advancementProgress = advancementProgressIn;
        this.refreshHover();
    }

    public void addGuiAdvancement(EnhancedAdvancementWidget advancementEntryScreen) {
        this.children.add(advancementEntryScreen);
    }

    public void drawHover(GuiGraphicsExtractor guiGraphicsExtractor, int scrollX, int scrollY, int left, int top) {
        if (EnhancedAdvancementsScreen.canEdit() && !ModConfig.get().showTooltipsInEditMode) {
            return;
        }

        this.refreshHover();
        boolean drawLeft = left + scrollX + this.x + this.width + ADVANCEMENT_SIZE >= this.advancementTabGui.getScreen().internalWidth;
        String s = this.advancementProgress == null || this.advancementProgress.getProgressText() == null ? null : this.advancementProgress.getProgressText().getString();
        int i = s == null ? 0 : this.minecraft.font.width(s);
        boolean drawTop;

        if (!ModConfig.get().requiresShift || EnhancedAdvancementsScreen.hasShiftDown()) {
            if (this.criterionGrid.height < this.advancementTabGui.getScreen().height) {
                drawTop = top + scrollY + this.y + this.description.size() * this.minecraft.font.lineHeight + this.criterionGrid.height + 50 >= this.advancementTabGui.getScreen().height;
            } else {
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
        int drawY = scrollY + this.y;
        int drawX;

        if (drawLeft) {
            drawX = scrollX + this.x - this.width + ADVANCEMENT_SIZE + 6;
        } else {
            drawX = scrollX + this.x;
        }
        int boxHeight;

        if (!ModConfig.get().requiresShift || EnhancedAdvancementsScreen.hasShiftDown()) {
            boxHeight = TITLE_SIZE + this.description.size() * this.minecraft.font.lineHeight + this.criterionGrid.height;
        } else {
            boxHeight = TITLE_SIZE + this.description.size() * this.minecraft.font.lineHeight;
        }

        if (!this.description.isEmpty()) {
            if (drawTop) {
                this.render9Sprite(guiGraphicsExtractor, drawX, drawY + ADVANCEMENT_SIZE - boxHeight, this.width, boxHeight, CORNER_SIZE, WIDGET_WIDTH, WIDGET_HEIGHT, 0, 52);
            } else {
                this.render9Sprite(guiGraphicsExtractor, drawX, drawY, this.width, boxHeight, CORNER_SIZE, WIDGET_WIDTH, WIDGET_HEIGHT, 0, 52);
            }
        }

        // Title left side
        int colorLeft = enhancedDisplayInfo.getTitleColor(stateTitleLeft);
        int left_side = Math.min(j, WIDGET_WIDTH - 16);
        guiGraphicsExtractor.blit(RenderPipelines.GUI_TEXTURED, Resources.Gui.WIDGETS, drawX, drawY, 0f, (float) (enhancedDisplayInfo.getTitleYMultiplier(stateTitleLeft) * WIDGET_HEIGHT), left_side, WIDGET_HEIGHT, 256, 256, colorLeft);
        if (left_side < j) {
            guiGraphicsExtractor.blit(RenderPipelines.GUI_TEXTURED, Resources.Gui.WIDGETS, drawX + left_side, drawY, 16f, (float) (enhancedDisplayInfo.getTitleYMultiplier(stateTitleLeft) * WIDGET_HEIGHT), j - left_side, WIDGET_HEIGHT, 256, 256, colorLeft);
        }

        // Title right side
        int colorRight = enhancedDisplayInfo.getTitleColor(stateTitleRight);
        int right_side = Math.min(k, WIDGET_WIDTH - 16);
        guiGraphicsExtractor.blit(RenderPipelines.GUI_TEXTURED, Resources.Gui.WIDGETS, drawX + j, drawY, (float) (WIDGET_WIDTH - right_side), (float) (enhancedDisplayInfo.getTitleYMultiplier(stateTitleRight) * WIDGET_HEIGHT), right_side, WIDGET_HEIGHT, 256, 256, colorRight);
        if (right_side < k) {
            guiGraphicsExtractor.blit(RenderPipelines.GUI_TEXTURED, Resources.Gui.WIDGETS, drawX + j + right_side - 2, drawY, (float) (WIDGET_WIDTH - k + right_side - 2), (float) (enhancedDisplayInfo.getTitleYMultiplier(stateTitleRight) * WIDGET_HEIGHT), k - right_side + 2, WIDGET_HEIGHT, 256, 256, colorRight);
        }

        // Advancement icon
        int iconColor = enhancedDisplayInfo.getIconColor(stateIcon);
        guiGraphicsExtractor.blitSprite(RenderPipelines.GUI_TEXTURED, stateIcon.frameSprite(this.displayInfo.getType()), scrollX + this.x + 3, scrollY + this.y, ICON_SIZE, ICON_SIZE, iconColor);

        if (drawLeft) {
            guiGraphicsExtractor.text(this.minecraft.font, this.title, drawX + 5, scrollY + this.y + 9, -1);

            if (s != null) {
                guiGraphicsExtractor.text(this.minecraft.font, s, scrollX + this.x - i, scrollY + this.y + 9, -1);
            }
        } else {
            guiGraphicsExtractor.text(this.minecraft.font, this.title, scrollX + this.x + 32, scrollY + this.y + 9, -1);

            if (s != null) {
                guiGraphicsExtractor.text(this.minecraft.font, s, scrollX + this.x + this.width - i - 5, scrollY + this.y + 9, -1);
            }
        }

        int yOffset;
        if (drawTop) {
            yOffset = drawY + 26 - boxHeight + 7;
        } else {
            yOffset = scrollY + this.y + 9 + 17;
        }
        for (int k1 = 0; k1 < this.description.size(); ++k1) {
            guiGraphicsExtractor.text(this.minecraft.font, this.description.get(k1), drawX + 5, yOffset + k1 * this.minecraft.font.lineHeight, -5592406, false);
        }
        if (this.criterionGrid != null && (!ModConfig.get().requiresShift || EnhancedAdvancementsScreen.hasShiftDown())) {
            int xOffset = drawX + 5;
            yOffset += this.description.size() * this.minecraft.font.lineHeight;
            for (int colIndex = 0; colIndex < this.criterionGrid.columns.size(); colIndex++) {
                CriterionGrid.Column col = this.criterionGrid.columns.get(colIndex);
                for (int rowIndex = 0; rowIndex < col.cells().size(); rowIndex++) {
                    guiGraphicsExtractor.text(this.minecraft.font, col.cells().get(rowIndex), xOffset, yOffset + rowIndex * this.minecraft.font.lineHeight, -5592406, false);
                }
                xOffset += col.width();
            }
        }

        guiGraphicsExtractor.fakeItem(this.displayInfo.getIcon().create(), scrollX + this.x + 8, scrollY + this.y + 5);
    }

    protected void render9Sprite(GuiGraphicsExtractor guiGraphicsExtractor, int x, int y, int width, int height, int textureHeight, int textureWidth, int textureDistance, int textureX, int textureY) {
        int color = 0xFFFFFFFF;

        // Top left corner
        guiGraphicsExtractor.blit(RenderPipelines.GUI_TEXTURED, Resources.Gui.WIDGETS, x, y, (float) textureX, (float) textureY, textureHeight, textureHeight, 256, 256, color);
        // Top side
        RenderUtil.renderRepeating(Resources.Gui.WIDGETS, guiGraphicsExtractor, x + textureHeight, y, width - textureHeight - textureHeight, textureHeight, textureX + textureHeight, textureY, textureWidth - textureHeight - textureHeight, textureDistance);
        // Top right corner
        guiGraphicsExtractor.blit(RenderPipelines.GUI_TEXTURED, Resources.Gui.WIDGETS, x + width - textureHeight, y, (float) (textureX + textureWidth - textureHeight), (float) textureY, textureHeight, textureHeight, 256, 256, color);
        // Bottom left corner
        guiGraphicsExtractor.blit(RenderPipelines.GUI_TEXTURED, Resources.Gui.WIDGETS, x, y + height - textureHeight, (float) textureX, (float) (textureY + textureDistance - textureHeight), textureHeight, textureHeight, 256, 256, color);
        // Bottom side
        RenderUtil.renderRepeating(Resources.Gui.WIDGETS, guiGraphicsExtractor, x + textureHeight, y + height - textureHeight, width - textureHeight - textureHeight, textureHeight, textureX + textureHeight, textureY + textureDistance - textureHeight, textureWidth - textureHeight - textureHeight, textureDistance);
        // Bottom right corner
        guiGraphicsExtractor.blit(RenderPipelines.GUI_TEXTURED, Resources.Gui.WIDGETS, x + width - textureHeight, y + height - textureHeight, (float) (textureX + textureWidth - textureHeight), (float) (textureY + textureDistance - textureHeight), textureHeight, textureHeight, 256, 256, color);
        // Left side
        RenderUtil.renderRepeating(Resources.Gui.WIDGETS, guiGraphicsExtractor, x, y + textureHeight, textureHeight, height - textureHeight - textureHeight, textureX, textureY + textureHeight, textureWidth, textureDistance - textureHeight - textureHeight);
        // Center
        RenderUtil.renderRepeating(Resources.Gui.WIDGETS, guiGraphicsExtractor, x + textureHeight, y + textureHeight, width - textureHeight - textureHeight, height - textureHeight - textureHeight, textureX + textureHeight, textureY + textureHeight, textureWidth - textureHeight - textureHeight, textureDistance - textureHeight - textureHeight);
        // Right side
        RenderUtil.renderRepeating(Resources.Gui.WIDGETS, guiGraphicsExtractor, x + width - textureHeight, y + textureHeight, textureHeight, height - textureHeight - textureHeight, textureX + textureWidth - textureHeight, textureY + textureHeight, textureWidth, textureDistance - textureHeight - textureHeight);
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