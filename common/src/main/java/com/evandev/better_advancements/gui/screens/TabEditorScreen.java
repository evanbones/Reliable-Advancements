package com.evandev.better_advancements.gui.screens;

import com.evandev.better_advancements.gui.BetterAdvancementTab;
import com.evandev.better_advancements.gui.model.AdvancementDraft;
import com.evandev.better_advancements.gui.widgets.SuggestingEditBox;
import com.evandev.better_advancements.network.EditAdvancementPayload;
import com.evandev.better_advancements.platform.Services;
import com.evandev.better_advancements.util.PersistentData;
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

    private final BetterAdvancementsScreen parentScreen;
    private final BetterAdvancementTab tab;
    private final AdvancementDraft draft;

    private EditBox nameBox;
    private SuggestingEditBox bgBox;
    private Button staticBgBtn;
    private EditBox widthBox;
    private EditBox heightBox;
    private EditBox indexBox;

    private boolean isStaticBg;

    private int uiX, uiY, uiW, uiH;

    public TabEditorScreen(BetterAdvancementsScreen parentScreen, BetterAdvancementTab tab, String rawJsonFromServer) {
        super(Component.literal("Edit Tab: " + tab.getRootNode().holder().id()));
        this.parentScreen = parentScreen;
        this.tab = tab;
        this.draft = new AdvancementDraft(rawJsonFromServer);
    }

    @Override
    protected void init() {
        uiW = 240;
        uiH = 290;
        uiX = (this.width - uiW) / 2;
        uiY = (this.height - uiH) / 2;

        int startX = uiX + 20;
        int startY = uiY + 55;

        String name = tab.customTitle != null ? tab.customTitle : "";
        String bg = tab.customBackground != null ? tab.customBackground.toString() : "";
        isStaticBg = tab.isStaticBackground;
        String tWidth = tab.customWidth > 0 ? String.valueOf(tab.customWidth) : "";
        String tHeight = tab.customHeight > 0 ? String.valueOf(tab.customHeight) : "";
        String tIndex = String.valueOf(tab.customIndex);

        if (draft.rootJson.has("better_tab")) {
            JsonObject bTab = draft.rootJson.getAsJsonObject("better_tab");
            if (bTab.has("title")) name = bTab.get("title").getAsString();
            if (bTab.has("background")) bg = bTab.get("background").getAsString();
            if (bTab.has("static_background")) isStaticBg = bTab.get("static_background").getAsBoolean();
            if (bTab.has("width")) tWidth = bTab.get("width").getAsString();
            if (bTab.has("height")) tHeight = bTab.get("height").getAsString();
            if (bTab.has("index")) tIndex = bTab.get("index").getAsString();
        }

        nameBox = new EditBox(this.font, startX, startY, 200, 20, Component.literal("Tab Name"));
        nameBox.setMaxLength(256);
        nameBox.setValue(name);
        this.addRenderableWidget(nameBox);

        List<String> textureSuggestions = this.minecraft.getResourceManager()
                .listResources("textures", loc -> loc.getPath().endsWith(".png"))
                .keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());

        bgBox = new SuggestingEditBox(this.font, startX, startY + 45, 200, 20, Component.literal("Background Texture"),
                () -> textureSuggestions);
        bgBox.setMaxLength(256);
        bgBox.setValue(bg);
        bgBox.setTooltip(Tooltip.create(Component.literal("Format: namespace:textures/...\nExample: minecraft:textures/gui/advancements/backgrounds/stone.png")));
        this.addRenderableWidget(bgBox);

        staticBgBtn = Button.builder(Component.literal("Static Background: " + isStaticBg), b -> {
            isStaticBg = !isStaticBg;
            b.setMessage(Component.literal("Static Background: " + isStaticBg));
        }).pos(startX, startY + 75).size(200, 20).build();
        this.addRenderableWidget(staticBgBtn);

        widthBox = new EditBox(this.font, startX, startY + 120, 95, 20, Component.literal("Width"));
        widthBox.setValue(tWidth);
        this.addRenderableWidget(widthBox);

        heightBox = new EditBox(this.font, startX + 105, startY + 120, 95, 20, Component.literal("Height"));
        heightBox.setValue(tHeight);
        this.addRenderableWidget(heightBox);

        indexBox = new EditBox(this.font, startX, startY + 165, 200, 20, Component.literal("Tab Index"));
        indexBox.setValue(tIndex);
        this.addRenderableWidget(indexBox);

        int btnW = 60, btnH = 20;
        int saveBtnX = uiX + uiW - btnW * 2 - 20 - 6;
        int saveBtnY = startY + 200;

        this.addRenderableWidget(Button.builder(Component.literal("Save"), b -> saveAndClose()).pos(saveBtnX, saveBtnY).size(btnW, btnH).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> this.minecraft.setScreen(parentScreen)).pos(saveBtnX + btnW + 6, saveBtnY).size(btnW, btnH).build());
    }

    private void saveAndClose() {
        JsonObject bTab = getBTab();

        draft.rootJson.add("better_tab", bTab);

        tab.customTitle = nameBox.getValue();
        tab.customBackground = bgBox.getValue().isEmpty() ? null : ResourceLocation.parse(bgBox.getValue());
        tab.isStaticBackground = isStaticBg;
        try {
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

        int startX = uiX + 20;
        int startY = uiY + 55;

        gfx.drawString(this.font, "Tab Name", startX, startY - 11, 0xFFA08060, false);
        gfx.drawString(this.font, "Background Texture", startX, startY + 34, 0xFFA08060, false);
        gfx.drawString(this.font, "Width", startX, startY + 109, 0xFFA08060, false);
        gfx.drawString(this.font, "Height", startX + 105, startY + 109, 0xFFA08060, false);
        gfx.drawString(this.font, "Tab Index", startX, startY + 154, 0xFFA08060, false);

        super.render(gfx, mouseX, mouseY, partialTicks);
    }
}