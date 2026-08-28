package com.evandev.reliable_advancements.util;

import com.evandev.reliable_advancements.gui.EnhancedAdvancementTab;
import com.evandev.reliable_advancements.gui.EnhancedAdvancementWidget;
import com.evandev.reliable_advancements.platform.Services;
import com.evandev.reliable_advancements.reference.Constants;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PersistentData {
    public static final File FILE = Services.PLATFORM.getConfigDirectory().resolve(Constants.MOD_ID + "_layout.json").toFile();
    public static final Gson GSON = new Gson().newBuilder().setPrettyPrinting().create();
    private static final Map<String, int[]> advancementPositions = new HashMap<>();
    private static final Map<String, JsonObject> cachedTabProperties = new HashMap<>();

    public static void save(Map<AdvancementHolder, EnhancedAdvancementTab> tabs) {
        try {
            JsonObject previousContents = null;
            if (FILE.exists()) {
                try (var reader = Files.newBufferedReader(FILE.toPath(), StandardCharsets.UTF_8)) {
                    previousContents = GSON.fromJson(reader, JsonObject.class);
                }
            }
            JsonObject json = new JsonObject();
            JsonObject positions = new JsonObject();
            for (Map.Entry<String, int[]> entry : advancementPositions.entrySet()) {
                JsonArray arr = new JsonArray();
                arr.add(entry.getValue()[0]);
                arr.add(entry.getValue()[1]);
                positions.add(entry.getKey(), arr);
            }

            JsonObject tabProperties = (previousContents != null && previousContents.has("tab_properties"))
                    ? previousContents.getAsJsonObject("tab_properties") : new JsonObject();

            if (tabs != null) {
                for (EnhancedAdvancementTab tab : tabs.values()) {
                    String tabId = tab.getRootNode().holder().id().toString();
                    if (hasCustomTabProperties(tab)) {
                        JsonObject tObj = tabPropertiesToJson(tab);
                        tabProperties.add(tabId, tObj);
                        cachedTabProperties.put(tabId, tObj);
                    } else {
                        tabProperties.remove(tabId);
                        cachedTabProperties.remove(tabId);
                    }
                }
            }
            json.add("positions", positions);
            json.add("tab_properties", tabProperties);

            if (FILE.getParentFile() != null && !FILE.getParentFile().exists()) {
                FILE.getParentFile().mkdirs();
            }
            try (var writer = Files.newBufferedWriter(FILE.toPath(), StandardCharsets.UTF_8)) {
                GSON.toJson(json, writer);
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to write persistent data", e);
        }
    }

    public static JsonObject tabPropertiesToJson(EnhancedAdvancementTab tab) {
        JsonObject tObj = new JsonObject();
        tObj.addProperty("title", tab.customTitle);
        if (tab.customBackground != null)
            tObj.addProperty("background", tab.customBackground.toString());
        tObj.addProperty("static_background", tab.isStaticBackground);
        tObj.addProperty("bg_width", tab.bgWidth);
        tObj.addProperty("bg_height", tab.bgHeight);
        tObj.addProperty("width", tab.customWidth);
        tObj.addProperty("height", tab.customHeight);
        tObj.addProperty("index", tab.customIndex);
        tObj.addProperty("background_rules", tab.rawBackgroundRules);
        return tObj;
    }

    public static void load() {
        if (!FILE.exists()) return;
        try (var reader = Files.newBufferedReader(FILE.toPath(), StandardCharsets.UTF_8)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            advancementPositions.clear();
            cachedTabProperties.clear();
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
            if (GsonHelper.isObjectNode(json, "tab_properties")) {
                JsonObject tabProperties = json.getAsJsonObject("tab_properties");
                for (String key : tabProperties.keySet()) {
                    if (tabProperties.get(key).isJsonObject()) {
                        cachedTabProperties.put(key, tabProperties.getAsJsonObject(key));
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

    public static void snapshotTabPositions(EnhancedAdvancementTab tab) {
        if (tab == null) return;
        for (EnhancedAdvancementWidget widget : tab.getWidgets().values()) {
            if (widget.getAdvancement() != null) {
                setMemoryPosition(widget.getAdvancement().holder().id(), widget.getX(), widget.getY());
            }
        }
    }

    public static void loadTabProperties(EnhancedAdvancementTab tab) {
        String id = tab.getRootNode().holder().id().toString();
        JsonObject tObj = cachedTabProperties.get(id);
        if (tObj != null) {
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
            if (tObj.has("background_rules"))
                tab.parseBackgroundRules(tObj.get("background_rules").getAsString());
        }
    }

    public static void setPosition(ResourceLocation id, int x, int y) {
        advancementPositions.put(id.toString(), new int[]{x, y});
        try {
            if (!FILE.exists()) {
                if (FILE.getParentFile() != null) FILE.getParentFile().mkdirs();
                FILE.createNewFile();
            }
            JsonObject json = null;
            if (FILE.exists()) {
                try (var reader = Files.newBufferedReader(FILE.toPath(), StandardCharsets.UTF_8)) {
                    json = GSON.fromJson(reader, JsonObject.class);
                }
            }
            if (json == null) json = new JsonObject();
            JsonObject positions = json.has("positions") ? json.getAsJsonObject("positions") : new JsonObject();
            JsonArray arr = new JsonArray();
            arr.add(x);
            arr.add(y);
            positions.add(id.toString(), arr);
            json.add("positions", positions);
            try (var writer = Files.newBufferedWriter(FILE.toPath(), StandardCharsets.UTF_8)) {
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
        if (id == null) return;
        removePositions(Collections.singletonList(id));
    }

    public static void removePositions(Collection<ResourceLocation> ids) {
        if (ids == null || ids.isEmpty()) return;
        for (ResourceLocation id : ids) {
            if (id != null) {
                advancementPositions.remove(id.toString());
            }
        }
        try {
            if (FILE.exists()) {
                JsonObject json;
                try (var reader = Files.newBufferedReader(FILE.toPath(), StandardCharsets.UTF_8)) {
                    json = GSON.fromJson(reader, JsonObject.class);
                }
                if (json != null && json.has("positions")) {
                    JsonObject posObj = json.getAsJsonObject("positions");
                    for (ResourceLocation id : ids) {
                        if (id != null) {
                            posObj.remove(id.toString());
                        }
                    }
                    try (var writer = Files.newBufferedWriter(FILE.toPath(), StandardCharsets.UTF_8)) {
                        GSON.toJson(json, writer);
                    }
                }
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to remove persistent data", e);
        }
    }

    public static void removeTabProperties(ResourceLocation id) {
        if (id != null) {
            cachedTabProperties.remove(id.toString());
        }
        try {
            if (FILE.exists()) {
                JsonObject json;
                try (var reader = Files.newBufferedReader(FILE.toPath(), StandardCharsets.UTF_8)) {
                    json = GSON.fromJson(reader, JsonObject.class);
                }
                if (json != null && json.has("tab_properties")) {
                    json.getAsJsonObject("tab_properties").remove(id.toString());
                    try (var writer = Files.newBufferedWriter(FILE.toPath(), StandardCharsets.UTF_8)) {
                        GSON.toJson(json, writer);
                    }
                }
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to remove tab properties", e);
        }
    }

    public static int getDefaultTabIndex(String id) {
        if (id == null) return 5;
        return switch (id) {
            case "minecraft:story/root" -> 0;
            case "minecraft:adventure/root" -> 1;
            case "minecraft:husbandry/root" -> 2;
            case "minecraft:nether/root" -> 3;
            case "minecraft:end/root" -> 4;
            default -> 5;
        };
    }

    public static boolean hasCustomTabProperties(EnhancedAdvancementTab tab) {
        if (tab == null || tab.getRootNode() == null) return false;
        String id = tab.getRootNode().holder().id().toString();
        int defaultIndex = getDefaultTabIndex(id);
        return (tab.customTitle != null && !tab.customTitle.isEmpty())
                || tab.customBackground != null
                || tab.isStaticBackground
                || tab.bgWidth != 16
                || tab.bgHeight != 16
                || tab.customWidth > 0
                || tab.customHeight > 0
                || tab.customIndex != defaultIndex
                || (tab.rawBackgroundRules != null && !tab.rawBackgroundRules.equals("[]") && !tab.rawBackgroundRules.isEmpty());
    }
}