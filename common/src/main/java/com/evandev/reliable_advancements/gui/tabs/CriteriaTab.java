package com.evandev.reliable_advancements.gui.tabs;

import com.evandev.reliable_advancements.gui.model.AdvancementDraft;
import com.evandev.reliable_advancements.gui.widgets.SuggestingEditBox;
import com.evandev.reliable_advancements.util.TriggerSchemaManager;
import com.google.gson.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CriteriaTab implements IEditorTab {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Font font;
    private final List<GuiEventListener> widgets = new ArrayList<>();
    private final List<CriterionEntry> criteriaList = new ArrayList<>();
    private final List<ConditionRow> conditionRows = new ArrayList<>();
    private int selectedCriterion = 0;
    private int startX, startY, width;
    private EditBox nameBox;
    private SuggestingEditBox triggerBox;
    private Button addConditionBtn;

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
                    if (critObj.has("conditions")) {
                        JsonObject conds = critObj.getAsJsonObject("conditions");
                        for (Map.Entry<String, JsonElement> cond : conds.entrySet()) {
                            String valStr;

                            if (cond.getValue().isJsonPrimitive() && cond.getValue().getAsJsonPrimitive().isString()) {
                                valStr = "\"" + cond.getValue().getAsString() + "\"";
                            } else if (cond.getValue().isJsonPrimitive()) {
                                valStr = cond.getValue().getAsString();
                            } else {
                                valStr = GSON.toJson(cond.getValue());
                            }

                            entry.conditions.add(new ConditionData(cond.getKey(), valStr));
                        }
                    }
                    criteriaList.add(entry);
                }
            }
        } catch (Exception ignored) {
        }

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
        nameBox.setResponder(s -> active.name = s);
        widgets.add(nameBox);
        widgets.add(Button.builder(Component.literal(">"), b -> switchCriterion(1, reinitScreen)).pos(x + width - 75, y).size(20, 20).build());
        widgets.add(Button.builder(Component.literal("+"), b -> addCriterion(reinitScreen)).pos(x + width - 50, y).size(20, 20).build());
        widgets.add(Button.builder(Component.literal("-"), b -> removeCriterion(reinitScreen)).pos(x + width - 25, y).size(20, 20).build());

        triggerBox = new SuggestingEditBox(font, x, y + 45, width, 20, Component.literal("Trigger"),
                () -> BuiltInRegistries.TRIGGER_TYPES.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList()));
        triggerBox.setMaxLength(256);
        triggerBox.setValue(active.trigger);
        triggerBox.setResponder(s -> active.trigger = s);
        widgets.add(triggerBox);

        int currentY = y + 90;

        for (int i = 0; i < active.conditions.size(); i++) {
            ConditionData data = active.conditions.get(i);
            ConditionRow row = new ConditionRow(data, x, currentY, width, active.trigger, () -> {
                syncActiveConditions();
                active.conditions.remove(data);
                reinitScreen.run();
            });
            conditionRows.add(row);
            widgets.addAll(row.getWidgets());
            currentY += 155;
        }

        addConditionBtn = Button.builder(Component.literal("+ Add Condition"), b -> {
            syncActiveConditions();
            active.conditions.add(new ConditionData("", ""));
            reinitScreen.run();
        }).pos(x, currentY).size(120, 20).build();
        widgets.add(addConditionBtn);
    }

    @Override
    public void saveState(AdvancementDraft draft) {
        syncActiveConditions();

        JsonObject criteriaObj = new JsonObject();
        JsonArray requirementsObj = new JsonArray();
        for (CriterionEntry entry : criteriaList) {
            JsonObject crit = new JsonObject();
            crit.addProperty("trigger", entry.trigger);

            JsonObject conds = new JsonObject();
            for (ConditionData data : entry.conditions) {
                if (data.key.isEmpty()) continue;
                conds.add(data.key, parseValue(data.value));
            }

            if (!conds.isEmpty()) {
                crit.add("conditions", conds);
            }

            criteriaObj.add(entry.name, crit);
            JsonArray req = new JsonArray();
            req.add(entry.name);
            requirementsObj.add(req);
        }

        draft.rootJson.add("criteria", criteriaObj);
        draft.rootJson.add("requirements", requirementsObj);
    }

    private JsonElement parseValue(String val) {
        String trimmed = val.trim();
        try {
            return JsonParser.parseString(trimmed);
        } catch (Exception e) {
            return new JsonPrimitive(trimmed);
        }
    }

    private void syncActiveConditions() {
        if (criteriaList.isEmpty()) return;
        CriterionEntry active = criteriaList.get(selectedCriterion);
        for (int i = 0; i < conditionRows.size(); i++) {
            if (i < active.conditions.size()) {
                ConditionRow row = conditionRows.get(i);
                active.conditions.get(i).key = row.keyBox.getValue();
                active.conditions.get(i).value = row.valBox.getValue();
            }
        }
    }

    private void switchCriterion(int dir, Runnable reinitScreen) {
        syncActiveConditions();
        selectedCriterion = (selectedCriterion + dir + criteriaList.size()) % criteriaList.size();
        reinitScreen.run();
    }

    private void addCriterion(Runnable reinitScreen) {
        syncActiveConditions();
        CriterionEntry newEntry = new CriterionEntry();
        newEntry.name = "new_criterion_" + criteriaList.size();
        criteriaList.add(newEntry);
        selectedCriterion = criteriaList.size() - 1;
        reinitScreen.run();
    }

    private void removeCriterion(Runnable reinitScreen) {
        syncActiveConditions();
        if (criteriaList.size() > 1) {
            criteriaList.remove(selectedCriterion);
            selectedCriterion = Math.max(0, selectedCriterion - 1);
        }
        reinitScreen.run();
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        gfx.drawString(font, "Criteria " + (selectedCriterion + 1) + "/" + criteriaList.size(), startX, startY - 11, 0xFFA08060, false);
        gfx.drawString(font, "Trigger", startX, startY + 34, 0xFFA08060, false);
        gfx.drawString(font, "Conditions", startX, startY + 79, 0xFF55FF55, false);
    }

    @Override
    public List<GuiEventListener> getWidgets() {
        return widgets;
    }

    private static class CriterionEntry {
        String name = "my_criterion";
        String trigger = "minecraft:inventory_changed";
        List<ConditionData> conditions = new ArrayList<>();
    }

    private static class ConditionData {
        String key;
        String value;

        ConditionData(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    private static class ConditionRow {
        SuggestingEditBox keyBox;
        JsonMultiLineEditBox valBox;
        Button removeBtn;

        ConditionRow(ConditionData data, int x, int y, int width, String trigger, Runnable onRemove) {
            keyBox = new SuggestingEditBox(Minecraft.getInstance().font, x, y, width - 25, 20, Component.literal("Key"), () -> {
                String parentType = TriggerSchemaManager.getRootType(trigger);
                return TriggerSchemaManager.getFields(parentType);
            });
            keyBox.setValue(data.key);

            valBox = new JsonMultiLineEditBox(Minecraft.getInstance().font, x, y + 25, width, 120, Component.literal("Value"));
            valBox.setValue(data.value);

            removeBtn = Button.builder(Component.literal("X"), b -> onRemove.run())
                    .pos(x + width - 20, y).size(20, 20).build();
        }

        List<GuiEventListener> getWidgets() {
            return List.of(keyBox, valBox, removeBtn);
        }
    }

    private static class JsonMultiLineEditBox extends MultiLineEditBox {
        private String lastText = null;
        private boolean isValid = true;

        public JsonMultiLineEditBox(Font font, int x, int y, int width, int height, Component title) {
            super(font, x, y, width, height, title, Component.empty());
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
            String currentText = this.getValue();
            if (lastText == null || !lastText.equals(currentText)) {
                lastText = currentText;
                validate(currentText);
            }

            super.renderWidget(gfx, mouseX, mouseY, partialTicks);

            int outlineColor = isValid ? 0xFF00FF00 : 0xFFFF0000;
            gfx.renderOutline(getX() - 1, getY() - 1, getWidth() + 2, getHeight() + 2, outlineColor);
        }

        private void validate(String text) {
            String trimmed = text.trim();
            if (trimmed.isEmpty()) {
                isValid = true;
                this.setTooltip(null);
                return;
            }
            try {
                JsonParser.parseString(trimmed);
                isValid = true;
                this.setTooltip(null);
            } catch (JsonSyntaxException e) {
                isValid = false;
                String errorMsg = e.getMessage();
                this.setTooltip(Tooltip.create(Component.literal("Invalid JSON: " + errorMsg).withStyle(ChatFormatting.RED)));
            }
        }
    }
}