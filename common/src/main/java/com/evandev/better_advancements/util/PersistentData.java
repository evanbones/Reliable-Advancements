package com.evandev.better_advancements.util;

import com.evandev.better_advancements.gui.BetterAdvancementTab;
import com.evandev.better_advancements.gui.BetterAdvancementWidget;
import com.evandev.better_advancements.reference.Constants;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.util.GsonHelper;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

public class PersistentData {
    public static final File FILE = new File(Constants.MOD_ID + "_layout.json");
    public static final Gson GSON = new Gson().newBuilder().setPrettyPrinting().create();
    private static final Map<String, int[]> advancementPositions = new HashMap<>();

    public static void save(Map<AdvancementHolder, BetterAdvancementTab> tabs) {
        try {
            JsonObject previousContents = FILE.exists() ? GSON.fromJson(new FileReader(FILE), JsonObject.class) : null;
            JsonObject json = new JsonObject();
            JsonObject positions = (previousContents != null && previousContents.has("positions"))
                    ? previousContents.getAsJsonObject("positions") : new JsonObject();

            for (BetterAdvancementTab tab : tabs.values()) {
                for (Map.Entry<AdvancementHolder, BetterAdvancementWidget> entry : tab.getWidgets().entrySet()) {
                    BetterAdvancementWidget widget = entry.getValue();
                    JsonArray arr = new JsonArray();
                    arr.add(widget.getX());
                    arr.add(widget.getY());
                    positions.add(entry.getKey().id().toString(), arr);
                }
            }
            json.add("positions", positions);

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

    public static void loadSavedPosition(AdvancementHolder holder, BetterAdvancementWidget widget) {
        String id = holder.id().toString();
        if (advancementPositions.containsKey(id)) {
            int[] pos = advancementPositions.get(id);
            widget.setX(pos[0]);
            widget.setY(pos[1]);
        }
    }
}