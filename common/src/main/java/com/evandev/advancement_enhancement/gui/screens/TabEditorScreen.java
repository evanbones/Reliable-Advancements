package com.evandev.advancement_enhancement.gui.screens;

import com.evandev.advancement_enhancement.gui.EnhancedAdvancementTab;
import com.evandev.advancement_enhancement.gui.model.AdvancementDraft;
import com.evandev.advancement_enhancement.gui.widgets.SuggestingEditBox;
import com.evandev.advancement_enhancement.network.EditAdvancementPayload;
import com.evandev.advancement_enhancement.platform.Services;
import com.evandev.advancement_enhancement.util.PersistentData;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class TabEditorScreen extends Screen {
    private static final int COL_GOLD = 0xFFC8AA64;
    private static final int COL_BG_OVERLAY = 0xBB101010;

    private final EnhancedAdvancementsScreen parentScreen;
    private final EnhancedAdvancementTab tab;
    private final AdvancementDraft draft;

    private EditBox nameBox;
    private SuggestingEditBox bgBox;
    private EditBox bgWidthBox;
    private EditBox bgHeightBox;
    private Button staticBgBtn;
    private EditBox widthBox;
    private EditBox heightBox;
    private EditBox indexBox;

    private Button saveBtn;
    private Button cancelBtn;

    private boolean isStaticBg;

    private int uiX, uiY, uiW, uiH;
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private boolean isDraggingScrollbar = false;

    public TabEditorScreen(EnhancedAdvancementsScreen parentScreen, EnhancedAdvancementTab tab, String rawJsonFromServer) {
        super(Component.literal("Edit Tab: " + tab.getRootNode().holder().id()));
        this.parentScreen = parentScreen;
        this.tab = tab;
        this.draft = new AdvancementDraft(rawJsonFromServer, tab.getRootNode().holder().id().toString(), false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        this.scrollOffset -= (int) (scrollY * 20);
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, this.maxScroll));
        this.init();
        return true;
    }

    private void updateScrollFromMouse(double my) {
        int scrollY = uiY + 32;
        int scrollH = uiH - 72;
        int thumbH = Math.max(20, scrollH * scrollH / (scrollH + maxScroll));
        int trackH = scrollH - thumbH;

        double pct = (my - scrollY - thumbH / 2.0) / trackH;
        pct = Math.max(0, Math.min(pct, 1));
        this.scrollOffset = (int) (pct * this.maxScroll);
        this.init();
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dragX, double dragY) {
        if (this.isDraggingScrollbar) {
            updateScrollFromMouse(my);
            return true;
        }
        return super.mouseDragged(mx, my, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        this.isDraggingScrollbar = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (this.maxScroll > 0 && button == 0) {
            int scrollX = uiX + uiW - 12;
            int scrollY = uiY + 32;
            int scrollH = uiH - 72;
            if (mx >= scrollX && mx <= scrollX + 8 && my >= scrollY && my <= scrollY + scrollH) {
                this.isDraggingScrollbar = true;
                updateScrollFromMouse(my);
                return true;
            }
        }
        
        if (my > uiY + uiH - 40 && my < uiY + uiH) {
            if (saveBtn != null && saveBtn.isMouseOver(mx, my)) return saveBtn.mouseClicked(mx, my, button);
            if (cancelBtn != null && cancelBtn.isMouseOver(mx, my)) return cancelBtn.mouseClicked(mx, my, button);
            return false;
        }
        if (my < uiY + 32) return false;

        return super.mouseClicked(mx, my, button);
    }

    @Override
    protected void init() {
        this.clearWidgets();

        uiW = 240;
        uiH = Math.max(120, Math.min(290, this.height - 40));

        uiX = (this.width - uiW) / 2;
        uiY = (this.height - uiH) / 2;

        int startX = uiX + 20;
        int currentY = uiY + 50 - scrollOffset;

        String name = tab.customTitle != null ? tab.customTitle : "";
        String bg = tab.customBackground != null ? tab.customBackground.toString() : "";
        String tBgWidth = tab.bgWidth > 0 ? String.valueOf(tab.bgWidth) : "16";
        String tBgHeight = tab.bgHeight > 0 ? String.valueOf(tab.bgHeight) : "16";
        isStaticBg = tab.isStaticBackground;
        String tWidth = tab.customWidth > 0 ? String.valueOf(tab.customWidth) : "";
        String tHeight = tab.customHeight > 0 ? String.valueOf(tab.customHeight) : "";
        String tIndex = String.valueOf(tab.customIndex);

        if (draft.rootJson.has("better_tab")) {
            JsonObject bTab = draft.rootJson.getAsJsonObject("better_tab");
            if (bTab.has("title")) name = bTab.get("title").getAsString();
            if (bTab.has("background")) bg = bTab.get("background").getAsString();
            if (bTab.has("bg_width")) tBgWidth = bTab.get("bg_width").getAsString();
            if (bTab.has("bg_height")) tBgHeight = bTab.get("bg_height").getAsString();
            if (bTab.has("static_background")) isStaticBg = bTab.get("static_background").getAsBoolean();
            if (bTab.has("width")) tWidth = bTab.get("width").getAsString();
            if (bTab.has("height")) tHeight = bTab.get("height").getAsString();
            if (bTab.has("index")) tIndex = bTab.get("index").getAsString();
        }

        nameBox = new EditBox(this.font, startX, currentY, 200, 20, Component.literal("Tab Name"));
        nameBox.setMaxLength(256);
        nameBox.setValue(name);
        this.addRenderableWidget(nameBox);

        currentY += 45;

        List<String> textureSuggestions = this.minecraft.getResourceManager()
                .listResources("textures", loc -> loc.getPath().endsWith(".png"))
                .keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());

        bgBox = new SuggestingEditBox(this.font, startX, currentY, 200, 20, Component.literal("Background Texture"), () -> textureSuggestions);
        bgBox.setMaxLength(256);
        bgBox.setValue(bg);
        bgBox.setTooltip(Tooltip.create(Component.literal("Format: namespace:textures/...\\nExample: minecraft:textures/gui/advancements/backgrounds/stone.png")));
        this.addRenderableWidget(bgBox);

        currentY += 45;

        bgWidthBox = new EditBox(this.font, startX, currentY, 95, 20, Component.literal("BG Width"));
        bgWidthBox.setValue(tBgWidth);
        this.addRenderableWidget(bgWidthBox);

        bgHeightBox = new EditBox(this.font, startX + 105, currentY, 95, 20, Component.literal("BG Height"));
        bgHeightBox.setValue(tBgHeight);
        this.addRenderableWidget(bgHeightBox);

        currentY += 45;

        staticBgBtn = Button.builder(Component.literal("Background: " + (!isStaticBg ? "Pannable" : "Fixed/Static")), b -> {
            isStaticBg = !isStaticBg;
            b.setMessage(Component.literal("Background: " + (!isStaticBg ? "Pannable" : "Fixed/Static")));
        }).pos(startX, currentY).size(200, 20).build();
        this.addRenderableWidget(staticBgBtn);

        currentY += 45;

        widthBox = new EditBox(this.font, startX, currentY, 95, 20, Component.literal("Width"));
        widthBox.setValue(tWidth);
        this.addRenderableWidget(widthBox);

        heightBox = new EditBox(this.font, startX + 105, currentY, 95, 20, Component.literal("Height"));
        heightBox.setValue(tHeight);
        this.addRenderableWidget(heightBox);

        currentY += 45;

        indexBox = new EditBox(this.font, startX, currentY, 200, 20, Component.literal("Tab Index"));
        indexBox.setValue(tIndex);
        this.addRenderableWidget(indexBox);

        currentY += 30;

        int contentHeight = currentY + scrollOffset - (uiY + 50);
        this.maxScroll = Math.max(0, contentHeight - (uiH - 90));

        if (this.scrollOffset > this.maxScroll) {
            this.scrollOffset = this.maxScroll;
            this.init();
            return;
        }

        int btnW = 60, btnH = 20;
        int saveBtnX = uiX + uiW - btnW * 2 - 20 - 6;
        int saveBtnY = uiY + uiH - 30;

        saveBtn = Button.builder(Component.literal("Save"), b -> saveAndClose()).pos(saveBtnX, saveBtnY).size(btnW, btnH).build();
        cancelBtn = Button.builder(Component.literal("Cancel"), b -> this.minecraft.setScreen(parentScreen)).pos(saveBtnX + btnW + 6, saveBtnY).size(btnW, btnH).build();

        this.addRenderableWidget(saveBtn);
        this.addRenderableWidget(cancelBtn);
    }

    private void saveAndClose() {
        JsonObject bTab = getBTab();

        draft.rootJson.add("better_tab", bTab);

        tab.customTitle = nameBox.getValue();
        tab.customBackground = bgBox.getValue().isEmpty() ? null : ResourceLocation.parse(bgBox.getValue());
        tab.isStaticBackground = isStaticBg;
        try {
            tab.bgWidth = bgWidthBox.getValue().isEmpty() ? 16 : Integer.parseInt(bgWidthBox.getValue());
            tab.bgHeight = bgHeightBox.getValue().isEmpty() ? 16 : Integer.parseInt(bgHeightBox.getValue());
            tab.customWidth = widthBox.getValue().isEmpty() ? 0 : Integer.parseInt(widthBox.getValue());
            tab.customHeight = heightBox.getValue().isEmpty() ? 0 : Integer.parseInt(heightBox.getValue());
            tab.customIndex = indexBox.getValue().isEmpty() ? 0 : Integer.parseInt(indexBox.getValue());
        } catch (NumberFormatException ignored) {
        }

        parentScreen.sortTabs();
        PersistentData.save(parentScreen.getTabs());

        String payloadStr = new GsonBuilder().setPrettyPrinting().create().toJson(draft.rootJson);
        EditAdvancementPayload payload = new EditAdvancementPayload(tab.getRootNode().holder().id(), payloadStr, false);

        if (Services.PLATFORM.canSendAdvancementEdit()) {
            Services.PLATFORM.sendAdvancementEdit(payload);
        }
        this.minecraft.setScreen(parentScreen);
    }

    private @NotNull JsonObject getBTab() {
        JsonObject bTab = new JsonObject();
        if (!nameBox.getValue().isEmpty()) bTab.addProperty("title", nameBox.getValue());
        if (!bgBox.getValue().isEmpty()) bTab.addProperty("background", bgBox.getValue());
        bTab.addProperty("static_background", isStaticBg);

        try {
            if (!bgWidthBox.getValue().isEmpty()) bTab.addProperty("bg_width", Integer.parseInt(bgWidthBox.getValue()));
            if (!bgHeightBox.getValue().isEmpty())
                bTab.addProperty("bg_height", Integer.parseInt(bgHeightBox.getValue()));
            if (!widthBox.getValue().isEmpty()) bTab.addProperty("width", Integer.parseInt(widthBox.getValue()));
            if (!heightBox.getValue().isEmpty()) bTab.addProperty("height", Integer.parseInt(heightBox.getValue()));
            if (!indexBox.getValue().isEmpty()) bTab.addProperty("index", Integer.parseInt(indexBox.getValue()));
        } catch (NumberFormatException ignored) {
        }
        return bTab;
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        super.renderBackground(gfx, mouseX, mouseY, partialTicks);

        gfx.fill(0, 0, this.width, this.height, COL_BG_OVERLAY);
        gfx.fill(uiX, uiY, uiX + uiW, uiY + uiH, 0xFF202020);

        gfx.drawString(this.font, "Edit Tab Properties", uiX + 20, uiY + 15, COL_GOLD, false);
        gfx.fill(uiX + 20, uiY + 30, uiX + uiW - 20, uiY + 31, 0x55808080);

        if (this.maxScroll > 0) {
            int scrollX = uiX + uiW - 12;
            int scrollY = uiY + 32;
            int scrollH = uiH - 72;
            int thumbH = Math.max(20, scrollH * scrollH / (scrollH + maxScroll));
            int thumbY = scrollY + (int) ((scrollH - thumbH) * (this.scrollOffset / (float) this.maxScroll));

            gfx.fill(scrollX, scrollY, scrollX + 8, scrollY + scrollH, 0xFF000000);
            gfx.fill(scrollX + 1, thumbY, scrollX + 7, thumbY + thumbH, 0xFF888888);
        }

        gfx.enableScissor(uiX, uiY + 32, uiX + uiW - 14, uiY + uiH - 40);

        if (nameBox != null)
            gfx.drawString(this.font, "Tab Name", nameBox.getX(), nameBox.getY() - 11, 0xFFA08060, false);
        if (bgBox != null)
            gfx.drawString(this.font, "Background Texture", bgBox.getX(), bgBox.getY() - 11, 0xFFA08060, false);
        if (bgWidthBox != null)
            gfx.drawString(this.font, "Tex Width", bgWidthBox.getX(), bgWidthBox.getY() - 11, 0xFFA08060, false);
        if (bgHeightBox != null)
            gfx.drawString(this.font, "Tex Height", bgHeightBox.getX(), bgHeightBox.getY() - 11, 0xFFA08060, false);
        if (widthBox != null)
            gfx.drawString(this.font, "UI Width", widthBox.getX(), widthBox.getY() - 11, 0xFFA08060, false);
        if (heightBox != null)
            gfx.drawString(this.font, "UI Height", heightBox.getX(), heightBox.getY() - 11, 0xFFA08060, false);
        if (indexBox != null)
            gfx.drawString(this.font, "Tab Index", indexBox.getX(), indexBox.getY() - 11, 0xFFA08060, false);

        super.render(gfx, mouseX, mouseY, partialTicks);

        gfx.disableScissor();

        if (saveBtn != null) saveBtn.render(gfx, mouseX, mouseY, partialTicks);
        if (cancelBtn != null) cancelBtn.render(gfx, mouseX, mouseY, partialTicks);
    }
}