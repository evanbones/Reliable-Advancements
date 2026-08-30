package com.evandev.reliable_advancements.gui.screens;

import com.evandev.reliable_advancements.config.ModConfig;
import com.evandev.reliable_advancements.gui.EnhancedAdvancementTab;
import com.evandev.reliable_advancements.gui.model.AdvancementDraft;
import com.evandev.reliable_advancements.gui.tabs.*;
import com.evandev.reliable_advancements.gui.theme.EditorTheme;
import com.evandev.reliable_advancements.gui.widgets.ModernButton;
import com.evandev.reliable_advancements.gui.widgets.ModernDropdown;
import com.evandev.reliable_advancements.gui.widgets.SuggestingEditBox;
import com.evandev.reliable_advancements.network.AdvancementBatchPayload;
import com.evandev.reliable_advancements.network.EditAdvancementPayload;
import com.evandev.reliable_advancements.network.TabActionPayload;
import com.evandev.reliable_advancements.platform.Services;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AdvancementEditorScreen extends Screen {
    private static final int MAX_UI_WIDTH = 580;
    private static final int MAX_UI_HEIGHT = 380;
    private static final int HEADER_HEIGHT = 36;
    private static final int TAB_ROW_H = 28;

    private final EnhancedAdvancementsScreen parentScreen;
    private final ResourceLocation initialId;
    private final AdvancementDraft draft;
    private final boolean isNew;
    private final int posX, posY;

    private final Map<String, IEditorTab> tabs = new LinkedHashMap<>();
    private final Set<String> visitedTabs = new HashSet<>();
    private String activeTabName;
    private IEditorTab activeTab;

    private int uiX, uiY, uiW, uiH;
    private int sidebarWidth = 130;
    private boolean isResizingSidebar = false;
    private ModernButton saveBtn;
    private ModernButton cancelBtn;

    public AdvancementEditorScreen(EnhancedAdvancementsScreen parentScreen, ResourceLocation id, boolean isNew, int posX, int posY, String initialTabName, String rawJsonFromServer) {
        super(Component.literal((isNew ? "Create" : "Edit") + " Advancement: " + id));
        this.parentScreen = parentScreen;
        this.initialId = id;
        this.isNew = isNew;
        this.posX = posX;
        this.posY = posY;

        this.draft = new AdvancementDraft(rawJsonFromServer, id.toString(), isNew);

        this.tabs.put("Properties", new PropertiesTab(Minecraft.getInstance().font));
        this.tabs.put("Layout", new LayoutTab(Minecraft.getInstance().font, posX, posY));
        this.tabs.put("Criteria", new CriteriaTab(Minecraft.getInstance().font));
        this.tabs.put("Rewards", new RewardsTab(Minecraft.getInstance().font));

        for (IEditorTab tab : this.tabs.values()) {
            tab.loadState(this.draft);
        }

        this.activeTabName = this.tabs.containsKey(initialTabName) ? initialTabName : "Properties";
        this.activeTab = this.tabs.get(this.activeTabName);
        this.visitedTabs.add(this.activeTabName);
    }

    private int getMinSidebarWidth() {
        int maxTextWidth = this.font.width("Properties");
        for (String tabName : tabs.keySet()) {
            maxTextWidth = Math.max(maxTextWidth, this.font.width(tabName));
        }
        return maxTextWidth + 30;
    }

    private int getMaxSidebarWidth() {
        return Math.max(getMinSidebarWidth(), uiW - 200);
    }

    private void defocusAllExcept(@Nullable GuiEventListener keepFocused) {
        if (this.getFocused() != keepFocused) {
            this.setFocused(keepFocused);
        }
        for (GuiEventListener child : this.children()) {
            if (child != keepFocused && child instanceof AbstractWidget aw) {
                aw.setFocused(false);
            }
        }
        if (activeTab != null) {
            for (GuiEventListener child : activeTab.getWidgets()) {
                if (child != keepFocused && child instanceof AbstractWidget aw) {
                    aw.setFocused(false);
                }
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (activeTab != null) {
            for (GuiEventListener listener : activeTab.getWidgets()) {
                if (listener instanceof ModernDropdown md && md.isOpen()) {
                    if (md.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                        return true;
                    }
                }
                if (listener instanceof SuggestingEditBox seb && seb.hasSuggestions()) {
                    if (seb.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                        return true;
                    }
                }
            }
        }

        int contentX = uiX + sidebarWidth + 8;
        int contentW = uiW - sidebarWidth - 16;
        int contentY = uiY + HEADER_HEIGHT + 2;
        int contentH = uiH - HEADER_HEIGHT - 40;

        if (mouseX >= contentX && mouseX <= contentX + contentW && mouseY >= contentY && mouseY <= contentY + contentH) {
            for (GuiEventListener listener : this.children()) {
                if (listener != null && listener != saveBtn && listener != cancelBtn) {
                    if (listener.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                        return true;
                    }
                }
            }
            if (activeTab != null && activeTab.getForm() != null) {
                return activeTab.getForm().mouseScrolled(mouseX, mouseY, scrollY);
            }
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (activeTab != null) {
                for (GuiEventListener child : activeTab.getWidgets()) {
                    if (child instanceof ModernDropdown md && md.isOpen()) {
                        if (md.handleDropdownClick(mouseX, mouseY, button)) {
                            defocusAllExcept(null);
                            return true;
                        }
                    }
                    if (child instanceof SuggestingEditBox seb && seb.hasSuggestions()) {
                        if (seb.tryClickSuggestion(mouseX, mouseY)) {
                            defocusAllExcept(seb);
                            return true;
                        }
                    }
                }
            }

            int splitterX = uiX + sidebarWidth;
            if (mouseX >= splitterX - 4 && mouseX <= splitterX + 4 && mouseY >= uiY + HEADER_HEIGHT && mouseY <= uiY + uiH) {
                isResizingSidebar = true;
                defocusAllExcept(null);
                return true;
            }

            int tabY = uiY + HEADER_HEIGHT + 10;
            int i = 0;
            for (String tabName : tabs.keySet()) {
                int ry = tabY + i * (TAB_ROW_H + 4);
                if (mouseX >= uiX + 8 && mouseX <= uiX + sidebarWidth - 8 && mouseY >= ry && mouseY <= ry + TAB_ROW_H) {
                    defocusAllExcept(null);
                    switchTab(tabName);
                    return true;
                }
                i++;
            }

            if (saveBtn != null && saveBtn.mouseClicked(mouseX, mouseY, button)) {
                defocusAllExcept(saveBtn);
                return true;
            }
            if (cancelBtn != null && cancelBtn.mouseClicked(mouseX, mouseY, button)) {
                defocusAllExcept(cancelBtn);
                return true;
            }

            if (activeTab != null && activeTab.getForm() != null) {
                if (activeTab.getForm().mouseClicked(mouseX, mouseY, button)) {
                    defocusAllExcept(null);
                    return true;
                }
            }
        }

        int contentX = uiX + sidebarWidth + 8;
        int contentW = uiW - sidebarWidth - 16;
        int contentY = uiY + HEADER_HEIGHT + 2;
        int contentH = uiH - HEADER_HEIGHT - 40;

        GuiEventListener clickedChild = null;
        if (mouseX >= contentX && mouseX <= contentX + contentW && mouseY >= contentY && mouseY <= contentY + contentH) {
            for (GuiEventListener child : this.children()) {
                if (child == saveBtn || child == cancelBtn) continue;
                if (child.mouseClicked(mouseX, mouseY, button)) {
                    clickedChild = child;
                    this.setFocused(child);
                    if (button == 0) this.setDragging(true);
                    break;
                }
            }
        }

        if (button == 0) {
            defocusAllExcept(clickedChild);
        }

        return clickedChild != null;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isResizingSidebar && button == 0) {
            int newWidth = (int) (mouseX - uiX);
            int minW = getMinSidebarWidth();
            int maxW = getMaxSidebarWidth();
            newWidth = Math.max(minW, Math.min(newWidth, maxW));
            if (newWidth != this.sidebarWidth) {
                this.sidebarWidth = newWidth;
                ModConfig.get().editorSidebarWidth = this.sidebarWidth;
                ModConfig.save();
                this.init();
            }
            return true;
        }

        if (activeTab != null && activeTab.getForm() != null) {
            if (activeTab.getForm().mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        }
        int contentX = uiX + sidebarWidth + 8;
        int contentW = uiW - sidebarWidth - 16;
        int contentY = uiY + HEADER_HEIGHT + 2;
        int contentH = uiH - HEADER_HEIGHT - 40;

        if (mouseX >= contentX && mouseX <= contentX + contentW && mouseY >= contentY && mouseY <= contentY + contentH) {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && isResizingSidebar) {
            isResizingSidebar = false;
            return true;
        }
        if (activeTab != null && activeTab.getForm() != null) {
            if (activeTab.getForm().mouseReleased(mouseX, mouseY, button)) return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected void init() {
        if (activeTab != null) {
            activeTab.syncFromWidgets();
        }
        this.clearWidgets();
        setupBounds();

        this.sidebarWidth = Math.max(getMinSidebarWidth(), Math.min(ModConfig.get().editorSidebarWidth, getMaxSidebarWidth()));

        int contentX = uiX + sidebarWidth + 16;
        int contentW = uiW - sidebarWidth - 28;
        int contentY = uiY + HEADER_HEIGHT + 10;
        int contentH = uiH - HEADER_HEIGHT - 55;

        activeTab.init(contentX, contentY, contentW, contentH, this::init);

        for (GuiEventListener listener : activeTab.getWidgets()) {
            if (listener instanceof NarratableEntry) {
                this.addWidget((GuiEventListener & NarratableEntry) listener);
            }
            if (listener.isFocused()) {
                this.setFocused(listener);
            }
        }

        addControlButtons();
    }

    private void setupBounds() {
        uiW = Math.min(this.width - 24, MAX_UI_WIDTH);
        uiH = Math.min(this.height - 24, MAX_UI_HEIGHT);
        uiX = (this.width - uiW) / 2;
        uiY = (this.height - uiH) / 2;
    }

    private void addControlButtons() {
        int btnW = 95, btnH = 20;
        int saveBtnX = uiX + uiW - btnW * 2 - 16;
        int saveBtnY = uiY + uiH - btnH - 10;

        saveBtn = ModernButton.modernBuilder(Component.literal("Save Changes"), b -> saveAndClose())
                .style(ModernButton.Style.PRIMARY)
                .pos(saveBtnX, saveBtnY).size(btnW, btnH).build();

        cancelBtn = ModernButton.modernBuilder(Component.literal("Cancel"), b -> this.minecraft.setScreen(parentScreen))
                .style(ModernButton.Style.SECONDARY)
                .pos(saveBtnX + btnW + 6, saveBtnY).size(btnW - 15, btnH).build();

        this.addRenderableWidget(saveBtn);
        this.addRenderableWidget(cancelBtn);
    }

    private void switchTab(String tabName) {
        if (!tabs.containsKey(tabName) || tabName.equals(activeTabName)) return;

        activeTab.saveState(draft);
        activeTabName = tabName;
        activeTab = tabs.get(tabName);
        visitedTabs.add(tabName);
        activeTab.loadState(draft);
        this.init();
    }

    private void saveAndClose() {
        for (Map.Entry<String, IEditorTab> entry : tabs.entrySet()) {
            if (visitedTabs.contains(entry.getKey())) {
                entry.getValue().saveState(draft);
            }
        }

        ResourceLocation finalId = ResourceLocation.parse(draft.id);

        int finalX = posX;
        int finalY = posY;
        if (tabs.get("Layout") instanceof LayoutTab layoutTab) {
            finalX = layoutTab.getX();
            finalY = layoutTab.getY();
        }

        if (!isNew && !initialId.equals(finalId)) {
            Services.PLATFORM.sendAdvancementBatch(new AdvancementBatchPayload(
                    AdvancementBatchPayload.Op.RESET_TO_VANILLA, List.of(initialId)));
        }

        EnhancedAdvancementTab owningTab = parentScreen.findTabContaining(finalId);
        if (owningTab == null) owningTab = parentScreen.selectedTab;
        if (owningTab != null) {
            parentScreen.savePositions(owningTab.getId(), Map.of(finalId, new int[]{finalX, finalY}));
        }

        String payloadStr = new GsonBuilder().setPrettyPrinting().create().toJson(draft.rootJson);
        EditAdvancementPayload payload = new EditAdvancementPayload(finalId, payloadStr, false);

        if (Services.PLATFORM.canSendAdvancementEdit()) {
            Services.PLATFORM.sendAdvancementEdit(payload);
        }

        if (isNew && !draft.rootJson.has("parent") && !draft.rootJson.has("parents") && parentScreen.selectedTab != null) {
            Services.PLATFORM.sendTabAction(TabActionPayload.addRoot(parentScreen.selectedTab.getId(), finalId));
        }

        parentScreen.awaitServerSync();
        this.minecraft.setScreen(parentScreen);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        gfx.fill(0, 0, this.width, this.height, EditorTheme.BG_OVERLAY);
        EditorTheme.drawWindow(gfx, uiX, uiY, uiW, uiH);

        String title = (isNew ? "Create" : "Edit") + " Advancement";
        gfx.drawString(this.font, title, uiX + 16, uiY + 12, EditorTheme.TEXT_GOLD, false);
        int titleW = this.font.width(title);
        if (isNew) {
            EditorTheme.drawBadge(gfx, font, "NEW", uiX + titleW + 24, uiY + 10, 0xFF2D3748, EditorTheme.TEXT_PRIMARY);
        }

        gfx.fill(uiX + 1, uiY + HEADER_HEIGHT, uiX + uiW - 1, uiY + HEADER_HEIGHT + 1, EditorTheme.BORDER_INNER);

        gfx.fill(uiX + 1, uiY + HEADER_HEIGHT + 1, uiX + sidebarWidth, uiY + uiH - 1, EditorTheme.SIDEBAR_BG);

        int splitterX = uiX + sidebarWidth;
        boolean splitterHovered = mouseX >= splitterX - 3 && mouseX <= splitterX + 3 && mouseY >= uiY + HEADER_HEIGHT && mouseY <= uiY + uiH;
        if (isResizingSidebar) {
            gfx.fill(splitterX - 1, uiY + HEADER_HEIGHT + 1, splitterX + 2, uiY + uiH - 1, EditorTheme.ACCENT_GOLD);
        } else if (splitterHovered) {
            gfx.fill(splitterX, uiY + HEADER_HEIGHT + 1, splitterX + 1, uiY + uiH - 1, 0xFF6A7B9F);
        } else {
            gfx.fill(splitterX, uiY + HEADER_HEIGHT + 1, splitterX + 1, uiY + uiH - 1, EditorTheme.BORDER_INNER);
        }

        int tabY = uiY + HEADER_HEIGHT + 10;
        int i = 0;
        for (String tabName : tabs.keySet()) {
            int ry = tabY + i * (TAB_ROW_H + 4);
            int rowBot = ry + TAB_ROW_H;
            boolean selected = tabName.equals(activeTabName);
            boolean hovered = mouseX >= uiX + 8 && mouseX <= uiX + sidebarWidth - 8 && mouseY >= ry && mouseY <= rowBot;

            int bg = selected ? EditorTheme.TAB_ACTIVE_BG : (hovered ? EditorTheme.TAB_HOVER_BG : EditorTheme.TAB_INACTIVE_BG);
            gfx.fill(uiX + 8, ry, uiX + sidebarWidth - 8, rowBot, bg);
            gfx.renderOutline(uiX + 8, ry, sidebarWidth - 16, TAB_ROW_H, selected ? EditorTheme.ACCENT_GOLD_MUTED : EditorTheme.CARD_BORDER);

            if (selected) {
                gfx.fill(uiX + 8, ry, uiX + 11, rowBot, EditorTheme.ACCENT_GOLD);
            }

            int textCol = selected ? EditorTheme.TEXT_GOLD : (hovered ? EditorTheme.TEXT_PRIMARY : EditorTheme.TEXT_MUTED);
            gfx.drawString(font, tabName, uiX + 16, ry + (TAB_ROW_H - font.lineHeight) / 2, textCol, false);
            i++;
        }

        gfx.enableScissor(uiX + sidebarWidth + 8, uiY + HEADER_HEIGHT + 2, uiX + uiW - 8, uiY + uiH - 38);
        activeTab.render(gfx, mouseX, mouseY, partialTicks);
        super.render(gfx, mouseX, mouseY, partialTicks);
        gfx.disableScissor();

        if (saveBtn != null) saveBtn.render(gfx, mouseX, mouseY, partialTicks);
        if (cancelBtn != null) cancelBtn.render(gfx, mouseX, mouseY, partialTicks);

        if (activeTab != null) {
            for (GuiEventListener child : activeTab.getWidgets()) {
                if (child instanceof ModernDropdown md && md.isOpen()) {
                    md.renderDropdownPopup(gfx, mouseX, mouseY);
                }
                if (child instanceof SuggestingEditBox seb && seb.hasSuggestions()) {
                    seb.renderSuggestionsPopup(gfx, mouseX, mouseY);
                }
            }
            activeTab.renderOverlay(gfx, mouseX, mouseY, partialTicks);
        }
    }
}
