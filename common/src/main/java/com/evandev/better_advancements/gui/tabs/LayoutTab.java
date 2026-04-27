package com.evandev.better_advancements.gui.tabs;

import com.evandev.better_advancements.gui.BetterAdvancementWidget;
import com.evandev.better_advancements.gui.model.AdvancementDraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class LayoutTab implements IEditorTab {
    private final Font font;
    private final BetterAdvancementWidget widget;
    private final List<GuiEventListener> widgets = new ArrayList<>();
    private EditBox xBox, yBox;
    private int startX, startY;

    public LayoutTab(Font font, BetterAdvancementWidget widget) {
        this.font = font;
        this.widget = widget;
    }

    @Override
    public void loadState(AdvancementDraft draft) {
    }

    @Override
    public void init(int x, int y, int width, int height, Runnable reinitScreen) {
        this.widgets.clear();
        this.startX = x;
        this.startY = y;

        xBox = new EditBox(font, x, y, 100, 20, Component.literal("X"));
        xBox.setValue(String.valueOf(widget.getX()));

        yBox = new EditBox(font, x, y + 45, 100, 20, Component.literal("Y"));
        yBox.setValue(String.valueOf(widget.getY()));

        widgets.addAll(List.of(xBox, yBox));
    }

    @Override
    public void saveState(AdvancementDraft draft) {
        try {
            if (xBox != null) widget.setX(Integer.parseInt(xBox.getValue()));
            if (yBox != null) widget.setY(Integer.parseInt(yBox.getValue()));
        } catch (NumberFormatException ignored) {
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        gfx.drawString(font, "X Position", startX, startY - 11, 0xFFA08060, false);
        gfx.drawString(font, "Y Position", startX, startY + 34, 0xFFA08060, false);
    }

    @Override
    public List<GuiEventListener> getWidgets() {
        return widgets;
    }
}