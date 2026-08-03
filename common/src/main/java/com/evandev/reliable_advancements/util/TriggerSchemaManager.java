package com.evandev.reliable_advancements.util;

import com.evandev.reliable_advancements.reference.Constants;
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
    private static final Map<String, JsonElement> TRIGGERS = new HashMap<>();
    private static final ResourceLocation SCHEMA_FILE = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "trigger_schemas.json");

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
                            TRIGGERS.put(entry.getKey(), entry.getValue());
                        }
                    }
                }
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to load flat trigger schemas", e);
        }
    }

    public static List<String> getFields(String triggerId) {
        if (TRIGGERS.isEmpty()) load();
        JsonElement schema = TRIGGERS.get(triggerId);
        if (schema == null) return List.of();

        if (schema.isJsonArray()) {
            List<String> fields = new ArrayList<>();
            for (JsonElement field : schema.getAsJsonArray()) {
                if (field.isJsonPrimitive()) fields.add(field.getAsString());
            }
            return fields;
        }

        return schema.isJsonObject() ? new ArrayList<>(schema.getAsJsonObject().keySet()) : List.of();
    }
}