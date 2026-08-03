package com.evandev.reliable_advancements.gui.tabs;

import com.evandev.reliable_advancements.gui.model.AdvancementDraft;
import com.evandev.reliable_advancements.gui.widgets.SuggestingEditBox;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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
    private EditBox idBox, titleBox, descriptionBox, parentBox;
    private SuggestingEditBox iconBox, frameBox;
    private Button hiddenBtn, telemetryBtn;

    private String id = "", title = "", description = "", icon = "minecraft:stone", parent = "", frame = "task";
    private boolean hidden = false;
    private boolean sendsTelemetryEvent = false;
    private int startX, startY;

    public PropertiesTab(Font font) {
        this.font = font;
    }

    @Override
    public void loadState(AdvancementDraft draft) {
        this.id = draft.id;
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

        try {
            this.frame = display.has("frame") ? display.get("frame").getAsString() : "task";
        } catch (Exception e) {
            this.frame = "task";
        }

        try {
            this.hidden = display.has("hidden") && display.get("hidden").getAsBoolean();
        } catch (Exception e) {
            this.hidden = false;
        }

        try {
            this.sendsTelemetryEvent = draft.rootJson.has("sends_telemetry_event") && draft.rootJson.get("sends_telemetry_event").getAsBoolean();
        } catch (Exception e) {
            this.sendsTelemetryEvent = false;
        }
    }

    @Override
    public void init(int x, int y, int width, int height, Runnable reinitScreen) {
        this.widgets.clear();
        this.startX = x;
        this.startY = y;

        idBox = new EditBox(font, x, y, width, 20, Component.literal("ID"));
        idBox.setMaxLength(256);
        idBox.setValue(id);

        titleBox = new EditBox(font, x, y + 45, width, 20, Component.literal("Title"));
        titleBox.setMaxLength(256);
        titleBox.setValue(title);

        descriptionBox = new EditBox(font, x, y + 90, width, 20, Component.literal("Description"));
        descriptionBox.setMaxLength(512);
        descriptionBox.setValue(description);

        iconBox = new SuggestingEditBox(font, x, y + 135, width, 20, Component.literal("Icon"),
                () -> BuiltInRegistries.ITEM.keySet().stream().map(ResourceLocation::toString).collect(Collectors.toList()));
        iconBox.setMaxLength(512);
        iconBox.setValue(icon);

        parentBox = new EditBox(font, x, y + 180, width, 20, Component.literal("Parent"));
        parentBox.setMaxLength(256);
        parentBox.setValue(parent);

        frameBox = new SuggestingEditBox(font, x, y + 225, width, 20, Component.literal("Frame"),
                () -> List.of("task", "goal", "challenge"));
        frameBox.setMaxLength(32);
        frameBox.setValue(frame);

        hiddenBtn = Button.builder(Component.literal("Hidden: " + hidden), b -> {
            hidden = !hidden;
            b.setMessage(Component.literal("Hidden: " + hidden));
        }).pos(x, y + 270).size(width / 2 - 5, 20).build();

        telemetryBtn = Button.builder(Component.literal("Telemetry: " + sendsTelemetryEvent), b -> {
            sendsTelemetryEvent = !sendsTelemetryEvent;
            b.setMessage(Component.literal("Telemetry: " + sendsTelemetryEvent));
        }).pos(x + width / 2 + 5, y + 270).size(width / 2 - 5, 20).build();

        widgets.addAll(List.of(idBox, titleBox, descriptionBox, iconBox, parentBox, frameBox, hiddenBtn, telemetryBtn));
    }

    @Override
    public void syncFromWidgets() {
        if (idBox != null) this.id = idBox.getValue();
        if (titleBox != null) this.title = titleBox.getValue();
        if (descriptionBox != null) this.description = descriptionBox.getValue();
        if (iconBox != null) this.icon = iconBox.getValue();
        if (parentBox != null) this.parent = parentBox.getValue();
        if (frameBox != null) this.frame = frameBox.getValue();
    }

    @Override
    public void saveState(AdvancementDraft draft) {
        if (idBox != null) draft.id = idBox.getValue();
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

        if (frameBox != null) {
            display.addProperty("frame", frameBox.getValue());
        }
        display.addProperty("hidden", hidden);

        draft.rootJson.add("display", display);

        draft.rootJson.addProperty("sends_telemetry_event", sendsTelemetryEvent);

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
        gfx.drawString(font, "ID", startX, startY - 11, 0xFFA08060, false);
        gfx.drawString(font, "Title", startX, startY + 34, 0xFFA08060, false);
        gfx.drawString(font, "Description", startX, startY + 79, 0xFFA08060, false);
        gfx.drawString(font, "Icon (Item ID)", startX, startY + 124, 0xFFA08060, false);
        gfx.drawString(font, "Parent ID", startX, startY + 169, 0xFFA08060, false);
        gfx.drawString(font, "Frame", startX, startY + 214, 0xFFA08060, false);
    }

    @Override
    public List<GuiEventListener> getWidgets() {
        return widgets;
    }
}