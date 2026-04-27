package com.evandev.better_advancements.gui.tabs;

import com.evandev.better_advancements.gui.model.AdvancementDraft;
import com.evandev.better_advancements.gui.widgets.SuggestingEditBox;
import com.evandev.better_advancements.util.TriggerSchemaManager;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
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
    private int selectedCriterion = 0;
    private int startX, startY, width;
    private EditBox nameBox;
    private SuggestingEditBox triggerBox;
    private JsonObjectNode rootConditionsNode;

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
                        entry.conditionsJson = new Gson().toJson(critObj.getAsJsonObject("conditions"));
                    }
                    criteriaList.add(entry);
                }
            }
        } catch (Exception ignored) {
        }

        if (criteriaList.isEmpty()) criteriaList.add(new CriterionEntry());
        selectedCriterion = Math.max(0, Math.min(selectedCriterion, criteriaList.size() - 1));
        rootConditionsNode = null;
    }

    @Override
    public void init(int x, int y, int width, int height, Runnable reinitScreen) {
        this.widgets.clear();
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

        if (rootConditionsNode == null) {
            rootConditionsNode = new JsonObjectNode("conditions", null, reinitScreen) {
                @Override
                public String getExpectedType() {
                    return TriggerSchemaManager.getRootType(active.trigger);
                }
            };
            try {
                JsonObject condObj = JsonParser.parseString(active.conditionsJson).getAsJsonObject();
                rootConditionsNode.fromJson(condObj, reinitScreen);
            } catch (Exception ignored) {
            }
        }

        int currentY = y + 90;
        rootConditionsNode.init(x, currentY, width);
        widgets.addAll(rootConditionsNode.getWidgets());
    }

    @Override
    public void saveState(AdvancementDraft draft) {
        if (rootConditionsNode != null && !criteriaList.isEmpty()) {
            CriterionEntry entry = criteriaList.get(selectedCriterion);
            JsonElement serializedConditions = rootConditionsNode.toJson();
            entry.conditionsJson = serializedConditions != null ? new Gson().toJson(serializedConditions) : "{}";
        }

        JsonObject criteriaObj = new JsonObject();
        JsonArray requirementsObj = new JsonArray();
        for (CriterionEntry entry : criteriaList) {
            JsonObject crit = new JsonObject();
            crit.addProperty("trigger", entry.trigger);
            try {
                JsonObject parsedConds = JsonParser.parseString(entry.conditionsJson).getAsJsonObject();
                if (!parsedConds.isEmpty()) crit.add("conditions", parsedConds);
            } catch (Exception ignored) {
            }

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
        rootConditionsNode = null;
        reinitScreen.run();
    }

    private void addCriterion(Runnable reinitScreen) {
        saveLocal();
        CriterionEntry newEntry = new CriterionEntry();
        newEntry.name = "new_criterion_" + criteriaList.size();
        criteriaList.add(newEntry);
        selectedCriterion = criteriaList.size() - 1;
        rootConditionsNode = null;
        reinitScreen.run();
    }

    private void removeCriterion(Runnable reinitScreen) {
        saveLocal();
        if (criteriaList.size() > 1) {
            criteriaList.remove(selectedCriterion);
            selectedCriterion = Math.max(0, selectedCriterion - 1);
        }
        rootConditionsNode = null;
        reinitScreen.run();
    }

    private void saveLocal() {
        if (rootConditionsNode != null) {
            CriterionEntry entry = criteriaList.get(selectedCriterion);
            JsonElement serializedConditions = rootConditionsNode.toJson();
            entry.conditionsJson = serializedConditions != null ? new Gson().toJson(serializedConditions) : "{}";
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        gfx.drawString(font, "Criteria " + (selectedCriterion + 1) + "/" + criteriaList.size(), startX, startY - 11, 0xFFA08060, false);
        gfx.drawString(font, "Trigger", startX, startY + 34, 0xFFA08060, false);
        gfx.drawString(font, "Conditions", startX, startY + 79, 0xFF55FF55, false);

        if (rootConditionsNode != null) {
            rootConditionsNode.render(gfx, mouseX, mouseY, partialTicks);
        }
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

    abstract static class JsonNode {
        public String key;
        public JsonNode parent;
        public Runnable reinit;

        public SuggestingEditBox keyBox;
        public Button removeBtn;

        public int x, y, width;

        public JsonNode(String key, JsonNode parent, Runnable reinit) {
            this.key = key;
            this.parent = parent;
            this.reinit = reinit;
        }

        public String getExpectedType() {
            if (parent instanceof JsonObjectNode objParent) {
                return TriggerSchemaManager.getFieldType(objParent.getExpectedType(), this.key);
            } else if (parent instanceof JsonArrayNode arrParent) {
                return TriggerSchemaManager.getListInnerType(arrParent.getExpectedType());
            }
            return "string";
        }

        public abstract void init(int x, int y, int width);

        public abstract int getHeight();

        public abstract void render(GuiGraphics gfx, int mouseX, int mouseY, float pt);

        public abstract List<GuiEventListener> getWidgets();

        public abstract JsonElement toJson();

        public abstract void fromJson(JsonElement el, Runnable reinit);

        protected void initBase(int x, int y, int width) {
            this.x = x;
            this.y = y;
            this.width = width;
            keyBox = new SuggestingEditBox(Minecraft.getInstance().font, x, y, 100, 20, Component.literal("Key"), () -> {
                if (parent instanceof JsonObjectNode objParent) {
                    return TriggerSchemaManager.getFields(objParent.getExpectedType());
                }
                return List.of();
            });
            keyBox.setValue(key);
            keyBox.setResponder(this::onKeyChanged);

            if (this instanceof JsonPrimitiveNode) {
                removeBtn = Button.builder(Component.literal("X"), b -> {
                    if (parent instanceof JsonObjectNode obj) obj.children.remove(this);
                    if (parent instanceof JsonArrayNode arr) arr.children.remove(this);
                    reinit.run();
                }).pos(x + width - 20, y).size(20, 20).build();
            }
        }

        private void onKeyChanged(String s) {
            this.key = s;
            if (parent == null) return;

            String expectedType = getExpectedType();
            boolean isObj = TriggerSchemaManager.isObject(expectedType);
            boolean isList = TriggerSchemaManager.isList(expectedType);

            if (isObj && !(this instanceof JsonObjectNode)) {
                replaceSelf(new JsonObjectNode(s, parent, reinit));
            } else if (isList && !(this instanceof JsonArrayNode)) {
                replaceSelf(new JsonArrayNode(s, parent, reinit));
            } else if (!isObj && !isList && !(this instanceof JsonPrimitiveNode)) {
                replaceSelf(new JsonPrimitiveNode(s, "", parent, reinit));
            }
        }

        protected void replaceSelf(JsonNode newNode) {
            if (parent instanceof JsonObjectNode obj) {
                int idx = obj.children.indexOf(this);
                if (idx >= 0) obj.children.set(idx, newNode);
            } else if (parent instanceof JsonArrayNode arr) {
                int idx = arr.children.indexOf(this);
                if (idx >= 0) arr.children.set(idx, newNode);
            }
            reinit.run();
        }

        protected List<GuiEventListener> getBaseWidgets() {
            List<GuiEventListener> list = new ArrayList<>();
            if (parent != null) {
                if (!(parent instanceof JsonArrayNode)) list.add(keyBox);
                if (removeBtn != null) list.add(removeBtn);
            }
            return list;
        }
    }

    static class JsonPrimitiveNode extends JsonNode {
        public SuggestingEditBox valBox;
        public String valueStr;

        public JsonPrimitiveNode(String key, String valueStr, JsonNode parent, Runnable reinit) {
            super(key, parent, reinit);
            this.valueStr = valueStr;
        }

        @Override
        public void init(int x, int y, int width) {
            initBase(x, y, width);
            int valX = (parent instanceof JsonArrayNode) ? x : x + 105;
            int valW = (parent instanceof JsonArrayNode) ? width - 25 : width - 130;

            valBox = new SuggestingEditBox(Minecraft.getInstance().font, valX, y, valW, 20, Component.literal("Value"), () -> {
                String type = getExpectedType();
                String effectiveKey = this.key.toLowerCase();

                if (effectiveKey.isEmpty() && parent instanceof JsonArrayNode arr) {
                    effectiveKey = arr.key.toLowerCase();
                }

                if (type.equals("boolean")) return List.of("true", "false");

                if (effectiveKey.equals("dimension") || effectiveKey.equals("to") || effectiveKey.equals("from")) {
                    return List.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end");
                }
                if (effectiveKey.contains("potion")) {
                    return BuiltInRegistries.POTION.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
                }
                if (effectiveKey.contains("entity") || effectiveKey.contains("vehicle") || type.contains("Entity") || type.contains("Damage")) {
                    return BuiltInRegistries.ENTITY_TYPE.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
                }
                if (effectiveKey.contains("block") || effectiveKey.equals("state")) {
                    return BuiltInRegistries.BLOCK.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
                }
                if (effectiveKey.contains("fluid")) {
                    return BuiltInRegistries.FLUID.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
                }

                if (effectiveKey.contains("item") || type.equals("resource_location") || type.contains("Item")) {
                    return BuiltInRegistries.ITEM.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList());
                }

                return List.of();
            });
            valBox.setMaxLength(2048);
            valBox.setValue(valueStr);

            valBox.setResponder(s -> {
                this.valueStr = s;
                String trimmed = s.trim();
                if (trimmed.equals("[")) {
                    replaceSelf(new JsonArrayNode(this.key, parent, reinit));
                } else if (trimmed.equals("{")) {
                    replaceSelf(new JsonObjectNode(this.key, parent, reinit));
                }
            });
        }

        @Override
        public int getHeight() {
            return 25;
        }

        @Override
        public void render(GuiGraphics gfx, int mouseX, int mouseY, float pt) {
        }

        @Override
        public List<GuiEventListener> getWidgets() {
            List<GuiEventListener> list = getBaseWidgets();
            list.add(valBox);
            return list;
        }

        @Override
        public JsonElement toJson() {
            if (valueStr == null || valueStr.trim().isEmpty()) return null;

            String type = getExpectedType();
            switch (type) {
                case "boolean" -> {
                    return new JsonPrimitive(Boolean.parseBoolean(valueStr));
                }
                case "int", "integer" -> {
                    try {
                        return new JsonPrimitive(Integer.parseInt(valueStr));
                    } catch (Exception e) {
                        return new JsonPrimitive(0);
                    }
                }
                case "double", "float" -> {
                    try {
                        return new JsonPrimitive(Double.parseDouble(valueStr));
                    } catch (Exception e) {
                        return new JsonPrimitive(0.0);
                    }
                }
            }
            return new JsonPrimitive(valueStr);
        }

        @Override
        public void fromJson(JsonElement el, Runnable reinit) {
            if (el.isJsonPrimitive()) {
                this.valueStr = el.getAsString();
            } else {
                this.valueStr = el.toString();
            }
        }
    }

    static class JsonObjectNode extends JsonNode {
        public List<JsonNode> children = new ArrayList<>();
        public Button addBtn;

        public JsonObjectNode(String key, JsonNode parent, Runnable reinit) {
            super(key, parent, reinit);
        }

        @Override
        public void init(int x, int y, int width) {
            initBase(x, y, width);
            int currentY = y + (parent == null ? 0 : 25);
            int childX = (parent == null) ? x : x + 10;
            int childW = (parent == null) ? width : width - 10;

            for (JsonNode child : children) {
                child.init(childX, currentY, childW);
                currentY += child.getHeight();
            }
            addBtn = Button.builder(Component.literal("+ Add Field"), b -> {
                children.add(new JsonPrimitiveNode("", "", this, reinit));
                reinit.run();
            }).pos(childX, currentY).size(80, 20).build();
        }

        @Override
        public int getHeight() {
            int h = (parent == null ? 0 : 25) + 25;
            for (JsonNode child : children) h += child.getHeight();
            return h;
        }

        @Override
        public void render(GuiGraphics gfx, int mouseX, int mouseY, float pt) {
            if (parent != null) {
                gfx.fill(x + 2, y + 22, x + 3, y + getHeight() - 5, 0xFF666666);
            }
            for (JsonNode child : children) child.render(gfx, mouseX, mouseY, pt);
        }

        @Override
        public List<GuiEventListener> getWidgets() {
            List<GuiEventListener> list = getBaseWidgets();
            for (JsonNode child : children) list.addAll(child.getWidgets());
            list.add(addBtn);
            return list;
        }

        @Override
        public JsonElement toJson() {
            JsonObject obj = new JsonObject();
            for (JsonNode child : children) {
                if (!child.key.isEmpty()) {
                    JsonElement childJson = child.toJson();
                    if (childJson != null) obj.add(child.key, childJson);
                }
            }
            if (obj.isEmpty() && parent != null) return null;
            return obj;
        }

        @Override
        public void fromJson(JsonElement el, Runnable reinit) {
            if (el.isJsonObject()) {
                JsonObject obj = el.getAsJsonObject();
                for (String k : obj.keySet()) {
                    JsonElement val = obj.get(k);
                    JsonNode child;

                    if (val.isJsonObject()) child = new JsonObjectNode(k, this, reinit);
                    else if (val.isJsonArray()) child = new JsonArrayNode(k, this, reinit);
                    else child = new JsonPrimitiveNode(k, "", this, reinit);

                    child.fromJson(val, reinit);
                    children.add(child);
                }
            }
        }
    }

    static class JsonArrayNode extends JsonNode {
        public List<JsonNode> children = new ArrayList<>();
        public Button addBtn;

        public JsonArrayNode(String key, JsonNode parent, Runnable reinit) {
            super(key, parent, reinit);
        }

        @Override
        public void init(int x, int y, int width) {
            initBase(x, y, width);
            int currentY = y + 25;
            for (JsonNode child : children) {
                child.init(x + 10, currentY, width - 10);
                currentY += child.getHeight();
            }
            addBtn = Button.builder(Component.literal("+ Add Item"), b -> {
                children.add(new JsonPrimitiveNode("", "", this, reinit));
                reinit.run();
            }).pos(x + 10, currentY).size(80, 20).build();
        }

        @Override
        public int getHeight() {
            int h = 50;
            for (JsonNode child : children) h += child.getHeight();
            return h;
        }

        @Override
        public void render(GuiGraphics gfx, int mouseX, int mouseY, float pt) {
            if (parent != null) {
                gfx.fill(x + 2, y + 22, x + 3, y + getHeight() - 5, 0xFF448844);
            }
            for (JsonNode child : children) child.render(gfx, mouseX, mouseY, pt);
        }

        @Override
        public List<GuiEventListener> getWidgets() {
            List<GuiEventListener> list = getBaseWidgets();
            for (JsonNode child : children) list.addAll(child.getWidgets());
            list.add(addBtn);
            return list;
        }

        @Override
        public JsonElement toJson() {
            JsonArray arr = new JsonArray();
            for (JsonNode child : children) {
                JsonElement childJson = child.toJson();
                if (childJson != null) arr.add(childJson);
            }
            if (arr.isEmpty() && parent != null) return null;
            return arr;
        }

        @Override
        public void fromJson(JsonElement el, Runnable reinit) {
            if (el.isJsonArray()) {
                JsonArray arr = el.getAsJsonArray();
                for (JsonElement val : arr) {
                    JsonNode child;

                    if (val.isJsonObject()) child = new JsonObjectNode("", this, reinit);
                    else if (val.isJsonArray()) child = new JsonArrayNode("", this, reinit);
                    else child = new JsonPrimitiveNode("", "", this, reinit);

                    child.fromJson(val, reinit);
                    children.add(child);
                }
            }
        }
    }
}