package com.evandev.reliable_advancements.gui.screens;

import com.evandev.reliable_advancements.gui.EnhancedAdvancementTab;
import com.evandev.reliable_advancements.gui.model.AdvancementDraft;
import com.evandev.reliable_advancements.gui.theme.EditorTheme;
import com.evandev.reliable_advancements.gui.widgets.EditorForm;
import com.evandev.reliable_advancements.gui.widgets.ModernButton;
import com.evandev.reliable_advancements.gui.widgets.SuggestingEditBox;
import com.evandev.reliable_advancements.network.EditAdvancementPayload;
import com.evandev.reliable_advancements.platform.Services;
import com.evandev.reliable_advancements.util.PersistentData;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class TabEditorScreen extends Screen {
    private static final int HEADER_HEIGHT = 36;

    private final EnhancedAdvancementsScreen parentScreen;
    private final EnhancedAdvancementTab tab;
    private final AdvancementDraft draft;
    private EditorForm form;

    private EditBox nameBox;
    private SuggestingEditBox bgBox;
    private EditBox bgWidthBox;
    private EditBox bgHeightBox;
    private EditBox widthBox;
    private EditBox heightBox;
    private EditBox indexBox;
    private MultiLineEditBox rulesBox;

    private boolean isStaticBg;
    private int uiX, uiY, uiW, uiH;
    private ModernButton saveBtn;
    private ModernButton cancelBtn;

    public TabEditorScreen(EnhancedAdvancementsScreen parentScreen, EnhancedAdvancementTab tab, String rawJsonFromServer) {
        super(Component.literal("Edit Tab: " + tab.getRootNode().holder().id()));
        this.parentScreen = parentScreen;
        this.tab = tab;
        this.draft = new AdvancementDraft(rawJsonFromServer, tab.getRootNode().holder().id().toString(), false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (form.mouseScrolled(mouseX, mouseY, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            for (GuiEventListener child : form.getWidgets()) {
                if (child instanceof SuggestingEditBox seb && seb.hasSuggestions()) {
                    if (seb.tryClickSuggestion(mx, my)) {
                        return true;
                    }
                }
            }
            if (form.mouseClicked(mx, my, button)) {
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dragX, double dragY) {
        if (form.mouseDragged(mx, my, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mx, my, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (form.mouseReleased(mx, my, button)) {
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    protected void init() {
        this.clearWidgets();

        if (this.form == null) {
            this.form = new EditorForm(this.font);
        }

        uiW = 280;
        uiH = Math.max(160, Math.min(340, this.height - 40));
        uiX = (this.width - uiW) / 2;
        uiY = (this.height - uiH) / 2;

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

        form.clear();
        form.addSection("Tab Properties");
        nameBox = form.addTextField("Tab Name", "Displayed tab title in header", name, s -> {
        });

        List<String> textureSuggestions = this.minecraft.getResourceManager()
                .listResources("textures", loc -> loc.getPath().endsWith(".png"))
                .keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());

        bgBox = form.addSuggestingField("Background Texture", "Format: namespace:textures/... Example: minecraft:textures/gui/advancements/backgrounds/stone.png", bg, () -> textureSuggestions, s -> {
        });

        bgWidthBox = form.addTextField("Texture Width", "Tile width in pixels", tBgWidth, s -> {
        });
        bgHeightBox = form.addTextField("Texture Height", "Tile height in pixels", tBgHeight, s -> {
        });

        form.addToggle("Static Background", "Fixed background or pannable canvas texture", isStaticBg, v -> isStaticBg = v);

        widthBox = form.addTextField("Custom UI Width", "Custom advancement window width (leave empty for default)", tWidth, s -> {
        });
        heightBox = form.addTextField("Custom UI Height", "Custom advancement window height (leave empty for default)", tHeight, s -> {
        });
        indexBox = form.addTextField("Tab Order Index", "Tab display position index", tIndex, s -> {
        });

        form.addSection("Background Rules");
        String rulesStr = tab.rawBackgroundRules;
        if (draft.rootJson.has("better_tab")) {
            JsonObject bTab = draft.rootJson.getAsJsonObject("better_tab");
            if (bTab.has("background_rules")) rulesStr = bTab.get("background_rules").getAsString();
        }

        rulesBox = new MultiLineEditBox(this.font, 0, 0, 100, 70, Component.literal("Background Rules JSON"), Component.empty());
        rulesBox.setValue(rulesStr);
        form.addCustomWidget("Rules JSON", rulesBox, 82);

        int contentX = uiX + 16;
        int contentY = uiY + HEADER_HEIGHT + 6;
        int contentW = uiW - 32;
        int contentH = uiH - HEADER_HEIGHT - 48;

        form.init(contentX, contentY, contentW, contentH);

        for (GuiEventListener listener : form.getWidgets()) {
            if (listener instanceof AbstractWidget aw) {
                this.addRenderableWidget(aw);
            }
        }

        int btnW = 80, btnH = 20;
        int saveBtnX = uiX + uiW - btnW * 2 - 20;
        int saveBtnY = uiY + uiH - 28;

        saveBtn = ModernButton.modernBuilder(Component.literal("Save"), b -> saveAndClose())
                .style(ModernButton.Style.PRIMARY).pos(saveBtnX, saveBtnY).size(btnW, btnH).build();
        cancelBtn = ModernButton.modernBuilder(Component.literal("Cancel"), b -> this.minecraft.setScreen(parentScreen))
                .style(ModernButton.Style.SECONDARY).pos(saveBtnX + btnW + 6, saveBtnY).size(btnW - 10, btnH).build();

        this.addRenderableWidget(saveBtn);
        this.addRenderableWidget(cancelBtn);
    }

    private void saveAndClose() {
        this.saveCurrentState();

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
            if (rulesBox != null && !rulesBox.getValue().isEmpty()) {
                bTab.addProperty("background_rules", rulesBox.getValue());
            }
        } catch (NumberFormatException ignored) {
        }
        return bTab;
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    private void saveCurrentState() {
        if (rulesBox != null) {
            tab.parseBackgroundRules(rulesBox.getValue());
        }
        if (nameBox != null) {
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
        }
    }

    @Override
    public void render(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        gfx.fill(0, 0, this.width, this.height, EditorTheme.BG_OVERLAY);
        EditorTheme.drawWindow(gfx, uiX, uiY, uiW, uiH);

        gfx.drawString(this.font, "Edit Tab Properties", uiX + 16, uiY + 12, EditorTheme.TEXT_GOLD, false);
        gfx.fill(uiX + 1, uiY + HEADER_HEIGHT, uiX + uiW - 1, uiY + HEADER_HEIGHT + 1, EditorTheme.BORDER_INNER);

        gfx.enableScissor(uiX + 8, uiY + HEADER_HEIGHT + 2, uiX + uiW - 8, uiY + uiH - 36);
        form.render(gfx, mouseX, mouseY, partialTicks);
        super.render(gfx, mouseX, mouseY, partialTicks);
        gfx.disableScissor();

        if (saveBtn != null) saveBtn.render(gfx, mouseX, mouseY, partialTicks);
        if (cancelBtn != null) cancelBtn.render(gfx, mouseX, mouseY, partialTicks);

        for (GuiEventListener child : form.getWidgets()) {
            if (child instanceof SuggestingEditBox seb && seb.hasSuggestions()) {
                seb.renderSuggestionsPopup(gfx, mouseX, mouseY);
            }
        }
    }
}