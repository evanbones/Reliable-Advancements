package com.evandev.advancement_enhancement.util;

import com.evandev.advancement_enhancement.gui.EnhancedAdvancementTab;
import com.evandev.advancement_enhancement.gui.EnhancedAdvancementWidget;
import com.evandev.advancement_enhancement.platform.Services;
import com.evandev.advancement_enhancement.reference.Constants;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

public class PersistentData {
    public static final File FILE = Services.PLATFORM.getConfigDirectory().resolve(Constants.MOD_ID + "_layout.json").toFile();
    public static final Gson GSON = new Gson().newBuilder().setPrettyPrinting().create();
    private static final Map<String, int[]> advancementPositions = new HashMap<>();

    public static void save(Map<AdvancementHolder, EnhancedAdvancementTab> tabs) {
        try {
            JsonObject previousContents = FILE.exists() ? GSON.fromJson(new FileReader(FILE), JsonObject.class) : null;
            JsonObject json = new JsonObject();
            JsonObject positions = (previousContents != null && previousContents.has("positions"))
                    ? previousContents.getAsJsonObject("positions") : new JsonObject();
            JsonObject tabProperties = (previousContents != null && previousContents.has("tab_properties"))
                    ? previousContents.getAsJsonObject("tab_properties") : new JsonObject();

            for (EnhancedAdvancementTab tab : tabs.values()) {
                for (Map.Entry<AdvancementHolder, EnhancedAdvancementWidget> entry : tab.getWidgets().entrySet()) {
                    EnhancedAdvancementWidget widget = entry.getValue();
                    JsonArray arr = new JsonArray();
                    arr.add(widget.getX());
                    arr.add(widget.getY());
                    positions.add(entry.getKey().id().toString(), arr);
                }

                JsonObject tObj = new JsonObject();
                tObj.addProperty("title", tab.customTitle);
                if (tab.customBackground != null) tObj.addProperty("background", tab.customBackground.toString());
                tObj.addProperty("static_background", tab.isStaticBackground);
                tObj.addProperty("bg_width", tab.bgWidth);
                tObj.addProperty("bg_height", tab.bgHeight);
                tObj.addProperty("width", tab.customWidth);
                tObj.addProperty("height", tab.customHeight);
                tObj.addProperty("index", tab.customIndex);
                tabProperties.add(tab.getRootNode().holder().id().toString(), tObj);
            }
            json.add("positions", positions);
            json.add("tab_properties", tabProperties);

            FileWriter writer = new FileWriter(FILE);
            GSON.toJson(json, writer);
            writer.close();
        } catch (Exception e) {
            Constants.LOG.error("Failed to write persistent data", e);
        }
    }

    public static void load() {
        if (!FILE.exists()) return;
        try {
            JsonObject json = GSON.fromJson(new FileReader(FILE), JsonObject.class);
            advancementPositions.clear();
            if (GsonHelper.isObjectNode(json, "positions")) {
                JsonObject positions = json.getAsJsonObject("positions");
                for (String key : positions.keySet()) {
                    if (GsonHelper.isArrayNode(positions, key)) {
                        JsonArray arr = positions.getAsJsonArray(key);
                        if (arr.size() >= 2) {
                            advancementPositions.put(key, new int[]{arr.get(0).getAsInt(), arr.get(1).getAsInt()});
                        }
                    }
                }
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to parse persistent data", e);
        }
    }

    public static boolean hasSavedPosition(AdvancementHolder holder) {
        return advancementPositions.containsKey(holder.id().toString());
    }

    public static void setMemoryPosition(ResourceLocation id, int x, int y) {
        advancementPositions.put(id.toString(), new int[]{x, y});
    }

    public static void loadTabProperties(EnhancedAdvancementTab tab) {
        String id = tab.getRootNode().holder().id().toString();
        try {
            if (!FILE.exists()) return;
            JsonObject json = GSON.fromJson(new FileReader(FILE), JsonObject.class);
            if (GsonHelper.isObjectNode(json, "tab_properties")) {
                JsonObject tabProperties = json.getAsJsonObject("tab_properties");
                if (tabProperties.has(id)) {
                    JsonObject tObj = tabProperties.getAsJsonObject(id);
                    if (tObj.has("title")) tab.customTitle = tObj.get("title").getAsString();
                    if (tObj.has("background"))
                        tab.customBackground = ResourceLocation.parse(tObj.get("background").getAsString());
                    if (tObj.has("static_background"))
                        tab.isStaticBackground = tObj.get("static_background").getAsBoolean();
                    if (tObj.has("bg_width")) tab.bgWidth = tObj.get("bg_width").getAsInt();
                    if (tObj.has("bg_height")) tab.bgHeight = tObj.get("bg_height").getAsInt();
                    if (tObj.has("width")) tab.customWidth = tObj.get("width").getAsInt();
                    if (tObj.has("height")) tab.customHeight = tObj.get("height").getAsInt();
                    if (tObj.has("index")) tab.customIndex = tObj.get("index").getAsInt();
                }
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to load tab properties", e);
        }
    }

    public static void setPosition(ResourceLocation id, int x, int y) {
        advancementPositions.put(id.toString(), new int[]{x, y});
        try {
            if (!FILE.exists()) {
                FILE.getParentFile().mkdirs();
                FILE.createNewFile();
            }
            JsonObject json = FILE.exists() ? GSON.fromJson(new FileReader(FILE), JsonObject.class) : new JsonObject();
            if (json == null) json = new JsonObject();
            JsonObject positions = json.has("positions") ? json.getAsJsonObject("positions") : new JsonObject();
            JsonArray arr = new JsonArray();
            arr.add(x);
            arr.add(y);
            positions.add(id.toString(), arr);
            json.add("positions", positions);
            try (FileWriter writer = new FileWriter(FILE)) {
                GSON.toJson(json, writer);
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to set persistent position", e);
        }
    }

    public static void loadSavedPosition(AdvancementHolder holder, EnhancedAdvancementWidget widget) {
        String id = holder.id().toString();
        if (advancementPositions.containsKey(id)) {
            int[] pos = advancementPositions.get(id);
            widget.setX(pos[0]);
            widget.setY(pos[1]);
        }
    }

    public static void removePosition(ResourceLocation id) {
        advancementPositions.remove(id.toString());
        try {
            if (FILE.exists()) {
                JsonObject json = GSON.fromJson(new FileReader(FILE), JsonObject.class);
                if (json != null && json.has("positions")) {
                    json.getAsJsonObject("positions").remove(id.toString());
                    try (FileWriter writer = new FileWriter(FILE)) {
                        GSON.toJson(json, writer);
                    }
                }
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to remove persistent data", e);
        }
    }

    public static void removeTabProperties(ResourceLocation id) {
        try {
            if (FILE.exists()) {
                JsonObject json = GSON.fromJson(new FileReader(FILE), JsonObject.class);
                if (json != null && json.has("tab_properties")) {
                    json.getAsJsonObject("tab_properties").remove(id.toString());
                    try (FileWriter writer = new FileWriter(FILE)) {
                        GSON.toJson(json, writer);
                    }
                }
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to remove tab properties", e);
        }
    }
}