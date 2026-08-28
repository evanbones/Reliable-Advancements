package com.evandev.reliable_advancements.gui.tabs;

import com.evandev.reliable_advancements.gui.model.AdvancementDraft;
import com.evandev.reliable_advancements.gui.widgets.EditorForm;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;

import java.util.List;

public class LayoutTab implements IEditorTab {
    private final EditorForm form;
    private int posX, posY;
    private EditBox xBox, yBox;

    public LayoutTab(Font font, int posX, int posY) {
        this.form = new EditorForm(font);
        this.posX = posX;
        this.posY = posY;
    }

    @Override
    public void loadState(AdvancementDraft draft) {
    }

    @Override
    public void init(int x, int y, int width, int height, Runnable reinitScreen) {
        form.clear();
        form.addSection("Canvas Coordinates");
        xBox = form.addTextField("X Position", "Horizontal canvas column coordinate", String.valueOf(posX), s -> {
            try {
                this.posX = Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
            }
        });
        yBox = form.addTextField("Y Position", "Vertical canvas row coordinate", String.valueOf(posY), s -> {
            try {
                this.posY = Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
            }
        });
        form.init(x, y, width, height);
    }

    @Override
    public void syncFromWidgets() {
        try {
            if (xBox != null && !xBox.getValue().trim().isEmpty()) this.posX = Integer.parseInt(xBox.getValue().trim());
            if (yBox != null && !yBox.getValue().trim().isEmpty()) this.posY = Integer.parseInt(yBox.getValue().trim());
        } catch (NumberFormatException ignored) {
        }
    }

    @Override
    public void saveState(AdvancementDraft draft) {
        syncFromWidgets();
    }

    public int getX() {
        return posX;
    }

    public int getY() {
        return posY;
    }

    @Override
    public void render(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTicks) {
        form.render(gfx, mouseX, mouseY, partialTicks);
    }

    @Override
    public List<GuiEventListener> getWidgets() {
        return form.getWidgets();
    }

    @Override
    public EditorForm getForm() {
        return form;
    }
}
