package com.evandev.better_advancements.gui;

import com.evandev.better_advancements.advancements.BetterDisplayInfo;
import com.evandev.better_advancements.advancements.BetterDisplayInfoRegistry;
import com.evandev.better_advancements.gui.screens.BetterAdvancementsScreen;
import com.evandev.better_advancements.util.PersistentData;
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

public class BetterAdvancementTab {
    public static final Map<AdvancementHolder, Tuple<Integer, Integer>> scrollHistory = Maps.newLinkedHashMap();
    public static boolean doFade = false;
    protected final Map<AdvancementHolder, BetterAdvancementWidget> widgets = Maps.newLinkedHashMap();
    private final Minecraft minecraft;
    private final BetterAdvancementsScreen screen;
    private final AdvancementNode rootNode;
    private final DisplayInfo display;
    private final ItemStack icon;
    private final Component title;
    private final BetterAdvancementWidget root;
    private final BetterDisplayInfoRegistry betterDisplayInfos;
    public int scrollX;
    public int scrollY;
    public String customTitle = "";
    public ResourceLocation customBackground = null;
    public boolean isStaticBackground = false;
    public int customWidth = 0;
    public int customHeight = 0;
    public int customIndex = 0;
    private BetterAdvancementTabType type;
    private int index;
    private int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
    private int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
    private float fade;
    private boolean centered;

    protected BetterAdvancementTab(Minecraft mc, BetterAdvancementsScreen betterAdvancementsScreen, BetterAdvancementTabType type, int index, AdvancementNode advancementNode, DisplayInfo displayInfo) {
        this.minecraft = mc;
        this.screen = betterAdvancementsScreen;
        this.type = type;
        this.index = index;
        this.rootNode = advancementNode;
        this.display = displayInfo;
        this.icon = displayInfo.getIcon();
        this.title = displayInfo.getTitle();
        this.betterDisplayInfos = new BetterDisplayInfoRegistry(advancementNode);
        this.root = new BetterAdvancementWidget(this, mc, advancementNode, displayInfo);
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

    public static BetterAdvancementTab create(Minecraft mc, BetterAdvancementsScreen betterAdvancementsScreen, int index, AdvancementNode advancementNode, int width, int height) {
        Optional<DisplayInfo> optional = advancementNode.advancement().display();
        if (optional.isEmpty()) {
            return null;
        } else {
            BetterAdvancementTabType advancementTabType = BetterAdvancementTabType.getTabType(width, height, index);
            if (advancementTabType == null) {
                return null;
            } else {
                return new BetterAdvancementTab(mc, betterAdvancementsScreen, advancementTabType, index, advancementNode, optional.get());
            }
        }
    }


    public void updateIndex(int index, int width, int height) {
        this.index = index;
        this.type = BetterAdvancementTabType.getTabType(width, height, index);
    }

    public Map<AdvancementHolder, BetterAdvancementWidget> getWidgets() {
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
            this.scrollX = (scaledWidth / 2) - (this.root.getX() + BetterAdvancementWidget.ADVANCEMENT_SIZE / 2);
            this.scrollY = (scaledHeight / 2) - (this.root.getY() + BetterAdvancementWidget.ADVANCEMENT_SIZE / 2);
            this.centered = true;
        }

        guiGraphics.enableScissor(left, top, left + width, top + height);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(left, top, 0);
        guiGraphics.pose().scale(zoom, zoom, 1.0F);

        ResourceLocation resourcelocation = this.customBackground != null ? this.customBackground : this.display.getBackground().orElse(TextureManager.INTENTIONAL_MISSING_TEXTURE);

        if (this.isStaticBackground) {
            guiGraphics.blit(resourcelocation, 0, 0, 0.0F, 0.0F, scaledWidth, scaledHeight, scaledWidth, scaledHeight);
        } else {
            int i = this.scrollX % 16;
            int j = this.scrollY % 16;

            int k = -1;
            for (; k <= 1 + scaledWidth / 16; k++) {
                int l = -1;
                for (; l <= scaledHeight / 16; l++) {
                    guiGraphics.blit(resourcelocation, i + 16 * k, j + 16 * l, 0.0F, 0.0F, 16, 16, 16, 16);
                }
                guiGraphics.blit(resourcelocation, i + 16 * k, j + 16 * l, 0.0F, 0.0F, 16, scaledHeight % 16, 16, 16);
            }
        }

        this.root.drawConnectivity(guiGraphics, this.scrollX, this.scrollY, true);
        this.root.drawConnectivity(guiGraphics, this.scrollX, this.scrollY, false);
        this.root.draw(guiGraphics, this.scrollX, this.scrollY, unzoomedX, unzoomedY);
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
            for (BetterAdvancementWidget betterAdvancementWidget : this.widgets.values()) {
                if (betterAdvancementWidget.isMouseOver(this.scrollX, this.scrollY, scaledMouseX, scaledMouseY)) {
                    flag = true;
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().scale(zoom, zoom, 1.0F);
                    betterAdvancementWidget.drawHover(guiGraphics, this.scrollX, this.scrollY, (int) (left / zoom), (int) (top / zoom));
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
            BetterAdvancementWidget betterAdvancementEntryScreen = new BetterAdvancementWidget(this, this.minecraft, advancementNode, optional.get());
            this.addWidget(betterAdvancementEntryScreen, advancementNode.holder());
        }
    }

    private void addWidget(BetterAdvancementWidget betterAdvancementEntryScreen, AdvancementHolder advancementHolder) {
        this.widgets.put(advancementHolder, betterAdvancementEntryScreen);
        int left = betterAdvancementEntryScreen.getX();
        int right = left + 28;
        int top = betterAdvancementEntryScreen.getY();
        int bottom = top + 27;
        this.minX = Math.min(this.minX, left);
        this.maxX = Math.max(this.maxX, right);
        this.minY = Math.min(this.minY, top);
        this.maxY = Math.max(this.maxY, bottom);

        for (BetterAdvancementWidget gui : this.widgets.values()) {
            gui.attachToParent();
        }
    }

    public BetterAdvancementWidget getWidget(AdvancementHolder advancementHolder) {
        return this.widgets.get(advancementHolder);
    }

    public BetterAdvancementsScreen getScreen() {
        return this.screen;
    }

    public BetterDisplayInfo getBetterDisplayInfo(AdvancementNode advancementNode) {
        return betterDisplayInfos.get(advancementNode.holder());
    }

    public void storeScroll() {
        if (this.centered) {
            scrollHistory.put(this.rootNode.holder(), new Tuple<>(scrollX, scrollY));
        }
    }

    public void loadScroll() {
        Tuple<Integer, Integer> scroll = scrollHistory.get(this.rootNode.holder());
        if (scroll != null) {
            this.centered = true;
            this.scrollX = scroll.getA();
            this.scrollY = scroll.getB();
        }
    }
}