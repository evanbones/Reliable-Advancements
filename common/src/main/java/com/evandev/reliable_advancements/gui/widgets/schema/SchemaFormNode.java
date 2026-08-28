package com.evandev.reliable_advancements.gui.widgets.schema;

import com.evandev.reliable_advancements.gui.widgets.EditorForm;
import com.google.gson.JsonElement;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface SchemaFormNode {
    @Nullable
    JsonElement currentJson();

    void setJson(@Nullable JsonElement value);

    List<GuiEventListener> getWidgets();

    EditorForm.FormRow createFormRow(String label, Font font, Runnable onFormChanged);

    default boolean isUnset() {
        JsonElement current = currentJson();
        return current == null || current.isJsonNull();
    }
}
