package com.evandev.reliable_advancements.gui;

import com.evandev.reliable_advancements.advancements.DisplayCompat;
import com.evandev.reliable_advancements.advancements.AdvancementDisplayInfo;
import com.evandev.reliable_advancements.api.IAdvancementEntryGui;
import com.evandev.reliable_advancements.api.event.IAdvancementDrawConnectionsEvent;
import com.evandev.reliable_advancements.client.ClientRewardTracker;
import com.evandev.reliable_advancements.client.ClientTabStore;
import com.evandev.reliable_advancements.config.ModConfig;
import com.evandev.reliable_advancements.gui.screens.EnhancedAdvancementsScreen;
import com.evandev.reliable_advancements.platform.Services;
import com.evandev.reliable_advancements.tabs.TabDefinition;
import com.evandev.reliable_advancements.util.ConnectionRouter;
import com.evandev.reliable_advancements.util.CriterionGrid;
import com.evandev.reliable_advancements.util.RenderUtil;
import com.google.common.collect.Lists;
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

import java.util.*;

//? if <26.1 {
import com.mojang.blaze3d.systems.RenderSystem;
//?}

public class EnhancedAdvancementWidget implements IAdvancementEntryGui {
    public static final int ADVANCEMENT_SIZE = 26;
    private static final ResourceLocation TITLE_BOX_SPRITE = ResourceLocation.withDefaultNamespace("advancements/title_box");
    private static final int WIDGET_WIDTH = 256;
    private static final int WIDGET_HEIGHT = 26;
    private static final int TITLE_SIZE = 32;
    private static final int ICON_SIZE = 26;
    private static final int BOX_TEX_WIDTH = 200;
    private static final int BOX_TEX_HEIGHT = 26;
    private static final int CAP_WIDTH = 3;
    private static final int MIDDLE_TEX_WIDTH = BOX_TEX_WIDTH - CAP_WIDTH * 2;
    public final AdvancementDisplayInfo enhancedDisplayInfo;
    private final AdvancementNode advancementNode;
    private final DisplayInfo displayInfo;
    private final String title;
    private final Minecraft minecraft;
    private final List<EnhancedAdvancementWidget> children = Lists.newArrayList();
    private final List<EnhancedAdvancementWidget> parents = Lists.newArrayList();
    public AdvancementProgress advancementProgress;
    protected int x, y;
    private EnhancedAdvancementTab advancementTabGui;
    private int width;
    private List<FormattedCharSequence> description;
    private CriterionGrid criterionGrid;
    private float hoverAnim = 0.0f;
    private ConnectionRouter.Side incomingSide = ConnectionRouter.Side.NONE;

    public EnhancedAdvancementWidget(EnhancedAdvancementTab advancementTabGui, Minecraft mc, AdvancementNode advancementNode, DisplayInfo displayInfo) {
        this.advancementTabGui = advancementTabGui;
        this.advancementNode = advancementNode;
        this.enhancedDisplayInfo = advancementTabGui.getDisplayInfo(this.advancementNode);
        this.displayInfo = displayInfo;
        this.minecraft = mc;
        this.title = DisplayCompat.title(displayInfo).getString(163);
        this.x = this.enhancedDisplayInfo.getPosX() != null ? this.enhancedDisplayInfo.getPosX() : Mth.floor(DisplayCompat.x(advancementNode) * TabDefinition.PIXELS_PER_COLUMN);
        this.y = this.enhancedDisplayInfo.getPosY() != null ? this.enhancedDisplayInfo.getPosY() : Mth.floor(DisplayCompat.y(advancementNode) * TabDefinition.PIXELS_PER_ROW);
        int[] saved = ClientTabStore.savedPosition(advancementTabGui.getId(), this.advancementNode.holder().id());
        if (saved != null) {
            this.x = saved[0];
            this.y = saved[1];
        }
    }

