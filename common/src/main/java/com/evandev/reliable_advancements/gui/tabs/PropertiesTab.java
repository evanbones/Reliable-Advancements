package com.evandev.reliable_advancements.gui.tabs;

import com.evandev.reliable_advancements.advancements.MultiParentHelper;
import com.evandev.reliable_advancements.gui.model.AdvancementDraft;
import com.evandev.reliable_advancements.gui.widgets.EditorForm;
import com.evandev.reliable_advancements.gui.widgets.ModernButton;
import com.evandev.reliable_advancements.gui.widgets.SuggestingEditBox;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PropertiesTab implements IEditorTab {
    private final EditorForm form;
    private final List<String> parentList = new ArrayList<>();
    private final List<SuggestingEditBox> parentBoxes = new ArrayList<>();
    private String id = "";
    private String title = "";
    private String description = "";
    private String icon = "minecraft:stone";
    private String frame = "task";
    private boolean hidden = false;
    private boolean sendsTelemetryEvent = false;

    private EditBox idBox;
    private EditBox titleBox;
    private EditBox descBox;
    private SuggestingEditBox iconBox;
    private SuggestingEditBox frameBox;

    public PropertiesTab(Font font) {
        this.form = new EditorForm(font);
    }

    public static List<String> getParentSuggestions() {
        var conn = Minecraft.getInstance().getConnection();
        if (conn != null) {
            return conn.getAdvancements().getTree().nodes().stream()
                    .map(n -> n.holder().id().toString()).sorted().collect(Collectors.toList());
        }
        return List.of();
    }

    @Override
    public void loadState(AdvancementDraft draft) {
        this.id = draft.id;
        this.parentList.clear();

        JsonObject display = draft.rootJson.has("display") ? draft.rootJson.getAsJsonObject("display") : new JsonObject();
        RegistryOps<JsonElement> ops = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.registryAccess().createSerializationContext(JsonOps.INSTANCE)
                : RegistryOps.create(JsonOps.INSTANCE, HolderLookup.Provider.create(Stream.empty()));

        try {
            if (display.has("title")) {
                Component titleComp = ComponentSerialization.CODEC.parse(ops, display.get("title")).result().orElse(Component.empty());
                this.title = titleComp.getString();
                if (this.title.isEmpty() && display.get("title").isJsonPrimitive()) {
                    this.title = display.get("title").getAsString();
                }
            }
        } catch (Exception ignored) {
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
        } catch (Exception ignored) {
            this.description = "";
        }

        try {
            this.icon = display.has("icon") && display.getAsJsonObject("icon").has("id")
                    ? display.getAsJsonObject("icon").get("id").getAsString() : "minecraft:stone";
        } catch (Exception ignored) {
            this.icon = "minecraft:stone";
        }

        try {
            List<ResourceLocation> parsedParents = MultiParentHelper.parseParents(draft.rootJson);
            for (ResourceLocation loc : parsedParents) {
                this.parentList.add(loc.toString());
            }
        } catch (Exception ignored) {
            this.parentList.clear();
        }

        try {
            this.frame = display.has("frame") ? display.get("frame").getAsString() : "task";
        } catch (Exception ignored) {
            this.frame = "task";
        }

        try {
            this.hidden = display.has("hidden") && display.get("hidden").getAsBoolean();
        } catch (Exception ignored) {
            this.hidden = false;
        }

        try {
            this.sendsTelemetryEvent = draft.rootJson.has("sends_telemetry_event") && draft.rootJson.get("sends_telemetry_event").getAsBoolean();
        } catch (Exception ignored) {
            this.sendsTelemetryEvent = false;
        }
    }

    @Override
    public void init(int x, int y, int width, int height, Runnable reinitScreen) {
        form.clear();
        parentBoxes.clear();

        form.addSection("Identity & Display");
        idBox = form.addTextField("Identifier", "ID, e.g. mod_id:my_advancement", id, s -> this.id = s);
        titleBox = form.addTextField("Title", "Displayed title text in the advancement window", title, s -> this.title = s);
        descBox = form.addTextField("Description", "Displayed tooltip description in the advancement window", description, s -> this.description = s);

        form.addSection("Visuals & Frame");
        iconBox = form.addItemSuggestingField("Icon Item", "ID of the display item, e.g. minecraft:diamond", icon, s -> this.icon = s);

        frameBox = form.addSuggestingField("Frame Type", "Advancement frame type: task, goal, or challenge", frame,
                () -> List.of("task", "goal", "challenge"),
                s -> this.frame = s);

        form.addSection("Parent Advancements");
        ModernButton addParentBtn = ModernButton.modernBuilder(Component.literal("+ Add Parent"), b -> {
                    syncDynamicLists();
                    parentList.add("");
                    reinitScreen.run();
                }).style(ModernButton.Style.SECONDARY).pos(0, 0).size(130, 20)
                .tooltip(Tooltip.create(Component.literal("Add a parent advancement. Leave empty for a root or disconnected advancement.")))
                .build();
        form.addCustomWidget("", addParentBtn, 24);

        for (int i = 0; i < parentList.size(); i++) {
            final int index = i;
            SuggestingEditBox box = makeParentAdvancementBox(i, index);
            parentBoxes.add(box);

            ModernButton removeBtn = ModernButton.modernBuilder(Component.literal("x"), b -> {
                        syncDynamicLists();
                        parentList.remove(index);
                        reinitScreen.run();
                    }).style(ModernButton.Style.DANGER).pos(0, 0).size(20, 20)
                    .tooltip(Tooltip.create(Component.literal("Remove Parent")))
                    .build();

            form.addCustomRow(new EditorForm.DynamicEntryRow(box, removeBtn), List.of(box, removeBtn));
        }

        form.addSection("Visibility & Events");
        form.addToggle("Hidden until Unlocked", "Hides this advancement in the tree until it is unlocked", hidden, v -> this.hidden = v);
        form.addToggle("Telemetry Event", "Sends a telemetry event to the server/game when completed", sendsTelemetryEvent, v -> this.sendsTelemetryEvent = v);

        form.init(x, y, width, height);
    }

    private @NotNull SuggestingEditBox makeParentAdvancementBox(int i, int index) {
        SuggestingEditBox box = new SuggestingEditBox(Minecraft.getInstance().font, 0, 0, 100, 20,
                Component.literal("Parent Advancement"),
                PropertiesTab::getParentSuggestions);
        box.setMaxLength(512);
        box.setValue(parentList.get(i));
        box.setTooltip(Tooltip.create(Component.literal("Parent advancement ID, e.g. minecraft:story/root")));
        box.setResponder(s -> {
            if (index < parentList.size()) parentList.set(index, s);
        });
        return box;
    }

    private void syncDynamicLists() {
        for (int i = 0; i < parentBoxes.size(); i++) {
            if (i < parentList.size()) parentList.set(i, parentBoxes.get(i).getValue());
        }
    }

    @Override
    public void syncFromWidgets() {
        if (idBox != null) this.id = idBox.getValue();
        if (titleBox != null) this.title = titleBox.getValue();
        if (descBox != null) this.description = descBox.getValue();
        if (iconBox != null) this.icon = iconBox.getValue();
        if (frameBox != null) this.frame = frameBox.getValue();
        syncDynamicLists();
    }

    @Override
    public void saveState(AdvancementDraft draft) {
        syncFromWidgets();
        draft.id = this.id.trim();

        JsonObject display = draft.rootJson.has("display") ? draft.rootJson.getAsJsonObject("display") : new JsonObject();
        boolean hasDisplay = false;

        if (!this.title.trim().isEmpty()) {
            display.add("title", new JsonPrimitive(this.title));
            hasDisplay = true;
        }

        if (!this.description.trim().isEmpty()) {
            display.add("description", new JsonPrimitive(this.description));
            hasDisplay = true;
        }

        if (!this.icon.trim().isEmpty()) {
            JsonObject iconObj = new JsonObject();
            iconObj.addProperty("id", this.icon.trim());
            display.add("icon", iconObj);
            hasDisplay = true;
        }

        if (!this.frame.trim().isEmpty()) {
            display.addProperty("frame", this.frame.trim());
            hasDisplay = true;
        }

        if (this.hidden) {
            display.addProperty("hidden", true);
            hasDisplay = true;
        } else {
            display.remove("hidden");
        }

        if (hasDisplay) {
            draft.rootJson.add("display", display);
        } else {
            draft.rootJson.remove("display");
        }

        List<ResourceLocation> parentsList = new ArrayList<>();
        for (String p : this.parentList) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) {
                ResourceLocation loc = ResourceLocation.tryParse(trimmed);
                if (loc != null && !parentsList.contains(loc)) {
                    parentsList.add(loc);
                }
            }
        }
        MultiParentHelper.applyParentsToJson(draft.rootJson, parentsList);

        if (this.sendsTelemetryEvent) {
            draft.rootJson.addProperty("sends_telemetry_event", true);
        } else {
            draft.rootJson.remove("sends_telemetry_event");
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
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