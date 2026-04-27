package com.evandev.better_advancements.util;

import com.evandev.better_advancements.reference.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TriggerSchemaManager {
    private static final Map<String, List<String>> SCHEMAS = new HashMap<>();
    private static final ResourceLocation SCHEMA_FILE = ResourceLocation.fromNamespaceAndPath("better_advancements", "trigger_schemas.json");

    public static void load() {
        SCHEMAS.clear();
        try {
            var resourceManager = Minecraft.getInstance().getResourceManager();
            for (Resource resource : resourceManager.getResourceStack(SCHEMA_FILE)) {
                try (InputStreamReader reader = new InputStreamReader(resource.open())) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                        List<String> params = new ArrayList<>();
                        JsonArray paramArray = entry.getValue().getAsJsonObject().getAsJsonArray("parameters");
                        for (JsonElement el : paramArray) {
                            params.add(el.getAsString());
                        }
                        SCHEMAS.put(entry.getKey(), params);
                    }
                }
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to load trigger schemas from {}", SCHEMA_FILE, e);
        }
    }

    public static List<String> getParameters(String triggerId) {
        if (SCHEMAS.isEmpty()) load();
        return SCHEMAS.getOrDefault(triggerId, List.of("player", "entity", "location", "item"));
    }
}