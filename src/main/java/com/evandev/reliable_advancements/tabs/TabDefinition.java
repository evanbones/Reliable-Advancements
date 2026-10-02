package com.evandev.reliable_advancements.tabs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TabDefinition {
    public static final int DEFAULT_TILE = 16;

    public static final float PIXELS_PER_COLUMN = 32.0F;
    public static final float PIXELS_PER_ROW = 27.0F;

    public final ResourceLocation id;
    public final List<ResourceLocation> roots = new ArrayList<>();
    public final Map<ResourceLocation, int[]> positions = new LinkedHashMap<>();
    public @Nullable String title;
    public @Nullable ResourceLocation icon;
    public @Nullable ResourceLocation background;
    public boolean staticBackground;
    public int bgWidth = DEFAULT_TILE;
    public int bgHeight = DEFAULT_TILE;
    public int windowWidth;
    public int windowHeight;
    public @Nullable Integer index;
    public String backgroundRules = "[]";
    public boolean deleted;

    public boolean permanentlyDeleted;

    public TabDefinition(ResourceLocation id) {
        this.id = id;
    }

    public static TabDefinition fromJson(ResourceLocation id, JsonObject json) {
        TabDefinition def = new TabDefinition(id);
        if (json.has("title")) def.title = json.get("title").getAsString();
        if (json.has("icon")) def.icon = ResourceLocation.tryParse(json.get("icon").getAsString());
        if (json.has("background")) def.background = ResourceLocation.tryParse(json.get("background").getAsString());
        if (json.has("static_background")) def.staticBackground = json.get("static_background").getAsBoolean();
        if (json.has("bg_width")) def.bgWidth = json.get("bg_width").getAsInt();
        if (json.has("bg_height")) def.bgHeight = json.get("bg_height").getAsInt();
        if (json.has("width")) def.windowWidth = json.get("width").getAsInt();
        if (json.has("height")) def.windowHeight = json.get("height").getAsInt();
        if (json.has("index")) def.index = json.get("index").getAsInt();
        if (json.has("background_rules")) def.backgroundRules = json.get("background_rules").getAsString();
        if (json.has("deleted")) def.deleted = json.get("deleted").getAsBoolean();
        if (json.has("permanently_deleted")) def.permanentlyDeleted = json.get("permanently_deleted").getAsBoolean();

        if (json.has("roots") && json.get("roots").isJsonArray()) {
            for (JsonElement el : json.getAsJsonArray("roots")) {
                ResourceLocation root = ResourceLocation.tryParse(el.getAsString());
                if (root != null && !def.roots.contains(root)) def.roots.add(root);
            }
        }

        if (json.has("positions") && json.get("positions").isJsonObject()) {
            JsonObject positions = json.getAsJsonObject("positions");
            for (String key : positions.keySet()) {
                ResourceLocation advancement = ResourceLocation.tryParse(key);
                if (advancement == null || !positions.get(key).isJsonArray()) continue;
                JsonArray pair = positions.getAsJsonArray(key);
                if (pair.size() >= 2) {
                    def.positions.put(advancement, new int[]{pair.get(0).getAsInt(), pair.get(1).getAsInt()});
                }
            }
        }
        return def;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        if (title != null) json.addProperty("title", title);
        if (icon != null) json.addProperty("icon", icon.toString());
        if (background != null) json.addProperty("background", background.toString());
        if (staticBackground) json.addProperty("static_background", true);
        if (bgWidth != DEFAULT_TILE) json.addProperty("bg_width", bgWidth);
        if (bgHeight != DEFAULT_TILE) json.addProperty("bg_height", bgHeight);
        if (windowWidth > 0) json.addProperty("width", windowWidth);
        if (windowHeight > 0) json.addProperty("height", windowHeight);
        if (index != null) json.addProperty("index", index);
        if (backgroundRules != null && !backgroundRules.isEmpty() && !backgroundRules.equals("[]")) {
            json.addProperty("background_rules", backgroundRules);
        }
        if (deleted) json.addProperty("deleted", true);
        if (permanentlyDeleted) json.addProperty("permanently_deleted", true);

        if (!roots.isEmpty()) {
            JsonArray arr = new JsonArray();
            for (ResourceLocation root : roots) arr.add(root.toString());
            json.add("roots", arr);
        }

        if (!positions.isEmpty()) {
            JsonObject pos = new JsonObject();
            for (Map.Entry<ResourceLocation, int[]> entry : positions.entrySet()) {
                JsonArray pair = new JsonArray();
                pair.add(entry.getValue()[0]);
                pair.add(entry.getValue()[1]);
                pos.add(entry.getKey().toString(), pair);
            }
            json.add("positions", pos);
        }
        return json;
    }
}
