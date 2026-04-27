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
    private static final Map<String, String> TRIGGERS = new HashMap<>();
    private static final Map<String, JsonObject> SCHEMAS = new HashMap<>();
    private static final ResourceLocation SCHEMA_FILE = ResourceLocation.fromNamespaceAndPath("better_advancements", "trigger_schemas.json");

    public static void load() {
        TRIGGERS.clear();
        SCHEMAS.clear();
        try {
            var resourceManager = Minecraft.getInstance().getResourceManager();
            for (Resource resource : resourceManager.getResourceStack(SCHEMA_FILE)) {
                try (InputStreamReader reader = new InputStreamReader(resource.open())) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    JsonObject triggers = root.getAsJsonObject("triggers");
                    for (Map.Entry<String, JsonElement> entry : triggers.entrySet()) {
                        TRIGGERS.put(entry.getKey(), entry.getValue().getAsString());
                    }
                    JsonObject schemas = root.getAsJsonObject("schemas");
                    for (Map.Entry<String, JsonElement> entry : schemas.entrySet()) {
                        SCHEMAS.put(entry.getKey(), entry.getValue().getAsJsonObject());
                    }
                }
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to load trigger schemas from {}", SCHEMA_FILE, e);
        }
    }

    public static String getRootType(String triggerId) {
        if (TRIGGERS.isEmpty()) load();
        return TRIGGERS.getOrDefault(triggerId, "");
    }

    public static String resolveAlias(String typeName) {
        if (typeName == null) return "string";
        if (typeName.equals("ContextAwarePredicate")) return "EntityPredicate";
        if (typeName.startsWith("net.minecraft.core.Holder") || typeName.startsWith("net.minecraft.resources.ResourceKey") || typeName.startsWith("net.minecraft.tags.TagKey")) {
            return "resource_location";
        }
        return typeName;
    }

    public static List<String> getFields(String typeName) {
        if (SCHEMAS.isEmpty()) load();
        typeName = resolveAlias(typeName);
        JsonObject schema = SCHEMAS.get(typeName);
        if (schema != null) return new ArrayList<>(schema.keySet());
        return List.of();
    }

    public static String getFieldType(String typeName, String fieldName) {
        if (SCHEMAS.isEmpty()) load();
        typeName = resolveAlias(typeName);
        JsonObject schema = SCHEMAS.get(typeName);
        if (schema != null && schema.has(fieldName)) return schema.get(fieldName).getAsString();
        return "string";
    }

    public static boolean isObject(String typeName) {
        if (SCHEMAS.isEmpty()) load();
        typeName = resolveAlias(typeName);
        return SCHEMAS.containsKey(typeName);
    }

    public static boolean isList(String typeName) {
        return typeName != null && typeName.startsWith("list[");
    }

    public static String getListInnerType(String listTypeName) {
        if (isList(listTypeName)) {
            return resolveAlias(listTypeName.substring(5, listTypeName.length() - 1));
        }
        return "string";
    }
}