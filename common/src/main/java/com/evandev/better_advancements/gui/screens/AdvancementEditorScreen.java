package com.evandev.better_advancements.gui.screens;

import com.evandev.better_advancements.gui.model.AdvancementDraft;
import com.evandev.better_advancements.gui.tabs.CriteriaTab;
import com.evandev.better_advancements.gui.tabs.IEditorTab;
import com.evandev.better_advancements.gui.tabs.LayoutTab;
import com.evandev.better_advancements.gui.tabs.PropertiesTab;
import com.evandev.better_advancements.network.EditAdvancementPayload;
import com.evandev.better_advancements.platform.Services;
import com.evandev.better_advancements.util.PersistentData;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

public class AdvancementEditorScreen extends Screen {
    private static final int COL_GOLD = 0xFFC8AA64;
    private static final int COL_BG_OVERLAY = 0xBB101010;
    private static final int COL_ROW_BG = 0xFF3A3A3A;
    private static final int COL_SEL_INNER = 0xCCFEFCF5;
    private static final int COL_SEL_TEXT = 0xFF3A3A3A;

    private static final int MAX_UI_WIDTH = 540;
    private static final int MAX_UI_HEIGHT = 340;
    private static final int SIDEBAR_WIDTH = 120;
    private static final int ROW_H = 24;

    private final BetterAdvancementsScreen parentScreen;
    private final ResourceLocation advId;
    private final AdvancementDraft draft;
    private final boolean isNew;
    private final int posX, posY;

    private final Map<String, IEditorTab> tabs = new LinkedHashMap<>();
    private String activeTabName;
    private IEditorTab activeTab;

    private int uiX, uiY, uiW, uiH;
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private boolean isDraggingScrollbar = false;

    private Button saveBtn;
    private Button cancelBtn;

