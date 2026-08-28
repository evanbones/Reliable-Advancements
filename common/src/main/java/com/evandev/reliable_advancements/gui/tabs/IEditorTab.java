package com.evandev.reliable_advancements.gui.tabs;

import com.evandev.reliable_advancements.gui.model.AdvancementDraft;
import com.evandev.reliable_advancements.gui.widgets.EditorForm;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface IEditorTab {
    void init(int x, int y, int width, int height, Runnable reinitScreen);

    void render(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTicks);

    default void renderOverlay(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTicks) {
        if (getForm() != null) {
            getForm().renderOverlay(gfx, mouseX, mouseY, partialTicks);
        }
    }

    void saveState(AdvancementDraft draft);

    void loadState(AdvancementDraft draft);

    default void syncFromWidgets() {
    }

    default @Nullable EditorForm getForm() {
        return null;
    }

    List<GuiEventListener> getWidgets();
}