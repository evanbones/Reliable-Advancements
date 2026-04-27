package com.evandev.better_advancements.gui;

import com.evandev.better_advancements.network.EditAdvancementPayload;
import com.evandev.better_advancements.platform.Services;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
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
    private final List<CriterionEntry> criteriaList = new ArrayList<>();
    private EditorTab activeTab;
    private int uiX, uiY, uiW, uiH;
    private EditBox titleBox, descriptionBox, iconBox, parentBox, xPosBox, yPosBox;
    private EditBox critNameBox, critTriggerBox, critItemBox;
    private List<String> suggestions = List.of();
    private int suggestionIndex = -1;
    private boolean isAutocompleteItems = false;
    private JsonObject rootJson = new JsonObject();
    private int selectedCriterion = 0;

    public AdvancementEditorScreen(BetterAdvancementsScreen parentScreen, BetterAdvancementWidget widget, EditorTab initialTab) {
        super(Component.literal("Edit Advancement: " + widget.getAdvancement().holder().id().toString()));
        this.parentScreen = parentScreen;
        this.widget = widget;
        this.activeTab = initialTab;
    }

    @Override
    protected void init() {
        if (rootJson.entrySet().isEmpty()) {
            buildInitialJson();
        } else {
            saveCurrentTabState();
        }

        this.clearWidgets();

        uiW = Math.min(this.width - 40, MAX_UI_WIDTH);
        uiW = Math.max(uiW, 360);
        uiH = Math.min(this.height - 40, MAX_UI_HEIGHT);
        uiH = Math.max(uiH, 260);
        uiX = (this.width - uiW) / 2;
        uiY = (this.height - uiH) / 2;

        int btnW = 60, btnH = 20;
        int saveBtnX = uiX + uiW - btnW * 2 - 16;
        int saveBtnY = uiY + uiH - btnH - 10;

        this.addRenderableWidget(Button.builder(Component.literal("Save"), b -> saveAndClose())
                .pos(saveBtnX, saveBtnY).size(btnW, btnH).build());
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> this.minecraft.setScreen(parentScreen))
                .pos(saveBtnX + btnW + 6, saveBtnY).size(btnW, btnH).build());

        int contentX = uiX + SIDEBAR_WIDTH + 20;
        int contentW = uiW - SIDEBAR_WIDTH - 40;
        int startY = uiY + START_Y_OFFSET;

        if (activeTab == EditorTab.PROPERTIES) {
            net.minecraft.advancements.DisplayInfo dInfo = widget.getAdvancement().advancement().display().orElse(null);

            titleBox = new EditBox(this.font, contentX, startY, contentW, 20, Component.literal("Title"));
            titleBox.setMaxLength(256);
            titleBox.setValue(dInfo != null ? dInfo.getTitle().getString() : "");
            this.addRenderableWidget(titleBox);

            descriptionBox = new EditBox(this.font, contentX, startY + FIELD_SPACING, contentW, 20, Component.literal("Description"));
            descriptionBox.setMaxLength(512);
            descriptionBox.setValue(dInfo != null ? dInfo.getDescription().getString() : "");
            this.addRenderableWidget(descriptionBox);

            iconBox = new EditBox(this.font, contentX, startY + FIELD_SPACING * 2, contentW, 20, Component.literal("Icon"));
            iconBox.setMaxLength(256);
            if (dInfo != null) {
                iconBox.setValue(BuiltInRegistries.ITEM.getKey(dInfo.getIcon().getItem()).toString());
            } else {
                iconBox.setValue("minecraft:stone");
            }
            iconBox.setResponder(t -> updateSuggestions(t, true));
            this.addRenderableWidget(iconBox);

            parentBox = new EditBox(this.font, contentX, startY + FIELD_SPACING * 3, contentW, 20, Component.literal("Parent"));
            parentBox.setMaxLength(256);

            String parentStr = "";
            AdvancementNode parentNode = widget.getAdvancement().parent();
            if (rootJson.has("parent")) {
                parentStr = rootJson.get("parent").getAsString();
            } else if (parentNode != null) {
                parentStr = parentNode.holder().id().toString();
            }
            parentBox.setValue(parentStr);

            this.addRenderableWidget(parentBox);

        } else if (activeTab == EditorTab.LAYOUT) {
            xPosBox = new EditBox(this.font, contentX, startY, 100, 20, Component.literal("X Position"));
            xPosBox.setValue(String.valueOf(widget.getX()));
            this.addRenderableWidget(xPosBox);

            yPosBox = new EditBox(this.font, contentX, startY + FIELD_SPACING, 100, 20, Component.literal("Y Position"));
            yPosBox.setValue(String.valueOf(widget.getY()));
            this.addRenderableWidget(yPosBox);

        } else if (activeTab == EditorTab.CRITERIA) {
            if (criteriaList.isEmpty()) criteriaList.add(new CriterionEntry());
            CriterionEntry activeCrit = criteriaList.get(selectedCriterion);

            this.addRenderableWidget(Button.builder(Component.literal("<"), b -> switchCriterion(-1))
                    .pos(contentX, startY).size(20, 20).build());

            critNameBox = new EditBox(this.font, contentX + 25, startY, contentW - 80, 20, Component.literal("Criterion Name"));
            critNameBox.setValue(activeCrit.name);
            this.addRenderableWidget(critNameBox);

            this.addRenderableWidget(Button.builder(Component.literal(">"), b -> switchCriterion(1))
                    .pos(contentX + contentW - 50, startY).size(20, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("+"), b -> addCriterion())
                    .pos(contentX + contentW - 25, startY).size(20, 20).build());

            critTriggerBox = new EditBox(this.font, contentX, startY + FIELD_SPACING, contentW, 20, Component.literal("Trigger"));
            critTriggerBox.setValue(activeCrit.trigger);
            critTriggerBox.setResponder(t -> updateSuggestions(t, false));
            this.addRenderableWidget(critTriggerBox);

            critItemBox = new EditBox(this.font, contentX, startY + FIELD_SPACING * 2, contentW, 20, Component.literal("Required Item / Target"));
            critItemBox.setValue(activeCrit.conditionValue);
            critItemBox.setResponder(t -> updateSuggestions(t, true));
            this.addRenderableWidget(critItemBox);
        }
    }

    private void buildInitialJson() {
        Advancement adv = widget.getAdvancement().advancement();

        rootJson = Advancement.CODEC
                .encodeStart(JsonOps.INSTANCE, adv)
                .result()
                .map(JsonElement::getAsJsonObject)
                .orElseGet(JsonObject::new);

        syncRootJsonToCriteriaList();
    }

    private void saveCurrentTabState() {
        if (activeTab == EditorTab.PROPERTIES) {
            JsonObject display = rootJson.has("display") ? rootJson.getAsJsonObject("display") : new JsonObject();
            if (titleBox != null) display.addProperty("title", titleBox.getValue());
            if (descriptionBox != null) display.addProperty("description", descriptionBox.getValue());
            if (iconBox != null) {
                JsonObject icon = display.has("icon") ? display.getAsJsonObject("icon") : new JsonObject();
                icon.addProperty("id", iconBox.getValue());
                display.add("icon", icon);
            }
            rootJson.add("display", display);

            if (parentBox != null) {
                if (parentBox.getValue().isEmpty()) rootJson.remove("parent");
                else rootJson.addProperty("parent", parentBox.getValue());
            }
        } else if (activeTab == EditorTab.CRITERIA) {
            if (critNameBox != null) {
                CriterionEntry entry = criteriaList.get(selectedCriterion);
                entry.name = critNameBox.getValue();
                entry.trigger = critTriggerBox.getValue();
                entry.conditionValue = critItemBox.getValue();
            }
            syncCriteriaListToRootJson();
        }
    }

    private void syncCriteriaListToRootJson() {
        JsonObject criteriaObj = new JsonObject();
        JsonArray requirementsObj = new JsonArray();
        for (CriterionEntry entry : criteriaList) {
            JsonObject crit = new JsonObject();
            crit.addProperty("trigger", entry.trigger);

            if (!entry.conditionValue.isEmpty()) {
                if (entry.trigger.contains("inventory_changed") || entry.trigger.contains("recipe_unlocked")) {
                    JsonObject conds = new JsonObject();
                    JsonArray items = new JsonArray();
                    JsonObject itemObj = new JsonObject();
                    itemObj.addProperty("items", entry.conditionValue);
                    items.add(itemObj);
                    conds.add("items", items);
                    crit.add("conditions", conds);
                } else if (entry.trigger.contains("changed_dimension")) {
                    JsonObject conds = new JsonObject();
                    conds.addProperty("to", entry.conditionValue);
                    crit.add("conditions", conds);
                } else if (entry.rawConditions != null) {
                    crit.add("conditions", entry.rawConditions);
                }
            } else if (entry.rawConditions != null) {
                crit.add("conditions", entry.rawConditions);
            }

            criteriaObj.add(entry.name, crit);
            JsonArray req = new JsonArray();
            req.add(entry.name);
            requirementsObj.add(req);
        }

        if (criteriaList.isEmpty()) {
            JsonObject dummy = new JsonObject();
            dummy.addProperty("trigger", "minecraft:impossible");
            criteriaObj.add("dummy", dummy);
        }

        rootJson.add("criteria", criteriaObj);
        if (!criteriaList.isEmpty()) {
            rootJson.add("requirements", requirementsObj);
        }
    }

    private void syncRootJsonToCriteriaList() {
        criteriaList.clear();
        Advancement adv = widget.getAdvancement().advancement();
        List<String> knownNames = new ArrayList<>();

        for (String req : adv.requirements().names()) {
            knownNames.add(req);
        }

        if (rootJson.has("criteria") && rootJson.get("criteria").isJsonObject()) {
            JsonObject criteriaObj = rootJson.getAsJsonObject("criteria");
            for (String key : criteriaObj.keySet()) {
                if (key.equals("dummy")) continue;
                if (!knownNames.contains(key)) knownNames.add(key);
            }
        }

        for (String key : knownNames) {
            CriterionEntry entry = new CriterionEntry();
            entry.name = key;

            if (rootJson.has("criteria") && rootJson.getAsJsonObject("criteria").has(key)) {
                JsonObject critObj = rootJson.getAsJsonObject("criteria").getAsJsonObject(key);
                if (critObj.has("trigger")) {
                    entry.trigger = critObj.get("trigger").getAsString();
                }

                if (critObj.has("conditions")) {
                    JsonObject conds = critObj.getAsJsonObject("conditions");
                    entry.rawConditions = conds.deepCopy();

                    if (conds.has("items")) {
                        JsonElement itemsElem = conds.get("items");
                        if (itemsElem.isJsonArray() && !itemsElem.getAsJsonArray().isEmpty()) {
                            JsonElement first = itemsElem.getAsJsonArray().get(0);
                            if (first.isJsonObject() && first.getAsJsonObject().has("items")) {
                                JsonElement innerItems = first.getAsJsonObject().get("items");
                                if (innerItems.isJsonPrimitive()) {
                                    entry.conditionValue = innerItems.getAsString();
                                } else if (innerItems.isJsonArray() && !innerItems.getAsJsonArray().isEmpty()) {
                                    entry.conditionValue = innerItems.getAsJsonArray().get(0).getAsString();
                                }
                            } else if (first.isJsonPrimitive()) {
                                entry.conditionValue = first.getAsString();
                            }
                        } else if (itemsElem.isJsonPrimitive()) {
                            entry.conditionValue = itemsElem.getAsString();
                        }
                    } else if (conds.has("to")) {
                        entry.conditionValue = conds.get("to").getAsString();
                    } else {
                        entry.conditionValue = "";
                    }
                } else {
                    entry.conditionValue = "";
                }
            } else {
                entry.conditionValue = "";
            }
            criteriaList.add(entry);
        }

        if (criteriaList.isEmpty()) criteriaList.add(new CriterionEntry());
        selectedCriterion = 0;
    }

    private void switchCriterion(int dir) {
        saveCurrentTabState();
        selectedCriterion = (selectedCriterion + dir + criteriaList.size()) % criteriaList.size();
        this.init();
    }

    private void addCriterion() {
        saveCurrentTabState();
        CriterionEntry newEntry = new CriterionEntry();
        newEntry.name = "new_criterion_" + criteriaList.size();
        criteriaList.add(newEntry);
        selectedCriterion = criteriaList.size() - 1;
        this.init();
    }

    private void updateSuggestions(String text, boolean items) {
        this.isAutocompleteItems = items;
        if (text.isEmpty()) {
            suggestions = List.of();
            suggestionIndex = -1;
            return;
        }

        var registry = items ? BuiltInRegistries.ITEM : BuiltInRegistries.TRIGGER_TYPES;

        suggestions = registry.keySet().stream()
                .map(ResourceLocation::toString)
                .filter(id -> id.contains(text.toLowerCase()))
                .limit(6)
                .collect(Collectors.toList());
        suggestionIndex = suggestions.isEmpty() ? -1 : 0;
    }

    private void saveAndClose() {
        saveCurrentTabState();
        String payloadStr = new GsonBuilder().setPrettyPrinting().create().toJson(rootJson);
        EditAdvancementPayload payload = new EditAdvancementPayload(widget.getAdvancement().holder().id(), payloadStr, false);

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
            gfx.drawString(this.font, "Criteria " + (selectedCriterion + 1) + "/" + criteriaList.size(), contentX, startY - 11, COL_TEXT_FAINT, false);
            gfx.drawString(this.font, "Trigger", contentX, startY + FIELD_SPACING - 11, COL_TEXT_FAINT, false);
            gfx.drawString(this.font, "Required Item / Target", contentX, startY + FIELD_SPACING * 2 - 11, COL_TEXT_FAINT, false);
        }

        super.render(gfx, mouseX, mouseY, partialTicks);

        EditBox activeBox = isAutocompleteItems ? (activeTab == EditorTab.PROPERTIES ? iconBox : critItemBox) : critTriggerBox;
        if (activeBox != null && activeBox.isFocused() && !suggestions.isEmpty()) {
            int dropX = activeBox.getX();
            int dropY = activeBox.getY() + 20;
            int dropW = activeBox.getWidth();
            int dropH = suggestions.size() * 14 + 4;

            gfx.pose().pushPose();
            gfx.pose().translate(0, 0, 500);
            gfx.fill(dropX, dropY, dropX + dropW, dropY + dropH, 0xF0101010);
            gfx.renderOutline(dropX, dropY, dropW, dropH, COL_GOLD);

            for (int i = 0; i < suggestions.size(); i++) {
                int color = (i == suggestionIndex) ? COL_SEL_TEXT : COL_TEXT_FAINT;
                if (i == suggestionIndex)
                    gfx.fill(dropX + 1, dropY + 2 + i * 14, dropX + dropW - 1, dropY + 2 + (i + 1) * 14, COL_GOLD);
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
                    saveCurrentTabState();
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
        EditBox activeBox = isAutocompleteItems ? (activeTab == EditorTab.PROPERTIES ? iconBox : critItemBox) : critTriggerBox;
        if (activeBox != null && activeBox.isFocused() && !suggestions.isEmpty()) {
            if (keyCode == 264) {
                suggestionIndex = (suggestionIndex + 1) % suggestions.size();
                return true;
            } else if (keyCode == 265) {
                suggestionIndex = (suggestionIndex - 1 + suggestions.size()) % suggestions.size();
                return true;
            } else if (keyCode == 257 || keyCode == 335) {
                activeBox.setValue(suggestions.get(suggestionIndex));
                activeBox.moveCursorToEnd(false);
                updateSuggestions("", isAutocompleteItems);
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

    private static class CriterionEntry {
        String name = "my_criterion";
        String trigger = "minecraft:inventory_changed";
        String conditionValue = "minecraft:stone";
        JsonObject rawConditions = null;
    }
}