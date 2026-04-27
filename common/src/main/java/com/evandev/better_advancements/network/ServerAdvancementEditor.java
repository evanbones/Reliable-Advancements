package com.evandev.better_advancements.network;

import com.evandev.better_advancements.reference.Constants;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.FileWriter;

public class ServerAdvancementEditor {
    public static void saveAdvancementEdit(MinecraftServer server, EditAdvancementPayload payload) {
        try {
            File datapackDir = new File(server.getWorldPath(LevelResource.DATAPACK_DIR).toFile(), "betteradvancements_edits");
            File dataDir = new File(datapackDir, "data/" + payload.advancementId().getNamespace() + "/advancement");
            File advFile = new File(dataDir, payload.advancementId().getPath() + ".json");

            if (payload.isDelete()) {
                if (advFile.exists()) {
                    advFile.delete();
                    Constants.LOG.info("Deleted advancement file: {}", advFile.getAbsolutePath());
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "reload");
                }
            } else {
                advFile.getParentFile().mkdirs();

                File packMeta = new File(datapackDir, "pack.mcmeta");
                if (!packMeta.exists()) {
                    JsonObject meta = new JsonObject();
                    JsonObject pack = new JsonObject();
                    pack.addProperty("pack_format", 48);
                    pack.addProperty("description", "In-game edits from Better Advancements");
                    meta.add("pack", pack);
                    try (FileWriter writer = new FileWriter(packMeta)) {
                        writer.write(meta.toString());
                    }
                }

                JsonObject advJson;
                try {
                    advJson = JsonParser.parseString(payload.jsonPayload()).getAsJsonObject();
                } catch (Exception e) {
                    Constants.LOG.error("Invalid JSON payload received from client. Aborting save.", e);
                    return;
                }

                try (FileWriter writer = new FileWriter(advFile)) {
                    writer.write(advJson.toString());
                }

                Constants.LOG.info("Saved edited advancement directly to datapack: {}", advFile.getAbsolutePath());
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "reload");
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to save and persist advancement edit", e);
        }
    }
}