    public AdvancementEditorScreen(BetterAdvancementsScreen parentScreen, ResourceLocation id, boolean isNew, int posX, int posY, String initialTabName, String rawJsonFromServer) {
        super(Component.literal((isNew ? "Create" : "Edit") + " Advancement: " + id));
        this.parentScreen = parentScreen;
        this.advId = id;
        this.isNew = isNew;
        this.posX = posX;
        this.posY = posY;

        this.draft = new AdvancementDraft(rawJsonFromServer, id.toString(), isNew);

        this.tabs.put("Properties", new PropertiesTab(Minecraft.getInstance().font));
        this.tabs.put("Layout", new LayoutTab(Minecraft.getInstance().font, posX, posY));
        this.tabs.put("Criteria", new CriteriaTab(Minecraft.getInstance().font));

        for (IEditorTab tab : this.tabs.values()) {
            tab.loadState(this.draft);
        }

        this.activeTabName = this.tabs.containsKey(initialTabName) ? initialTabName : "Properties";
        this.activeTab = this.tabs.get(this.activeTabName);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        this.scrollOffset -= (int) (scrollY * 20);
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, this.maxScroll));
        this.init();
        return true;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        setupBounds();

        int contentX = uiX + SIDEBAR_WIDTH + 20;
        int contentW = uiW - SIDEBAR_WIDTH - 40;
        int contentY = uiY + 50 - scrollOffset;
        int contentH = uiH - 80;

        activeTab.init(contentX, contentY, contentW, contentH, this::init);

        int bottomMostY = uiY + 50;
        for (GuiEventListener listener : activeTab.getWidgets()) {
            if (listener instanceof AbstractWidget aw) {
                bottomMostY = Math.max(bottomMostY, aw.getY() + aw.getHeight());
                this.addRenderableWidget(aw);
            }
        }

        int actualContentHeight = (bottomMostY + scrollOffset) - (uiY + 50);
        this.maxScroll = Math.max(0, actualContentHeight - (uiH - 90));

        if (this.scrollOffset > this.maxScroll) {
            this.scrollOffset = this.maxScroll;
            this.clearWidgets();
            activeTab.init(contentX, uiY + 50 - scrollOffset, contentW, contentH, this::init);
            for (GuiEventListener listener : activeTab.getWidgets()) {
                if (listener instanceof AbstractWidget aw) {
                    this.addRenderableWidget(aw);
                }
            }
        }

        addControlButtons();
    }

    private void setupBounds() {
        uiW = Math.min(this.width - 20, MAX_UI_WIDTH);
        uiH = Math.min(this.height - 20, MAX_UI_HEIGHT);
        uiX = (this.width - uiW) / 2;
        uiY = (this.height - uiH) / 2;
    }

    private void addControlButtons() {
        int btnW = 60, btnH = 20;
        int saveBtnX = uiX + uiW - btnW * 2 - 16;
        int saveBtnY = uiY + uiH - btnH - 10;

        saveBtn = Button.builder(Component.literal("Save"), b -> saveAndClose())
                .pos(saveBtnX, saveBtnY).size(btnW, btnH).build();
        cancelBtn = Button.builder(Component.literal("Cancel"), b -> this.minecraft.setScreen(parentScreen))
                .pos(saveBtnX + btnW + 6, saveBtnY).size(btnW, btnH).build();

        this.addRenderableWidget(saveBtn);
        this.addRenderableWidget(cancelBtn);
    }

    private void switchTab(String tabName) {
        if (!tabs.containsKey(tabName) || tabName.equals(activeTabName)) return;

        activeTab.saveState(draft);
        activeTabName = tabName;
        activeTab = tabs.get(tabName);
        activeTab.loadState(draft);
        scrollOffset = 0;
        this.init();
    }

    private void saveAndClose() {
        activeTab.saveState(draft);

        ResourceLocation finalId = ResourceLocation.parse(draft.id);

        int finalX = posX;
        int finalY = posY;
        if (tabs.get("Layout") instanceof LayoutTab layoutTab) {
            finalX = layoutTab.getX();
            finalY = layoutTab.getY();
        }
        PersistentData.setPosition(finalId, finalX, finalY);

        String payloadStr = new GsonBuilder().setPrettyPrinting().create().toJson(draft.rootJson);
        EditAdvancementPayload payload = new EditAdvancementPayload(finalId, payloadStr, false);

        if (Services.PLATFORM.canSendAdvancementEdit()) {
            Services.PLATFORM.sendAdvancementEdit(payload);
        }
        this.minecraft.setScreen(parentScreen);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        super.renderBackground(gfx, mouseX, mouseY, partialTicks);

        gfx.fill(0, 0, this.width, this.height, COL_BG_OVERLAY);
        gfx.fill(uiX, uiY, uiX + uiW, uiY + uiH, 0xFF202020);
        gfx.fill(uiX, uiY, uiX + SIDEBAR_WIDTH, uiY + uiH, 0xFF181818);

        gfx.drawString(this.font, (isNew ? "Create" : "Edit") + " Advancement", uiX + SIDEBAR_WIDTH + 20, uiY + 15, COL_GOLD, false);
        gfx.fill(uiX + SIDEBAR_WIDTH + 20, uiY + 30, uiX + uiW - 20, uiY + 31, 0x55808080);

        int treeTop = uiY + 15;
        int i = 0;
        for (String tabName : tabs.keySet()) {
            int ry = treeTop + i * (ROW_H + 4);
            int rowBot = ry + ROW_H;
            boolean selected = (tabName.equals(activeTabName));

            gfx.fill(uiX + 5, ry, uiX + SIDEBAR_WIDTH - 5, rowBot, COL_ROW_BG);
            if (selected) {
                gfx.fill(uiX + 5, ry, uiX + SIDEBAR_WIDTH - 5, rowBot, COL_SEL_INNER);
                gfx.fill(uiX + 5, ry + 1, uiX + 7, rowBot - 1, COL_GOLD);
            }

            int textCol = selected ? COL_SEL_TEXT : COL_GOLD;
            gfx.drawString(font, tabName, uiX + 15, ry + (ROW_H - font.lineHeight) / 2 + 1, textCol, false);
            i++;
        }

        if (this.maxScroll > 0) {
            int scrollX = uiX + uiW - 12;
            int scrollY = uiY + 32;
            int scrollH = uiH - 72;
            int thumbH = Math.max(20, scrollH * scrollH / (scrollH + maxScroll));
            int thumbY = scrollY + (int) ((scrollH - thumbH) * (this.scrollOffset / (float) this.maxScroll));

            gfx.fill(scrollX, scrollY, scrollX + 8, scrollY + scrollH, 0xFF000000);
            gfx.fill(scrollX + 1, thumbY, scrollX + 7, thumbY + thumbH, 0xFF888888);
        }

        gfx.enableScissor(uiX + SIDEBAR_WIDTH, uiY + 32, uiX + uiW, uiY + uiH - 40);
        activeTab.render(gfx, mouseX, mouseY, partialTicks);
        super.render(gfx, mouseX, mouseY, partialTicks);
        gfx.disableScissor();

        if (saveBtn != null) saveBtn.render(gfx, mouseX, mouseY, partialTicks);
        if (cancelBtn != null) cancelBtn.render(gfx, mouseX, mouseY, partialTicks);
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

        int x = (int) mx, y = (int) my;
        int treeTop = uiY + 15;
        int i = 0;
        for (String tabName : tabs.keySet()) {
            int ry = treeTop + i * (ROW_H + 4);
            if (x >= uiX + 5 && x < uiX + SIDEBAR_WIDTH - 5 && y >= ry && y < ry + ROW_H) {
                switchTab(tabName);
                return true;
            }
            i++;
        }

        if (x > uiX + SIDEBAR_WIDTH && x < uiX + uiW) {
            if (y > uiY && y < uiY + 32) return false;
            if (y > uiY + uiH - 40 && y < uiY + uiH) {
                if (saveBtn != null && saveBtn.isMouseOver(mx, my)) return saveBtn.mouseClicked(mx, my, button);
                if (cancelBtn != null && cancelBtn.isMouseOver(mx, my)) return cancelBtn.mouseClicked(mx, my, button);
                return false;
            }
        }

        return super.mouseClicked(mx, my, button);
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.minecraft.setScreen(parentScreen);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}