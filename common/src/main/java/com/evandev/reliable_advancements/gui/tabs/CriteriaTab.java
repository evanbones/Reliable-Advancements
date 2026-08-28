package com.evandev.reliable_advancements.gui.tabs;

import com.evandev.reliable_advancements.gui.model.AdvancementDraft;
import com.evandev.reliable_advancements.gui.theme.EditorTheme;
import com.evandev.reliable_advancements.gui.widgets.*;
import com.evandev.reliable_advancements.gui.widgets.schema.SchemaFormNode;
import com.evandev.reliable_advancements.gui.widgets.schema.SchemaWidgetFactory;
import com.evandev.reliable_advancements.util.TriggerSchemaManager;
import com.google.gson.*;
import net.mehvahdjukaar.codecui.Schema;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CriteriaTab implements IEditorTab {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Font font;
    private final EditorForm form;
    private final List<CriterionEntry> criteriaList = new ArrayList<>();

    private int selectedCriterion = 0;
    private TriggerEditBox triggerBox;
    private JsonElement loadedRequirements;
    private boolean isInitializing = false;

    public CriteriaTab(Font font) {
        this.font = font;
        this.form = new EditorForm(font);
    }

    private static boolean coversExactly(JsonElement requirements, List<String> names) {
        if (requirements == null || !requirements.isJsonArray()) return false;
        Set<String> referenced = new HashSet<>();
        for (JsonElement group : requirements.getAsJsonArray()) {
            if (!group.isJsonArray()) return false;
            for (JsonElement name : group.getAsJsonArray()) {
                if (!name.isJsonPrimitive()) return false;
                referenced.add(name.getAsString());
            }
        }
        return referenced.equals(new HashSet<>(names));
    }

    @Override
    public void loadState(AdvancementDraft draft) {
        criteriaList.clear();
        loadedRequirements = draft.rootJson.has("requirements") ? draft.rootJson.get("requirements").deepCopy() : null;

        if (draft.rootJson.has("criteria")) {
            JsonObject criteriaObj = draft.rootJson.getAsJsonObject("criteria");
            for (String key : criteriaObj.keySet()) {
                try {
                    CriterionEntry entry = new CriterionEntry();
                    entry.name = key;
                    JsonObject critObj = criteriaObj.getAsJsonObject(key);
                    if (critObj.has("trigger")) {
                        entry.trigger = critObj.get("trigger").getAsString();
                        entry.lastValidTrigger = entry.trigger;
                    }
                    if (critObj.has("conditions")) {
                        entry.conditionsJson = critObj.getAsJsonObject("conditions").deepCopy();
                        entry.rawJsonText = GSON.toJson(entry.conditionsJson);
                    }
                    criteriaList.add(entry);
                } catch (Exception ignored) {
                }
            }
        }

        if (criteriaList.isEmpty()) criteriaList.add(new CriterionEntry());
        selectedCriterion = Math.max(0, Math.min(selectedCriterion, criteriaList.size() - 1));
    }

    @Override
    public void init(int x, int y, int width, int height, Runnable reinitScreen) {
        if (isInitializing) return;
        isInitializing = true;
        try {
            form.clear();

            if (criteriaList.isEmpty()) criteriaList.add(new CriterionEntry());
            selectedCriterion = Math.max(0, Math.min(selectedCriterion, criteriaList.size() - 1));
            CriterionEntry active = criteriaList.get(selectedCriterion);

            if (active.schemaNode == null) {
                Schema<?> triggerSchema = TriggerSchemaManager.getSchema(active.trigger);
                if (triggerSchema == null) {
                    triggerSchema = new Schema.Record<>(Object.class, List.of());
                }
                active.schemaNode = SchemaWidgetFactory.createNode(triggerSchema, font, () -> {
                    if (!isInitializing) {
                        syncActiveConditions(active);
                        reinitScreen.run();
                    }
                });
                if (active.conditionsJson != null) {
                    active.schemaNode.setJson(active.conditionsJson);
                }
            }

            CriterionNavBarRow navBarRow = new CriterionNavBarRow(selectedCriterion, criteriaList.size(), font,
                    () -> switchCriterion(-1, reinitScreen),
                    () -> switchCriterion(1, reinitScreen),
                    () -> addCriterion(reinitScreen),
                    () -> removeCriterion(reinitScreen),
                    active.name,
                    s -> active.name = s);
            form.addCustomRow(navBarRow, navBarRow.getWidgets());

            boolean wasTriggerFocused = triggerBox != null && triggerBox.isFocused();
            int savedCursorPos = triggerBox != null ? triggerBox.getCursorPosition() : -1;

            triggerBox = new TriggerEditBox(font, 0, 0, 100, 20, Component.literal("Trigger"),
                    () -> BuiltInRegistries.TRIGGER_TYPES.keySet().stream().map(Identifier::toString).collect(Collectors.toList()));
            triggerBox.setMaxLength(256);
            triggerBox.setValue(active.trigger);
            if (wasTriggerFocused) {
                triggerBox.setFocused(true);
                if (savedCursorPos >= 0)
                    triggerBox.setCursorPosition(Math.min(savedCursorPos, active.trigger.length()));
            }
            triggerBox.setResponder(s -> {
                active.trigger = s;
                Identifier id = Identifier.tryParse(s.trim());
                if (id != null && BuiltInRegistries.TRIGGER_TYPES.containsKey(id)) {
                    String validId = id.toString();
                    if (!Objects.equals(validId, active.lastValidTrigger)) {
                        active.lastValidTrigger = validId;
                        Schema<?> newSchema = TriggerSchemaManager.getSchema(s);
                        if (newSchema == null) newSchema = new Schema.Record<>(Object.class, List.of());
                        active.schemaNode = SchemaWidgetFactory.createNode(newSchema, font, () -> {
                            syncActiveConditions(active);
                            reinitScreen.run();
                        });
                        if (active.conditionsJson != null) active.schemaNode.setJson(active.conditionsJson);
                        if (!isInitializing && active.isVisualMode) {
                            reinitScreen.run();
                        }
                    }
                }
            });

            ModernButton modeToggle = ModernButton.modernBuilder(
                            Component.literal(active.isVisualMode ? "Visual Editor" : "{ } Raw JSON"),
                            b -> {
                                syncActiveConditions(active);
                                active.isVisualMode = !active.isVisualMode;
                                if (active.isVisualMode) {
                                    try {
                                        active.conditionsJson = JsonParser.parseString(active.rawJsonText).getAsJsonObject();
                                    } catch (Exception e) {
                                        active.conditionsJson = new JsonObject();
                                    }
                                    if (active.schemaNode != null) active.schemaNode.setJson(active.conditionsJson);
                                } else {
                                    if (active.conditionsJson != null) {
                                        active.rawJsonText = GSON.toJson(active.conditionsJson);
                                    }
                                }
                                reinitScreen.run();
                            })
                    .style(ModernButton.Style.SECONDARY)
                    .pos(0, 0).size(105, 20)
                    .tooltip(Tooltip.create(Component.literal("Toggle between Visual Schema Editor and Raw JSON Editor")))
                    .build();

            TriggerHeaderRow triggerRow = new TriggerHeaderRow(triggerBox, modeToggle);
            form.addCustomRow(triggerRow, List.of(triggerBox, modeToggle));

            if (active.isVisualMode) {
                EditorForm.FormRow schemaRow = active.schemaNode.createFormRow("", font, () -> {
                    syncActiveConditions(active);
                    reinitScreen.run();
                });
                form.addCustomRow(schemaRow, active.schemaNode.getWidgets());
            } else {
                JsonEditorWidget rawBox = new JsonEditorWidget(font, 0, 0, 100, 130, Component.literal("Conditions JSON"));
                rawBox.setValue(active.rawJsonText != null ? active.rawJsonText : "{}");
                rawBox.setResponder(s -> {
                    active.rawJsonText = s;
                    try {
                        active.conditionsJson = JsonParser.parseString(s).getAsJsonObject();
                    } catch (Exception ignored) {
                    }
                    form.updateWidgetPositions();
                });

                ModernButton copyBtn = ModernButton.modernBuilder(Component.literal("Copy"), b -> {
                    Minecraft.getInstance().keyboardHandler.setClipboard(rawBox.getValue());
                }).style(ModernButton.Style.SECONDARY).pos(0, 0).size(38, 18).tooltip(Tooltip.create(Component.literal("Copy conditions JSON to clipboard"))).build();

                ModernButton formatBtn = ModernButton.modernBuilder(Component.literal("Format"), b -> {
                    try {
                        JsonElement el = JsonParser.parseString(rawBox.getValue());
                        String formatted = GSON.toJson(el);
                        rawBox.setValue(formatted);
                        active.rawJsonText = formatted;
                        if (el.isJsonObject()) active.conditionsJson = el.getAsJsonObject();
                        form.updateWidgetPositions();
                    } catch (Exception ignored) {
                    }
                }).style(ModernButton.Style.SECONDARY).pos(0, 0).size(48, 18).tooltip(Tooltip.create(Component.literal("Format and indent JSON"))).build();

                ModernButton templateBtn = ModernButton.modernBuilder(Component.literal("Template"), b -> {
                    Schema<?> s = TriggerSchemaManager.getSchema(active.trigger);
                    if (s != null) {
                        String tpl = TriggerSchemaManager.generateTemplateJson(s, 0);
                        rawBox.setValue(tpl);
                        active.rawJsonText = tpl;
                        try {
                            active.conditionsJson = JsonParser.parseString(tpl).getAsJsonObject();
                        } catch (Exception ignored) {
                        }
                        form.updateWidgetPositions();
                    }
                }).style(ModernButton.Style.SECONDARY).pos(0, 0).size(58, 18).tooltip(Tooltip.create(Component.literal("Generate template JSON from trigger schema"))).build();

                RawJsonRow rawRow = new RawJsonRow(rawBox, copyBtn, formatBtn, templateBtn);
                form.addCustomRow(rawRow, List.of(rawBox, copyBtn, formatBtn, templateBtn));
            }

            form.init(x, y, width, height);
        } finally {
            isInitializing = false;
        }
    }

    @Override
    public void syncFromWidgets() {
        if (!criteriaList.isEmpty() && selectedCriterion < criteriaList.size()) {
            syncActiveConditions(criteriaList.get(selectedCriterion));
        }
    }

    private void syncActiveConditions(CriterionEntry active) {
        if (active.isVisualMode && active.schemaNode != null) {
            JsonElement current = active.schemaNode.currentJson();
            if (current != null && current.isJsonObject()) {
                active.conditionsJson = current.getAsJsonObject();
            } else {
                active.conditionsJson = null;
            }
            active.rawJsonText = active.conditionsJson != null ? GSON.toJson(active.conditionsJson) : "{}";
        } else if (!active.isVisualMode) {
            try {
                active.conditionsJson = JsonParser.parseString(active.rawJsonText).getAsJsonObject();
            } catch (Exception ignored) {
            }
        }
    }

    private void switchCriterion(int direction, Runnable reinit) {
        syncFromWidgets();
        selectedCriterion = Math.max(0, Math.min(selectedCriterion + direction, criteriaList.size() - 1));
        reinit.run();
    }

    private void addCriterion(Runnable reinit) {
        syncFromWidgets();
        CriterionEntry entry = new CriterionEntry();
        entry.name = "criterion_" + (criteriaList.size() + 1);
        criteriaList.add(entry);
        selectedCriterion = criteriaList.size() - 1;
        reinit.run();
    }

    private void removeCriterion(Runnable reinit) {
        if (criteriaList.size() <= 1) return;
        syncFromWidgets();
        criteriaList.remove(selectedCriterion);
        selectedCriterion = Math.max(0, Math.min(selectedCriterion, criteriaList.size() - 1));
        reinit.run();
    }

    @Override
    public void saveState(AdvancementDraft draft) {
        syncFromWidgets();
        JsonObject criteriaObj = new JsonObject();
        for (CriterionEntry c : criteriaList) {
            JsonObject critObj = new JsonObject();
            critObj.addProperty("trigger", c.trigger);
            if (c.conditionsJson != null && !c.conditionsJson.isEmpty()) {
                critObj.add("conditions", c.conditionsJson);
            }
            criteriaObj.add(c.name, critObj);
        }
        draft.rootJson.add("criteria", criteriaObj);

        List<String> names = criteriaList.stream().map(c -> c.name).toList();
        if (coversExactly(loadedRequirements, names)) {
            draft.rootJson.add("requirements", loadedRequirements);
        } else {
            JsonArray reqArray = new JsonArray();
            for (String name : names) {
                JsonArray group = new JsonArray();
                group.add(name);
                reqArray.add(group);
            }
            draft.rootJson.add("requirements", reqArray);
        }
    }

    @Override
    public List<GuiEventListener> getWidgets() {
        return form.getWidgets();
    }

    @Override
    public void render(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTicks) {
        form.render(gfx, mouseX, mouseY, partialTicks);
    }

    @Override
    public EditorForm getForm() {
        return form;
    }

    private static class CriterionEntry {
        String name = "my_criterion";
        String trigger = "minecraft:inventory_changed";
        String lastValidTrigger = "minecraft:inventory_changed";
        JsonObject conditionsJson = new JsonObject();
        String rawJsonText = "{}";
        boolean isVisualMode = true;
        @Nullable SchemaFormNode schemaNode;
    }

    private static class TriggerHeaderRow implements EditorForm.FormRow {
        private final TriggerEditBox triggerBox;
        private final ModernButton modeToggle;
        private int x, y;

        TriggerHeaderRow(TriggerEditBox triggerBox, ModernButton modeToggle) {
            this.triggerBox = triggerBox;
            this.modeToggle = modeToggle;
        }

        @Override
        public void layout(int x, int y, int width, Font font) {
            this.x = x;
            this.y = y;
            int modeW = 105;
            modeToggle.setX(x + width - modeW);
            modeToggle.setY(y);
            modeToggle.setWidth(modeW);
            modeToggle.setHeight(20);

            int labelW = font.width("Trigger:") + 6;
            int boxX = x + labelW;
            int boxW = Math.max(80, width - labelW - modeW - 8);
            triggerBox.setX(boxX);
            triggerBox.setY(y);
            triggerBox.setWidth(boxW);
            triggerBox.setHeight(20);
        }

        @Override
        public void render(GuiGraphicsExtractor gfx, Font font, int mouseX, int mouseY, float partialTicks) {
            gfx.text(font, "Trigger:", x, y + 6, EditorTheme.TEXT_LABEL, false);
            triggerBox.extractRenderState(gfx, mouseX, mouseY, partialTicks);
            modeToggle.extractRenderState(gfx, mouseX, mouseY, partialTicks);
        }

        @Override
        public int getHeight() {
            return 22;
        }

        @Override
        public int getY() {
            return y;
        }
    }

    private static class RawJsonRow implements EditorForm.FormRow {
        private final JsonEditorWidget rawBox;
        private final ModernButton copyBtn;
        private final ModernButton formatBtn;
        private final ModernButton templateBtn;
        private Font font;
        private int x, y, width;

        RawJsonRow(JsonEditorWidget rawBox, ModernButton copyBtn, ModernButton formatBtn, ModernButton templateBtn) {
            this.rawBox = rawBox;
            this.copyBtn = copyBtn;
            this.formatBtn = formatBtn;
            this.templateBtn = templateBtn;
        }

        private List<FormattedCharSequence> getErrorLines(Font font, int maxWidth) {
            String err = rawBox.getSyntaxError();
            if (err == null || font == null) return List.of();
            int textW = Math.max(10, maxWidth - 16);
            return font.split(Component.literal(err), textW);
        }

        private int getErrorBoxHeight() {
            String err = rawBox.getSyntaxError();
            if (err == null) return 0;
            int lineCount = (font != null && width > 0)
                    ? Math.max(1, getErrorLines(font, width).size())
                    : 1;
            return lineCount * 10 + 8;
        }

        @Override
        public void layout(int x, int y, int width, Font font) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.font = font;
            templateBtn.setX(x + width - 58);
            templateBtn.setY(y);
            templateBtn.setWidth(58);
            templateBtn.setHeight(18);

            formatBtn.setX(x + width - 110);
            formatBtn.setY(y);
            formatBtn.setWidth(48);
            formatBtn.setHeight(18);

            copyBtn.setX(x + width - 152);
            copyBtn.setY(y);
            copyBtn.setWidth(38);
            copyBtn.setHeight(18);

            rawBox.setX(x);
            rawBox.setY(y + 24);
            rawBox.setWidth(width);
            rawBox.setHeight(130);
        }

        @Override
        public void render(GuiGraphicsExtractor gfx, Font font, int mouseX, int mouseY, float partialTicks) {
            this.font = font;
            gfx.text(font, "Conditions JSON", x, y + 5, EditorTheme.TEXT_LABEL, false);

            String err = rawBox.getSyntaxError();
            boolean valid = err == null;
            String statusText = valid ? "Valid" : "Invalid";
            int badgeBg = valid ? 0x3340C057 : 0x33FA5252;
            int textCol = valid ? EditorTheme.TEXT_GREEN : EditorTheme.TEXT_RED;
            int labelW = font.width("Conditions JSON");
            int badgeX = x + labelW + 6;
            int badgeY = y + 3;
            int badgeW = font.width(statusText) + 8;
            int badgeH = 12;
            EditorTheme.drawBadge(gfx, font, statusText, badgeX, badgeY, badgeBg, textCol);

            copyBtn.extractRenderState(gfx, mouseX, mouseY, partialTicks);
            formatBtn.extractRenderState(gfx, mouseX, mouseY, partialTicks);
            templateBtn.extractRenderState(gfx, mouseX, mouseY, partialTicks);
            rawBox.extractRenderState(gfx, mouseX, mouseY, partialTicks);

            if (err != null) {
                int errY = y + 158;
                List<FormattedCharSequence> errLines = getErrorLines(font, width);
                int errH = Math.max(20, errLines.size() * 10 + 8);
                gfx.fill(x, errY, x + width, errY + errH, 0x33FA5252);
                gfx.outline(x, errY, width, errH, EditorTheme.BORDER_ERROR);

                int textY = errY + 5;
                for (FormattedCharSequence line : errLines) {
                    gfx.text(font, line, x + 8, textY, EditorTheme.TEXT_RED, false);
                    textY += 10;
                }
            }
        }

        @Override
        public void renderOverlay(GuiGraphicsExtractor gfx, Font font, int mouseX, int mouseY, float partialTicks) {
            String err = rawBox.getSyntaxError();
            if (err != null) {
                int labelW = font.width("Conditions JSON");
                int badgeX = x + labelW + 6;
                int badgeY = y + 3;
                int badgeW = font.width("Invalid") + 8;
                int badgeH = 12;
                if (mouseX >= badgeX && mouseX <= badgeX + badgeW && mouseY >= badgeY && mouseY <= badgeY + badgeH) {
                    List<FormattedCharSequence> tooltipLines = font.split(Component.literal(err), 280);
                    gfx.setTooltipForNextFrame(font, tooltipLines, mouseX, mouseY);
                }
            }
        }

        @Override
        public int getHeight() {
            String err = rawBox.getSyntaxError();
            if (err != null) {
                return 158 + getErrorBoxHeight();
            }
            return 158;
        }

        @Override
        public int getY() {
            return y;
        }
    }

    private static class CriterionNavBarRow implements EditorForm.FormRow {
        private final int index, total;
        private final ModernButton prevBtn, nextBtn, addBtn, removeBtn;
        private final EditBox nameBox;
        private int y;

        CriterionNavBarRow(int index, int total, Font font, Runnable onPrev, Runnable onNext, Runnable onAdd, Runnable onRemove, String currentName, Consumer<String> onNameChange) {
            this.index = index;
            this.total = total;

            this.prevBtn = ModernButton.modernBuilder(Component.literal("<"), b -> onPrev.run())
                    .pos(0, 0).size(20, 20)
                    .tooltip(Tooltip.create(Component.literal("Previous Criterion")))
                    .build();
            this.nextBtn = ModernButton.modernBuilder(Component.literal(">"), b -> onNext.run())
                    .pos(0, 0).size(20, 20)
                    .tooltip(Tooltip.create(Component.literal("Next Criterion")))
                    .build();
            this.addBtn = ModernButton.modernBuilder(Component.literal("+"), b -> onAdd.run())
                    .pos(0, 0).size(20, 20)
                    .tooltip(Tooltip.create(Component.literal("Add Criterion")))
                    .build();
            this.removeBtn = ModernButton.modernBuilder(Component.literal("x"), b -> onRemove.run())
                    .style(ModernButton.Style.DANGER).pos(0, 0).size(20, 20)
                    .tooltip(Tooltip.create(Component.literal("Remove Criterion")))
                    .build();

            this.nameBox = new ModernEditBox(font, 0, 0, 100, 20, Component.literal("Criterion Name"));
            this.nameBox.setMaxLength(256);
            this.nameBox.setValue(currentName);
            this.nameBox.setTooltip(Tooltip.create(Component.literal("Criterion Name / ID")));
            this.nameBox.setResponder(onNameChange);
        }

        public List<GuiEventListener> getWidgets() {
            return List.of(prevBtn, nameBox, nextBtn, addBtn, removeBtn);
        }

        @Override
        public void layout(int x, int y, int width, Font font) {
            this.y = y;
            int nameX = x + 24;
            int nameW = width - 110;

            prevBtn.setX(x);
            prevBtn.setY(y + 12);
            nameBox.setX(nameX);
            nameBox.setY(y + 12);
            nameBox.setWidth(nameW);
            nextBtn.setX(nameX + nameW + 4);
            nextBtn.setY(y + 12);
            addBtn.setX(nameX + nameW + 28);
            addBtn.setY(y + 12);
            removeBtn.setX(nameX + nameW + 52);
            removeBtn.setY(y + 12);
        }

        @Override
        public void render(GuiGraphicsExtractor gfx, Font font, int mouseX, int mouseY, float partialTicks) {
            String label = "Criterion " + (index + 1) + " of " + total;
            gfx.text(font, label, nameBox.getX(), y, EditorTheme.TEXT_GOLD, false);
            prevBtn.extractRenderState(gfx, mouseX, mouseY, partialTicks);
            nameBox.extractRenderState(gfx, mouseX, mouseY, partialTicks);
            nextBtn.extractRenderState(gfx, mouseX, mouseY, partialTicks);
            addBtn.extractRenderState(gfx, mouseX, mouseY, partialTicks);
            removeBtn.extractRenderState(gfx, mouseX, mouseY, partialTicks);
        }

        @Override
        public int getHeight() {
            return 36;
        }

        @Override
        public int getY() {
            return y;
        }
    }

    private static class TriggerEditBox extends SuggestingEditBox {
        private String lastText = null;
        private boolean isValid = true;

        TriggerEditBox(Font font, int x, int y, int width, int height, Component title, Supplier<List<String>> suggestions) {
            super(font, x, y, width, height, title, suggestions);
        }

        @Override
        public void extractWidgetRenderState(@NotNull GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTicks) {
            String currentText = this.getValue();
            if (lastText == null || !lastText.equals(currentText)) {
                lastText = currentText;
                validate(currentText);
            }
            super.extractWidgetRenderState(gfx, mouseX, mouseY, partialTicks);

            int outline = isValid ? EditorTheme.BORDER_VALID : EditorTheme.BORDER_ERROR;
            gfx.outline(getX() - 1, getY() - 1, getWidth() + 2, getHeight() + 2, outline);
        }

        private void validate(String text) {
            String trimmed = text.trim();
            Identifier id = Identifier.tryParse(trimmed);
            isValid = id != null && BuiltInRegistries.TRIGGER_TYPES.containsKey(id);
            this.setTooltip(isValid ? null : Tooltip.create(
                    Component.literal("Unknown trigger \"" + trimmed + "\"").withStyle(ChatFormatting.RED)));
        }
    }
}