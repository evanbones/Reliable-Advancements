package com.evandev.better_advancements.gui.tabs;

import com.evandev.better_advancements.gui.model.AdvancementDraft;
import com.evandev.better_advancements.gui.widgets.SuggestingEditBox;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PropertiesTab implements IEditorTab {
    private final Font font;
    private final List<GuiEventListener> widgets = new ArrayList<>();
    private EditBox titleBox, descriptionBox, parentBox;
    private SuggestingEditBox iconBox;

    private String title = "", description = "", icon = "minecraft:stone", parent = "";
    private int startX, startY;

    public PropertiesTab(Font font) {
        this.font = font;
    }

    @Override
    public void loadState(AdvancementDraft draft) {
        JsonObject display = draft.rootJson.has("display") ? draft.rootJson.getAsJsonObject("display") : new JsonObject();
        RegistryOps<JsonElement> ops = Minecraft.getInstance().level.registryAccess().createSerializationContext(JsonOps.INSTANCE);

        try {
            if (display.has("title")) {
                Component titleComp = ComponentSerialization.CODEC.parse(ops, display.get("title")).result().orElse(Component.empty());
                this.title = titleComp.getString();

                if (this.title.isEmpty() && display.get("title").isJsonPrimitive()) {
                    this.title = display.get("title").getAsString();
                }
            }
        } catch (Exception e) {
            this.title = "";
        }

        try {
            if (display.has("description")) {
                Component descComp = ComponentSerialization.CODEC.parse(ops, display.get("description")).result().orElse(Component.empty());
                this.description = descComp.getString();

                if (this.description.isEmpty() && display.get("description").isJsonPrimitive()) {
                    this.description = display.get("description").getAsString();
                }
            }
        } catch (Exception e) {
            this.description = "";
        }

        try {
            this.icon = display.has("icon") && display.getAsJsonObject("icon").has("id")
                    ? display.getAsJsonObject("icon").get("id").getAsString() : "minecraft:stone";
        } catch (Exception e) {
            this.icon = "minecraft:stone";
        }

        try {
            this.parent = draft.rootJson.has("parent") ? draft.rootJson.get("parent").getAsString() : "";
        } catch (Exception e) {
            this.parent = "";
        }
    }

    @Override
    public void init(int x, int y, int width, int height, Runnable reinitScreen) {
        this.widgets.clear();
        this.startX = x;
        this.startY = y;

        titleBox = new EditBox(font, x, y, width, 20, Component.literal("Title"));
        titleBox.setMaxLength(256);
        titleBox.setValue(title);

        descriptionBox = new EditBox(font, x, y + 45, width, 20, Component.literal("Description"));
        descriptionBox.setMaxLength(512);
        descriptionBox.setValue(description);

        iconBox = new SuggestingEditBox(font, x, y + 90, width, 20, Component.literal("Icon"),
                () -> BuiltInRegistries.ITEM.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList()));
        iconBox.setValue(icon);

        parentBox = new EditBox(font, x, y + 135, width, 20, Component.literal("Parent"));
        parentBox.setMaxLength(256);
        parentBox.setValue(parent);

        widgets.addAll(List.of(titleBox, descriptionBox, iconBox, parentBox));
    }

    @Override
    public void saveState(AdvancementDraft draft) {
        JsonObject display = draft.rootJson.has("display") ? draft.rootJson.getAsJsonObject("display") : new JsonObject();

        if (titleBox != null) {
            JsonObject titleObj = new JsonObject();
            titleObj.addProperty("text", titleBox.getValue());
            display.add("title", titleObj);
        }
        if (descriptionBox != null) {
            JsonObject descObj = new JsonObject();
            descObj.addProperty("text", descriptionBox.getValue());
            display.add("description", descObj);
        }

        if (iconBox != null) {
            JsonObject iconObj = display.has("icon") ? display.getAsJsonObject("icon") : new JsonObject();
            iconObj.addProperty("id", iconBox.getValue());
            display.add("icon", iconObj);
        }
        draft.rootJson.add("display", display);

        if (parentBox != null) {
            if (parentBox.getValue().isEmpty()) {
                draft.rootJson.remove("parent");
            } else {
                draft.rootJson.addProperty("parent", parentBox.getValue());
            }
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        gfx.drawString(font, "Title", startX, startY - 11, 0xFFA08060, false);
        gfx.drawString(font, "Description", startX, startY + 34, 0xFFA08060, false);
        gfx.drawString(font, "Icon (Item ID)", startX, startY + 79, 0xFFA08060, false);
        gfx.drawString(font, "Parent ID", startX, startY + 124, 0xFFA08060, false);
    }

    @Override
    public List<GuiEventListener> getWidgets() {
        return widgets;
    }
}