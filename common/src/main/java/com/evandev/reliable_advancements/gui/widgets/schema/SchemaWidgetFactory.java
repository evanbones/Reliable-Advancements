package com.evandev.reliable_advancements.gui.widgets.schema;

import com.evandev.reliable_advancements.gui.theme.EditorTheme;
import com.evandev.reliable_advancements.gui.widgets.*;
import com.evandev.reliable_advancements.util.TextUtil;
import com.evandev.reliable_advancements.util.TriggerSchemaManager;
import com.google.gson.*;
import net.mehvahdjukaar.codecui.Schema;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class SchemaWidgetFactory {
    private static final int INDENT_STEP = 10;

    private SchemaWidgetFactory() {
    }

    public static boolean isLeafSchema(Schema<?> schema) {
        if (schema == null) return false;
        Schema<?> unwrapped = TriggerSchemaManager.unwrapRef(schema);
        return unwrapped instanceof Schema.Bool
                || unwrapped instanceof Schema.IntRange
                || unwrapped instanceof Schema.LongRange
                || unwrapped instanceof Schema.FloatRange
                || unwrapped instanceof Schema.DoubleRange
                || unwrapped instanceof Schema.Str
                || unwrapped instanceof Schema.ResourceId
                || unwrapped instanceof Schema.TagId
                || unwrapped instanceof Schema.Enum;
    }

    public static SchemaFormNode createNode(Schema<?> schema, Font font, Runnable onFormChanged) {
        return createNode(schema, font, onFormChanged, 0, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    public static SchemaFormNode createNode(Schema<?> schema, Font font, Runnable onFormChanged, int depth, Set<Schema<?>> path) {
        if (schema == null) {
            return new OpaqueNode(font, onFormChanged, depth);
        }

        if (isLeafSchema(schema)) {
            return createDirectNode(schema, font, onFormChanged, depth, path);
        }

        if (schema instanceof Schema.Ref<?>) {
            return new LazyRefNode(schema, font, onFormChanged, depth);
        }

        if (depth > 30 || path.contains(schema)) {
            return new LazyRefNode(schema, font, onFormChanged, depth);
        }

        return createDirectNode(schema, font, onFormChanged, depth, path);
    }

    private static SchemaFormNode createDirectNode(Schema<?> schema, Font font, Runnable onFormChanged, int depth, Set<Schema<?>> path) {
        Schema<?> unwrapped = TriggerSchemaManager.unwrapRef(schema);
        if (unwrapped == null) {
            return new OpaqueNode(font, onFormChanged, depth);
        }

        Set<Schema<?>> nextPath = Collections.newSetFromMap(new IdentityHashMap<>());
        nextPath.addAll(path);
        nextPath.add(schema);
        if (unwrapped != schema) {
            nextPath.add(unwrapped);
        }

        switch (unwrapped) {
            case Schema.Bool ignored -> {
                return new BoolNode(onFormChanged, depth);
            }
            case Schema.IntRange r -> {
                return new NumberNode(NumberType.INT, r.min(), r.max(), font, onFormChanged, depth);
            }
            case Schema.LongRange r -> {
                return new NumberNode(NumberType.LONG, r.min(), r.max(), font, onFormChanged, depth);
            }
            case Schema.FloatRange r -> {
                return new NumberNode(NumberType.FLOAT, r.min(), r.max(), font, onFormChanged, depth);
            }
            case Schema.DoubleRange r -> {
                return new NumberNode(NumberType.DOUBLE, r.min(), r.max(), font, onFormChanged, depth);
            }
            case Schema.Str ignored -> {
                return new StringNode(font, onFormChanged, depth);
            }
            default -> {
            }
        }
        if (unwrapped instanceof Schema.ResourceId || unwrapped instanceof Schema.TagId) {
            return new ResourceNode(unwrapped, font, onFormChanged, depth);
        }
        return switch (unwrapped) {
            case Schema.Enum<?> e -> new EnumNode(e, font, onFormChanged, depth);
            case Schema.Record<?> rec -> new RecordNode(rec, font, onFormChanged, depth, nextPath);
            case Schema.ListOf<?> list -> new ListNode(list, font, onFormChanged, depth, nextPath);
            case Schema.MapOf<?, ?> map -> new MapNode(map, font, onFormChanged, depth, nextPath);
            case Schema.AnyOf<?> anyOf -> new AnyOfNode(anyOf, font, onFormChanged, depth, nextPath);
            case Schema.OneOf<?> oneOf -> new OneOfNode(oneOf, font, onFormChanged, depth, nextPath);
            case Schema.PairOf<?, ?> pair -> new PairNode(pair, font, onFormChanged, depth, nextPath);
            default -> new OpaqueNode(font, onFormChanged, depth);
        };

    }

    private static void drawTreeGuideline(GuiGraphics gfx, int x, int y, int height, int depth) {
        for (int d = 0; d < depth; d++) {
            int lineX = x + d * INDENT_STEP + 5;
            gfx.fill(lineX, y, lineX + 1, y + height, 0x223D445F);
        }
    }

    public enum NumberType {INT, LONG, FLOAT, DOUBLE}

    public static class TreeChevronWidget extends AbstractWidget {
        private final Runnable onToggle;
        private boolean expanded;

        public TreeChevronWidget(int x, int y, boolean initialExpanded, Runnable onToggle) {
            super(x, y, 16, 16, Component.empty());
            this.expanded = initialExpanded;
            this.onToggle = onToggle;
        }

        public boolean isExpanded() {
            return expanded;
        }

        public void setExpanded(boolean expanded) {
            this.expanded = expanded;
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            this.expanded = !this.expanded;
            if (this.onToggle != null) this.onToggle.run();
        }

        @Override
        protected void renderWidget(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
            boolean hovered = isHoveredOrFocused();
            if (hovered) {
                gfx.fill(getX(), getY(), getX() + width, getY() + height, 0x335C668A);
                gfx.renderOutline(getX(), getY(), width, height, EditorTheme.CARD_BORDER);
            }
            EditorTheme.drawChevronIcon(gfx, getX(), getY(), expanded, hovered);
        }

        @Override
        protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
        }
    }

    public static class LazyRefNode implements SchemaFormNode {
        private final Schema<?> targetSchema;
        private final Font font;
        private final Runnable onFormChanged;
        private final int depth;
        private final TreeChevronWidget chevron;
        private SchemaFormNode materialized = null;
        private JsonElement pendingJson = null;

        public LazyRefNode(Schema<?> targetSchema, Font font, Runnable onFormChanged, int depth) {
            this.targetSchema = targetSchema;
            this.font = font;
            this.onFormChanged = onFormChanged;
            this.depth = depth;

            this.chevron = new TreeChevronWidget(0, 0, false, this::toggleExpanded);
        }

        private void toggleExpanded() {
            materialize();
            onFormChanged.run();
        }

        private void materialize() {
            if (materialized != null) return;
            Schema<?> target = TriggerSchemaManager.unwrapRef(targetSchema);
            if (target == null) {
                materialized = new OpaqueNode(font, onFormChanged, depth + 1);
            } else {
                Set<Schema<?>> nextPath = Collections.newSetFromMap(new IdentityHashMap<>());
                nextPath.add(targetSchema);
                nextPath.add(target);
                materialized = createDirectNode(target, font, onFormChanged, depth + 1, nextPath);
            }
            if (pendingJson != null) {
                materialized.setJson(pendingJson);
            }
        }

        @Override
        public @Nullable JsonElement currentJson() {
            if (materialized != null) return materialized.currentJson();
            return pendingJson;
        }

        @Override
        public void setJson(@Nullable JsonElement value) {
            pendingJson = value;
            if (value != null && !value.isJsonNull() && (!(value.isJsonObject() && value.getAsJsonObject().isEmpty()))) {
                materialize();
                if (materialized != null) {
                    materialized.setJson(value);
                    chevron.setExpanded(true);
                }
            } else if (materialized != null) {
                materialized.setJson(value);
            }
        }

        @Override
        public List<GuiEventListener> getWidgets() {
            List<GuiEventListener> list = new ArrayList<>();
            list.add(chevron);
            if (materialized != null && chevron.isExpanded()) {
                list.addAll(materialized.getWidgets());
            }
            return list;
        }

        @Override
        public boolean isUnset() {
            if (materialized != null) return materialized.isUnset();
            return pendingJson == null || pendingJson.isJsonNull() || (pendingJson.isJsonObject() && pendingJson.getAsJsonObject().isEmpty());
        }

        @Override
        public EditorForm.FormRow createFormRow(String label, Font font, Runnable onFormChanged) {
            return new EditorForm.FormRow() {
                private int x;
                private int y;
                private int totalHeight;
                private EditorForm.FormRow innerRow;

                @Override
                public void layout(int x, int y, int width, Font font) {
                    this.x = x;
                    this.y = y;
                    int indentX = x + depth * INDENT_STEP;
                    int labelW = (label != null && !label.isEmpty()) ? font.width(label) : 0;
                    chevron.setX(indentX + labelW + 4);
                    chevron.setY(y + 2);

                    if (materialized != null && chevron.isExpanded()) {
                        if (innerRow == null) {
                            innerRow = materialized.createFormRow("", font, onFormChanged);
                        }
                        innerRow.layout(x, y + 20, width, font);
                        this.totalHeight = 20 + innerRow.getHeight();
                    } else {
                        this.totalHeight = 20;
                    }
                }

                @Override
                public void render(GuiGraphics gfx, Font font, int mouseX, int mouseY, float partialTicks) {
                    int indentX = x + depth * INDENT_STEP;
                    drawTreeGuideline(gfx, x, y, totalHeight, depth);
                    if (label != null && !label.isEmpty()) {
                        int labelCol = !isUnset() ? EditorTheme.TEXT_GOLD : EditorTheme.TEXT_LABEL;
                        gfx.drawString(font, label, indentX, y + 5, labelCol, false);
                    }
                    chevron.render(gfx, mouseX, mouseY, partialTicks);
                    if (materialized != null && chevron.isExpanded() && innerRow != null) {
                        innerRow.render(gfx, font, mouseX, mouseY, partialTicks);
                    }
                }

                @Override
                public int getHeight() {
                    return totalHeight;
                }

                @Override
                public int getY() {
                    return y;
                }
            };
        }
    }

    public static class BoolNode implements SchemaFormNode {
        private final ModernButton button;
        private final int depth;
        private Boolean value = null;

        public BoolNode(Runnable onFormChanged, int depth) {
            this.depth = depth;
            this.button = ModernButton.modernBuilder(Component.literal("Unset"), b -> {
                if (value == null) value = true;
                else if (value) value = false;
                else value = null;
                updateButton();
                onFormChanged.run();
            }).style(ModernButton.Style.SECONDARY).pos(0, 0).size(65, 18).build();
            updateButton();
        }

        private void updateButton() {
            if (value == null) {
                button.setMessage(Component.literal("Unset"));
                button.setStyle(ModernButton.Style.SECONDARY);
            } else if (value) {
                button.setMessage(Component.literal("TRUE"));
                button.setStyle(ModernButton.Style.TOGGLE_ON);
            } else {
                button.setMessage(Component.literal("FALSE"));
                button.setStyle(ModernButton.Style.TOGGLE_OFF);
            }
        }

        @Override
        public @Nullable JsonElement currentJson() {
            return value != null ? new JsonPrimitive(value) : null;
        }

        @Override
        public void setJson(@Nullable JsonElement json) {
            if (json != null && json.isJsonPrimitive() && json.getAsJsonPrimitive().isBoolean()) {
                value = json.getAsBoolean();
            } else {
                value = null;
            }
            updateButton();
        }

        @Override
        public boolean isUnset() {
            return value == null;
        }

        @Override
        public List<GuiEventListener> getWidgets() {
            return List.of(button);
        }

        @Override
        public EditorForm.FormRow createFormRow(String label, Font font, Runnable onFormChanged) {
            boolean isInline = (label == null || label.isEmpty());
            return new EditorForm.FormRow() {
                private int x;
                private int y;

                @Override
                public void layout(int x, int y, int width, Font font) {
                    this.x = x;
                    this.y = y;
                    if (isInline) {
                        button.setX(x);
                        button.setY(y);
                    } else {
                        button.setX(x + width - 65);
                        button.setY(y);
                    }
                }

                @Override
                public void render(GuiGraphics gfx, Font font, int mouseX, int mouseY, float partialTicks) {
                    if (!isInline) {
                        int indentX = x + depth * INDENT_STEP - 2;
                        drawTreeGuideline(gfx, x, y, 20, depth);
                        int labelCol = !isUnset() ? EditorTheme.TEXT_GOLD : EditorTheme.TEXT_LABEL;
                        gfx.drawString(font, label, indentX + 4, y + 5, labelCol, false);
                    }
                    button.render(gfx, mouseX, mouseY, partialTicks);
                }

                @Override
                public int getHeight() {
                    return 20;
                }

                @Override
                public int getY() {
                    return y;
                }
            };
        }
    }

    public static class NumberNode implements SchemaFormNode {
        private final NumberType type;
        private final ModernEditBox box;
        private final int depth;
        private boolean suppressEvents = false;

        public NumberNode(NumberType type, double min, double max, Font font, Runnable onFormChanged, int depth) {
            this.type = type;
            this.depth = depth;
            this.box = new ModernEditBox(font, 0, 0, 90, 18, Component.literal("Number"));
            this.box.setMaxLength(64);
            this.box.setResponder(s -> {
                if (!suppressEvents) onFormChanged.run();
            });
            if (min > -1e9 || max < 1e9) {
                this.box.setTooltip(Tooltip.create(Component.literal("Range: [" + (long) min + " .. " + (long) max + "]")));
            }
        }

        @Override
        public boolean isUnset() {
            return box.getValue().trim().isEmpty();
        }

        @Override
        public @Nullable JsonElement currentJson() {
            String text = box.getValue().trim();
            if (text.isEmpty()) return null;
            try {
                if (type == NumberType.INT) return new JsonPrimitive(Integer.parseInt(text));
                if (type == NumberType.LONG) return new JsonPrimitive(Long.parseLong(text));
                if (type == NumberType.FLOAT) return new JsonPrimitive(Float.parseFloat(text));
                return new JsonPrimitive(Double.parseDouble(text));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        @Override
        public void setJson(@Nullable JsonElement value) {
            suppressEvents = true;
            try {
                if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                    box.setValue(value.getAsString());
                } else {
                    box.setValue("");
                }
            } finally {
                suppressEvents = false;
            }
        }

        @Override
        public List<GuiEventListener> getWidgets() {
            return List.of(box);
        }

        @Override
        public EditorForm.FormRow createFormRow(String label, Font font, Runnable onFormChanged) {
            boolean isInline = (label == null || label.isEmpty());
            return new EditorForm.FormRow() {
                private int x;
                private int y;

                @Override
                public void layout(int x, int y, int width, Font font) {
                    this.x = x;
                    this.y = y;
                    if (isInline) {
                        box.setX(x);
                        box.setY(y);
                        box.setWidth(Math.max(40, width));
                        box.setHeight(18);
                    } else {
                        box.setX(x + width - 90);
                        box.setY(y);
                        box.setWidth(90);
                        box.setHeight(18);
                    }
                }

                @Override
                public void render(GuiGraphics gfx, Font font, int mouseX, int mouseY, float partialTicks) {
                    if (!isInline) {
                        int indentX = x + depth * INDENT_STEP - 2;
                        drawTreeGuideline(gfx, x, y, 20, depth);
                        int labelCol = !isUnset() ? EditorTheme.TEXT_GOLD : EditorTheme.TEXT_LABEL;
                        gfx.drawString(font, label, indentX + 4, y + 5, labelCol, false);
                    }
                    box.render(gfx, mouseX, mouseY, partialTicks);
                }

                @Override
                public int getHeight() {
                    return 20;
                }

                @Override
                public int getY() {
                    return y;
                }
            };
        }
    }

    public static class StringNode implements SchemaFormNode {
        private final ModernEditBox box;
        private final int depth;
        private boolean suppressEvents = false;

        public StringNode(Font font, Runnable onFormChanged, int depth) {
            this.depth = depth;
            this.box = new ModernEditBox(font, 0, 0, 120, 18, Component.literal("Text"));
            this.box.setMaxLength(512);
            this.box.setResponder(s -> {
                if (!suppressEvents) onFormChanged.run();
            });
        }

        @Override
        public boolean isUnset() {
            return box.getValue().trim().isEmpty();
        }

        @Override
        public @Nullable JsonElement currentJson() {
            String text = box.getValue();
            return text.isEmpty() ? null : new JsonPrimitive(text);
        }

        @Override
        public void setJson(@Nullable JsonElement value) {
            suppressEvents = true;
            try {
                if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                    box.setValue(value.getAsString());
                } else {
                    box.setValue("");
                }
            } finally {
                suppressEvents = false;
            }
        }

        @Override
        public List<GuiEventListener> getWidgets() {
            return List.of(box);
        }

        @Override
        public EditorForm.FormRow createFormRow(String label, Font font, Runnable onFormChanged) {
            boolean isInline = (label == null || label.isEmpty());
            return new EditorForm.FormRow() {
                private int x;
                private int y;

                @Override
                public void layout(int x, int y, int width, Font font) {
                    this.x = x;
                    this.y = y;
                    if (isInline) {
                        box.setX(x);
                        box.setY(y);
                        box.setWidth(Math.max(40, width));
                        box.setHeight(18);
                    } else {
                        int boxW = Math.min(160, (width - (depth * INDENT_STEP)) / 2);
                        box.setX(x + width - boxW);
                        box.setY(y);
                        box.setWidth(boxW);
                        box.setHeight(18);
                    }
                }

                @Override
                public void render(GuiGraphics gfx, Font font, int mouseX, int mouseY, float partialTicks) {
                    if (!isInline) {
                        int indentX = x + depth * INDENT_STEP - 2;
                        drawTreeGuideline(gfx, x, y, 20, depth);
                        int labelCol = !isUnset() ? EditorTheme.TEXT_GOLD : EditorTheme.TEXT_LABEL;
                        gfx.drawString(font, label, indentX + 4, y + 5, labelCol, false);
                    }
                    box.render(gfx, mouseX, mouseY, partialTicks);
                }

                @Override
                public int getHeight() {
                    return 20;
                }

                @Override
                public int getY() {
                    return y;
                }
            };
        }
    }

    public static class ResourceNode implements SchemaFormNode {
        private final SuggestingEditBox box;
        private final Schema<?> schema;
        private final int depth;
        private boolean suppressEvents = false;

        public ResourceNode(Schema<?> schema, Font font, Runnable onFormChanged, int depth) {
            this.schema = schema;
            this.depth = depth;
            this.box = new SuggestingEditBox(font, 0, 0, 130, 18, Component.literal("ID"),
                    () -> TriggerSchemaManager.getSuggestionsForSchema(schema));
            if (isItemOrBlock(schema)) {
                this.box.setIconResolver(SuggestingEditBox::defaultItemIconResolver);
            } else if (isLootTable(schema)) {
                this.box.setIconResolver(SuggestingEditBox::lootTableIconResolver);
            }
            this.box.setMaxLength(256);
            this.box.setResponder(s -> {
                if (!suppressEvents) onFormChanged.run();
            });
        }

        private static boolean isItemOrBlock(Schema<?> schema) {
            Schema<?> unwrapped = TriggerSchemaManager.unwrapRef(schema);
            if (unwrapped instanceof Schema.ResourceId(
                    ResourceKey<? extends Registry<?>> registry
            )) {
                return Registries.ITEM.equals(registry) || Registries.BLOCK.equals(registry);
            }
            return false;
        }

        private static boolean isLootTable(Schema<?> schema) {
            Schema<?> unwrapped = TriggerSchemaManager.unwrapRef(schema);
            if (unwrapped instanceof Schema.ResourceId(
                    ResourceKey<? extends Registry<?>> registry
            )) {
                return Registries.LOOT_TABLE.equals(registry);
            }
            return false;
        }

        @Override
        public @Nullable JsonElement currentJson() {
            String text = box.getValue().trim();
            return text.isEmpty() ? null : new JsonPrimitive(text);
        }

        @Override
        public void setJson(@Nullable JsonElement value) {
            suppressEvents = true;
            try {
                if (value != null && value.isJsonPrimitive()) {
                    box.setValue(value.getAsString());
                } else {
                    box.setValue("");
                }
            } finally {
                suppressEvents = false;
            }
        }

        @Override
        public boolean isUnset() {
            return box.getValue().trim().isEmpty();
        }

        @Override
        public List<GuiEventListener> getWidgets() {
            return List.of(box);
        }

        @Override
        public EditorForm.FormRow createFormRow(String label, Font font, Runnable onFormChanged) {
            boolean hasPreview = isItemOrBlock(schema);
            boolean isInline = (label == null || label.isEmpty());
            return new EditorForm.FormRow() {
                private int x;
                private int y;

                @Override
                public void layout(int x, int y, int width, Font font) {
                    this.x = x;
                    this.y = y;
                    int previewSlotW = hasPreview ? 20 : 0;
                    if (isInline) {
                        box.setX(x);
                        box.setY(y);
                        box.setWidth(Math.max(40, width - previewSlotW));
                        box.setHeight(18);
                    } else {
                        int boxW = Math.min(160, (width - (depth * INDENT_STEP)) / 2);
                        box.setX(x + width - boxW - previewSlotW);
                        box.setY(y);
                        box.setWidth(boxW);
                        box.setHeight(18);
                    }
                }

                @Override
                public void render(GuiGraphics gfx, Font font, int mouseX, int mouseY, float partialTicks) {
                    if (!isInline) {
                        int indentX = x + depth * INDENT_STEP - 2;
                        drawTreeGuideline(gfx, x, y, 20, depth);
                        int labelCol = !isUnset() ? EditorTheme.TEXT_GOLD : EditorTheme.TEXT_LABEL;
                        gfx.drawString(font, label, indentX + 4, y + 5, labelCol, false);
                    }
                    box.render(gfx, mouseX, mouseY, partialTicks);

                    if (hasPreview) {
                        int prevX = box.getX() + box.getWidth() + 2;
                        int prevY = y;
                        String val = box.getValue().trim();
                        ItemStack stack = SuggestingEditBox.defaultItemIconResolver(val);
                        if (!stack.isEmpty()) {
                            gfx.renderFakeItem(stack, prevX, prevY + 1);
                        }
                    }
                }

                @Override
                public void renderOverlay(GuiGraphics gfx, Font font, int mouseX, int mouseY, float partialTicks) {
                    if (hasPreview) {
                        int prevX = box.getX() + box.getWidth() + 2;
                        int prevY = y;
                        String val = box.getValue().trim();
                        ItemStack stack = SuggestingEditBox.defaultItemIconResolver(val);
                        if (!stack.isEmpty() && mouseX >= prevX && mouseX <= prevX + 18 && mouseY >= prevY && mouseY <= prevY + 18) {
                            gfx.renderTooltip(font, stack, mouseX, mouseY);
                        }
                    }
                }

                @Override
                public int getHeight() {
                    return 20;
                }

                @Override
                public int getY() {
                    return y;
                }
            };
        }
    }

    public static class EnumNode implements SchemaFormNode {
        private final ModernDropdown dropdown;
        private final int depth;

        public EnumNode(Schema.Enum<?> enumSchema, Font font, Runnable onFormChanged, int depth) {
            this.depth = depth;
            List<String> rawOptions = TriggerSchemaManager.getSuggestionsForSchema(enumSchema);
            List<ModernDropdown.Option> dropdownOptions = new ArrayList<>();
            dropdownOptions.add(new ModernDropdown.Option("", "Unset"));
            for (String opt : rawOptions) {
                dropdownOptions.add(new ModernDropdown.Option(opt, opt));
            }

            this.dropdown = new ModernDropdown(font, 0, 0, 90, 18, dropdownOptions, 0, opt -> onFormChanged.run());
        }

        @Override
        public boolean isUnset() {
            ModernDropdown.Option opt = dropdown.getSelectedOption();
            return opt == null || opt.id().isEmpty();
        }

        @Override
        public @Nullable JsonElement currentJson() {
            ModernDropdown.Option opt = dropdown.getSelectedOption();
            return (opt != null && !opt.id().isEmpty()) ? new JsonPrimitive(opt.id()) : null;
        }

        @Override
        public void setJson(@Nullable JsonElement value) {
            if (value != null && value.isJsonPrimitive()) {
                dropdown.setSelectedId(value.getAsString());
            } else {
                dropdown.setSelectedId("");
            }
        }

        @Override
        public List<GuiEventListener> getWidgets() {
            return List.of(dropdown);
        }

        @Override
        public EditorForm.FormRow createFormRow(String label, Font font, Runnable onFormChanged) {
            boolean isInline = (label == null || label.isEmpty());
            return new EditorForm.FormRow() {
                private int x;
                private int y;

                @Override
                public void layout(int x, int y, int width, Font font) {
                    this.x = x;
                    this.y = y;
                    int dropW = Math.min(140, dropdown.getOptimalWidth());
                    dropdown.setWidth(dropW);
                    if (isInline) {
                        dropdown.setX(x);
                        dropdown.setY(y);
                    } else {
                        dropdown.setX(x + width - dropW);
                        dropdown.setY(y);
                    }
                }

                @Override
                public void render(GuiGraphics gfx, Font font, int mouseX, int mouseY, float partialTicks) {
                    if (!isInline) {
                        int indentX = x + depth * INDENT_STEP - 2;
                        drawTreeGuideline(gfx, x, y, 20, depth);
                        int labelCol = !isUnset() ? EditorTheme.TEXT_GOLD : EditorTheme.TEXT_LABEL;
                        gfx.drawString(font, label, indentX + 4, y + 5, labelCol, false);
                    }
                    dropdown.render(gfx, mouseX, mouseY, partialTicks);
                }

                @Override
                public int getHeight() {
                    return 20;
                }

                @Override
                public int getY() {
                    return y;
                }
            };
        }
    }

    public static class RecordNode implements SchemaFormNode {
        private final Map<String, FieldItem> fields = new LinkedHashMap<>();
        private final TreeChevronWidget chevron;
        private final int depth;

        public RecordNode(Schema.Record<?> recordSchema, Font font, Runnable onFormChanged, int depth, Set<Schema<?>> seen) {
            this.depth = depth;

            this.chevron = new TreeChevronWidget(0, 0, false, onFormChanged);

            for (Schema.Field<?, ?> f : recordSchema.fields()) {
                SchemaFormNode childNode = createNode(f.schema(), font, onFormChanged, depth + 1, seen);
                fields.put(f.name(), new FieldItem(f.name(), f, childNode));
            }
        }

        public boolean hasFields() {
            return !fields.isEmpty();
        }

        @Override
        public boolean isUnset() {
            for (FieldItem item : fields.values()) {
                if (!item.node.isUnset()) return false;
            }
            return true;
        }

        @Override
        public @Nullable JsonElement currentJson() {
            JsonObject obj = new JsonObject();
            for (FieldItem item : fields.values()) {
                if (item.fieldDef.optional() && item.node.isUnset()) {
                    continue;
                }
                JsonElement sub = item.node.currentJson();
                if (sub != null && !sub.isJsonNull()) {
                    if (item.fieldDef.inline() && sub.isJsonObject()) {
                        for (Map.Entry<String, JsonElement> kv : sub.getAsJsonObject().entrySet()) {
                            obj.add(kv.getKey(), kv.getValue());
                        }
                    } else {
                        obj.add(item.name, sub);
                    }
                }
            }
            return obj.isEmpty() ? null : obj;
        }

        @Override
        public void setJson(@Nullable JsonElement value) {
            if (value != null && value.isJsonObject()) {
                JsonObject obj = value.getAsJsonObject();
                for (FieldItem item : fields.values()) {
                    if (item.fieldDef.inline()) {
                        item.node.setJson(obj);
                    } else if (obj.has(item.name)) {
                        item.node.setJson(obj.get(item.name));
                    } else {
                        item.node.setJson(null);
                    }
                }
                if (!obj.isEmpty()) {
                    chevron.setExpanded(true);
                }
            } else {
                for (FieldItem item : fields.values()) {
                    item.node.setJson(null);
                }
                chevron.setExpanded(false);
            }
        }

        @Override
        public List<GuiEventListener> getWidgets() {
            List<GuiEventListener> list = new ArrayList<>();
            list.add(chevron);
            for (FieldItem item : fields.values()) {
                list.addAll(item.node.getWidgets());
            }
            return list;
        }

        @Override
        public EditorForm.FormRow createFormRow(String label, Font font, Runnable onFormChanged) {
            for (FieldItem item : fields.values()) {
                item.row = item.node.createFormRow(TextUtil.titleCase(item.name), font, onFormChanged);
            }

            boolean isHeaderless = (label == null || label.isEmpty());

            return new EditorForm.FormRow() {
                private int x;
                private int y;
                private int totalHeight;

                @Override
                public void layout(int x, int y, int width, Font font) {
                    this.x = x;
                    this.y = y;

                    int curY = y;
                    if (!isHeaderless) {
                        int indentX = x + depth * INDENT_STEP - 2;
                        chevron.setX(indentX);
                        chevron.setY(y + 2);
                        curY += 20;
                    }

                    if (isHeaderless || chevron.isExpanded()) {
                        for (FieldItem item : fields.values()) {
                            if (item.row != null) {
                                item.row.layout(x, curY, width, font);
                                curY += item.row.getHeight() + 4;
                            }
                        }
                        this.totalHeight = Math.max(isHeaderless ? 0 : 20, curY - y);
                    } else {
                        this.totalHeight = 20;
                    }
                }

                @Override
                public void render(GuiGraphics gfx, Font font, int mouseX, int mouseY, float partialTicks) {
                    int indentX = x + depth * INDENT_STEP - 2;
                    if (!isHeaderless) {
                        drawTreeGuideline(gfx, x, y, totalHeight, depth);
                        chevron.render(gfx, mouseX, mouseY, partialTicks);
                        int labelCol = !isUnset() ? EditorTheme.TEXT_GOLD : EditorTheme.TEXT_LABEL;
                        gfx.drawString(font, label, indentX + 18, y + 5, labelCol, false);
                    }

                    if (isHeaderless || chevron.isExpanded()) {
                        for (FieldItem item : fields.values()) {
                            if (item.row != null) {
                                item.row.render(gfx, font, mouseX, mouseY, partialTicks);
                            }
                        }
                    }
                }

                @Override
                public int getHeight() {
                    return totalHeight;
                }

                @Override
                public int getY() {
                    return y;
                }
            };
        }

        public static class FieldItem {
            public final String name;
            public final Schema.Field<?, ?> fieldDef;
            public final SchemaFormNode node;
            public EditorForm.FormRow row;

            public FieldItem(String name, Schema.Field<?, ?> fieldDef, SchemaFormNode node) {
                this.name = name;
                this.fieldDef = fieldDef;
                this.node = node;
            }
        }
    }

    public static class ListNode implements SchemaFormNode {
        private final Schema.ListOf<?> listSchema;
        private final Font font;
        private final Runnable onFormChanged;
        private final int depth;
        private final Set<Schema<?>> seen;
        private final List<ListItem> items = new ArrayList<>();
        private final TreeChevronWidget chevron;
        private final ModernButton addBtn;

        public ListNode(Schema.ListOf<?> listSchema, Font font, Runnable onFormChanged, int depth, Set<Schema<?>> seen) {
            this.listSchema = listSchema;
            this.font = font;
            this.onFormChanged = onFormChanged;
            this.depth = depth;
            this.seen = seen;

            this.chevron = new TreeChevronWidget(0, 0, true, onFormChanged);

            this.addBtn = ModernButton.modernBuilder(Component.literal("+ Add"), b -> {
                        addItem(null);
                        chevron.setExpanded(true);
                        onFormChanged.run();
                    }).style(ModernButton.Style.SECONDARY).pos(0, 0).size(50, 18)
                    .tooltip(Tooltip.create(Component.literal("Add new item to list")))
                    .build();
        }

        private String getItemLabel(int index) {
            return items.size() <= 1 ? "Condition" : ("Condition " + (index + 1));
        }

        private void addItem(@Nullable JsonElement initialJson) {
            SchemaFormNode child = createNode(listSchema.element(), font, onFormChanged, depth, seen);
            if (initialJson != null) child.setJson(initialJson);
            ListItem item = new ListItem(child, () -> {
                items.removeIf(i -> i.node == child);
                onFormChanged.run();
            });
            item.row = child.createFormRow(getItemLabel(items.size()), font, onFormChanged);
            items.add(item);
        }

        @Override
        public @Nullable JsonElement currentJson() {
            JsonArray arr = new JsonArray();
            for (ListItem item : items) {
                JsonElement json = item.node.currentJson();
                if (json != null) arr.add(json);
            }
            return arr.isEmpty() ? null : arr;
        }

        @Override
        public void setJson(@Nullable JsonElement value) {
            items.clear();
            if (value != null && value.isJsonArray()) {
                for (JsonElement el : value.getAsJsonArray()) {
                    addItem(el);
                }
            }
        }

        @Override
        public boolean isUnset() {
            return items.isEmpty();
        }

        @Override
        public List<GuiEventListener> getWidgets() {
            List<GuiEventListener> list = new ArrayList<>();
            list.add(chevron);
            list.add(addBtn);
            if (chevron.isExpanded()) {
                for (ListItem item : items) {
                    list.add(item.removeBtn);
                    list.addAll(item.node.getWidgets());
                }
            }
            return list;
        }

        @Override
        public EditorForm.FormRow createFormRow(String label, Font font, Runnable onFormChanged) {
            return new EditorForm.FormRow() {
                private int x;
                private int y;
                private int totalHeight;

                @Override
                public void layout(int x, int y, int width, Font font) {
                    this.x = x;
                    this.y = y;

                    int indentX = x + depth * INDENT_STEP - 2;
                    chevron.setX(indentX);
                    chevron.setY(y + 2);
                    addBtn.setX(x + width - 50);
                    addBtn.setY(y);

                    int curY = y + 20;
                    if (chevron.isExpanded()) {
                        for (int i = 0; i < items.size(); i++) {
                            ListItem item = items.get(i);
                            item.row = item.node.createFormRow(getItemLabel(i), font, onFormChanged);
                            item.removeBtn.setX(x + width - 18);
                            item.removeBtn.setY(curY + 2);
                            item.row.layout(x, curY, width - 22, font);
                            curY += item.row.getHeight() + 4;
                        }
                        this.totalHeight = Math.max(20, curY - y);
                    } else {
                        this.totalHeight = 20;
                    }
                }

                @Override
                public void render(GuiGraphics gfx, Font font, int mouseX, int mouseY, float partialTicks) {
                    int indentX = x + depth * INDENT_STEP - 2;
                    drawTreeGuideline(gfx, x, y, totalHeight, depth);

                    chevron.render(gfx, mouseX, mouseY, partialTicks);
                    String header = label + " (" + items.size() + " items)";
                    int labelCol = !isUnset() ? EditorTheme.TEXT_GOLD : EditorTheme.TEXT_LABEL;
                    gfx.drawString(font, header, indentX + 18, y + 5, labelCol, false);
                    addBtn.render(gfx, mouseX, mouseY, partialTicks);

                    if (chevron.isExpanded()) {
                        for (ListItem item : items) {
                            if (item.row != null) item.row.render(gfx, font, mouseX, mouseY, partialTicks);
                            item.removeBtn.render(gfx, mouseX, mouseY, partialTicks);
                        }
                    }
                }

                @Override
                public int getHeight() {
                    return totalHeight;
                }

                @Override
                public int getY() {
                    return y;
                }
            };
        }

        public static class ListItem {
            public final SchemaFormNode node;
            public final ModernButton removeBtn;
            public EditorForm.FormRow row;

            public ListItem(SchemaFormNode node, Runnable onRemove) {
                this.node = node;
                this.removeBtn = ModernButton.modernBuilder(Component.literal("x"), b -> onRemove.run())
                        .style(ModernButton.Style.DANGER).pos(0, 0).size(16, 16)
                        .tooltip(Tooltip.create(Component.literal("Remove item")))
                        .build();
            }
        }
    }

    public static class MapNode implements SchemaFormNode {
        private final Schema.MapOf<?, ?> mapSchema;
        private final Font font;
        private final Runnable onFormChanged;
        private final int depth;
        private final Set<Schema<?>> seen;
        private final List<MapEntryItem> entries = new ArrayList<>();
        private final TreeChevronWidget chevron;
        private final ModernButton addBtn;

        public MapNode(Schema.MapOf<?, ?> mapSchema, Font font, Runnable onFormChanged, int depth, Set<Schema<?>> seen) {
            this.mapSchema = mapSchema;
            this.font = font;
            this.onFormChanged = onFormChanged;
            this.depth = depth;
            this.seen = seen;

            this.chevron = new TreeChevronWidget(0, 0, true, onFormChanged);

            this.addBtn = ModernButton.modernBuilder(Component.literal("+ Add"), b -> {
                        addEntry("", null);
                        chevron.setExpanded(true);
                        onFormChanged.run();
                    }).style(ModernButton.Style.SECONDARY).pos(0, 0).size(50, 18)
                    .tooltip(Tooltip.create(Component.literal("Add key-value entry")))
                    .build();
        }

        private void addEntry(String key, @Nullable JsonElement valJson) {
            SchemaFormNode child = createNode(mapSchema.value(), font, onFormChanged, depth + 1, seen);
            if (valJson != null) child.setJson(valJson);
            MapEntryItem item = new MapEntryItem(key, child, font, () -> {
                entries.removeIf(e -> e.valNode == child);
                onFormChanged.run();
            }, onFormChanged);
            item.valRow = child.createFormRow("", font, onFormChanged);
            entries.add(item);
        }

        @Override
        public boolean isUnset() {
            for (MapEntryItem e : entries) {
                String k = e.keyBox.getValue().trim();
                if (!k.isEmpty() && !e.valNode.isUnset()) return false;
            }
            return true;
        }

        @Override
        public @Nullable JsonElement currentJson() {
            JsonObject obj = new JsonObject();
            for (MapEntryItem e : entries) {
                String k = e.keyBox.getValue().trim();
                JsonElement v = e.valNode.currentJson();
                if (!k.isEmpty() && v != null) {
                    obj.add(k, v);
                }
            }
            return obj.isEmpty() ? null : obj;
        }

        @Override
        public void setJson(@Nullable JsonElement value) {
            entries.clear();
            if (value != null && value.isJsonObject()) {
                for (Map.Entry<String, JsonElement> kv : value.getAsJsonObject().entrySet()) {
                    addEntry(kv.getKey(), kv.getValue());
                }
            }
        }

        @Override
        public List<GuiEventListener> getWidgets() {
            List<GuiEventListener> list = new ArrayList<>();
            list.add(chevron);
            list.add(addBtn);
            if (chevron.isExpanded()) {
                for (MapEntryItem e : entries) {
                    list.add(e.keyBox);
                    list.add(e.removeBtn);
                    list.addAll(e.valNode.getWidgets());
                }
            }
            return list;
        }

        @Override
        public EditorForm.FormRow createFormRow(String label, Font font, Runnable onFormChanged) {
            return new EditorForm.FormRow() {
                private int x;
                private int y;
                private int totalHeight;

                @Override
                public void layout(int x, int y, int width, Font font) {
                    this.x = x;
                    this.y = y;

                    int indentX = x + depth * INDENT_STEP - 2;
                    chevron.setX(indentX);
                    chevron.setY(y + 2);
                    addBtn.setX(x + width - 50);
                    addBtn.setY(y);

                    int curY = y + 20;
                    if (chevron.isExpanded()) {
                        for (MapEntryItem item : entries) {
                            int childIndentX = x + (depth + 1) * INDENT_STEP;
                            item.keyBox.setX(childIndentX);
                            item.keyBox.setY(curY);
                            item.keyBox.setWidth(75);
                            item.keyBox.setHeight(18);

                            if (item.valRow == null) {
                                item.valRow = item.valNode.createFormRow("", font, onFormChanged);
                            }
                            item.valRow.layout(childIndentX + 80, curY, width - (childIndentX - x) - 105, font);
                            item.removeBtn.setX(x + width - 18);
                            item.removeBtn.setY(curY + 2);
                            curY += Math.max(20, item.valRow.getHeight()) + 4;
                        }
                        this.totalHeight = Math.max(20, curY - y);
                    } else {
                        this.totalHeight = 20;
                    }
                }

                @Override
                public void render(GuiGraphics gfx, Font font, int mouseX, int mouseY, float partialTicks) {
                    int indentX = x + depth * INDENT_STEP - 2;
                    drawTreeGuideline(gfx, x, y, totalHeight, depth);

                    chevron.render(gfx, mouseX, mouseY, partialTicks);
                    String header = label + " (Map)";
                    int labelCol = !isUnset() ? EditorTheme.TEXT_GOLD : EditorTheme.TEXT_LABEL;
                    gfx.drawString(font, header, indentX + 18, y + 5, labelCol, false);
                    addBtn.render(gfx, mouseX, mouseY, partialTicks);

                    if (chevron.isExpanded()) {
                        for (MapEntryItem item : entries) {
                            item.keyBox.render(gfx, mouseX, mouseY, partialTicks);
                            if (item.valRow != null) item.valRow.render(gfx, font, mouseX, mouseY, partialTicks);
                            item.removeBtn.render(gfx, mouseX, mouseY, partialTicks);
                        }
                    }
                }

                @Override
                public int getHeight() {
                    return totalHeight;
                }

                @Override
                public int getY() {
                    return y;
                }
            };
        }

        public static class MapEntryItem {
            public final ModernEditBox keyBox;
            public final SchemaFormNode valNode;
            public final ModernButton removeBtn;
            public EditorForm.FormRow valRow;

            public MapEntryItem(String key, SchemaFormNode valNode, Font font, Runnable onRemove, Runnable onFormChanged) {
                this.keyBox = new ModernEditBox(font, 0, 0, 75, 18, Component.literal("Key"));
                this.keyBox.setValue(key);
                this.keyBox.setResponder(s -> onFormChanged.run());
                this.valNode = valNode;
                this.removeBtn = ModernButton.modernBuilder(Component.literal("x"), b -> onRemove.run())
                        .style(ModernButton.Style.DANGER).pos(0, 0).size(16, 16)
                        .tooltip(Tooltip.create(Component.literal("Remove entry")))
                        .build();
            }
        }
    }

    public static class AnyOfNode implements SchemaFormNode {
        private final List<OptionEntry> options = new ArrayList<>();
        private final ModernDropdown dropdown;
        private final int depth;

        public AnyOfNode(Schema.AnyOf<?> anyOfSchema, Font font, Runnable onFormChanged, int depth, Set<Schema<?>> seen) {
            this.depth = depth;

            List<ModernDropdown.Option> dropdownOptions = new ArrayList<>();
            for (int i = 0; i < anyOfSchema.options().size(); i++) {
                Schema.AnyOf.Option opt = anyOfSchema.options().get(i);
                String id = opt.label() != null ? opt.label() : ("opt_" + i);
                String label = TextUtil.titleCase(id);
                SchemaFormNode node = createNode(opt.schema(), font, onFormChanged, depth, seen);
                OptionEntry entry = new OptionEntry(id, label, opt.schema(), node);
                options.add(entry);
                dropdownOptions.add(new ModernDropdown.Option(id, label));
            }

            this.dropdown = new ModernDropdown(font, 0, 0, 80, 18, dropdownOptions, 0, opt -> onFormChanged.run());
        }

        private OptionEntry getActiveEntry() {
            ModernDropdown.Option opt = dropdown.getSelectedOption();
            if (opt != null) {
                for (OptionEntry oe : options) {
                    if (oe.id.equals(opt.id())) return oe;
                }
            }
            return options.isEmpty() ? null : options.getFirst();
        }

        @Override
        public boolean isUnset() {
            OptionEntry active = getActiveEntry();
            return active == null || active.node.isUnset();
        }

        @Override
        public @Nullable JsonElement currentJson() {
            OptionEntry active = getActiveEntry();
            return active != null ? active.node.currentJson() : null;
        }

        @Override
        public void setJson(@Nullable JsonElement value) {
            if (value == null) {
                if (!options.isEmpty()) options.getFirst().node.setJson(null);
                return;
            }

            int bestMatch = 0;
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString() && value.getAsString().startsWith("#")) {
                for (int i = 0; i < options.size(); i++) {
                    Schema<?> s = TriggerSchemaManager.unwrapRef(options.get(i).schema);
                    if (s instanceof Schema.TagId) {
                        bestMatch = i;
                        break;
                    }
                }
            } else if (value.isJsonArray()) {
                for (int i = 0; i < options.size(); i++) {
                    Schema<?> s = TriggerSchemaManager.unwrapRef(options.get(i).schema);
                    if (s instanceof Schema.ListOf) {
                        bestMatch = i;
                        break;
                    }
                }
            } else if (value.isJsonObject()) {

                int recordMatch = -1;
                for (int i = 0; i < options.size(); i++) {
                    Schema<?> s = TriggerSchemaManager.unwrapRef(options.get(i).schema);
                    if (s instanceof Schema.OneOf<?> || s instanceof Schema.AnyOf<?>) {
                        bestMatch = i;
                        break;
                    }
                    if (s instanceof Schema.Record<?> && recordMatch < 0) {
                        recordMatch = i;
                    }
                }
                if (bestMatch == 0 && recordMatch > 0) {
                    bestMatch = recordMatch;
                }
            } else if (value.isJsonPrimitive()) {
                for (int i = 0; i < options.size(); i++) {
                    Schema<?> s = TriggerSchemaManager.unwrapRef(options.get(i).schema);
                    if (s instanceof Schema.ResourceId || s instanceof Schema.Str) {
                        bestMatch = i;
                        break;
                    }
                }
            }

            if (bestMatch < options.size()) {
                dropdown.setSelectedId(options.get(bestMatch).id);
                options.get(bestMatch).node.setJson(value);
            }
        }

        @Override
        public List<GuiEventListener> getWidgets() {
            List<GuiEventListener> list = new ArrayList<>();
            list.add(dropdown);
            for (OptionEntry oe : options) {
                list.addAll(oe.node.getWidgets());
            }
            return list;
        }

        private boolean isInlineOption(OptionEntry active) {
            if (active == null) return false;
            Schema<?> s = TriggerSchemaManager.unwrapRef(active.schema);
            return s instanceof Schema.ResourceId || s instanceof Schema.TagId || s instanceof Schema.Str
                    || s instanceof Schema.IntRange || s instanceof Schema.LongRange || s instanceof Schema.FloatRange
                    || s instanceof Schema.DoubleRange || s instanceof Schema.Bool || s instanceof Schema.Enum;
        }

        @Override
        public EditorForm.FormRow createFormRow(String label, Font font, Runnable onFormChanged) {
            for (OptionEntry oe : options) {
                oe.row = oe.node.createFormRow("", font, onFormChanged);
            }

            return new EditorForm.FormRow() {
                private int x;
                private int y;
                private int totalHeight;

                @Override
                public void layout(int x, int y, int width, Font font) {
                    this.x = x;
                    this.y = y;

                    OptionEntry active = getActiveEntry();
                    boolean inline = isInlineOption(active);

                    int indentX = x + depth * INDENT_STEP;
                    int labelW = (label != null && !label.isEmpty()) ? font.width(label) + 6 : 0;
                    int dropW = Math.max(50, Math.min(80, dropdown.getOptimalWidth()));

                    dropdown.setWidth(dropW);
                    dropdown.setX(indentX + labelW);
                    dropdown.setY(y);

                    if (inline) {
                        int inputX = dropdown.getX() + dropW + 4;
                        int inputW = Math.max(60, (x + width) - inputX);
                        if (active.row != null) {
                            active.row.layout(inputX, y, inputW, font);
                        }
                        this.totalHeight = 20;
                    } else {
                        int curY = y + 20;
                        if (active != null && active.row != null) {
                            active.row.layout(x, curY, width, font);
                            curY += active.row.getHeight() + 2;
                        }
                        this.totalHeight = Math.max(20, curY - y);
                    }
                }

                @Override
                public void render(GuiGraphics gfx, Font font, int mouseX, int mouseY, float partialTicks) {
                    int indentX = x + depth * INDENT_STEP;
                    drawTreeGuideline(gfx, x, y, totalHeight, depth);
                    if (label != null && !label.isEmpty()) {
                        int labelCol = !isUnset() ? EditorTheme.TEXT_GOLD : EditorTheme.TEXT_LABEL;
                        gfx.drawString(font, label, indentX, y + 5, labelCol, false);
                    }
                    dropdown.render(gfx, mouseX, mouseY, partialTicks);

                    OptionEntry active = getActiveEntry();
                    if (active != null && active.row != null) {
                        active.row.render(gfx, font, mouseX, mouseY, partialTicks);
                    }
                }

                @Override
                public int getHeight() {
                    return totalHeight;
                }

                @Override
                public int getY() {
                    return y;
                }
            };
        }

        public static class OptionEntry {
            public final String id;
            public final String label;
            public final Schema<?> schema;
            public final SchemaFormNode node;
            public EditorForm.FormRow row;

            public OptionEntry(String id, String label, Schema<?> schema, SchemaFormNode node) {
                this.id = id;
                this.label = label;
                this.schema = schema;
                this.node = node;
            }
        }
    }

    public static class OneOfNode implements SchemaFormNode {
        private final Schema.OneOf<?> oneOfSchema;
        private final Map<String, OptionEntry> variants = new LinkedHashMap<>();
        private final ModernDropdown dropdown;
        private final int depth;

        public OneOfNode(Schema.OneOf<?> oneOfSchema, Font font, Runnable onFormChanged, int depth, Set<Schema<?>> seen) {
            this.oneOfSchema = oneOfSchema;
            this.depth = depth;

            List<ModernDropdown.Option> dropdownOptions = new ArrayList<>();
            for (Map.Entry<String, ? extends Schema<?>> entry : oneOfSchema.variants().entrySet()) {
                String typeKey = entry.getKey();
                String label = formatVariantName(typeKey);
                SchemaFormNode node = createNode(entry.getValue(), font, onFormChanged, depth, seen);
                OptionEntry oe = new OptionEntry(typeKey, label, entry.getValue(), node);
                variants.put(typeKey, oe);
                dropdownOptions.add(new ModernDropdown.Option(typeKey, label));
            }

            this.dropdown = new ModernDropdown(font, 0, 0, 120, 18, dropdownOptions, 0, opt -> onFormChanged.run());
        }

        private static String formatVariantName(String raw) {
            if (raw.contains(":")) raw = raw.substring(raw.indexOf(':') + 1);
            return TextUtil.titleCase(raw.replace('_', ' '));
        }

        private OptionEntry getActiveEntry() {
            ModernDropdown.Option opt = dropdown.getSelectedOption();
            if (opt != null && variants.containsKey(opt.id())) {
                return variants.get(opt.id());
            }
            return variants.isEmpty() ? null : variants.values().iterator().next();
        }

        @Override
        public boolean isUnset() {
            OptionEntry oe = getActiveEntry();
            if (oe == null) return true;
            if (oe.node instanceof RecordNode rec && !rec.hasFields()) return false;
            return oe.node.isUnset();
        }

        @Override
        public @Nullable JsonElement currentJson() {
            OptionEntry oe = getActiveEntry();
            if (oe == null) return null;
            JsonElement childJson = oe.node.currentJson();
            String valueField = oneOfSchema.valueField();
            if (valueField != null) {
                JsonObject obj = new JsonObject();
                obj.addProperty(oneOfSchema.typeField(), oe.typeKey);
                if (childJson != null && !childJson.isJsonNull()
                        && !(childJson.isJsonObject() && childJson.getAsJsonObject().isEmpty())) {
                    obj.add(valueField, childJson);
                }
                return obj;
            }
            JsonObject obj = (childJson != null && childJson.isJsonObject()) ? childJson.getAsJsonObject().deepCopy() : new JsonObject();
            obj.addProperty(oneOfSchema.typeField(), oe.typeKey);
            return obj;
        }

        @Override
        public void setJson(@Nullable JsonElement value) {
            if (value != null && value.isJsonObject()) {
                JsonObject obj = value.getAsJsonObject();
                if (obj.has(oneOfSchema.typeField())) {
                    String type = obj.get(oneOfSchema.typeField()).getAsString();
                    if (variants.containsKey(type)) {
                        dropdown.setSelectedId(type);
                    } else if (!type.contains(":") && variants.containsKey("minecraft:" + type)) {
                        dropdown.setSelectedId("minecraft:" + type);
                    } else if (type.startsWith("minecraft:") && variants.containsKey(type.substring("minecraft:".length()))) {
                        dropdown.setSelectedId(type.substring("minecraft:".length()));
                    }
                }
                OptionEntry oe = getActiveEntry();
                if (oe != null) {
                    String valueField = oneOfSchema.valueField();
                    if (valueField != null) {
                        oe.node.setJson(obj.has(valueField) ? obj.get(valueField) : null);
                    } else {
                        oe.node.setJson(obj);
                    }
                }
            }
        }

        @Override
        public List<GuiEventListener> getWidgets() {
            List<GuiEventListener> list = new ArrayList<>();
            list.add(dropdown);
            OptionEntry oe = getActiveEntry();
            if (oe != null) list.addAll(oe.node.getWidgets());
            return list;
        }

        @Override
        public EditorForm.FormRow createFormRow(String label, Font font, Runnable onFormChanged) {
            for (OptionEntry oe : variants.values()) {
                oe.row = oe.node.createFormRow("", font, onFormChanged);
            }

            return new EditorForm.FormRow() {
                private int x;
                private int y;
                private int totalHeight;

                @Override
                public void layout(int x, int y, int width, Font font) {
                    this.x = x;
                    this.y = y;
                    int indentX = x + depth * INDENT_STEP;
                    int labelW = (label != null && !label.isEmpty()) ? font.width(label) + 6 : 0;
                    int dropW = Math.max(70, Math.min(140, dropdown.getOptimalWidth()));
                    dropdown.setWidth(dropW);
                    dropdown.setX(indentX + labelW);
                    dropdown.setY(y);

                    int curY = y + 20;
                    OptionEntry oe = getActiveEntry();
                    if (oe != null) {
                        oe.row.layout(x, curY, width, font);
                        curY += oe.row.getHeight() + 2;
                    }
                    this.totalHeight = Math.max(20, curY - y);
                }

                @Override
                public void render(GuiGraphics gfx, Font font, int mouseX, int mouseY, float partialTicks) {
                    int indentX = x + depth * INDENT_STEP;
                    drawTreeGuideline(gfx, x, y, totalHeight, depth);
                    if (label != null && !label.isEmpty()) {
                        int labelCol = !isUnset() ? EditorTheme.TEXT_GOLD : EditorTheme.TEXT_LABEL;
                        gfx.drawString(font, label, indentX, y + 5, labelCol, false);
                    }
                    dropdown.render(gfx, mouseX, mouseY, partialTicks);

                    OptionEntry oe = getActiveEntry();
                    if (oe != null) {
                        oe.row.render(gfx, font, mouseX, mouseY, partialTicks);
                    }
                }

                @Override
                public int getHeight() {
                    return totalHeight;
                }

                @Override
                public int getY() {
                    return y;
                }
            };
        }

        public static class OptionEntry {
            public final String typeKey;
            public final String label;
            public final Schema<?> schema;
            public final SchemaFormNode node;
            public EditorForm.FormRow row;

            public OptionEntry(String typeKey, String label, Schema<?> schema, SchemaFormNode node) {
                this.typeKey = typeKey;
                this.label = label;
                this.schema = schema;
                this.node = node;
            }
        }
    }

    public static class PairNode implements SchemaFormNode {
        private final SchemaFormNode firstNode;
        private final SchemaFormNode secondNode;
        private final int depth;

        public PairNode(Schema.PairOf<?, ?> pairSchema, Font font, Runnable onFormChanged, int depth, Set<Schema<?>> seen) {
            this.depth = depth;
            this.firstNode = createNode(pairSchema.first(), font, onFormChanged, depth + 1, seen);
            this.secondNode = createNode(pairSchema.second(), font, onFormChanged, depth + 1, seen);
        }

        @Override
        public boolean isUnset() {
            return firstNode.isUnset() && secondNode.isUnset();
        }

        @Override
        public @Nullable JsonElement currentJson() {
            JsonElement f = firstNode.currentJson();
            JsonElement s = secondNode.currentJson();
            if (f == null && s == null) return null;
            JsonArray arr = new JsonArray();
            arr.add(f != null ? f : JsonNull.INSTANCE);
            arr.add(s != null ? s : JsonNull.INSTANCE);
            return arr;
        }

        @Override
        public void setJson(@Nullable JsonElement value) {
            if (value != null && value.isJsonArray() && value.getAsJsonArray().size() >= 2) {
                firstNode.setJson(value.getAsJsonArray().get(0));
                secondNode.setJson(value.getAsJsonArray().get(1));
            } else {
                firstNode.setJson(null);
                secondNode.setJson(null);
            }
        }

        @Override
        public List<GuiEventListener> getWidgets() {
            List<GuiEventListener> list = new ArrayList<>(firstNode.getWidgets());
            list.addAll(secondNode.getWidgets());
            return list;
        }

        @Override
        public EditorForm.FormRow createFormRow(String label, Font font, Runnable onFormChanged) {
            EditorForm.FormRow firstRow = firstNode.createFormRow("First", font, onFormChanged);
            EditorForm.FormRow secondRow = secondNode.createFormRow("Second", font, onFormChanged);

            return new EditorForm.FormRow() {
                private int x;
                private int y;
                private int totalHeight;

                @Override
                public void layout(int x, int y, int width, Font font) {
                    this.x = x;
                    this.y = y;
                    firstRow.layout(x, y + 20, width, font);
                    secondRow.layout(x, y + 20 + firstRow.getHeight() + 4, width, font);
                    this.totalHeight = 20 + firstRow.getHeight() + secondRow.getHeight() + 6;
                }

                @Override
                public void render(GuiGraphics gfx, Font font, int mouseX, int mouseY, float partialTicks) {
                    int indentX = x + depth * INDENT_STEP - 2;
                    drawTreeGuideline(gfx, x, y, totalHeight, depth);
                    int labelCol = !isUnset() ? EditorTheme.TEXT_GOLD : EditorTheme.TEXT_LABEL;
                    gfx.drawString(font, label, indentX + 4, y + 5, labelCol, false);
                    firstRow.render(gfx, font, mouseX, mouseY, partialTicks);
                    secondRow.render(gfx, font, mouseX, mouseY, partialTicks);
                }

                @Override
                public int getHeight() {
                    return totalHeight;
                }

                @Override
                public int getY() {
                    return y;
                }
            };
        }
    }

    public static class OpaqueNode implements SchemaFormNode {
        private final ModernEditBox box;
        private final int depth;
        private boolean suppressEvents = false;

        public OpaqueNode(Font font, Runnable onFormChanged, int depth) {
            this.depth = depth;
            this.box = new ModernEditBox(font, 0, 0, 120, 18, Component.literal("Raw"));
            this.box.setMaxLength(2048);
            this.box.setResponder(s -> {
                if (!suppressEvents) onFormChanged.run();
            });
        }

        @Override
        public boolean isUnset() {
            return box.getValue().trim().isEmpty();
        }

        @Override
        public @Nullable JsonElement currentJson() {
            String text = box.getValue().trim();
            if (text.isEmpty()) return null;
            try {
                return JsonParser.parseString(text);
            } catch (JsonSyntaxException e) {
                return new JsonPrimitive(text);
            }
        }

        @Override
        public void setJson(@Nullable JsonElement value) {
            suppressEvents = true;
            try {
                if (value != null) {
                    box.setValue(value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                            ? value.getAsString()
                            : value.toString());
                } else {
                    box.setValue("");
                }
            } finally {
                suppressEvents = false;
            }
        }

        @Override
        public List<GuiEventListener> getWidgets() {
            return List.of(box);
        }

        @Override
        public EditorForm.FormRow createFormRow(String label, Font font, Runnable onFormChanged) {
            return new EditorForm.FormRow() {
                private int x;
                private int y;

                @Override
                public void layout(int x, int y, int width, Font font) {
                    this.x = x;
                    this.y = y;
                    int boxW = Math.min(160, (width - depth * INDENT_STEP) / 2);
                    box.setX(x + width - boxW);
                    box.setY(y);
                    box.setWidth(boxW);
                    box.setHeight(18);
                }

                @Override
                public void render(GuiGraphics gfx, Font font, int mouseX, int mouseY, float partialTicks) {
                    int indentX = x + depth * INDENT_STEP - 2;
                    drawTreeGuideline(gfx, x, y, 20, depth);
                    int labelCol = !isUnset() ? EditorTheme.TEXT_GOLD : EditorTheme.TEXT_LABEL;
                    gfx.drawString(font, label, indentX + 4, y + 5, labelCol, false);
                    box.render(gfx, mouseX, mouseY, partialTicks);
                }

                @Override
                public int getHeight() {
                    return 20;
                }

                @Override
                public int getY() {
                    return y;
                }
            };
        }
    }
}
