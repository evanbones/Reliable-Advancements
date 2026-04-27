package com.evandev.better_advancements.util;

import com.evandev.better_advancements.reference.Constants;
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
    private static final Map<String, JsonObject> TRIGGERS = new HashMap<>();
    private static final ResourceLocation SCHEMA_FILE = ResourceLocation.fromNamespaceAndPath("better_advancements", "trigger_schemas.json");

    public static void load() {
        TRIGGERS.clear();
        try {
            var resourceManager = Minecraft.getInstance().getResourceManager();
            for (Resource resource : resourceManager.getResourceStack(SCHEMA_FILE)) {
                try (InputStreamReader reader = new InputStreamReader(resource.open())) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

                    if (root.has("triggers")) {
                        JsonObject triggers = root.getAsJsonObject("triggers");
                        for (Map.Entry<String, JsonElement> entry : triggers.entrySet()) {
                            TRIGGERS.put(entry.getKey(), entry.getValue().getAsJsonObject());
                        }
                    }
                }
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to load flat trigger schemas", e);
        }
    }

    // TODO: this is middleman code
    public static String getRootType(String triggerId) {
        return triggerId != null ? triggerId : "";
    }

    public static List<String> getFields(String triggerId) {
        if (TRIGGERS.isEmpty()) load();
        JsonObject schema = TRIGGERS.get(triggerId);
        if (schema != null) return new ArrayList<>(schema.keySet());
        return List.of();
    }

    public static String getFieldType(String triggerId, String fieldName) {
        if (TRIGGERS.isEmpty()) load();
        JsonObject schema = TRIGGERS.get(triggerId);
        if (schema != null && schema.has(fieldName)) return schema.get(fieldName).getAsString();
        return "string";
    }

    public static boolean isObject(String type) {
        return "object".equals(type);
    }

    public static boolean isList(String type) {
        return "list".equals(type);
    }
}