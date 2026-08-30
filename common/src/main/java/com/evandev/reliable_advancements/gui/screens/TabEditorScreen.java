package com.evandev.reliable_advancements.gui.screens;

import com.evandev.reliable_advancements.client.ClientTabStore;
import com.evandev.reliable_advancements.gui.EnhancedAdvancementTab;
import com.evandev.reliable_advancements.gui.theme.EditorTheme;
import com.evandev.reliable_advancements.gui.widgets.EditorForm;
import com.evandev.reliable_advancements.gui.widgets.JsonEditorWidget;
import com.evandev.reliable_advancements.gui.widgets.ModernButton;
import com.evandev.reliable_advancements.gui.widgets.SuggestingEditBox;
import com.evandev.reliable_advancements.network.TabActionPayload;
import com.evandev.reliable_advancements.platform.Services;
import com.evandev.reliable_advancements.tabs.ResolvedTab;
import com.evandev.reliable_advancements.tabs.TabDefinition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

public class TabEditorScreen extends Screen {
    private static final int MAX_UI_WIDTH = 340;
    private static final int MAX_UI_HEIGHT = 380;
    private static final int HEADER_HEIGHT = 36;

    private final EnhancedAdvancementsScreen parentScreen;
    private final ResourceLocation tabId;
    private final ResolvedTab resolved;
    private EditorForm form;

    private EditBox nameBox;
    private SuggestingEditBox iconBox;
    private SuggestingEditBox bgBox;
    private EditBox bgWidthBox;
    private EditBox bgHeightBox;
    private EditBox widthBox;
    private EditBox heightBox;
    private EditBox indexBox;
    private JsonEditorWidget rulesBox;

    private boolean isStaticBg;
    private int uiX, uiY, uiW, uiH;
    private ModernButton saveBtn;
    private ModernButton cancelBtn;

    public TabEditorScreen(EnhancedAdvancementsScreen parentScreen, EnhancedAdvancementTab tab) {
        super(Component.literal("Edit Tab: " + tab.getId()));
        this.parentScreen = parentScreen;
        this.tabId = tab.getId();
        this.resolved = tab.getDefinition();
        this.isStaticBg = this.resolved.staticBackground();
    }

    private static int parseOr(String value, int fallback) {
        try {
            return value.isEmpty() ? fallback : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void defocusAllExcept(@Nullable GuiEventListener keepFocused) {
        if (this.getFocused() != keepFocused) {
            this.setFocused(keepFocused);
        }
        for (GuiEventListener child : form.getWidgets()) {
            if (child != keepFocused && child instanceof AbstractWidget aw) {
                aw.setFocused(false);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        for (GuiEventListener child : form.getWidgets()) {
            if (child instanceof SuggestingEditBox seb && seb.hasSuggestions()) {
                if (seb.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                    return true;
                }
            }
        }
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
                        defocusAllExcept(seb);
                        return true;
                    }
                }
            }
            if (form.mouseClicked(mx, my, button)) {
                defocusAllExcept(null);
                return true;
            }
            if (saveBtn != null && saveBtn.mouseClicked(mx, my, button)) {
                defocusAllExcept(saveBtn);
                return true;
            }
            if (cancelBtn != null && cancelBtn.mouseClicked(mx, my, button)) {
                defocusAllExcept(cancelBtn);
                return true;
            }
        }

        GuiEventListener clickedChild = null;
        for (GuiEventListener child : this.children()) {
            if (child == saveBtn || child == cancelBtn) continue;
            if (child.mouseClicked(mx, my, button)) {
                clickedChild = child;
                this.setFocused(child);
                if (button == 0) this.setDragging(true);
                break;
            }
        }

        if (button == 0) {
            defocusAllExcept(clickedChild);
        }

        return clickedChild != null;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dragX, double dragY) {
        return form.mouseDragged(mx, my, button, dragX, dragY) || super.mouseDragged(mx, my, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        return form.mouseReleased(mx, my, button) || super.mouseReleased(mx, my, button);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parentScreen);
    }

    @Override
    protected void init() {
        this.clearWidgets();

        if (this.form == null) {
            this.form = new EditorForm(this.font);
        }

        uiW = Math.min(this.width - 24, MAX_UI_WIDTH);
        uiH = Math.min(this.height - 24, MAX_UI_HEIGHT);
        uiX = (this.width - uiW) / 2;
        uiY = (this.height - uiH) / 2;

        TabDefinition stored = ClientTabStore.get().tab(tabId);
        String name = stored != null && stored.title != null ? stored.title : "";
        String icon = stored != null && stored.icon != null ? stored.icon.toString() : "";
        String bg = stored != null && stored.background != null ? stored.background.toString() : "";
        String tBgWidth = String.valueOf(resolved.bgWidth());
        String tBgHeight = String.valueOf(resolved.bgHeight());
        String tWidth = resolved.windowWidth() > 0 ? String.valueOf(resolved.windowWidth()) : "";
        String tHeight = resolved.windowHeight() > 0 ? String.valueOf(resolved.windowHeight()) : "";
        String tIndex = String.valueOf(resolved.index());

        form.clear();
        form.addSection("Tab Properties");
        nameBox = form.addTextField("Tab Name", "Displayed tab title (blank inherits the root advancement)", name, s -> {
        });

        List<String> itemSuggestions = BuiltInRegistries.ITEM.keySet().stream()
                .map(ResourceLocation::toString).sorted().collect(Collectors.toList());
        iconBox = form.addSuggestingField("Tab Icon", "Item shown on the tab (blank inherits the root advancement)", icon, () -> itemSuggestions, s -> {
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
        rulesBox = new JsonEditorWidget(this.font, 0, 0, 100, 70, Component.literal("Background Rules JSON"));
        rulesBox.setValue(resolved.backgroundRules());
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
        cancelBtn = ModernButton.modernBuilder(Component.literal("Cancel"), b -> onClose())
                .style(ModernButton.Style.SECONDARY).pos(saveBtnX + btnW + 6, saveBtnY).size(btnW - 10, btnH).build();

        this.addRenderableWidget(saveBtn);
        this.addRenderableWidget(cancelBtn);
    }

    private void saveAndClose() {
        TabDefinition def = new TabDefinition(tabId);
        if (!nameBox.getValue().isEmpty()) def.title = nameBox.getValue();
        if (!iconBox.getValue().isEmpty()) def.icon = ResourceLocation.tryParse(iconBox.getValue());
        if (!bgBox.getValue().isEmpty()) def.background = ResourceLocation.tryParse(bgBox.getValue());
        def.staticBackground = isStaticBg;
        def.bgWidth = parseOr(bgWidthBox.getValue(), TabDefinition.DEFAULT_TILE);
        def.bgHeight = parseOr(bgHeightBox.getValue(), TabDefinition.DEFAULT_TILE);
        def.windowWidth = parseOr(widthBox.getValue(), 0);
        def.windowHeight = parseOr(heightBox.getValue(), 0);
        if (!indexBox.getValue().isEmpty()) def.index = parseOr(indexBox.getValue(), resolved.index());
        if (rulesBox != null && !rulesBox.getValue().isEmpty()) def.backgroundRules = rulesBox.getValue();

        Services.PLATFORM.sendTabAction(new TabActionPayload(
                TabActionPayload.Action.SAVE, tabId, def.toJson().toString()));

        EnhancedAdvancementsScreen.setSavedSelectedTab(tabId);
        parentScreen.awaitServerSync();
        this.minecraft.setScreen(parentScreen);
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
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
