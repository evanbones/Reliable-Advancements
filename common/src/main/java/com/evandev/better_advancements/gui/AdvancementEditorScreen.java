package com.evandev.better_advancements.gui;

import com.evandev.better_advancements.network.EditAdvancementPayload;
import com.evandev.better_advancements.platform.Services;
import com.evandev.better_advancements.reference.Constants;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.stream.Collectors;

public class AdvancementEditorScreen extends Screen {

    private static final int COL_GOLD = 0xFFC8AA64;
    private static final int COL_BG_OVERLAY = 0xBB101010;
    private static final int COL_TEXT_FAINT = 0xFFA08060;
    private static final int COL_ROW_BG = 0xFF3A3A3A;
    private static final int COL_SEL_INNER = 0xCCFEFCF5;
    private static final int COL_SEL_TEXT = 0xFF3A3A3A;

    private static final int MAX_UI_WIDTH = 540;
    private static final int MAX_UI_HEIGHT = 340;
    private static final int SIDEBAR_WIDTH = 120;
    private static final int ROW_H = 24;

    private static final int START_Y_OFFSET = 50;
    private static final int FIELD_SPACING = 45;

    private final BetterAdvancementsScreen parentScreen;
    private final BetterAdvancementWidget widget;

    private EditorTab activeTab;
    private int uiX, uiY, uiW, uiH;

    private EditBox titleBox, descriptionBox, iconBox, parentBox, xPosBox, yPosBox;
    private EditBox critNameBox, critTriggerBox, critItemBox;
    private List<String> suggestions = List.of();
    private int suggestionIndex = -1;

    public AdvancementEditorScreen(BetterAdvancementsScreen parentScreen, BetterAdvancementWidget widget, EditorTab initialTab) {
        super(Component.literal("Edit Advancement: " + widget.getAdvancement().holder().id().toString()));
        this.parentScreen = parentScreen;
        this.widget = widget;
        this.activeTab = initialTab;
    }

    @Override
    protected void init() {
        this.clearWidgets();

        uiW = Math.min(this.width - 40, MAX_UI_WIDTH);
        uiW = Math.max(uiW, 360);
        uiH = Math.min(this.height - 40, MAX_UI_HEIGHT);
        uiH = Math.max(uiH, 260);

        uiX = (this.width - uiW) / 2;
        uiY = (this.height - uiH) / 2;

        int btnW = 60;
        int btnH = 20;
        int saveBtnX = uiX + uiW - btnW * 2 - 16;
        int saveBtnY = uiY + uiH - btnH - 10;
        int cancelBtnX = uiX + uiW - btnW - 10;

        this.addRenderableWidget(Button.builder(Component.literal("Save"), b -> saveAndClose())
                .pos(saveBtnX, saveBtnY).size(btnW, btnH).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> this.minecraft.setScreen(parentScreen))
                .pos(cancelBtnX, saveBtnY).size(btnW, btnH).build());

        AdvancementNode node = widget.getAdvancement();
        DisplayInfo display = node.advancement().display().orElse(null);

        int contentX = uiX + SIDEBAR_WIDTH + 20;
        int contentW = uiW - SIDEBAR_WIDTH - 40;
        int startY = uiY + START_Y_OFFSET;

