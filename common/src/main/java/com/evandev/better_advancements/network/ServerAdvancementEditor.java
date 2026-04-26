package com.evandev.better_advancements.network;

import com.evandev.better_advancements.reference.Constants;
import com.google.gson.JsonObject;
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

            JsonObject advJson = new JsonObject();
            JsonObject display = new JsonObject();

            display.addProperty("title", payload.title());
            display.addProperty("description", payload.description());

            JsonObject icon = new JsonObject();
            icon.addProperty("id", payload.iconId());
            display.add("icon", icon);

            advJson.add("display", display);

            if (!payload.parentId().isEmpty()) {
                advJson.addProperty("parent", payload.parentId());
            }

            JsonObject criteria = new JsonObject();
            JsonObject requirement = new JsonObject();
            requirement.addProperty("trigger", "minecraft:impossible");
            criteria.add("dummy", requirement);
            advJson.add("criteria", criteria);

            try (FileWriter writer = new FileWriter(advFile)) {
                writer.write(advJson.toString());
            }

            Constants.LOG.info("Saved edited advancement directly to datapack: {}", advFile.getAbsolutePath());

            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "reload");

        } catch (Exception e) {
            Constants.LOG.error("Failed to save and persist advancement edit", e);
        }
    }
}