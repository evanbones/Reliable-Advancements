package com.evandev.reliable_advancements.util;

import com.evandev.reliable_advancements.network.TabActionPayload;
import com.evandev.reliable_advancements.platform.Services;
import com.evandev.reliable_advancements.reference.Constants;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class PersistentData {
    public static final File FILE = Services.PLATFORM.getConfigDirectory().resolve(Constants.MOD_ID + "_layout.json").toFile();
    public static final Gson GSON = new Gson().newBuilder().setPrettyPrinting().create();

    private static @Nullable ResourceLocation lastTab;
    private static float zoom = 1.0F;
    private static boolean loaded;

    public static @Nullable ResourceLocation getLastTab() {
        return lastTab;
    }

    public static void setLastTab(@Nullable ResourceLocation id) {
        lastTab = id;
    }

    public static float getZoom() {
        return zoom;
    }

    public static void setZoom(float value) {
        zoom = value;
    }

    public static void load() {
        if (loaded) return;
        loaded = true;
        if (!FILE.exists()) return;
        try (var reader = Files.newBufferedReader(FILE.toPath(), StandardCharsets.UTF_8)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json == null) return;
            if (json.has("last_tab")) {
                lastTab = ResourceLocation.tryParse(json.get("last_tab").getAsString());
            }
            if (json.has("zoom")) {
                zoom = json.get("zoom").getAsFloat();
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to parse persistent data", e);
        }
    }

    public static void save() {
        try {
            JsonObject json = readRaw();
            if (json == null) json = new JsonObject();

            if (lastTab != null) {
                json.addProperty("last_tab", lastTab.toString());
            } else {
                json.remove("last_tab");
            }
            json.addProperty("zoom", zoom);

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

    private static @Nullable JsonObject readRaw() {
        if (!FILE.exists()) return null;
        try (var reader = Files.newBufferedReader(FILE.toPath(), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    public static void migrateLegacyLayoutToServer() {
        if (!Services.PLATFORM.canSendAdvancementEdit()) return;

        JsonObject json = readRaw();
        if (json == null) return;

        boolean hasTabProperties = json.has("tab_properties") && !json.getAsJsonObject("tab_properties").isEmpty();
        boolean hasPositions = json.has("positions") && !json.getAsJsonObject("positions").isEmpty();
        if (!hasTabProperties && !hasPositions) return;

        JsonObject payload = new JsonObject();
        if (hasTabProperties) payload.add("tab_properties", json.get("tab_properties"));
        if (hasPositions) payload.add("positions", json.get("positions"));

        Services.PLATFORM.sendTabAction(new TabActionPayload(
                TabActionPayload.Action.MIGRATE_CLIENT_LAYOUT,
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "client_layout"),
                payload.toString()
        ));

        json.remove("tab_properties");
        json.remove("positions");
        try (var writer = Files.newBufferedWriter(FILE.toPath(), StandardCharsets.UTF_8)) {
            GSON.toJson(json, writer);
            Constants.LOG.info("Uploaded legacy client layout to the server and cleared it locally");
        } catch (Exception e) {
            Constants.LOG.error("Failed to clear legacy client layout", e);
        }
    }
}
