package com.evandev.reliable_advancements.gui.tabs;

import com.evandev.reliable_advancements.gui.model.AdvancementDraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;

import java.util.List;

public interface IEditorTab {
    void init(int x, int y, int width, int height, Runnable reinitScreen);

    void render(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTicks);

    void saveState(AdvancementDraft draft);

    void loadState(AdvancementDraft draft);

    List<GuiEventListener> getWidgets();
}