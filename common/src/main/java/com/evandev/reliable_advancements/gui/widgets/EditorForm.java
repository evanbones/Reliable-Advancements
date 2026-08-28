package com.evandev.reliable_advancements.gui.widgets;

import com.evandev.reliable_advancements.gui.theme.EditorTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class EditorForm {
    private final Font font;
    private final List<FormRow> rows = new ArrayList<>();
    private final List<GuiEventListener> widgets = new ArrayList<>();

    private int x, y, width, height;
    private int scrollOffset = 0;
    private int totalContentHeight = 0;
    private int maxScroll = 0;
    private boolean isDraggingScrollbar = false;

    public EditorForm(Font font) {
        this.font = font;
    }

    public void clear() {
        this.rows.clear();
        this.widgets.clear();
    }

    public ModernEditBox addTextField(String label, String initialValue, Consumer<String> responder) {
        return addTextField(label, null, initialValue, responder);
    }

    public ModernEditBox addTextField(String label, @Nullable String tooltip, String initialValue, Consumer<String> responder) {
        ModernEditBox box = new ModernEditBox(font, 0, 0, 100, 20, Component.literal(label));
        box.setMaxLength(512);
        box.setValue(initialValue != null ? initialValue : "");
        box.setCursorPosition(0);
        box.setHighlightPos(0);
        box.setResponder(responder);
        if (tooltip != null && !tooltip.isEmpty()) {
            box.setTooltip(Tooltip.create(Component.literal(tooltip)));
        }
        FieldRow row = new FieldRow(label, box, 20);
        rows.add(row);
        widgets.add(box);
        return box;
    }

    public SuggestingEditBox addSuggestingField(String label, String initialValue, Supplier<List<String>> suggestions, Consumer<String> responder) {
        return addSuggestingField(label, null, initialValue, suggestions, responder);
    }

    public SuggestingEditBox addSuggestingField(String label, @Nullable String tooltip, String initialValue, Supplier<List<String>> suggestions, Consumer<String> responder) {
        SuggestingEditBox box = new SuggestingEditBox(font, 0, 0, 100, 20, Component.literal(label), suggestions);
        box.setMaxLength(512);
        box.setValue(initialValue != null ? initialValue : "");
        box.setCursorPosition(0);
        box.setHighlightPos(0);
        box.setResponder(responder);
        if (tooltip != null && !tooltip.isEmpty()) {
            box.setTooltip(Tooltip.create(Component.literal(tooltip)));
        }
        FieldRow row = new FieldRow(label, box, 20);
        rows.add(row);
        widgets.add(box);
        return box;
    }

    public SuggestingEditBox addItemSuggestingField(String label, String initialValue, Consumer<String> responder) {
        return addItemSuggestingField(label, null, initialValue, responder);
    }

    public SuggestingEditBox addItemSuggestingField(String label, @Nullable String tooltip, String initialValue, Consumer<String> responder) {
        return addItemSuggestingField(label, tooltip, initialValue,
                () -> BuiltInRegistries.ITEM.keySet().stream().map(Identifier::toString).toList(),
                responder);
    }

    public SuggestingEditBox addItemSuggestingField(String label, @Nullable String tooltip, String initialValue, Supplier<List<String>> suggestions, Consumer<String> responder) {
        SuggestingEditBox box = new SuggestingEditBox(font, 0, 0, 100, 20, Component.literal(label), suggestions, SuggestingEditBox::defaultItemIconResolver);
        box.setMaxLength(512);
        box.setValue(initialValue != null ? initialValue : "");
        box.setCursorPosition(0);
        box.setHighlightPos(0);
        box.setResponder(responder);
        if (tooltip != null && !tooltip.isEmpty()) {
            box.setTooltip(Tooltip.create(Component.literal(tooltip)));
        }
        ItemFieldRow row = new ItemFieldRow(label, box, 20);
        rows.add(row);
        widgets.add(box);
        return box;
    }

    public ModernButton addToggle(String label, @Nullable String tooltip, boolean initialValue, Consumer<Boolean> onChange) {
        final boolean[] state = new boolean[]{initialValue};
        ModernButton btn = ModernButton.modernBuilder(
                        Component.literal(state[0] ? "ON" : "OFF"),
                        b -> {
                            state[0] = !state[0];
                            b.setMessage(Component.literal(state[0] ? "ON" : "OFF"));
                            if (b instanceof ModernButton mb) {
                                mb.setStyle(state[0] ? ModernButton.Style.TOGGLE_ON : ModernButton.Style.TOGGLE_OFF);
                            }
                            onChange.accept(state[0]);
                        })
                .style(initialValue ? ModernButton.Style.TOGGLE_ON : ModernButton.Style.TOGGLE_OFF)
                .pos(0, 0).size(60, 20)
                .tooltip(tooltip != null && !tooltip.isEmpty() ? Tooltip.create(Component.literal(tooltip)) : null)
                .build();

        ToggleRow row = new ToggleRow(label, btn);
        rows.add(row);
        widgets.add(btn);
        return btn;
    }

    public void addSection(String title) {
        rows.add(new SectionRow(title));
    }

    public void addCustomWidget(String label, AbstractWidget widget, int rowHeight) {
        rows.add(new CustomWidgetRow(label, widget, rowHeight));
        widgets.add(widget);
    }

    public void addCustomRow(FormRow row, List<GuiEventListener> rowWidgets) {
        rows.add(row);
        widgets.addAll(rowWidgets);
    }

    public void init(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        int currentY = y - scrollOffset;
        int fieldW = width - 18;

        for (FormRow row : rows) {
            row.layout(x, currentY, fieldW, font);
            currentY += row.getHeight() + 10;
        }

        this.totalContentHeight = (currentY + scrollOffset) - y;
        this.maxScroll = Math.max(0, totalContentHeight - height);
        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
            updateWidgetPositions();
        }
    }

    public void updateWidgetPositions() {
        int currentY = y - scrollOffset;
        int fieldW = width - 18;
        for (FormRow row : rows) {
            row.layout(x, currentY, fieldW, font);
            currentY += row.getHeight() + 10;
        }
        this.totalContentHeight = (currentY + scrollOffset) - y;
        this.maxScroll = Math.max(0, totalContentHeight - height);
        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
            currentY = y - scrollOffset;
            for (FormRow row : rows) {
                row.layout(x, currentY, fieldW, font);
                currentY += row.getHeight() + 10;
            }
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (maxScroll <= 0) return false;
        int prevOffset = scrollOffset;
        scrollOffset -= (int) (scrollY * 24);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        if (scrollOffset != prevOffset) {
            updateWidgetPositions();
            return true;
        }
        return false;
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && maxScroll > 0) {
            int scrollX = x + width - 10;
            if (mx >= scrollX && mx <= scrollX + 12 && my >= y && my <= y + height) {
                isDraggingScrollbar = true;
                updateScrollFromMouse(my);
                return true;
            }
        }
        return false;
    }

    public boolean mouseDragged(double mx, double my, int button, double dragX, double dragY) {
        if (isDraggingScrollbar && button == 0) {
            updateScrollFromMouse(my);
            return true;
        }
        return false;
    }

    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0 && isDraggingScrollbar) {
            isDraggingScrollbar = false;
            return true;
        }
        return false;
    }

    private void updateScrollFromMouse(double my) {
        int thumbH = Math.max(20, height * height / Math.max(1, totalContentHeight));
        int trackH = height - thumbH;
        if (trackH <= 0) return;
        double pct = (my - y - thumbH / 2.0) / trackH;
        pct = Math.max(0, Math.min(pct, 1));
        this.scrollOffset = (int) (pct * maxScroll);
        updateWidgetPositions();
    }

    public void render(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTicks) {
        for (FormRow row : rows) {
            if (row.getY() + row.getHeight() >= y && row.getY() <= y + height) {
                row.render(gfx, font, mouseX, mouseY, partialTicks);
            }
        }

        if (maxScroll > 0) {
            int scrollX = x + width - 6;
            int scrollH = height;
            int thumbH = Math.max(16, scrollH * scrollH / Math.max(1, totalContentHeight));
            int thumbY = y + (int) ((scrollH - thumbH) * (scrollOffset / (float) maxScroll));

            boolean hovered = mouseX >= scrollX - 3 && mouseX <= scrollX + 8 && mouseY >= y && mouseY <= y + height;
            gfx.fill(scrollX, y, scrollX + 5, y + scrollH, EditorTheme.SCROLL_TRACK);
            gfx.fill(scrollX, thumbY, scrollX + 5, thumbY + thumbH, hovered || isDraggingScrollbar ? EditorTheme.SCROLL_THUMB_HOVER : EditorTheme.SCROLL_THUMB);
        }
    }

    public void renderOverlay(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTicks) {
        for (FormRow row : rows) {
            if (row.getY() + row.getHeight() >= y && row.getY() <= y + height) {
                row.renderOverlay(gfx, font, mouseX, mouseY, partialTicks);
            }
        }
    }

    public List<GuiEventListener> getWidgets() {
        return widgets;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getScrollOffset() {
        return scrollOffset;
    }

    public interface FormRow {
        void layout(int x, int y, int width, Font font);

        void render(GuiGraphicsExtractor gfx, Font font, int mouseX, int mouseY, float partialTicks);

        default void renderOverlay(GuiGraphicsExtractor gfx, Font font, int mouseX, int mouseY, float partialTicks) {
        }

        int getHeight();

        int getY();
    }

    public static class SectionRow implements FormRow {
        private final String title;
        private int x, y, width;

        public SectionRow(String title) {
            this.title = title;
        }

        @Override
        public void layout(int x, int y, int width, Font font) {
            this.x = x;
            this.y = y;
            this.width = width;
        }

        @Override
        public void render(GuiGraphicsExtractor gfx, Font font, int mouseX, int mouseY, float partialTicks) {
            EditorTheme.drawSectionHeader(gfx, font, title, x, y + 4, width);
        }

        @Override
        public int getHeight() {
            return 18;
        }

        @Override
        public int getY() {
            return y;
        }
    }

    public static class FieldRow implements FormRow {
        private final String label;
        private final AbstractWidget widget;
        private final int widgetHeight;
        private int x;
        private int y;

        public FieldRow(String label, AbstractWidget widget, int widgetHeight) {
            this.label = label;
            this.widget = widget;
            this.widgetHeight = widgetHeight;
        }

        @Override
        public void layout(int x, int y, int width, Font font) {
            this.x = x;
            this.y = y;
            int labelH = (label != null && !label.isEmpty()) ? 12 : 0;
            this.widget.setX(x);
            this.widget.setY(y + labelH);
            this.widget.setWidth(width);
            this.widget.setHeight(widgetHeight);
        }

        @Override
        public void render(GuiGraphicsExtractor gfx, Font font, int mouseX, int mouseY, float partialTicks) {
            if (label != null && !label.isEmpty()) {
                gfx.text(font, label, x, y, EditorTheme.TEXT_LABEL, false);
            }
            this.widget.extractRenderState(gfx, mouseX, mouseY, partialTicks);
        }

        @Override
        public int getHeight() {
            return ((label != null && !label.isEmpty()) ? 12 : 0) + widgetHeight;
        }

        @Override
        public int getY() {
            return y;
        }
    }

    public static class ItemFieldRow implements FormRow {
        private final String label;
        private final SuggestingEditBox widget;
        private final int widgetHeight;
        private int x, y, width;

        public ItemFieldRow(String label, SuggestingEditBox widget, int widgetHeight) {
            this.label = label;
            this.widget = widget;
            this.widgetHeight = widgetHeight;
        }

        @Override
        public void layout(int x, int y, int width, Font font) {
            this.x = x;
            this.y = y;
            this.width = width;
            int labelH = (label != null && !label.isEmpty()) ? 12 : 0;
            int iconSlotSize = 20;
            int spacing = 4;
            this.widget.setX(x);
            this.widget.setY(y + labelH);
            this.widget.setWidth(width - iconSlotSize - spacing);
            this.widget.setHeight(widgetHeight);
        }

        @Override
        public void render(GuiGraphicsExtractor gfx, Font font, int mouseX, int mouseY, float partialTicks) {
            int labelH = (label != null && !label.isEmpty()) ? 12 : 0;
            if (label != null && !label.isEmpty()) {
                gfx.text(font, label, x, y, EditorTheme.TEXT_LABEL, false);
            }
            this.widget.extractRenderState(gfx, mouseX, mouseY, partialTicks);

            int iconSlotSize = 20;
            int prevX = x + width - iconSlotSize;
            int prevY = y + labelH;

            gfx.fill(prevX, prevY, prevX + iconSlotSize, prevY + iconSlotSize, 0xF411131A);
            gfx.outline(prevX, prevY, iconSlotSize, iconSlotSize, EditorTheme.BORDER_INNER);

            String val = widget.getValue().trim();
            ItemStack stack = ItemStack.EMPTY;
            if (!val.isEmpty()) {
                Identifier loc = Identifier.tryParse(val);
                if (loc != null && BuiltInRegistries.ITEM.containsKey(loc)) {
                    Item item = BuiltInRegistries.ITEM.getValue(loc);
                    if (item != Items.AIR) {
                        stack = new ItemStack(item);
                    }
                }
            }

            if (!stack.isEmpty()) {
                gfx.fakeItem(stack, prevX + 2, prevY + 2);
                if (mouseX >= prevX && mouseX <= prevX + iconSlotSize && mouseY >= prevY && mouseY <= prevY + iconSlotSize) {
                    gfx.outline(prevX, prevY, iconSlotSize, iconSlotSize, EditorTheme.ACCENT_GOLD_MUTED);
                }
            }
        }

        @Override
        public void renderOverlay(GuiGraphicsExtractor gfx, Font font, int mouseX, int mouseY, float partialTicks) {
            int labelH = (label != null && !label.isEmpty()) ? 12 : 0;
            int iconSlotSize = 20;
            int prevX = x + width - iconSlotSize;
            int prevY = y + labelH;

            String val = widget.getValue().trim();
            ItemStack stack = ItemStack.EMPTY;
            if (!val.isEmpty()) {
                Identifier loc = Identifier.tryParse(val);
                if (loc != null && BuiltInRegistries.ITEM.containsKey(loc)) {
                    Item item = BuiltInRegistries.ITEM.getValue(loc);
                    if (item != Items.AIR) {
                        stack = new ItemStack(item);
                    }
                }
            }

            if (!stack.isEmpty() && mouseX >= prevX && mouseX <= prevX + iconSlotSize && mouseY >= prevY && mouseY <= prevY + iconSlotSize) {
                gfx.setTooltipForNextFrame(font, stack, mouseX, mouseY);
            }
        }

        @Override
        public int getHeight() {
            return ((label != null && !label.isEmpty()) ? 12 : 0) + widgetHeight;
        }

        @Override
        public int getY() {
            return y;
        }
    }

    public static class ToggleRow implements FormRow {
        private final String label;
        private final Button button;
        private int x;
        private int y;

        public ToggleRow(String label, Button button) {
            this.label = label;
            this.button = button;
        }

        @Override
        public void layout(int x, int y, int width, Font font) {
            this.x = x;
            this.y = y;
            int btnW = 60;
            this.button.setX(x + width - btnW);
            this.button.setY(y);
            this.button.setWidth(btnW);
            this.button.setHeight(20);
        }

        @Override
        public void render(GuiGraphicsExtractor gfx, Font font, int mouseX, int mouseY, float partialTicks) {
            gfx.text(font, label, x, y + 6, EditorTheme.TEXT_LABEL, false);
            this.button.extractRenderState(gfx, mouseX, mouseY, partialTicks);
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

    public static class CustomWidgetRow implements FormRow {
        private final String label;
        private final AbstractWidget widget;
        private final int height;
        private int x;
        private int y;

        public CustomWidgetRow(String label, AbstractWidget widget, int height) {
            this.label = label;
            this.widget = widget;
            this.height = height;
        }

        @Override
        public void layout(int x, int y, int width, Font font) {
            this.x = x;
            this.y = y;
            int labelH = (label != null && !label.isEmpty()) ? 12 : 0;
            this.widget.setX(x);
            this.widget.setY(y + labelH);
            this.widget.setWidth(width);
            this.widget.setHeight(height - labelH);
        }

        @Override
        public void render(GuiGraphicsExtractor gfx, Font font, int mouseX, int mouseY, float partialTicks) {
            if (label != null && !label.isEmpty()) {
                gfx.text(font, label, x, y, EditorTheme.TEXT_LABEL, false);
            }
            this.widget.extractRenderState(gfx, mouseX, mouseY, partialTicks);
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public int getY() {
            return y;
        }
    }

    public static class DynamicEntryRow implements FormRow {
        private final AbstractWidget widget;
        private final Button removeBtn;
        private int y;

        public DynamicEntryRow(AbstractWidget widget, Button removeBtn) {
            this.widget = widget;
            this.removeBtn = removeBtn;
        }

        @Override
        public void layout(int x, int y, int width, Font font) {
            this.y = y;
            this.widget.setX(x);
            this.widget.setY(y);
            this.widget.setWidth(width - 24);
            this.widget.setHeight(20);

            this.removeBtn.setX(x + width - 20);
            this.removeBtn.setY(y);
            this.removeBtn.setWidth(20);
            this.removeBtn.setHeight(20);
        }

        @Override
        public void render(GuiGraphicsExtractor gfx, Font font, int mouseX, int mouseY, float partialTicks) {
            this.widget.extractRenderState(gfx, mouseX, mouseY, partialTicks);
            this.removeBtn.extractRenderState(gfx, mouseX, mouseY, partialTicks);
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
}