        if (activeTab == EditorTab.PROPERTIES) {
            titleBox = new EditBox(this.font, contentX, startY, contentW, 20, Component.literal("Title"));
            titleBox.setMaxLength(256);
            titleBox.setValue(display != null ? display.getTitle().getString() : "");
            this.addRenderableWidget(titleBox);

            descriptionBox = new EditBox(this.font, contentX, startY + FIELD_SPACING, contentW, 20, Component.literal("Description"));
            descriptionBox.setMaxLength(512);
            descriptionBox.setValue(display != null ? display.getDescription().getString() : "");
            this.addRenderableWidget(descriptionBox);

            iconBox = new EditBox(this.font, contentX, startY + FIELD_SPACING * 2, contentW, 20, Component.literal("Icon Item ID"));
            iconBox.setMaxLength(256);
            iconBox.setValue(display != null ? BuiltInRegistries.ITEM.getKey(display.getIcon().getItem()).toString() : "minecraft:stone");
            this.addRenderableWidget(iconBox);

            parentBox = new EditBox(this.font, contentX, startY + FIELD_SPACING * 3, contentW, 20, Component.literal("Parent ID"));
            parentBox.setMaxLength(256);
            parentBox.setValue(node.parent() != null ? node.parent().holder().id().toString() : "");
            this.addRenderableWidget(parentBox);
        } else if (activeTab == EditorTab.LAYOUT) {
            xPosBox = new EditBox(this.font, contentX, startY, 100, 20, Component.literal("X Position"));
            xPosBox.setValue(String.valueOf(widget.getX()));
            this.addRenderableWidget(xPosBox);

            yPosBox = new EditBox(this.font, contentX, startY + FIELD_SPACING, 100, 20, Component.literal("Y Position"));
            yPosBox.setValue(String.valueOf(widget.getY()));
            this.addRenderableWidget(yPosBox);
        } else if (activeTab == EditorTab.CRITERIA) {
            critNameBox = new EditBox(this.font, contentX, startY, contentW, 20, Component.literal("Criterion Name"));
            critNameBox.setValue("has_item");
            this.addRenderableWidget(critNameBox);

            critTriggerBox = new EditBox(this.font, contentX, startY + FIELD_SPACING, contentW, 20, Component.literal("Trigger"));
            critTriggerBox.setValue("minecraft:inventory_changed");
            this.addRenderableWidget(critTriggerBox);

            critItemBox = new EditBox(this.font, contentX, startY + FIELD_SPACING * 2, contentW, 20, Component.literal("Required Item (Autocomplete)"));
            critItemBox.setResponder(this::updateSuggestions);
            this.addRenderableWidget(critItemBox);
        }
    }

    private void updateSuggestions(String text) {
        if (text.isEmpty()) {
            suggestions = List.of();
            suggestionIndex = -1;
            return;
        }
        suggestions = BuiltInRegistries.ITEM.keySet().stream()
                .map(ResourceLocation::toString)
                .filter(id -> id.contains(text.toLowerCase()))
                .limit(6)
                .collect(Collectors.toList());
        suggestionIndex = suggestions.isEmpty() ? -1 : 0;
    }

    private void saveAndClose() {
        ResourceLocation advancementId = widget.getAdvancement().holder().id();
        AdvancementNode node = widget.getAdvancement();
        DisplayInfo display = node.advancement().display().orElse(null);

        String title = titleBox != null ? titleBox.getValue() : (display != null ? display.getTitle().getString() : "");
        String desc = descriptionBox != null ? descriptionBox.getValue() : (display != null ? display.getDescription().getString() : "");
        String icon = iconBox != null ? iconBox.getValue() : (display != null ? BuiltInRegistries.ITEM.getKey(display.getIcon().getItem()).toString() : "minecraft:stone");
        String parent = parentBox != null ? parentBox.getValue() : (node.parent() != null ? node.parent().holder().id().toString() : "");

        EditAdvancementPayload payload = new EditAdvancementPayload(advancementId, title, desc, icon, parent, false);

        if (Services.PLATFORM.canSendAdvancementEdit()) {
            Services.PLATFORM.sendAdvancementEdit(payload);
            if (activeTab == EditorTab.CRITERIA) {
                Constants.LOG.info("Criteria parameters captured ({} / {}) but full backend schema is not ready yet.", critNameBox.getValue(), critItemBox.getValue());
            }
        } else {
            this.minecraft.player.displayClientMessage(
                    Component.translatable("message.betteradvancements.no_server_support").withStyle(net.minecraft.ChatFormatting.RED), false
            );
        }

        this.minecraft.setScreen(parentScreen);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        gfx.fill(0, 0, this.width, this.height, COL_BG_OVERLAY);

        gfx.fill(uiX, uiY, uiX + uiW, uiY + uiH, 0xFF202020);
        gfx.fill(uiX, uiY, uiX + SIDEBAR_WIDTH, uiY + uiH, 0xFF181818);

        gfx.drawString(this.font, "Edit Advancement", uiX + SIDEBAR_WIDTH + 20, uiY + 15, COL_GOLD, false);
        gfx.fill(uiX + SIDEBAR_WIDTH + 20, uiY + 30, uiX + uiW - 20, uiY + 31, 0x55808080);

        EditorTab[] tabs = EditorTab.values();
        int treeTop = uiY + 15;
        for (int i = 0; i < tabs.length; i++) {
            EditorTab tab = tabs[i];
            int ry = treeTop + i * (ROW_H + 4);
            int rowBot = ry + ROW_H;
            boolean selected = (tab == activeTab);

            gfx.fill(uiX + 5, ry, uiX + SIDEBAR_WIDTH - 5, rowBot, COL_ROW_BG);
            if (selected) {
                gfx.fill(uiX + 5, ry, uiX + SIDEBAR_WIDTH - 5, rowBot, COL_SEL_INNER);
                gfx.fill(uiX + 5, ry + 1, uiX + 7, rowBot - 1, COL_GOLD);
            }

            int textCol = selected ? COL_SEL_TEXT : COL_GOLD;
            gfx.drawString(font, tab.name(), uiX + 15, ry + (ROW_H - font.lineHeight) / 2 + 1, textCol, false);
        }

        int contentX = uiX + SIDEBAR_WIDTH + 20;
        int startY = uiY + START_Y_OFFSET;

        if (activeTab == EditorTab.PROPERTIES) {
            gfx.drawString(this.font, "Title", contentX, startY - 11, COL_TEXT_FAINT, false);
            gfx.drawString(this.font, "Description", contentX, startY + FIELD_SPACING - 11, COL_TEXT_FAINT, false);
            gfx.drawString(this.font, "Icon (Item ID)", contentX, startY + FIELD_SPACING * 2 - 11, COL_TEXT_FAINT, false);
            gfx.drawString(this.font, "Parent ID", contentX, startY + FIELD_SPACING * 3 - 11, COL_TEXT_FAINT, false);
        } else if (activeTab == EditorTab.LAYOUT) {
            gfx.drawString(this.font, "X Position", contentX, startY - 11, COL_TEXT_FAINT, false);
            gfx.drawString(this.font, "Y Position", contentX, startY + FIELD_SPACING - 11, COL_TEXT_FAINT, false);
        } else if (activeTab == EditorTab.CRITERIA) {
            gfx.drawString(this.font, "Criterion Name", contentX, startY - 11, COL_TEXT_FAINT, false);
            gfx.drawString(this.font, "Trigger", contentX, startY + FIELD_SPACING - 11, COL_TEXT_FAINT, false);
            gfx.drawString(this.font, "Required Item", contentX, startY + FIELD_SPACING * 2 - 11, COL_TEXT_FAINT, false);
        }

        super.render(gfx, mouseX, mouseY, partialTicks);

        if (activeTab == EditorTab.CRITERIA && critItemBox != null && critItemBox.isFocused() && !suggestions.isEmpty()) {
            int dropX = critItemBox.getX();
            int dropY = critItemBox.getY() + 20;
            int dropW = critItemBox.getWidth();
            int dropH = suggestions.size() * 14 + 4;

            gfx.pose().pushPose();
            gfx.pose().translate(0, 0, 500);
            gfx.fill(dropX, dropY, dropX + dropW, dropY + dropH, 0xF0101010);
            gfx.renderOutline(dropX, dropY, dropW, dropH, COL_GOLD);

            for (int i = 0; i < suggestions.size(); i++) {
                int color = (i == suggestionIndex) ? COL_SEL_TEXT : COL_TEXT_FAINT;
                if (i == suggestionIndex) {
                    gfx.fill(dropX + 1, dropY + 2 + i * 14, dropX + dropW - 1, dropY + 2 + (i + 1) * 14, COL_GOLD);
                }
                gfx.drawString(font, suggestions.get(i), dropX + 4, dropY + 5 + i * 14, color, false);
            }
            gfx.pose().popPose();
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x = (int) mx, y = (int) my;

        int treeTop = uiY + 15;
        EditorTab[] tabs = EditorTab.values();
        for (int i = 0; i < tabs.length; i++) {
            int ry = treeTop + i * (ROW_H + 4);
            if (x >= uiX + 5 && x < uiX + SIDEBAR_WIDTH - 5 && y >= ry && y < ry + ROW_H) {
                if (activeTab != tabs[i]) {
                    activeTab = tabs[i];
                    this.init();
                }
                return true;
            }
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (activeTab == EditorTab.CRITERIA && critItemBox != null && critItemBox.isFocused() && !suggestions.isEmpty()) {
            if (keyCode == 264) {
                suggestionIndex = (suggestionIndex + 1) % suggestions.size();
                return true;
            } else if (keyCode == 265) {
                suggestionIndex = (suggestionIndex - 1 + suggestions.size()) % suggestions.size();
                return true;
            } else if (keyCode == 257 || keyCode == 335) {
                critItemBox.setValue(suggestions.get(suggestionIndex));
                critItemBox.moveCursorToEnd(false);
                updateSuggestions("");
                return true;
            }
        }

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

    public enum EditorTab {PROPERTIES, LAYOUT, CRITERIA}
}