    public void setTab(EnhancedAdvancementTab tab) {
        this.advancementTabGui = tab;
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

        if (!ModConfig.get().requiresShift || GuiCompat.hasShiftDown()) {
            maxWidth = Math.max(titleWidth, this.criterionGrid.width);
        } else {
            maxWidth = titleWidth;
        }
        this.description = Language.getInstance().getVisualOrder(
                this.findOptimalLines(ComponentUtils.mergeStyles(
                        DisplayCompat.description(displayInfo).copy(),
                        Style.EMPTY.withColor(DisplayCompat.type(displayInfo).getChatColor())
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

    public boolean shouldRender() {
        if (EnhancedAdvancementsScreen.canEdit()) return true;

        boolean isDone = this.advancementProgress != null && this.advancementProgress.isDone();
        boolean isClaimed = !ModConfig.get().requireRewardClaiming || ClientRewardTracker.isClaimed(this.advancementNode.holder().id());
        if (isDone && isClaimed) return true;

        if (hasDoneDescendant(new HashSet<>())) return true;
        if (DisplayCompat.hidden(this.displayInfo)) return false;

        int depth = ModConfig.get().visibilityDepth;
        if (depth < 0) {
            return isUnhiddenPathFromRoot(new HashSet<>());
        }
        if (depth == 0) {
            return false;
        }

        return isAncestorVisible(0, depth, new HashSet<>());
    }

    private boolean hasDoneDescendant(Set<EnhancedAdvancementWidget> visited) {
        if (!visited.add(this)) return false;
        for (EnhancedAdvancementWidget child : this.children) {
            if (child != null) {
                boolean childDone = child.advancementProgress != null && child.advancementProgress.isDone();
                boolean childClaimed = !ModConfig.get().requireRewardClaiming || ClientRewardTracker.isClaimed(child.getAdvancement().holder().id());
                if (childDone && childClaimed) {
                    return true;
                }
                if (child.hasDoneDescendant(visited)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isUnhiddenPathFromRoot(Set<EnhancedAdvancementWidget> visited) {
        if (this.parents.isEmpty()) return true;
        if (!visited.add(this)) return false;

        for (EnhancedAdvancementWidget p : this.parents) {
            if (p == null) continue;
            boolean parentCompleted = p.advancementProgress != null && p.advancementProgress.isDone();
            boolean parentClaimed = !ModConfig.get().requireRewardClaiming || ClientRewardTracker.isClaimed(p.getAdvancement().holder().id());
            if (parentCompleted && parentClaimed) {
                return true;
            }
            if (!DisplayCompat.hidden(p.displayInfo) && p.isUnhiddenPathFromRoot(visited)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAncestorVisible(int currentDepth, int maxDepth, Set<EnhancedAdvancementWidget> visited) {
        if (this.parents.isEmpty()) return true;
        if (currentDepth >= maxDepth || !visited.add(this)) return false;

        for (EnhancedAdvancementWidget p : this.parents) {
            if (p == null) continue;
            boolean parentCompleted = p.advancementProgress != null && p.advancementProgress.isDone();
            boolean parentClaimed = !ModConfig.get().requireRewardClaiming || ClientRewardTracker.isClaimed(p.getAdvancement().holder().id());
            if (parentCompleted && parentClaimed) {
                return true;
            }
            if (!DisplayCompat.hidden(p.displayInfo) && p.isAncestorVisible(currentDepth + 1, maxDepth, visited)) {
                return true;
            }
        }
        return false;
    }

    public void drawConnectivity(GuiGraphics guiGraphics, int scrollX, int scrollY, boolean drawInside) {
        // Check if connections should be drawn at all
        if (this.shouldRender() && (EnhancedAdvancementsScreen.canEdit() || !this.enhancedDisplayInfo.hideLines())) {
            // Draw connections to all parents
            for (EnhancedAdvancementWidget p : this.parents) {
                if (p != null && p.shouldRender()) {
                    this.drawConnection(guiGraphics, p, scrollX, scrollY, drawInside);
                }
            }
            // Create and post event to get extra connections
            IAdvancementDrawConnectionsEvent event = Services.PLATFORM.getEventHelper().postAdvancementDrawConnectionsEvent(this.advancementNode);
            // Draw extra connections from event
            for (AdvancementHolder parent : event.getExtraConnections()) {
                final EnhancedAdvancementWidget parentGui = this.advancementTabGui.getWidget(parent.id());
                if (parentGui != null && parentGui.shouldRender() && !this.parents.contains(parentGui)) {
                    this.drawConnection(guiGraphics, parentGui, scrollX, scrollY, drawInside);
                }
            }
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

        boolean goalFrame = DisplayCompat.type(this.displayInfo) == AdvancementType.GOAL;

        if (this.enhancedDisplayInfo.drawDirectLines()) {
            if (parent == this.getParent()) {
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
            if (parent == this.getParent()) {
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
                } else if (isCompleted) {
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

            GuiCompat.push(guiGraphics);

            float scale = 1.0f + (hoverAnim * 0.1f);
            float centerX = scrollX + this.x + 3 + ICON_SIZE / 2.0f;
            float centerY = scrollY + this.y + ICON_SIZE / 2.0f;
            GuiCompat.translate(guiGraphics, centerX, centerY, 0);
            GuiCompat.scale(guiGraphics, scale, scale);
            GuiCompat.translate(guiGraphics, -centerX, -centerY, 0);

            //? if <26.1 {
            RenderUtil.setColor(baseColor);
            RenderSystem.enableBlend();

            if (isDimmed) {
                RenderSystem.setShaderColor(0.5F, 0.5F, 0.5F, 1.0F);
            }

            guiGraphics.blitSprite(advancementState.frameSprite(DisplayCompat.type(this.displayInfo)), scrollX + this.x + 3, scrollY + this.y, ICON_SIZE, ICON_SIZE);

            if (isDimmed) {
                RenderSystem.setShaderColor(0.25F, 0.25F, 0.25F, 1.0F);
            } else {
                RenderUtil.setColor(enhancedDisplayInfo.defaultIconColor());
            }

            guiGraphics.renderFakeItem(DisplayCompat.icon(this.displayInfo), scrollX + this.x + 8, scrollY + this.y + 5);

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            //?} else {
            /*GuiCompat.blitSprite(guiGraphics, advancementState.frameSprite(DisplayCompat.type(this.displayInfo)), scrollX + this.x + 3, scrollY + this.y, ICON_SIZE, ICON_SIZE, isDimmed ? 0x808080 : baseColor);
            guiGraphics.renderFakeItem(DisplayCompat.icon(this.displayInfo), scrollX + this.x + 8, scrollY + this.y + 5);
            if (isDimmed) {
                guiGraphics.fill(scrollX + this.x + 8, scrollY + this.y + 5, scrollX + this.x + 24, scrollY + this.y + 21, 0xC0000000);
            }
            *///?}
            GuiCompat.pop(guiGraphics);
        }
    }

    public void getAdvancementProgress(AdvancementProgress advancementProgressIn) {
        this.advancementProgress = advancementProgressIn;
    }

    public void drawHover(GuiGraphics guiGraphics, int scrollX, int scrollY, int left, int top) {
        if (EnhancedAdvancementsScreen.canEdit() && !ModConfig.get().showTooltipsInEditMode) {
            return;
        }

        this.refreshHover();
        boolean drawLeft = left + scrollX + this.x + this.width + ADVANCEMENT_SIZE >= this.advancementTabGui.getScreen().internalWidth;
        String s = this.advancementProgress == null || this.advancementProgress.getProgressText() == null ? null : this.advancementProgress.getProgressText().getString();
        int i = s == null ? 0 : this.minecraft.font.width(s);
        boolean showCriteria = this.criterionGrid != null && this.criterionGrid.height > 0 && (!ModConfig.get().requiresShift || GuiCompat.hasShiftDown());
        boolean drawTop;
        int totalContentHeight = this.description.size() * this.minecraft.font.lineHeight + (showCriteria ? this.criterionGrid.height : 0);

        if (showCriteria && this.criterionGrid.height >= this.advancementTabGui.getScreen().height) {
            drawTop = false;
        } else {
            drawTop = top + scrollY + this.y + totalContentHeight + 50 >= this.advancementTabGui.getScreen().height;
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

        GuiCompat.enableBlend();
        int drawY = scrollY + this.y;
        int drawX;

        if (drawLeft) {
            drawX = scrollX + this.x - this.width + ADVANCEMENT_SIZE + 6;
        } else {
            drawX = scrollX + this.x;
        }
        int boxHeight = TITLE_SIZE + this.description.size() * this.minecraft.font.lineHeight + (showCriteria ? this.criterionGrid.height : 0);

        boolean hasBody = !this.description.isEmpty() || showCriteria;
        if (hasBody) {
            if (drawTop) {
                GuiCompat.blitSprite(guiGraphics, TITLE_BOX_SPRITE, drawX, drawY + ADVANCEMENT_SIZE - boxHeight, this.width, boxHeight);
            } else {
                GuiCompat.blitSprite(guiGraphics, TITLE_BOX_SPRITE, drawX, drawY, this.width, boxHeight);
            }
        }

        this.drawTitleBox(guiGraphics, drawX, drawY, this.width, j, stateTitleLeft, stateTitleRight);

        GuiCompat.blitSprite(guiGraphics, stateIcon.frameSprite(DisplayCompat.type(this.displayInfo)), scrollX + this.x + 3, scrollY + this.y, ICON_SIZE, ICON_SIZE, enhancedDisplayInfo.getIconColor(stateIcon));
        //? if <26.1 {
        RenderUtil.setColor(enhancedDisplayInfo.defaultIconColor());
        //?}

        if (drawLeft) {
            GuiCompat.drawString(guiGraphics, this.minecraft.font, this.title, drawX + 5, scrollY + this.y + 9, -1);

            if (s != null) {
                GuiCompat.drawString(guiGraphics, this.minecraft.font, s, scrollX + this.x - i, scrollY + this.y + 9, -1);
            }
        } else {
            GuiCompat.drawString(guiGraphics, this.minecraft.font, this.title, scrollX + this.x + 32, scrollY + this.y + 9, -1);

            if (s != null) {
                GuiCompat.drawString(guiGraphics, this.minecraft.font, s, scrollX + this.x + this.width - i - 5, scrollY + this.y + 9, -1);
            }
        }

        int yOffset;
        if (drawTop) {
            yOffset = drawY + 26 - boxHeight + 7;
        } else {
            yOffset = scrollY + this.y + 9 + 17;
        }
        for (int k1 = 0; k1 < this.description.size(); ++k1) {
            GuiCompat.drawString(guiGraphics, this.minecraft.font, this.description.get(k1), drawX + 5, yOffset + k1 * this.minecraft.font.lineHeight, -5592406, false);
        }
        if (showCriteria) {
            int xOffset = drawX + 5;
            yOffset += this.description.size() * this.minecraft.font.lineHeight;
            for (int colIndex = 0; colIndex < this.criterionGrid.columns.size(); colIndex++) {
                CriterionGrid.Column col = this.criterionGrid.columns.get(colIndex);
                for (int rowIndex = 0; rowIndex < col.cells().size(); rowIndex++) {
                    GuiCompat.drawString(guiGraphics, this.minecraft.font, col.cells().get(rowIndex), xOffset, yOffset + rowIndex * this.minecraft.font.lineHeight, -5592406, false);
                }
                xOffset += col.width();
            }
        }

        guiGraphics.renderFakeItem(DisplayCompat.icon(this.displayInfo), scrollX + this.x + 8, scrollY + this.y + 5);
    }

    private void drawTitleBox(GuiGraphics guiGraphics, int x, int y, int width, int j,
                              AdvancementWidgetType stateLeft, AdvancementWidgetType stateRight) {
        int k = width - j;
        if (stateLeft == stateRight) {
            drawBoxSection(guiGraphics, stateLeft.boxSprite(), x, y, width, true, true, enhancedDisplayInfo.getTitleColor(stateLeft));
        } else {
            drawBoxSection(guiGraphics, stateLeft.boxSprite(), x, y, j, true, false, enhancedDisplayInfo.getTitleColor(stateLeft));
            drawBoxSection(guiGraphics, stateRight.boxSprite(), x + j, y, k, false, true, enhancedDisplayInfo.getTitleColor(stateRight));
        }
    }

    private void drawBoxSection(GuiGraphics guiGraphics, ResourceLocation sprite, int x, int y, int sectionWidth,
                                boolean hasLeftCap, boolean hasRightCap, int color) {
        if (sectionWidth <= 0) return;

        int leftCapWidth = hasLeftCap ? Math.min(CAP_WIDTH, sectionWidth) : 0;
        int rightCapWidth = hasRightCap ? Math.min(CAP_WIDTH, sectionWidth - leftCapWidth) : 0;
        int middleWidth = sectionWidth - leftCapWidth - rightCapWidth;

        if (leftCapWidth > 0) {
            GuiCompat.blitSprite(guiGraphics, sprite, BOX_TEX_WIDTH, BOX_TEX_HEIGHT, 0, 0, x, y, leftCapWidth, BOX_TEX_HEIGHT, color);
        }

        int middleX = x + leftCapWidth;
        for (int offset = 0; offset < middleWidth; offset += MIDDLE_TEX_WIDTH) {
            int chunk = Math.min(MIDDLE_TEX_WIDTH, middleWidth - offset);
            GuiCompat.blitSprite(guiGraphics, sprite, BOX_TEX_WIDTH, BOX_TEX_HEIGHT, CAP_WIDTH, 0, middleX + offset, y, chunk, BOX_TEX_HEIGHT, color);
        }

        if (rightCapWidth > 0) {
            int rightCapX = x + sectionWidth - rightCapWidth;
            int uOffset = BOX_TEX_WIDTH - rightCapWidth;
            GuiCompat.blitSprite(guiGraphics, sprite, BOX_TEX_WIDTH, BOX_TEX_HEIGHT, uOffset, 0, rightCapX, y, rightCapWidth, BOX_TEX_HEIGHT, color);
        }
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

    public void link(EnhancedAdvancementWidget parent) {
        if (parent == null || parent == this || this.parents.contains(parent)) return;
        this.parents.add(parent);
        parent.children.add(this);
    }

    public void unlink(EnhancedAdvancementWidget parent) {
        if (parent != null && this.parents.remove(parent)) {
            parent.children.remove(this);
        }
    }

    public void unlinkAll() {
        for (EnhancedAdvancementWidget parent : new ArrayList<>(this.parents)) {
            unlink(parent);
        }
        for (EnhancedAdvancementWidget child : new ArrayList<>(this.children)) {
            child.unlink(this);
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

    public List<EnhancedAdvancementWidget> getParents() {
        return Collections.unmodifiableList(this.parents);
    }

    public EnhancedAdvancementWidget getParent() {
        return this.parents.isEmpty() ? null : this.parents.getFirst();
    }

    public List<EnhancedAdvancementWidget> getChildren() {
        return Collections.unmodifiableList(this.children);
    }

    @Override
    public AdvancementNode getAdvancement() {
        return this.advancementNode;
    }
}