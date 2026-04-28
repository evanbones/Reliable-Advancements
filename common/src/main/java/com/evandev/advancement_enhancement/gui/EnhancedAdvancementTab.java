package com.evandev.advancement_enhancement.gui;

import com.evandev.advancement_enhancement.advancements.AdvancementDisplayInfo;
import com.evandev.advancement_enhancement.advancements.AdvancementDisplayInfoRegistry;
import com.evandev.advancement_enhancement.gui.screens.EnhancedAdvancementsScreen;
import com.evandev.advancement_enhancement.util.PersistentData;
import com.google.common.collect.Maps;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Optional;

public class EnhancedAdvancementTab {
    public static final Map<ResourceLocation, Tuple<Integer, Integer>> scrollHistory = Maps.newLinkedHashMap();
    public static boolean doFade = false;
    protected final Map<AdvancementHolder, EnhancedAdvancementWidget> widgets = Maps.newLinkedHashMap();
    private final Minecraft minecraft;
    private final EnhancedAdvancementsScreen screen;
    private final AdvancementNode rootNode;
    private final DisplayInfo display;
    private final ItemStack icon;
    private final Component title;
    private final EnhancedAdvancementWidget root;
    private final AdvancementDisplayInfoRegistry displayInfos;
    public int scrollX;
    public int scrollY;
    public String customTitle = "";
    public ResourceLocation customBackground = null;
    public boolean isStaticBackground = false;
    public int bgWidth = 16;
    public int bgHeight = 16;
    public int customWidth = 0;
    public int customHeight = 0;
    public int customIndex = 0;
    private EnhancedAdvancementTabType type;
    private int index;
    private int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
    private int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
    private float fade;
    private boolean centered;

    protected EnhancedAdvancementTab(Minecraft mc, EnhancedAdvancementsScreen advancementsScreen, EnhancedAdvancementTabType type, int index, AdvancementNode advancementNode, DisplayInfo displayInfo) {
        this.minecraft = mc;
        this.screen = advancementsScreen;
        this.type = type;
        this.index = index;
        this.rootNode = advancementNode;
        this.display = displayInfo;
        this.icon = displayInfo.getIcon();
        this.title = displayInfo.getTitle();
        this.displayInfos = new AdvancementDisplayInfoRegistry(advancementNode);
        this.root = new EnhancedAdvancementWidget(this, mc, advancementNode, displayInfo);
        this.addWidget(this.root, advancementNode.holder());

        String id = advancementNode.holder().id().toString();
        switch (id) {
            case "minecraft:story/root" -> this.customIndex = 0;
            case "minecraft:adventure/root" -> this.customIndex = 1;
            case "minecraft:husbandry/root" -> this.customIndex = 2;
            case "minecraft:nether/root" -> this.customIndex = 3;
            case "minecraft:end/root" -> this.customIndex = 4;
            default -> this.customIndex = 5;
        }

        PersistentData.loadTabProperties(this);
    }

    public static EnhancedAdvancementTab create(Minecraft mc, EnhancedAdvancementsScreen advancementsScreen, int index, AdvancementNode advancementNode, int width, int height) {
        Optional<DisplayInfo> optional = advancementNode.advancement().display();
        if (optional.isEmpty()) {
            return null;
        } else {
            EnhancedAdvancementTabType advancementTabType = EnhancedAdvancementTabType.getTabType(width, height, index);
            if (advancementTabType == null) {
                return null;
            } else {
                return new EnhancedAdvancementTab(mc, advancementsScreen, advancementTabType, index, advancementNode, optional.get());
            }
        }
    }


    public void updateIndex(int index, int width, int height) {
        this.index = index;
        this.type = EnhancedAdvancementTabType.getTabType(width, height, index);
    }

    public Map<AdvancementHolder, EnhancedAdvancementWidget> getWidgets() {
        return this.widgets;
    }

    public AdvancementNode getRootNode() {
        return this.rootNode;
    }

    public Component getTitle() {
        if (this.customTitle != null && !this.customTitle.isEmpty()) return Component.literal(this.customTitle);
        return this.title;
    }

    public void drawTab(GuiGraphics guiGraphics, int left, int top, int width, int height, boolean selected) {
        this.type.draw(guiGraphics, left, top, width, height, selected, this.index);
    }

    public void drawIcon(GuiGraphics guiGraphics, int left, int top, int width, int height) {
        this.type.drawIcon(guiGraphics, left, top, width, height, this.index, this.icon);
    }

