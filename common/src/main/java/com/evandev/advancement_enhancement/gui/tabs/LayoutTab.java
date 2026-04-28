package com.evandev.advancement_enhancement.gui.tabs;

import com.evandev.advancement_enhancement.gui.model.AdvancementDraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class LayoutTab implements IEditorTab {
    private final Font font;
    private final List<GuiEventListener> widgets = new ArrayList<>();
    private int posX, posY;
    private EditBox xBox, yBox;
    private int startX, startY;

    public LayoutTab(Font font, int posX, int posY) {
        this.font = font;
        this.posX = posX;
        this.posY = posY;
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
        xBox.setValue(String.valueOf(posX));

        yBox = new EditBox(font, x, y + 45, 100, 20, Component.literal("Y"));
        yBox.setValue(String.valueOf(posY));

        widgets.addAll(List.of(xBox, yBox));
    }

    @Override
    public void saveState(AdvancementDraft draft) {
        try {
            if (xBox != null) this.posX = Integer.parseInt(xBox.getValue());
            if (yBox != null) this.posY = Integer.parseInt(yBox.getValue());
        } catch (NumberFormatException ignored) {
        }
    }

    public int getX() {
        return posX;
    }

    public int getY() {
        return posY;
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