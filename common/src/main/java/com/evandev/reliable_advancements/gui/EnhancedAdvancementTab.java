package com.evandev.reliable_advancements.gui;

import com.evandev.reliable_advancements.advancements.AdvancementDisplayInfo;
import com.evandev.reliable_advancements.advancements.AdvancementDisplayInfoRegistry;
import com.evandev.reliable_advancements.config.ModConfig;
import com.evandev.reliable_advancements.gui.screens.EnhancedAdvancementsScreen;
import com.evandev.reliable_advancements.reference.Constants;
import com.evandev.reliable_advancements.util.PersistentData;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.ClientAsset;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class EnhancedAdvancementTab {
    public record ScrollPos(int getA, int getB) {}
    public static final Map<Identifier, ScrollPos> scrollHistory = Maps.newLinkedHashMap();
    public final List<BackgroundRule> backgroundRules = new ArrayList<>();
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
    public Identifier customBackground = null;
    public boolean isStaticBackground = false;
    public int bgWidth = 16;
    public int bgHeight = 16;
    public int customWidth = 0;
    public int customHeight = 0;
    public int customIndex = 0;
    public String rawBackgroundRules = "[]";
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
        this.icon = displayInfo.getIcon().create();
        this.title = displayInfo.getTitle();
        this.displayInfos = new AdvancementDisplayInfoRegistry(advancementNode);
        this.root = new EnhancedAdvancementWidget(this, mc, advancementNode, displayInfo);
        this.addWidget(this.root, advancementNode.holder());

        String id = advancementNode.holder().id().toString();
        this.customIndex = switch (id) {
            case "minecraft:story/root" -> 0;
            case "minecraft:adventure/root" -> 1;
            case "minecraft:husbandry/root" -> 2;
            case "minecraft:nether/root" -> 3;
            case "minecraft:end/root" -> 4;
            default -> 5;
        };

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

    public void parseBackgroundRules(String jsonStr) {
        this.backgroundRules.clear();
        this.rawBackgroundRules = jsonStr != null ? jsonStr : "[]";
        try {
            JsonArray arr = JsonParser.parseString(this.rawBackgroundRules).getAsJsonArray();
            for (JsonElement el : arr) {
                this.backgroundRules.add(BackgroundRule.fromJson(el.getAsJsonObject()));
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to parse background rules", e);
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

    public void drawTab(GuiGraphicsExtractor guiGraphicsExtractor, int left, int top, int width, int height, boolean selected) {
        this.type.draw(guiGraphicsExtractor, left, top, width, height, selected, this.index);
    }

    public void drawIcon(GuiGraphicsExtractor guiGraphicsExtractor, int left, int top, int width, int height) {
        this.type.drawIcon(guiGraphicsExtractor, left, top, width, height, this.index, this.icon);
    }

    public void drawContents(GuiGraphicsExtractor guiGraphicsExtractor, int left, int top, int width, int height, double mouseX, double mouseY) {
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

        guiGraphicsExtractor.enableScissor(left, top, left + width, top + height);
        guiGraphicsExtractor.pose().pushMatrix();
        guiGraphicsExtractor.pose().translate((float) left, (float) top);
        guiGraphicsExtractor.pose().scale(zoom, zoom);

        Identifier defaultRes = this.customBackground != null
                ? this.customBackground
                : this.display.getBackground()
                .map(ClientAsset.Texture::texturePath)
                .orElse(TextureManager.INTENTIONAL_MISSING_TEXTURE);

        if (this.isStaticBackground && this.bgWidth == 0 && this.bgHeight == 0) {
            guiGraphicsExtractor.blit(RenderPipelines.GUI_TEXTURED, defaultRes, 0, 0, 0.0F, 0.0F, scaledWidth, scaledHeight, scaledWidth, scaledHeight);
        } else {
            int texW = this.bgWidth > 0 ? this.bgWidth : 16;
            int texH = this.bgHeight > 0 ? this.bgHeight : 16;

            int i = this.isStaticBackground ? 0 : this.scrollX % texW;
            int j = this.isStaticBackground ? 0 : this.scrollY % texH;

            Random random = new Random();

            int k = -1;
            for (; k <= 1 + scaledWidth / texW; k++) {
                int l = -1;
                for (; l <= 1 + scaledHeight / texH; l++) {
                    Identifier texToDraw = defaultRes;

                    if (!this.backgroundRules.isEmpty() && !this.isStaticBackground) {
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

                    guiGraphicsExtractor.blit(RenderPipelines.GUI_TEXTURED, texToDraw, i + texW * k, j + texH * l, 0.0F, 0.0F, texW, texH, texW, texH);
                }
            }
        }

        if (ModConfig.get().blurBackground) {
            int alpha = (int) (ModConfig.get().blurBackgroundOpacity / 100.0f * 255.0f);
            int color = (alpha << 24);

            guiGraphicsExtractor.fill(0, 0, scaledWidth, scaledHeight, color);
        }

        this.root.drawConnectivity(guiGraphicsExtractor, this.scrollX, this.scrollY, true);
        this.root.drawConnectivity(guiGraphicsExtractor, this.scrollX, this.scrollY, false);
        this.root.draw(guiGraphicsExtractor, this.scrollX, this.scrollY, unzoomedX, unzoomedY);

        for (EnhancedAdvancementWidget advancementWidget : this.widgets.values()) {
            if (EnhancedAdvancementsScreen.selectedWidgets.contains(advancementWidget)) {
                guiGraphicsExtractor.fill(advancementWidget.getX() + this.scrollX + 1, advancementWidget.getY() + this.scrollY - 2, advancementWidget.getX() + this.scrollX + 31, advancementWidget.getY() + this.scrollY + 28, 0x6600FF00);
            }
        }
        guiGraphicsExtractor.pose().popMatrix();
        guiGraphicsExtractor.disableScissor();
    }

    public void setCentered(boolean centered) {
        this.centered = centered;
    }

    public void drawToolTips(GuiGraphicsExtractor guiGraphicsExtractor, int mouseX, int mouseY, int left, int top, int width, int height) {
        guiGraphicsExtractor.pose().pushMatrix();
        guiGraphicsExtractor.fill(0, 0, width, height, Mth.floor(this.fade * 255.0F) << 24);
        boolean flag = false;

        float zoom = this.screen.getZoom();
        double scaledMouseX = mouseX / zoom;
        double scaledMouseY = mouseY / zoom;

        if (mouseX > 0 && mouseX < width && mouseY > 0 && mouseY < height) {
            for (EnhancedAdvancementWidget advancementWidget : this.widgets.values()) {
                if (advancementWidget.isMouseOver(this.scrollX, this.scrollY, scaledMouseX, scaledMouseY)) {
                    flag = true;
                    guiGraphicsExtractor.pose().pushMatrix();
                    guiGraphicsExtractor.pose().scale(zoom, zoom);
                    advancementWidget.drawHover(guiGraphicsExtractor, this.scrollX, this.scrollY, (int) (left / zoom), (int) (top / zoom));
                    guiGraphicsExtractor.pose().popMatrix();
                    break;
                }
            }
        }

        guiGraphicsExtractor.pose().popMatrix();

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
            scrollHistory.put(this.rootNode.holder().id(), new ScrollPos(scrollX, scrollY));
        }
    }

    public void loadScroll() {
        ScrollPos scroll = scrollHistory.get(this.rootNode.holder().id());
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
        public Identifier texture;

        public static BackgroundRule fromJson(com.google.gson.JsonObject json) {
            BackgroundRule rule = new BackgroundRule();
            if (json.has("min_y")) rule.minY = json.get("min_y").getAsInt();
            if (json.has("max_y")) rule.maxY = json.get("max_y").getAsInt();
            if (json.has("chance")) rule.chance = json.get("chance").getAsFloat();
            if (json.has("absolute_y")) rule.absoluteY = json.get("absolute_y").getAsBoolean();
            if (json.has("texture")) rule.texture = Identifier.parse(json.get("texture").getAsString());
            return rule;
        }
    }
}