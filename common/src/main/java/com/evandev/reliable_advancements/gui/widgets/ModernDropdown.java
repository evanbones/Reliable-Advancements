package com.evandev.reliable_advancements.gui.widgets;

import com.evandev.reliable_advancements.gui.theme.EditorTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ModernDropdown extends AbstractWidget {
    private static final int ITEM_HEIGHT = 18;
    private static final int MAX_VISIBLE_ITEMS = 8;
    private final Font font;
    private final List<Option> options = new ArrayList<>();
    private final Consumer<Option> onSelect;
    private int selectedIndex;
    private boolean isOpen = false;
    private int scrollOffset = 0;

    public ModernDropdown(Font font, int x, int y, int width, int height, List<Option> options, int initialIndex, Consumer<Option> onSelect) {
        super(x, y, width, height, Component.empty());
        this.font = font;
        this.options.addAll(options);
        this.selectedIndex = Mth.clamp(initialIndex, 0, Math.max(0, options.size() - 1));
        this.onSelect = onSelect;
    }

    public void setOptions(List<Option> newOptions, String selectedId) {
        this.options.clear();
        this.options.addAll(newOptions);
        this.selectedIndex = 0;
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).id().equals(selectedId)) {
                this.selectedIndex = i;
                break;
            }
        }
    }

    public void setSelectedId(String id) {
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).id().equals(id)) {
                this.selectedIndex = i;
                break;
            }
        }
    }

    public Option getSelectedOption() {
        if (selectedIndex >= 0 && selectedIndex < options.size()) {
            return options.get(selectedIndex);
        }
        return null;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void setOpen(boolean open) {
        this.isOpen = open;
    }

    public int getOptimalWidth() {
        int maxW = 50;
        for (Option opt : options) {
            maxW = Math.max(maxW, font.width(opt.label()));
        }
        return Math.max(70, maxW + 28);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (!isOpen) {
            isOpen = true;
            scrollOffset = 0;
        } else {
            isOpen = false;
        }
    }

    public boolean handleDropdownClick(double mouseX, double mouseY, int button) {
        if (!isOpen || button != 0) return false;

        if (mouseX >= getX() && mouseX <= getX() + width && mouseY >= getY() && mouseY <= getY() + height) {
            this.isOpen = false;
            return true;
        }

        int dropX = getX();
        int dropY = getY() + height + 1;
        int dropW = Math.max(width, getOptimalWidth());
        int visibleCount = Math.min(options.size(), MAX_VISIBLE_ITEMS);
        int dropH = visibleCount * ITEM_HEIGHT + 4;

        if (mouseX >= dropX && mouseX <= dropX + dropW && mouseY >= dropY && mouseY <= dropY + dropH) {
            int clickedIdx = (int) ((mouseY - dropY - 2) / ITEM_HEIGHT) + scrollOffset;
            if (clickedIdx >= 0 && clickedIdx < options.size()) {
                this.selectedIndex = clickedIdx;
                this.isOpen = false;
                if (this.onSelect != null) {
                    this.onSelect.accept(options.get(clickedIdx));
                }
            }
            return true;
        }

        isOpen = false;
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isOpen && options.size() > MAX_VISIBLE_ITEMS) {
            int dropX = getX();
            int dropY = getY();
            int dropW = Math.max(width, getOptimalWidth());
            int dropH = height + 1 + MAX_VISIBLE_ITEMS * ITEM_HEIGHT + 4;

            if (mouseX >= dropX && mouseX <= dropX + dropW && mouseY >= dropY && mouseY <= dropY + dropH) {
                int maxScroll = options.size() - MAX_VISIBLE_ITEMS;
                scrollOffset = Mth.clamp(scrollOffset - (int) Math.signum(scrollY), 0, maxScroll);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        boolean hovered = isHoveredOrFocused();

        gfx.fill(getX(), getY(), getX() + width, getY() + height, hovered ? 0xFF2A3042 : 0xF4181B26);
        gfx.renderOutline(getX(), getY(), width, height, isOpen ? EditorTheme.BORDER_FOCUSED : (hovered ? EditorTheme.ACCENT_GOLD_MUTED : EditorTheme.BORDER_INNER));

        Option selected = getSelectedOption();
        String label = selected != null ? selected.label() : "Select...";
        int textCol = hovered ? EditorTheme.TEXT_GOLD : EditorTheme.TEXT_PRIMARY;
        gfx.drawString(font, font.plainSubstrByWidth(label, width - 24), getX() + 6, getY() + (height - 8) / 2, textCol, false);

        EditorTheme.drawChevronIcon(gfx, getX() + width - 16, getY() + (height - 16) / 2, isOpen, hovered);
    }

    public void renderDropdownPopup(GuiGraphics gfx, int mouseX, int mouseY) {
        if (!isOpen || options.isEmpty()) return;

        int dropX = getX();
        int dropY = getY() + height + 1;
        int dropW = Math.max(width, getOptimalWidth());
        int visibleCount = Math.min(options.size(), MAX_VISIBLE_ITEMS);
        int dropH = visibleCount * ITEM_HEIGHT + 4;

        gfx.pose().pushPose();
        gfx.pose().translate(0, 0, 500);

        gfx.fill(dropX, dropY, dropX + dropW, dropY + dropH, 0xF4141620);
        gfx.renderOutline(dropX, dropY, dropW, dropH, EditorTheme.ACCENT_GOLD_MUTED);

        for (int i = 0; i < visibleCount; i++) {
            int optIdx = i + scrollOffset;
            if (optIdx >= options.size()) break;

            int itemY = dropY + 2 + i * ITEM_HEIGHT;
            boolean itemHovered = mouseX >= dropX && mouseX <= dropX + dropW && mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT;
            boolean isSelected = (optIdx == selectedIndex);

            if (itemHovered) {
                gfx.fill(dropX + 1, itemY, dropX + dropW - 1, itemY + ITEM_HEIGHT, 0xFF2A3042);
            } else if (isSelected) {
                gfx.fill(dropX + 1, itemY, dropX + dropW - 1, itemY + ITEM_HEIGHT, 0xFF1E2333);
            }

            int textColor = isSelected ? EditorTheme.TEXT_GOLD : (itemHovered ? EditorTheme.TEXT_PRIMARY : EditorTheme.TEXT_LABEL);
            Option opt = options.get(optIdx);
            gfx.drawString(font, font.plainSubstrByWidth(opt.label(), dropW - 12), dropX + 6, itemY + 5, textColor, false);
        }

        if (options.size() > MAX_VISIBLE_ITEMS) {
            int maxScroll = options.size() - MAX_VISIBLE_ITEMS;
            int barH = dropH - 4;
            int thumbH = Math.max(8, barH * MAX_VISIBLE_ITEMS / options.size());
            int thumbY = dropY + 2 + (barH - thumbH) * scrollOffset / maxScroll;
            gfx.fill(dropX + dropW - 3, dropY + 2, dropX + dropW - 1, dropY + dropH - 2, 0x33FFFFFF);
            gfx.fill(dropX + dropW - 3, thumbY, dropX + dropW - 1, thumbY + thumbH, EditorTheme.ACCENT_GOLD);
        }

        gfx.pose().popPose();
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
    }

    public record Option(String id, String label) {
    }
}
