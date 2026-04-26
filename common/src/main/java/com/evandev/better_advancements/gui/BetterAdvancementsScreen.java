package com.evandev.better_advancements.gui;

import com.evandev.better_advancements.reference.Resources;
import com.evandev.better_advancements.util.PersistentData;
import com.evandev.better_advancements.util.RenderUtil;
import com.evandev.better_advancements.platform.Services;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.Map;

public class BetterAdvancementsScreen extends Screen implements ClientAdvancements.Listener {
    private static final Component VERY_SAD_LABEL = Component.translatable("advancements.sad_label");
    private static final Component NO_ADVANCEMENTS_LABEL = Component.translatable("advancements.empty");
    private static final Component TITLE = Component.translatable("gui.advancements");
    private static final int WIDTH = 252, HEIGHT = 140, CORNER_SIZE = 30;
    private static final int SIDE = 30, TOP = 40, BOTTOM = 30, PADDING = 9;
    private static final float MIN_ZOOM = 0.25F, MAX_ZOOM = 2.0F, ZOOM_STEP = 0.15F;

    public static boolean freeformLayoutEditing = true;
    public static boolean enableEditMode = false;
    public static int uiScaling = 100;
    public static boolean showDebugCoordinates = false;
    public static boolean orderTabsAlphabetically = false;

    private static int tabPage, maxPages;
    private final ClientAdvancements clientAdvancements;
    private final Map<AdvancementHolder, BetterAdvancementTab> tabs = Maps.newLinkedHashMap();
    protected int internalWidth, internalHeight;
    private BetterAdvancementTab selectedTab;
    private float zoom = 1.0F;
    private boolean isScrolling;
    private BetterAdvancementWidget advConnectedToMouse = null;
    private AdvancementContextMenu contextMenu = null;

    public BetterAdvancementsScreen(ClientAdvancements clientAdvancements) {
        super(GameNarrator.NO_TITLE);
        this.clientAdvancements = clientAdvancements;
    }

    public float getZoom() {
        return this.zoom;
    }