    public void drawContents(GuiGraphics guiGraphics, int left, int top, int width, int height, double mouseX, double mouseY) {
        float zoom = this.screen.getZoom();
        int scaledWidth = (int) (width / zoom);
        int scaledHeight = (int) (height / zoom);

        double unzoomedX = (mouseX - left) / zoom;
        double unzoomedY = (mouseY - top) / zoom;

        if (!this.centered) {
            this.scrollX = (scaledWidth / 2) - (this.root.getX() + EnhancedAdvancementWidget.ADVANCEMENT_SIZE / 2);
            this.scrollY = (scaledHeight / 2) - (this.root.getY() + EnhancedAdvancementWidget.ADVANCEMENT_SIZE / 2);
            this.centered = true;
        }

        guiGraphics.enableScissor(left, top, left + width, top + height);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(left, top, 0);
        guiGraphics.pose().scale(zoom, zoom, 1.0F);

        ResourceLocation resourcelocation = this.customBackground != null ? this.customBackground : this.display.getBackground().orElse(TextureManager.INTENTIONAL_MISSING_TEXTURE);

        if (this.isStaticBackground && this.bgWidth == 0 && this.bgHeight == 0) {
            guiGraphics.blit(resourcelocation, 0, 0, 0.0F, 0.0F, scaledWidth, scaledHeight, scaledWidth, scaledHeight);
        } else {
            int texW = this.bgWidth > 0 ? this.bgWidth : 16;
            int texH = this.bgHeight > 0 ? this.bgHeight : 16;

            int i = this.isStaticBackground ? 0 : this.scrollX % texW;
            int j = this.isStaticBackground ? 0 : this.scrollY % texH;

            int k = -1;
            for (; k <= 1 + scaledWidth / texW; k++) {
                int l = -1;
                for (; l <= 1 + scaledHeight / texH; l++) {
                    guiGraphics.blit(resourcelocation, i + texW * k, j + texH * l, 0.0F, 0.0F, texW, texH, texW, texH);
                }
            }
        }

        this.root.drawConnectivity(guiGraphics, this.scrollX, this.scrollY, true);
        this.root.drawConnectivity(guiGraphics, this.scrollX, this.scrollY, false);
        this.root.draw(guiGraphics, this.scrollX, this.scrollY, unzoomedX, unzoomedY);

        for (EnhancedAdvancementWidget advancementWidget : this.widgets.values()) {
            if (advancementWidget == EnhancedAdvancementsScreen.selectedWidget) {
                guiGraphics.fill(advancementWidget.getX() + this.scrollX + 1, advancementWidget.getY() + this.scrollY - 2, advancementWidget.getX() + this.scrollX + 31, advancementWidget.getY() + this.scrollY + 28, 0x6600FF00);
            }
        }
        guiGraphics.pose().popPose();
        guiGraphics.disableScissor();
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

        if (doFade && flag) {
            this.fade = Mth.clamp(this.fade + 0.02F, 0.0F, 0.3F);
        } else {
            this.fade = Mth.clamp(this.fade - 0.04F, 0.0F, 1.0F);
        }
    }

    public boolean isMouseOver(int left, int top, int width, int height, double mouseX, double mouseY) {
        return this.type.isMouseOver(left, top, width, height, this.index, mouseX, mouseY);
    }

    public void scroll(double scrollX, double scrollY, int width, int height) {
        float zoom = this.screen.getZoom();
        int scaledWidth = (int) (width / zoom);
        int scaledHeight = (int) (height / zoom);

        int marginX = scaledWidth / 2;
        int marginY = scaledHeight / 2;

        this.scrollX = (int) Math.round(Mth.clamp(this.scrollX + scrollX, -(this.maxX - marginX), -this.minX + marginX));
        this.scrollY = (int) Math.round(Mth.clamp(this.scrollY + scrollY, -(this.maxY - marginY), -this.minY + marginY));
    }

    public void addAdvancement(AdvancementNode advancementNode) {
        Optional<DisplayInfo> optional = advancementNode.advancement().display();
        if (optional.isPresent()) {
            EnhancedAdvancementWidget advancementEntryScreen = new EnhancedAdvancementWidget(this, this.minecraft, advancementNode, optional.get());
            this.addWidget(advancementEntryScreen, advancementNode.holder());
        }
    }

    private void addWidget(EnhancedAdvancementWidget advancementEntryScreen, AdvancementHolder advancementHolder) {
        this.widgets.put(advancementHolder, advancementEntryScreen);
        int left = advancementEntryScreen.getX();
        int right = left + 28;
        int top = advancementEntryScreen.getY();
        int bottom = top + 27;
        this.minX = Math.min(this.minX, left);
        this.maxX = Math.max(this.maxX, right);
        this.minY = Math.min(this.minY, top);
        this.maxY = Math.max(this.maxY, bottom);

        for (EnhancedAdvancementWidget gui : this.widgets.values()) {
            gui.attachToParent();
        }
    }

    public EnhancedAdvancementWidget getWidget(AdvancementHolder advancementHolder) {
        return this.widgets.get(advancementHolder);
    }

    public EnhancedAdvancementsScreen getScreen() {
        return this.screen;
    }

    public AdvancementDisplayInfo getDisplayInfo(AdvancementNode advancementNode) {
        return displayInfos.get(advancementNode.holder());
    }

    public void storeScroll() {
        if (this.centered) {
            scrollHistory.put(this.rootNode.holder().id(), new Tuple<>(scrollX, scrollY));
        }
    }

    public void loadScroll() {
        Tuple<Integer, Integer> scroll = scrollHistory.get(this.rootNode.holder().id());
        if (scroll != null) {
            this.centered = true;
            this.scrollX = scroll.getA();
            this.scrollY = scroll.getB();
        }
    }
}