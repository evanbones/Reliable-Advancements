package com.evandev.reliable_advancements.advancements;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MultiParentHelper {

    public static List<ResourceLocation> parseParents(JsonObject json) {
        if (json == null) {
            return new ArrayList<>();
        }

        Set<ResourceLocation> parents = new LinkedHashSet<>();

        if (json.has("parents") && json.get("parents").isJsonArray()) {
            JsonArray arr = json.getAsJsonArray("parents");
            for (JsonElement el : arr) {
                if (el.isJsonPrimitive()) {
                    ResourceLocation loc = ResourceLocation.tryParse(el.getAsString());
                    if (loc != null) {
                        parents.add(loc);
                    }
                }
            }
            return new ArrayList<>(parents);
        }

        if (json.has("parent")) {
            JsonElement p = json.get("parent");
            if (p.isJsonArray()) {
                for (JsonElement el : p.getAsJsonArray()) {
                    if (el.isJsonPrimitive()) {
                        ResourceLocation loc = ResourceLocation.tryParse(el.getAsString());
                        if (loc != null) {
                            parents.add(loc);
                        }
                    }
                }
            } else if (p.isJsonPrimitive()) {
                ResourceLocation loc = ResourceLocation.tryParse(p.getAsString());
                if (loc != null) {
                    parents.add(loc);
                }
            }
        }

        return new ArrayList<>(parents);
    }

    public static void applyParentsToJson(JsonObject json, List<ResourceLocation> parents) {
        applyParentsToJson(json, parents, null);
    }

    public static void applyParentsToJson(JsonObject json, List<ResourceLocation> parents, ResourceLocation primaryParent) {
        if (json == null) return;

        boolean isTabRoot = json.has("display") && json.getAsJsonObject("display").has("background");
        if (isTabRoot) {
            json.remove("parent");
            json.remove("parents");
            return;
        }

        JsonArray arr = new JsonArray();
        if (parents != null) {
            for (ResourceLocation parent : parents) {
                arr.add(parent.toString());
            }
        }
        if (arr.isEmpty()) {
            json.remove("parents");
        } else {
            json.add("parents", arr);
        }

        if (primaryParent != null) {
            json.addProperty("parent", primaryParent.toString());
        } else if (parents != null && !parents.isEmpty()) {
            json.addProperty("parent", parents.getFirst().toString());
        } else {
            json.remove("parent");
        }
    }

    public static JsonObject prepareJsonForCodec(JsonObject original) {
        if (original == null) return new JsonObject();
        JsonObject copy = original.deepCopy();

        copy.remove("parents");

        if (copy.has("parent") && copy.get("parent").isJsonArray()) {
            JsonArray arr = copy.getAsJsonArray("parent");
            if (!arr.isEmpty() && arr.get(0).isJsonPrimitive()) {
                copy.addProperty("parent", arr.get(0).getAsString());
            } else {
                copy.remove("parent");
            }
        }

        return copy;
    }
}
