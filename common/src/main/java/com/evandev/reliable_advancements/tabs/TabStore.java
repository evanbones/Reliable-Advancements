package com.evandev.reliable_advancements.tabs;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TabStore {
    public static final int VERSION = 1;

    public static final ResourceLocation STANDALONE_DELETE =
            ResourceLocation.fromNamespaceAndPath("reliable_advancements", "standalone");

    public static final ResourceLocation PERMANENT_DELETE =
            ResourceLocation.fromNamespaceAndPath("reliable_advancements", "permanent");

    private final Map<ResourceLocation, TabDefinition> tabs = new LinkedHashMap<>();

    private final Map<ResourceLocation, Deletion> deletedAdvancements = new LinkedHashMap<>();
    private final Map<ResourceLocation, Presentation> presentation = new LinkedHashMap<>();

    public static TabStore fromJson(@Nullable JsonObject root) {
        TabStore store = new TabStore();
        if (root == null) return store;

        if (root.has("tabs") && root.get("tabs").isJsonObject()) {
            JsonObject tabsJson = root.getAsJsonObject("tabs");
            for (String key : tabsJson.keySet()) {
                ResourceLocation id = ResourceLocation.tryParse(key);
                if (id != null && tabsJson.get(key).isJsonObject()) {
                    store.tabs.put(id, TabDefinition.fromJson(id, tabsJson.getAsJsonObject(key)));
                }
            }
        }

        if (root.has("deleted_advancements") && root.get("deleted_advancements").isJsonObject()) {
            JsonObject deleted = root.getAsJsonObject("deleted_advancements");
            for (String key : deleted.keySet()) {
                ResourceLocation advancement = ResourceLocation.tryParse(key);
                if (advancement == null) continue;

                if (!deleted.get(key).isJsonObject()) {
                    ResourceLocation owner = ResourceLocation.tryParse(deleted.get(key).getAsString());
                    store.deletedAdvancements.put(advancement,
                            new Deletion(owner == null ? STANDALONE_DELETE : owner, null, null, null));
                    continue;
                }

                JsonObject value = deleted.getAsJsonObject(key);
                ResourceLocation owner = value.has("owner") ? ResourceLocation.tryParse(value.get("owner").getAsString()) : null;
                ResourceLocation tab = value.has("tab") ? ResourceLocation.tryParse(value.get("tab").getAsString()) : null;
                String title = value.has("title") ? value.get("title").getAsString() : null;
                ResourceLocation icon = value.has("icon") ? ResourceLocation.tryParse(value.get("icon").getAsString()) : null;
                store.deletedAdvancements.put(advancement,
                        new Deletion(owner == null ? STANDALONE_DELETE : owner, tab, title, icon));
            }
        }
        if (root.has("derived_presentation") && root.get("derived_presentation").isJsonObject()) {
            JsonObject seen = root.getAsJsonObject("derived_presentation");
            for (String key : seen.keySet()) {
                ResourceLocation tabId = ResourceLocation.tryParse(key);
                if (tabId == null || !seen.get(key).isJsonObject()) continue;
                JsonObject value = seen.getAsJsonObject(key);
                if (!value.has("title")) continue;
                ResourceLocation icon = value.has("icon") ? ResourceLocation.tryParse(value.get("icon").getAsString()) : null;
                ResourceLocation background = value.has("background") ? ResourceLocation.tryParse(value.get("background").getAsString()) : null;
                store.presentation.put(tabId, new Presentation(value.get("title").getAsString(), icon, background));
            }
        }
        return store;
    }

    public static TabStore parse(String json) {
        try {
            return fromJson(JsonParser.parseString(json).getAsJsonObject());
        } catch (Exception e) {
            return new TabStore();
        }
    }

    public Collection<TabDefinition> tabs() {
        return tabs.values();
    }

    public @Nullable TabDefinition tab(ResourceLocation id) {
        return tabs.get(id);
    }

    public TabDefinition getOrCreate(ResourceLocation id) {
        return tabs.computeIfAbsent(id, TabDefinition::new);
    }

    public void put(TabDefinition definition) {
        tabs.put(definition.id, definition);
    }

    public void remove(ResourceLocation id) {
        tabs.remove(id);
        presentation.remove(id);
    }

    public @Nullable Presentation presentation(ResourceLocation tabId) {
        return presentation.get(tabId);
    }

    public boolean cachePresentation(ResourceLocation tabId, Presentation seen) {
        return !seen.equals(presentation.put(tabId, seen));
    }

    public Map<ResourceLocation, Deletion> deletedAdvancements() {
        return deletedAdvancements;
    }

    public boolean isAdvancementDeleted(ResourceLocation id) {
        return deletedAdvancements.containsKey(id);
    }

    public boolean isTabDeleted(ResourceLocation id) {
        TabDefinition def = tabs.get(id);
        return def != null && def.deleted;
    }

    public void markAdvancementDeleted(ResourceLocation advancement, Deletion deletion) {
        deletedAdvancements.put(advancement, deletion);
    }

    public void clearAdvancementDeletion(ResourceLocation advancement) {
        deletedAdvancements.remove(advancement);
    }

    public void clearDeletionsOwnedBy(ResourceLocation tabId) {
        deletedAdvancements.entrySet().removeIf(entry -> entry.getValue().owner().equals(tabId));
    }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("version", VERSION);

        JsonObject tabsJson = new JsonObject();
        for (TabDefinition def : tabs.values()) {
            tabsJson.add(def.id.toString(), def.toJson());
        }
        root.add("tabs", tabsJson);

        JsonObject deleted = new JsonObject();
        for (Map.Entry<ResourceLocation, Deletion> entry : deletedAdvancements.entrySet()) {
            JsonObject value = new JsonObject();
            value.addProperty("owner", entry.getValue().owner().toString());
            if (entry.getValue().tab() != null) value.addProperty("tab", entry.getValue().tab().toString());
            if (entry.getValue().titleJson() != null) value.addProperty("title", entry.getValue().titleJson());
            if (entry.getValue().icon() != null) value.addProperty("icon", entry.getValue().icon().toString());
            deleted.add(entry.getKey().toString(), value);
        }
        root.add("deleted_advancements", deleted);

        JsonObject seen = new JsonObject();
        for (Map.Entry<ResourceLocation, Presentation> entry : presentation.entrySet()) {
            JsonObject value = new JsonObject();
            value.addProperty("title", entry.getValue().title());
            if (entry.getValue().icon() != null) value.addProperty("icon", entry.getValue().icon().toString());
            if (entry.getValue().background() != null) value.addProperty("background", entry.getValue().background().toString());
            seen.add(entry.getKey().toString(), value);
        }
        root.add("derived_presentation", seen);
        return root;
    }

    public record Deletion(ResourceLocation owner, @Nullable ResourceLocation tab,
                           @Nullable String titleJson, @Nullable ResourceLocation icon) {
    }

    public record Presentation(String title, @Nullable ResourceLocation icon, @Nullable ResourceLocation background) {
    }
}
