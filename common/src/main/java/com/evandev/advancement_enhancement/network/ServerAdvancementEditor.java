package com.evandev.advancement_enhancement.network;

import com.evandev.advancement_enhancement.platform.Services;
import com.evandev.advancement_enhancement.reference.Constants;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.FileWriter;

public class ServerAdvancementEditor {

    public static void handleJsonRequest(MinecraftServer server, ServerPlayer player, RequestAdvancementJsonPayload payload) {
        if (player != null && !player.hasPermissions(2)) return;

        AdvancementHolder holder = server.getAdvancements().get(payload.advancementId());
        if (holder != null) {
            RegistryOps<JsonElement> ops = server.registryAccess().createSerializationContext(JsonOps.INSTANCE);

            JsonObject json = Advancement.CODEC.encodeStart(ops, holder.value())
                    .getOrThrow()
                    .getAsJsonObject();

            AdvancementJsonPayload response = new AdvancementJsonPayload(payload.advancementId(), json.toString(), payload.initialTab());
            Services.PLATFORM.sendAdvancementJsonToClient(player, response);
        }
    }

    public static void handleLinkAdvancement(MinecraftServer server, ServerPlayer player, LinkAdvancementPayload payload) {
        if (player != null && !player.hasPermissions(2)) return;

        AdvancementHolder holder = server.getAdvancements().get(payload.childId());
        if (holder != null) {
            RegistryOps<JsonElement> ops = server.registryAccess().createSerializationContext(JsonOps.INSTANCE);

            JsonObject json = Advancement.CODEC.encodeStart(ops, holder.value())
                    .getOrThrow()
                    .getAsJsonObject();

            json.addProperty("parent", payload.parentId().toString());
            saveAdvancementEdit(server, player, new EditAdvancementPayload(payload.childId(), json.toString(), false));
        }
    }

    public static void saveAdvancementEdit(MinecraftServer server, ServerPlayer player, EditAdvancementPayload payload) {
        if (player != null && !player.hasPermissions(2)) return;

        try {
            File datapackDir = new File(server.getWorldPath(LevelResource.DATAPACK_DIR).toFile(), Constants.MOD_ID + "_edits");
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
                    pack.addProperty("description", "In-game edits from " + Constants.MOD_NAME);
                    meta.add("pack", pack);
                    try (FileWriter writer = new FileWriter(packMeta)) {
                        writer.write(meta.toString());
                    }
                }

                JsonObject advJson = JsonParser.parseString(payload.jsonPayload()).getAsJsonObject();
                try (FileWriter writer = new FileWriter(advFile)) {
                    writer.write(new Gson().newBuilder().setPrettyPrinting().create().toJson(advJson));
                }

                Constants.LOG.info("Saved edited advancement directly to datapack: {}", advFile.getAbsolutePath());
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "reload");
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to save and persist advancement edit", e);
        }
    }
}