package com.evandev.better_advancements.gui.tabs;

import com.evandev.better_advancements.gui.model.AdvancementDraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;

import java.util.List;

public interface IEditorTab {
    void init(int x, int y, int width, int height, Runnable reinitScreen);

    void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks);

    void saveState(AdvancementDraft draft);

    void loadState(AdvancementDraft draft);

    List<GuiEventListener> getWidgets();
}