    @Override
    protected void init() {
        PersistentData.load();
        this.internalHeight = this.height * uiScaling / 100;
        this.internalWidth = this.width * uiScaling / 100;

        this.tabs.clear();
        this.selectedTab = null;
        this.clientAdvancements.setListener(this);

        if (this.selectedTab == null && !this.tabs.isEmpty()) {
            BetterAdvancementTab advancementTab = this.tabs.values().iterator().next();
            this.clientAdvancements.setSelectedTab(advancementTab.getRootNode().holder(), true);
        } else {
            this.clientAdvancements.setSelectedTab(this.selectedTab == null ? null : this.selectedTab.getRootNode().holder(), true);
        }

        int left = SIDE + (width - internalWidth) / 2;
        int top = TOP + (height - internalHeight) / 2;
        int right = internalWidth - SIDE + (width - internalWidth) / 2;
        int bottom = internalHeight - SIDE + (height - internalHeight) / 2;
        int width = right - left;
        int height = bottom - top;

        int maxTabs = BetterAdvancementTabType.getMaxTabs(width, height);

        if (this.tabs.size() > maxTabs) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> tabPage = Math.max(tabPage - 1, 0)).pos(left, bottom + 4).size(20, 20).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> tabPage = Math.min(tabPage + 1, maxPages)).pos(right - 20, bottom + 4).size(20, 20).build());
            maxPages = this.tabs.size() / maxTabs;
            tabPage = Math.min(tabPage, maxPages);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int modifiers) {
        if (this.contextMenu != null) {
            if (this.contextMenu.mouseClicked(mouseX, mouseY, modifiers)) {
                return true;
            } else {
                this.contextMenu = null;
            }
        }

        int left = SIDE + (width - internalWidth) / 2;
        int top = TOP + (height - internalHeight) / 2;
        int right = internalWidth - SIDE + (width - internalWidth) / 2;
        int bottom = internalHeight - SIDE + (height - internalHeight) / 2;
        int width = right - left;
        int height = bottom - top;

        if (modifiers == 0) {
            int maxTabs = BetterAdvancementTabType.getMaxTabs(width, height);
            int skip = tabPage * maxTabs;

            for (BetterAdvancementTab tab : this.tabs.values().stream().skip(skip).limit(maxTabs).toList()) {
                if (tab.isMouseOver(left, top, internalWidth - 2 * SIDE, internalHeight - top - BOTTOM, mouseX, mouseY)) {
                    this.clientAdvancements.setSelectedTab(tab.getRootNode().holder(), true);
                    break;
                }
            }
        } else if (modifiers == 1 && BetterAdvancementsScreen.enableEditMode) {
            boolean inGui = mouseX < left + internalWidth - 2 * SIDE - PADDING && mouseX > left + PADDING && mouseY < top + internalHeight - TOP + 1 && mouseY > top + 2 * PADDING;

            if (inGui) {
                BetterAdvancementWidget hoveredWidget = getHoveredWidget(mouseX, mouseY);
                if (hoveredWidget != null) {
                    this.contextMenu = new AdvancementContextMenu(this, hoveredWidget, (int) mouseX, (int) mouseY);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, modifiers);
    }

    private BetterAdvancementWidget getHoveredWidget(double mouseX, double mouseY) {
        if (this.selectedTab == null) return null;

        int left = SIDE + (width - internalWidth) / 2;
        int top = TOP + (height - internalHeight) / 2;

        double unzoomedX = (mouseX - left - PADDING) / this.zoom;
        double unzoomedY = (mouseY - top - 2 * PADDING) / this.zoom;

        for (BetterAdvancementWidget widget : this.selectedTab.getWidgets().values()) {
            if (widget.isMouseOver(this.selectedTab.scrollX, this.selectedTab.scrollY, unzoomedX, unzoomedY)) {
                return widget;
            }
        }
        return null;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.contextMenu != null) this.contextMenu = null;

        if (this.selectedTab != null) {
            if (Screen.hasControlDown()) {
                int left = SIDE + (width - internalWidth) / 2;
                int top = TOP + (height - internalHeight) / 2;

                double relMouseX = mouseX - (left + PADDING);
                double relMouseY = mouseY - (top + 2 * PADDING);

                float oldZoom = this.zoom;
                this.zoom = Mth.clamp(this.zoom + (float) scrollY * ZOOM_STEP, MIN_ZOOM, MAX_ZOOM);

                if (this.zoom != oldZoom) {
                    double shiftX = (relMouseX / this.zoom) - (relMouseX / oldZoom);
                    double shiftY = (relMouseY / this.zoom) - (relMouseY / oldZoom);
                    this.selectedTab.scroll(shiftX, shiftY, internalWidth - 2 * SIDE - 3 * PADDING, internalHeight - TOP - BOTTOM - 3 * PADDING);
                }
            } else if (Screen.hasShiftDown()) {
                this.selectedTab.scroll(scrollY * 16.0 / this.zoom, 0, internalWidth - 2 * SIDE - 3 * PADDING, internalHeight - TOP - BOTTOM - 3 * PADDING);
            } else {
                this.selectedTab.scroll(scrollX * 16.0 / this.zoom, scrollY * 16.0 / this.zoom, internalWidth - 2 * SIDE - 3 * PADDING, internalHeight - TOP - BOTTOM - 3 * PADDING);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.contextMenu != null) this.contextMenu = null;

        if (this.minecraft.options.keyAdvancements.matches(keyCode, scanCode)) {
            this.minecraft.setScreen(null);
            this.minecraft.mouseHandler.grabMouse();
            return true;
        } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.advConnectedToMouse != null) {
            Services.PLATFORM.getEventHelper().postAdvancementMovementEvent(this.advConnectedToMouse);
            this.advConnectedToMouse = null;
            if (BetterAdvancementsScreen.freeformLayoutEditing) {
                PersistentData.save(this.tabs);
            }
        }
        this.isScrolling = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void removed() {
        if (BetterAdvancementsScreen.freeformLayoutEditing) {
            PersistentData.save(this.tabs);
        }
        super.removed();
    }

    @Override
    public void onClose() {
        this.clientAdvancements.setListener(null);
        ClientPacketListener clientpacketlistener = this.minecraft.getConnection();
        if (clientpacketlistener != null) {
            clientpacketlistener.send(ServerboundSeenAdvancementsPacket.closedScreen());
        }
        super.onClose();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double mouseDeltaX, double mouseDeltaY) {
        if (this.contextMenu != null) this.contextMenu = null;

        int left = SIDE + (width - internalWidth) / 2;
        int top = TOP + (height - internalHeight) / 2;

        if (button != 0 && button != 2) {
            this.isScrolling = false;
            return false;
        }

        if (!this.isScrolling) {
            if (this.advConnectedToMouse == null) {
                boolean inGui = mouseX < left + internalWidth - 2 * SIDE - PADDING && mouseX > left + PADDING && mouseY < top + internalHeight - TOP + 1 && mouseY > top + 2 * PADDING;
                if (this.selectedTab != null && inGui) {
                    for (BetterAdvancementWidget betterAdvancementEntryScreen : this.selectedTab.getWidgets().values()) {
                        if (betterAdvancementEntryScreen.isMouseOver(this.selectedTab.scrollX, this.selectedTab.scrollY, (mouseX - left - PADDING) / this.zoom, (mouseY - top - 2 * PADDING) / this.zoom)) {
                            if ((BetterAdvancementsScreen.freeformLayoutEditing || betterAdvancementEntryScreen.betterDisplayInfo.allowDragging()) && button == 0) {
                                this.advConnectedToMouse = betterAdvancementEntryScreen;
                                break;
                            }
                        }
                    }
                }
            } else {
                double unzoomedDeltaX = mouseDeltaX / this.zoom;
                double unzoomedDeltaY = mouseDeltaY / this.zoom;

                int newPosX = (int) Math.round(this.advConnectedToMouse.getX() + unzoomedDeltaX);
                int newPosY = (int) Math.round(this.advConnectedToMouse.getY() + unzoomedDeltaY);

                if (Screen.hasShiftDown()) {
                    newPosX = 4 * (newPosX / 4);
                    newPosY = 4 * (newPosY / 4);
                }

                this.advConnectedToMouse.setX(newPosX);
                this.advConnectedToMouse.setY(newPosY);
            }
        } else {
            if (this.advConnectedToMouse != null) {
                Services.PLATFORM.getEventHelper().postAdvancementMovementEvent(advConnectedToMouse);
            }
            this.advConnectedToMouse = null;
        }

        if (this.advConnectedToMouse == null) {
            if (!this.isScrolling) {
                this.isScrolling = true;
            } else if (this.selectedTab != null) {
                this.selectedTab.scroll(mouseDeltaX / this.zoom, mouseDeltaY / this.zoom, internalWidth - 2 * SIDE - 3 * PADDING, internalHeight - TOP - BOTTOM - 3 * PADDING);
            }
        }
        return true;
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        int left = SIDE + (width - internalWidth) / 2;
        int top = TOP + (height - internalHeight) / 2;

        int right = internalWidth - SIDE + (width - internalWidth) / 2;
        int bottom = internalHeight - SIDE + (height - internalHeight) / 2;

        int width = right - left;
        int height = bottom - top;

        int maxTabs = BetterAdvancementTabType.getMaxTabs(width, height);
        int skip = tabPage * maxTabs;

        this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
        if (maxPages != 0) {
            Component page = Component.literal(String.format("%d / %d", tabPage + 1, maxPages + 1));
            int textWidth = this.font.width(page);
            guiGraphics.drawString(this.font, page.getVisualOrderText(), left + (internalWidth - textWidth) / 2 - textWidth, bottom + 8, -1);
            super.render(guiGraphics, mouseX, mouseY, partialTicks);
        }
        this.renderInside(guiGraphics, mouseX, mouseY, left, top, right, bottom, maxTabs, skip);
        this.renderWindow(guiGraphics, left, top, right, bottom, maxTabs, skip);

        if (this.advConnectedToMouse == null && this.contextMenu == null) {
            this.renderToolTips(guiGraphics, mouseX, mouseY, left, top, right, bottom, maxTabs, skip);
        }

        if (this.advConnectedToMouse != null) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(left + PADDING, top + 2 * PADDING, 0);
            guiGraphics.pose().scale(zoom, zoom, 1.0F);

            for (BetterAdvancementWidget betterAdvancementEntryScreen : this.selectedTab.widgets.values()) {
                if (betterAdvancementEntryScreen != this.advConnectedToMouse) {
                    int x1 = betterAdvancementEntryScreen.x + this.selectedTab.scrollX + 3;
                    int x2 = this.advConnectedToMouse.x + this.selectedTab.scrollX + 3;
                    int y1 = betterAdvancementEntryScreen.y + this.selectedTab.scrollY;
                    int y2 = this.advConnectedToMouse.y + this.selectedTab.scrollY;
                    int centerX1 = x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE / 2;
                    int centerX2 = x2 + BetterAdvancementWidget.ADVANCEMENT_SIZE / 2;
                    int centerY1 = y1 + BetterAdvancementWidget.ADVANCEMENT_SIZE / 2;
                    int centerY2 = y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE / 2;
                    double degrees = Math.toDegrees(Math.atan2(centerX1 - centerX2, centerY1 - centerY2));
                    if (degrees < 0) {
                        degrees += 360;
                    }

                    if (betterAdvancementEntryScreen.x == this.advConnectedToMouse.x) {
                        if (y1 > y2) {
                            RenderUtil.drawRect(guiGraphics, x1, y1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, x2, y2, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, x2, y1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x2, y1, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, x2, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y2, x2, y2, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, x2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y2, 1, 0x00FF00);
                        } else {
                            RenderUtil.drawRect(guiGraphics, x1, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, x2, y1, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, x2, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y2, x2, y2, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, x2, y1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x2, y1, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, x2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, 1, 0x00FF00);
                        }
                    }
                    if (betterAdvancementEntryScreen.y == this.advConnectedToMouse.y) {
                        if (x1 > x2) {
                            RenderUtil.drawRect(guiGraphics, x2, y1, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y2, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x1, y1, x1, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x2, y1, x2, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x2, y1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                        } else {
                            RenderUtil.drawRect(guiGraphics, x2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x1, y2, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x2, y1, x2, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x1, y1, x1, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, x1, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                        }
                    }
                    if (degrees == 45 || degrees == 135 || degrees == 225 || degrees == 315) {
                        RenderUtil.drawRect(guiGraphics, x1, y1, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, 1, 0x00FF00);
                        RenderUtil.drawRect(guiGraphics, x1, y1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                        RenderUtil.drawRect(guiGraphics, x1, y1, x1, y1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                        RenderUtil.drawRect(guiGraphics, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);

                        RenderUtil.drawRect(guiGraphics, x2, y2, x2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y2, 1, 0x00FF00);
                        RenderUtil.drawRect(guiGraphics, x2, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, x2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                        RenderUtil.drawRect(guiGraphics, x2, y2, x2, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                        RenderUtil.drawRect(guiGraphics, x2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y2, x2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);

                        if (degrees == 45 || degrees == 225) {
                            RenderUtil.drawRect(guiGraphics, x1, y1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, x2, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y1, x2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y2, 1, 0x00FF00);
                        } else {
                            RenderUtil.drawRect(guiGraphics, x1, y1, x2, y2, 1, 0x00FF00);
                            RenderUtil.drawRect(guiGraphics, x1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y1 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, x2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, y2 + BetterAdvancementWidget.ADVANCEMENT_SIZE - 1, 1, 0x00FF00);
                        }
                    }
                }
            }
            guiGraphics.pose().popPose();
        }

        if (BetterAdvancementsScreen.showDebugCoordinates && this.selectedTab != null && mouseX < internalWidth - SIDE - PADDING && mouseX > SIDE + PADDING && mouseY < internalHeight - top + 1 && mouseY > top + PADDING * 2) {
            if (this.advConnectedToMouse != null) {
                int currentX = (int) ((this.advConnectedToMouse.x + this.selectedTab.scrollX + 4) * zoom) + left + PADDING;
                int currentY = (int) ((this.advConnectedToMouse.y + this.selectedTab.scrollY) * zoom) + top + 2 * PADDING - font.lineHeight + 1;
                guiGraphics.drawString(font, this.advConnectedToMouse.x + "," + this.advConnectedToMouse.y, currentX, currentY, 0x000000);
            } else {
                int xMouse = (int) ((mouseX - left - PADDING) / zoom);
                int yMouse = (int) ((mouseY - top - 2 * PADDING) / zoom);
                int currentX = xMouse - this.selectedTab.scrollX - 4;
                int currentY = yMouse - this.selectedTab.scrollY - 1;
                guiGraphics.drawString(font, currentX + "," + currentY, mouseX, mouseY - font.lineHeight, 0x000000);
            }
        }

        if (this.contextMenu != null) {
            this.contextMenu.render(guiGraphics, mouseX, mouseY, partialTicks);
        }
    }

    private void renderInside(GuiGraphics guiGraphics, int mouseX, int mouseY, int left, int top, int right, int bottom, int maxTabs, int skip) {
        BetterAdvancementTab betterAdvancementTab = this.selectedTab;
        int boxLeft = left + PADDING;
        int boxTop = top + 2 * PADDING;
        int boxRight = right - PADDING;
        int boxBottom = bottom - PADDING;

        int width = boxRight - boxLeft;
        int height = boxBottom - boxTop;

        if (betterAdvancementTab == null) {
            guiGraphics.fill(boxLeft, boxTop, boxRight, boxBottom, -16777216);
            guiGraphics.drawString(this.font, NO_ADVANCEMENTS_LABEL, boxLeft + (width - this.font.width(NO_ADVANCEMENTS_LABEL)) / 2, boxTop + height / 2 - this.font.lineHeight, -1);
            guiGraphics.drawString(this.font, VERY_SAD_LABEL, boxLeft + (width - this.font.width(VERY_SAD_LABEL)) / 2, boxTop + height / 2 + this.font.lineHeight, -1);
        } else {
            betterAdvancementTab.drawContents(guiGraphics, boxLeft, boxTop, width, height);
        }
    }

    public void renderWindow(GuiGraphics guiGraphics, int left, int top, int right, int bottom, int maxTabs, int skip) {
        RenderSystem.enableBlend();
        guiGraphics.blit(Resources.Gui.WINDOW, left, top, 0, 0, CORNER_SIZE, CORNER_SIZE);
        RenderUtil.renderRepeating(Resources.Gui.WINDOW, guiGraphics, left + CORNER_SIZE, top, internalWidth - CORNER_SIZE - 2 * SIDE - CORNER_SIZE, CORNER_SIZE, CORNER_SIZE, 0, WIDTH - CORNER_SIZE - CORNER_SIZE, CORNER_SIZE);
        guiGraphics.blit(Resources.Gui.WINDOW, right - CORNER_SIZE, top, WIDTH - CORNER_SIZE, 0, CORNER_SIZE, CORNER_SIZE);
        RenderUtil.renderRepeating(Resources.Gui.WINDOW, guiGraphics, left, top + CORNER_SIZE, CORNER_SIZE, bottom - top - 2 * CORNER_SIZE, 0, CORNER_SIZE, CORNER_SIZE, HEIGHT - CORNER_SIZE - CORNER_SIZE);
        RenderUtil.renderRepeating(Resources.Gui.WINDOW, guiGraphics, right - CORNER_SIZE, top + CORNER_SIZE, CORNER_SIZE, bottom - top - 2 * CORNER_SIZE, WIDTH - CORNER_SIZE, CORNER_SIZE, CORNER_SIZE, HEIGHT - CORNER_SIZE - CORNER_SIZE);
        guiGraphics.blit(Resources.Gui.WINDOW, left, bottom - CORNER_SIZE, 0, HEIGHT - CORNER_SIZE, CORNER_SIZE, CORNER_SIZE);
        RenderUtil.renderRepeating(Resources.Gui.WINDOW, guiGraphics, left + CORNER_SIZE, bottom - CORNER_SIZE, internalWidth - CORNER_SIZE - 2 * SIDE - CORNER_SIZE, CORNER_SIZE, CORNER_SIZE, HEIGHT - CORNER_SIZE, WIDTH - CORNER_SIZE - CORNER_SIZE, CORNER_SIZE);
        guiGraphics.blit(Resources.Gui.WINDOW, right - CORNER_SIZE, bottom - CORNER_SIZE, WIDTH - CORNER_SIZE, HEIGHT - CORNER_SIZE, CORNER_SIZE, CORNER_SIZE);

        int width = right - left;
        int height = bottom - top;

        if (this.tabs.size() > 1) {
            for (BetterAdvancementTab tab : this.tabs.values().stream().skip(skip).limit(maxTabs).toList()) {
                tab.drawTab(guiGraphics, left, top, width, height, tab == this.selectedTab);
            }

            RenderSystem.defaultBlendFunc();

            for (BetterAdvancementTab tab : this.tabs.values().stream().skip(skip).limit(maxTabs).toList()) {
                tab.drawIcon(guiGraphics, left, top, width, height);
            }

            RenderSystem.disableBlend();
        }

        FormattedCharSequence windowTitle = TITLE.getVisualOrderText();
        if (selectedTab != null) {
            windowTitle = FormattedCharSequence.composite(
                    windowTitle,
                    Component.literal(" - ").getVisualOrderText(),
                    selectedTab.getTitle().getVisualOrderText()
            );
        }
        guiGraphics.drawString(this.font, windowTitle, left + 8, top + 6, 4210752, false);
    }

    private void renderToolTips(GuiGraphics guiGraphics, int mouseX, int mouseY, int left, int top, int right, int bottom, int maxTabs, int skip) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (this.selectedTab != null) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(left + PADDING, top + 2 * PADDING, 400.0D);
            RenderSystem.enableDepthTest();
            this.selectedTab.drawToolTips(guiGraphics, mouseX - left - PADDING, mouseY - top - 2 * PADDING, left, top, right - left - 2 * PADDING, bottom - top - 3 * PADDING);
            RenderSystem.disableDepthTest();
            guiGraphics.pose().popPose();
        }

        int width = right - left;
        int height = bottom - top;

        if (this.tabs.size() > 1) {
            for (BetterAdvancementTab tab : this.tabs.values().stream().skip(skip).limit(maxTabs).toList()) {
                if (tab.isMouseOver(left, top, width, height, mouseX, mouseY)) {
                    guiGraphics.renderTooltip(this.font, tab.getTitle(), mouseX, mouseY);
                }
            }
        }
    }

    @Override
    public void onAddAdvancementRoot(AdvancementNode advancement) {
        BetterAdvancementTab betterAdvancementTabGui = BetterAdvancementTab.create(this.minecraft, this, this.tabs.size(), advancement, internalWidth - 2 * SIDE, internalHeight - TOP - SIDE);
        if (betterAdvancementTabGui != null) {
            this.tabs.put(advancement.holder(), betterAdvancementTabGui);
        }
    }

    @Override
    public void onRemoveAdvancementRoot(AdvancementNode advancement) {
    }

    @Override
    public void onAddAdvancementTask(AdvancementNode advancement) {
        BetterAdvancementTab betterAdvancementTabGui = this.getTab(advancement);
        if (betterAdvancementTabGui != null) {
            betterAdvancementTabGui.addAdvancement(advancement);
        }
    }

    @Override
    public void onRemoveAdvancementTask(AdvancementNode advancement) {
    }

    @Override
    public void onUpdateAdvancementProgress(AdvancementNode advancement, AdvancementProgress advancementProgress) {
        BetterAdvancementWidget betterAdvancementEntryScreen = this.getAdvancementWidget(advancement);
        if (betterAdvancementEntryScreen != null) {
            betterAdvancementEntryScreen.getAdvancementProgress(advancementProgress);
        }
    }

    @Override
    public void onSelectedTabChanged(AdvancementHolder advancement) {
        if (this.selectedTab != null) {
            this.selectedTab.storeScroll();
        }
        this.selectedTab = this.tabs.get(advancement);
        if (this.selectedTab != null) {
            this.selectedTab.loadScroll();
        }
    }

    @Override
    public void onAdvancementsCleared() {
        this.tabs.clear();
        this.selectedTab = null;
    }

    public BetterAdvancementWidget getAdvancementWidget(AdvancementNode advancement) {
        BetterAdvancementTab betterAdvancementTab = this.getTab(advancement);
        return betterAdvancementTab == null ? null : betterAdvancementTab.getWidget(advancement.holder());
    }

    private BetterAdvancementTab getTab(AdvancementNode advancement) {
        AdvancementNode advancementNode = advancement.root();
        return this.tabs.get(advancementNode.holder());
    }
}