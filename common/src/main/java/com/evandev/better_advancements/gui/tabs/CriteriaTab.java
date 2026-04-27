package com.evandev.better_advancements.gui.tabs;

import com.evandev.better_advancements.gui.model.AdvancementDraft;
import com.evandev.better_advancements.gui.widgets.SuggestingEditBox;
import com.evandev.better_advancements.util.TriggerSchemaManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CriteriaTab implements IEditorTab {
    private final Font font;
    private final List<GuiEventListener> widgets = new ArrayList<>();
    private final List<CriterionEntry> criteriaList = new ArrayList<>();
    private final List<ConditionRow> conditionRows = new ArrayList<>();
    private int selectedCriterion = 0;
    private int startX, startY, width;
    private EditBox nameBox;
    private SuggestingEditBox triggerBox;

    public CriteriaTab(Font font) {
        this.font = font;
    }

    @Override
    public void loadState(AdvancementDraft draft) {
        criteriaList.clear();
        try {
            if (draft.rootJson.has("criteria")) {
                JsonObject criteriaObj = draft.rootJson.getAsJsonObject("criteria");
                for (String key : criteriaObj.keySet()) {
                    CriterionEntry entry = new CriterionEntry();
                    entry.name = key;
                    JsonObject critObj = criteriaObj.getAsJsonObject(key);
                    if (critObj.has("trigger")) entry.trigger = critObj.get("trigger").getAsString();
                    if (critObj.has("conditions"))
                        entry.conditionsJson = new Gson().toJson(critObj.getAsJsonObject("conditions"));
                    criteriaList.add(entry);
                }
            }
        } catch (Exception ignored) {}

        if (criteriaList.isEmpty()) criteriaList.add(new CriterionEntry());
        selectedCriterion = Math.max(0, Math.min(selectedCriterion, criteriaList.size() - 1));
    }

    @Override
    public void init(int x, int y, int width, int height, Runnable reinitScreen) {
        this.widgets.clear();
        this.conditionRows.clear();
        this.startX = x;
        this.startY = y;
        this.width = width;

        CriterionEntry active = criteriaList.get(selectedCriterion);

        widgets.add(Button.builder(Component.literal("<"), b -> switchCriterion(-1, reinitScreen)).pos(x, y).size(20, 20).build());
        nameBox = new EditBox(font, x + 25, y, width - 105, 20, Component.literal("Name"));
        nameBox.setMaxLength(256);
        nameBox.setValue(active.name);
        widgets.add(nameBox);
        widgets.add(Button.builder(Component.literal(">"), b -> switchCriterion(1, reinitScreen)).pos(x + width - 75, y).size(20, 20).build());
        widgets.add(Button.builder(Component.literal("+"), b -> addCriterion(reinitScreen)).pos(x + width - 50, y).size(20, 20).build());
        widgets.add(Button.builder(Component.literal("-"), b -> removeCriterion(reinitScreen)).pos(x + width - 25, y).size(20, 20).build());

        triggerBox = new SuggestingEditBox(font, x, y + 45, width, 20, Component.literal("Trigger"),
                () -> BuiltInRegistries.TRIGGER_TYPES.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList()));
        triggerBox.setMaxLength(256);
        triggerBox.setValue(active.trigger);
        widgets.add(triggerBox);

        JsonObject condObj = getSafelyParsedJson(active.conditionsJson);
        int rowY = y + 90;

        for (String key : condObj.keySet()) {
            ConditionRow row = new ConditionRow();
            row.keyBox = new SuggestingEditBox(font, x, rowY, 100, 20, Component.literal("Key"),
                    () -> TriggerSchemaManager.getParameters(triggerBox.getValue()));
            row.keyBox.setMaxLength(256);
            row.keyBox.setValue(key);

            row.valBox = new EditBox(font, x + 105, rowY, width - 130, 20, Component.literal("Value"));
            row.valBox.setMaxLength(2048);
            row.valBox.setValue(new Gson().toJson(condObj.get(key)));

            Button removeBtn = Button.builder(Component.literal("-"), b -> removeConditionRow(row, reinitScreen))
                    .pos(x + width - 20, rowY).size(20, 20).build();

            widgets.addAll(List.of(row.keyBox, row.valBox, removeBtn));
            conditionRows.add(row);
            rowY += 25;
        }

        widgets.add(Button.builder(Component.literal("+ Add Condition"), b -> addConditionRow(reinitScreen))
                .pos(x, rowY).size(100, 20).build());
    }

    @Override
    public void saveState(AdvancementDraft draft) {
        if (nameBox != null && !criteriaList.isEmpty()) {
            CriterionEntry entry = criteriaList.get(selectedCriterion);
            entry.name = nameBox.getValue();
            entry.trigger = triggerBox.getValue();

            JsonObject newConds = new JsonObject();
            for (ConditionRow row : conditionRows) {
                if (!row.keyBox.getValue().isEmpty()) {
                    try {
                        newConds.add(row.keyBox.getValue(), JsonParser.parseString(row.valBox.getValue()));
                    } catch (Exception e) {
                        newConds.addProperty(row.keyBox.getValue(), row.valBox.getValue());
                    }
                }
            }
            entry.conditionsJson = new Gson().toJson(newConds);
        }

        JsonObject criteriaObj = new JsonObject();
        JsonArray requirementsObj = new JsonArray();
        for (CriterionEntry entry : criteriaList) {
            JsonObject crit = new JsonObject();
            crit.addProperty("trigger", entry.trigger);
            JsonObject parsedConds = getSafelyParsedJson(entry.conditionsJson);
            if (!parsedConds.isEmpty()) crit.add("conditions", parsedConds);

            criteriaObj.add(entry.name, crit);
            JsonArray req = new JsonArray();
            req.add(entry.name);
            requirementsObj.add(req);
        }

        draft.rootJson.add("criteria", criteriaObj);
        draft.rootJson.add("requirements", requirementsObj);
    }

    private void switchCriterion(int dir, Runnable reinitScreen) {
        saveLocal();
        selectedCriterion = (selectedCriterion + dir + criteriaList.size()) % criteriaList.size();
        reinitScreen.run();
    }

    private void addCriterion(Runnable reinitScreen) {
        saveLocal();
        CriterionEntry newEntry = new CriterionEntry();
        newEntry.name = "new_criterion_" + criteriaList.size();
        criteriaList.add(newEntry);
        selectedCriterion = criteriaList.size() - 1;
        reinitScreen.run();
    }

    private void removeCriterion(Runnable reinitScreen) {
        saveLocal();
        if (criteriaList.size() > 1) {
            criteriaList.remove(selectedCriterion);
            selectedCriterion = Math.max(0, selectedCriterion - 1);
        }
        reinitScreen.run();
    }

    private void addConditionRow(Runnable reinitScreen) {
        saveLocal();
        JsonObject condObj = getSafelyParsedJson(criteriaList.get(selectedCriterion).conditionsJson);
        condObj.addProperty("new_key_" + conditionRows.size(), "");
        criteriaList.get(selectedCriterion).conditionsJson = new Gson().toJson(condObj);
        reinitScreen.run();
    }

    private void removeConditionRow(ConditionRow row, Runnable reinitScreen) {
        saveLocal();
        JsonObject condObj = getSafelyParsedJson(criteriaList.get(selectedCriterion).conditionsJson);
        condObj.remove(row.keyBox.getValue());
        criteriaList.get(selectedCriterion).conditionsJson = new Gson().toJson(condObj);
        reinitScreen.run();
    }

    private void saveLocal() {
        if (nameBox != null) {
            CriterionEntry entry = criteriaList.get(selectedCriterion);
            entry.name = nameBox.getValue();
            entry.trigger = triggerBox.getValue();
            JsonObject newConds = new JsonObject();
            for (ConditionRow row : conditionRows) {
                if (!row.keyBox.getValue().isEmpty()) {
                    try {
                        newConds.add(row.keyBox.getValue(), JsonParser.parseString(row.valBox.getValue()));
                    } catch (Exception e) {
                        newConds.addProperty(row.keyBox.getValue(), row.valBox.getValue());
                    }
                }
            }
            entry.conditionsJson = new Gson().toJson(newConds);
        }
    }

    private JsonObject getSafelyParsedJson(String jsonStr) {
        try {
            return JsonParser.parseString(jsonStr).getAsJsonObject();
        } catch (Exception e) {
            return new JsonObject();
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        gfx.drawString(font, "Criteria " + (selectedCriterion + 1) + "/" + criteriaList.size(), startX, startY - 11, 0xFFA08060, false);
        gfx.drawString(font, "Trigger", startX, startY + 34, 0xFFA08060, false);
        gfx.drawString(font, "Conditions", startX, startY + 79, 0xFFA08060, false);
    }

    @Override
    public List<GuiEventListener> getWidgets() {
        return widgets;
    }

    private static class CriterionEntry {
        String name = "my_criterion";
        String trigger = "minecraft:inventory_changed";
        String conditionsJson = "{}";
    }

    private static class ConditionRow {
        SuggestingEditBox keyBox;
        EditBox valBox;
    }
}