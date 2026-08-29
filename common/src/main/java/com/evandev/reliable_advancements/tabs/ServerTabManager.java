package com.evandev.reliable_advancements.tabs;

import com.evandev.reliable_advancements.network.SyncTabsPayload;
import com.evandev.reliable_advancements.platform.Services;
import com.evandev.reliable_advancements.reference.Constants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public final class ServerTabManager {
    public static final String FILE_NAME = "tabs.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static TabStore store = new TabStore();

    private ServerTabManager() {
    }

    public static TabStore store() {
        return store;
    }

    public static void load(List<Path> searchRoots, Path writeRoot) {
        for (Path root : searchRoots) {
            Path file = root.resolve(FILE_NAME);
            if (!Files.isRegularFile(file)) continue;
            try {
                store = TabStore.parse(Files.readString(file, StandardCharsets.UTF_8));
                return;
            } catch (Exception e) {
                Constants.LOG.error("Could not read tab configuration from {}", file, e);
            }
        }

        store = new TabStore();
        if (migrateLegacyTabProperties(searchRoots)) {
            save(writeRoot);
        }
    }

    public static void save(Path writeRoot) {
        Path file = writeRoot.resolve(FILE_NAME);
        try {
            Files.createDirectories(writeRoot);
            Files.writeString(file, GSON.toJson(store.toJson()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Constants.LOG.error("Could not write tab configuration to {}", file, e);
        }
    }

    private static boolean migrateLegacyTabProperties(List<Path> searchRoots) {
        boolean migrated = false;
        for (Path root : searchRoots) {
            Path dataDir = root.resolve("data");
            if (!Files.isDirectory(dataDir)) continue;

            try (Stream<Path> namespaces = Files.list(dataDir)) {
                for (Path namespaceDir : (Iterable<Path>) namespaces.filter(Files::isDirectory)::iterator) {
                    Path advDir = namespaceDir.resolve("advancement");
                    if (!Files.isDirectory(advDir)) continue;
                    String namespace = namespaceDir.getFileName().toString();

                    try (Stream<Path> files = Files.walk(advDir)) {
                        for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".json"))::iterator) {
                            String path = advDir.relativize(file).toString().replace(File.separatorChar, '/');
                            path = path.substring(0, path.length() - ".json".length());
                            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, path);
                            if (migrateLegacyFile(file, id)) migrated = true;
                        }
                    } catch (Exception e) {
                        Constants.LOG.error("Could not scan {} for legacy tab properties", advDir, e);
                    }
                }
            } catch (Exception e) {
                Constants.LOG.error("Could not scan {} for legacy tab properties", dataDir, e);
            }
        }
        return migrated;
    }

    private static boolean migrateLegacyFile(Path file, ResourceLocation id) {
        try {
            JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!json.has("better_tab") || !json.get("better_tab").isJsonObject()) return false;

            applyLegacyProperties(store.getOrCreate(id), json.getAsJsonObject("better_tab"));

            json.remove("better_tab");
            Files.writeString(file, GSON.toJson(json), StandardCharsets.UTF_8);
            Constants.LOG.info("Migrated legacy tab properties for {}", id);
            return true;
        } catch (Exception e) {
            Constants.LOG.error("Could not migrate legacy tab properties for {}", id, e);
            return false;
        }
    }

    public static void applyLegacyProperties(TabDefinition def, JsonObject legacy) {
        if (legacy.has("title")) {
            String title = legacy.get("title").getAsString();
            if (!title.isEmpty()) def.title = title;
        }
        if (legacy.has("background")) {
            String background = legacy.get("background").getAsString();
            if (!background.isEmpty()) def.background = ResourceLocation.tryParse(background);
        }
        if (legacy.has("static_background")) def.staticBackground = legacy.get("static_background").getAsBoolean();
        if (legacy.has("bg_width")) def.bgWidth = legacy.get("bg_width").getAsInt();
        if (legacy.has("bg_height")) def.bgHeight = legacy.get("bg_height").getAsInt();
        if (legacy.has("width")) def.windowWidth = legacy.get("width").getAsInt();
        if (legacy.has("height")) def.windowHeight = legacy.get("height").getAsInt();
        if (legacy.has("index")) def.index = legacy.get("index").getAsInt();
        if (legacy.has("background_rules")) def.backgroundRules = legacy.get("background_rules").getAsString();
    }

    public static void applyTombstones(Map<ResourceLocation, AdvancementHolder> map) {
        map.keySet().removeAll(store.deletedAdvancements().keySet());

        boolean removedAny;
        do {
            removedAny = map.values().removeIf(holder -> {
                Optional<ResourceLocation> parent = holder.value().parent();
                return parent.isPresent() && !map.containsKey(parent.get());
            });
        } while (removedAny);

    }

    public static Set<ResourceLocation> collectSubtrees(Map<ResourceLocation, AdvancementHolder> map,
                                                        Set<ResourceLocation> rootIds) {
        Set<ResourceLocation> collected = new LinkedHashSet<>(rootIds);
        boolean addedAny;
        do {
            addedAny = false;
            for (AdvancementHolder holder : map.values()) {
                if (collected.contains(holder.id())) continue;
                Optional<ResourceLocation> parent = holder.value().parent();
                if (parent.isPresent() && collected.contains(parent.get())) {
                    collected.add(holder.id());
                    addedAny = true;
                }
            }
        } while (addedAny);
        return collected;
    }

    public static void syncToPlayer(@Nullable ServerPlayer player) {
        if (player == null) return;
        Services.PLATFORM.sendTabsToClient(player, new SyncTabsPayload(store.toJson().toString()));
    }

    public static void syncToAll(MinecraftServer server) {
        for (ServerPlayer player : new ArrayList<>(server.getPlayerList().getPlayers())) {
            syncToPlayer(player);
        }
    }
}
