package com.evandev.reliable_advancements.advancements;

import com.evandev.reliable_advancements.reference.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MultiParentHelper {
    public static final String TAB_MEMBER_KEY = Constants.MOD_ID + ":tab_member";
    //? if <26.1 {
    private static final String PLACEHOLDER_BACKGROUND = "minecraft:textures/gui/advancements/backgrounds/stone.png";
    //?} else {
    /*private static final String PLACEHOLDER_BACKGROUND = "minecraft:gui/advancements/backgrounds/stone";
    *///?}

    public static List<ResourceLocation> parseParents(JsonObject json) {
        if (json == null) {
            return new ArrayList<>();
        }

        Set<ResourceLocation> parents = new LinkedHashSet<>();

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

        if (json.has("parents")) {
            JsonElement ps = json.get("parents");
            if (ps.isJsonArray()) {
                for (JsonElement el : ps.getAsJsonArray()) {
                    if (el.isJsonPrimitive()) {
                        ResourceLocation loc = ResourceLocation.tryParse(el.getAsString());
                        if (loc != null) {
                            parents.add(loc);
                        }
                    }
                }
            } else if (ps.isJsonPrimitive()) {
                ResourceLocation loc = ResourceLocation.tryParse(ps.getAsString());
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

        boolean hasParents = (parents != null && !parents.isEmpty()) || primaryParent != null;

        if (!hasParents) {
            json.remove("parent");
            json.remove("parents");
            return;
        }

        if (json.has("display") && json.get("display").isJsonObject()) {
            json.getAsJsonObject("display").remove("background");
        }

        Set<ResourceLocation> allParents = new LinkedHashSet<>();
        if (primaryParent != null) {
            allParents.add(primaryParent);
        }
        if (parents != null) {
            allParents.addAll(parents);
        }

        if (primaryParent == null && !allParents.isEmpty()) {
            primaryParent = allParents.iterator().next();
        }

        if (primaryParent != null) {
            json.addProperty("parent", primaryParent.toString());
        } else {
            json.remove("parent");
        }

        JsonArray arr = new JsonArray();
        for (ResourceLocation p : allParents) {
            arr.add(p.toString());
        }

        if (arr.isEmpty()) {
            json.remove("parents");
        } else {
            json.add("parents", arr);
        }
    }

    public static JsonObject prepareJsonForCodec(JsonObject original) {
        if (original == null) return new JsonObject();
        JsonObject copy = original.deepCopy();

        copy.remove("parents");

        List<ResourceLocation> parents = parseParents(original);
        if (!parents.isEmpty()) {
            copy.addProperty("parent", parents.getFirst().toString());
            if (copy.has("display") && copy.get("display").isJsonObject()) {
                copy.getAsJsonObject("display").remove("background");
            }
        } else {
            copy.remove("parent");
        }
        stripPlaceholderBackground(copy);

        return copy;
    }

    public static void stripPlaceholderBackground(JsonObject json) {
        JsonElement marker = json.remove(TAB_MEMBER_KEY);
        if (marker != null && marker.isJsonPrimitive() && marker.getAsBoolean()
                && json.has("display") && json.get("display").isJsonObject()) {
            json.getAsJsonObject("display").remove("background");
        }
    }

    public static boolean prepareForDisk(JsonObject json) {
        JsonObject display = json.has("display") && json.get("display").isJsonObject() ? json.getAsJsonObject("display") : null;
        if (display == null || !parseParents(json).isEmpty()) {
            return json.remove(TAB_MEMBER_KEY) != null;
        }
        if (display.has("background")) return false;

        display.addProperty("background", PLACEHOLDER_BACKGROUND);
        json.addProperty(TAB_MEMBER_KEY, true);
        return true;
    }
}
