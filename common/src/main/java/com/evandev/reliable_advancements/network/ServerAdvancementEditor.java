package com.evandev.reliable_advancements.network;

import com.evandev.reliable_advancements.mixin.AdvancementListAccessor;
import com.evandev.reliable_advancements.mixin.ServerAdvancementManagerAccessor;
import com.evandev.reliable_advancements.platform.Services;
import com.evandev.reliable_advancements.reference.Constants;
import com.evandev.reliable_advancements.util.RewardTrackerData;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementList;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ServerAdvancementEditor {
    private static void cleanAdvancementJson(JsonObject json) {
        if (json.has("rewards") && (json.get("rewards").isJsonNull() || !json.get("rewards").isJsonObject())) {
            json.remove("rewards");
        }
    }

    public static void handleJsonRequest(MinecraftServer server, ServerPlayer player, RequestAdvancementJsonPayload payload) {
        if (player != null && !player.hasPermissions(2)) return;

        if (payload.advancementId().toString().equals("reliable_advancements:resync")) {
            player.getAdvancements().reload(server.getAdvancements());
            return;
        }

        Advancement holder = server.getAdvancements().getAdvancement(payload.advancementId());
        if (holder != null) {
            JsonObject json = holder.deconstruct().serializeToJson().getAsJsonObject();
            cleanAdvancementJson(json);

            AdvancementJsonPayload response = new AdvancementJsonPayload(payload.advancementId(), json.toString(), payload.initialTab());
            Services.PLATFORM.sendAdvancementJsonToClient(player, response);
        }
    }

    public static void handleLinkAdvancement(MinecraftServer server, ServerPlayer player, LinkAdvancementPayload payload) {
        if (player != null && !player.hasPermissions(2)) return;

        Advancement holder = server.getAdvancements().getAdvancement(payload.childId());
        if (holder != null) {
            JsonObject json = holder.deconstruct().serializeToJson().getAsJsonObject();
            cleanAdvancementJson(json);

            json.addProperty("parent", payload.parentId().toString());
            saveAdvancementEdit(server, player, new EditAdvancementPayload(payload.childId(), json.toString(), false));
        }
    }

    public static void saveAdvancementEdit(MinecraftServer server, ServerPlayer player, EditAdvancementPayload payload) {
        if (player != null && !player.hasPermissions(2)) return;
        try {
            File datapackDir = new File(server.getWorldPath(LevelResource.DATAPACK_DIR).toFile(), Constants.MOD_ID + "_edits");
            File dataDir = new File(datapackDir, "data/" + payload.advancementId().getNamespace() + "/advancements");
            File advFile = new File(dataDir, payload.advancementId().getPath() + ".json");

            if (payload.isDelete()) {
                if (advFile.exists()) advFile.delete();
            } else {
                advFile.getParentFile().mkdirs();
                ensurePackMetaExists(datapackDir);
                JsonObject advJson = JsonParser.parseString(payload.jsonPayload()).getAsJsonObject();
                cleanAdvancementJson(advJson);
                try (FileWriter writer = new FileWriter(advFile)) {
                    writer.write(new Gson().newBuilder().setPrettyPrinting().create().toJson(advJson));
                }
            }

            ServerAdvancementManagerAccessor manager = (ServerAdvancementManagerAccessor) server.getAdvancements();
            AdvancementList advancementsList = manager.getAdvancementsList();
            Map<ResourceLocation, Advancement> map = ((AdvancementListAccessor) advancementsList).getAdvancements();

            Map<ResourceLocation, Advancement.Builder> builders = new HashMap<>();
            for (Map.Entry<ResourceLocation, Advancement> entry : map.entrySet()) {
                if (!entry.getKey().equals(payload.advancementId())) {
                    builders.put(entry.getKey(), entry.getValue().deconstruct());
                }
            }

            if (!payload.isDelete()) {
                JsonObject advJson = JsonParser.parseString(payload.jsonPayload()).getAsJsonObject();
                cleanAdvancementJson(advJson);
                net.minecraft.advancements.critereon.DeserializationContext context = new net.minecraft.advancements.critereon.DeserializationContext(payload.advancementId(), server.getLootData());
                Advancement.Builder builder = Advancement.Builder.fromJson(advJson, context);
                builders.put(payload.advancementId(), builder);
            }

            advancementsList.clear();
            advancementsList.add(builders);

            sendFullTreeToAll(server);
        } catch (Exception e) {
            Constants.LOG.error("Failed to save and persist advancement edit", e);
        }
    }

    public static void handleResetTab(MinecraftServer server, ServerPlayer player, ResetTabPayload payload) {
        if (player != null && !player.hasPermissions(2)) return;
        File datapackDir = new File(server.getWorldPath(LevelResource.DATAPACK_DIR).toFile(), Constants.MOD_ID + "_edits");

        for (ResourceLocation id : payload.advancementIds()) {
            File advFile = new File(datapackDir, "data/" + id.getNamespace() + "/advancements/" + id.getPath() + ".json");
            if (advFile.exists()) {
                advFile.delete();
            }
        }

        server.reloadResources(server.getPackRepository().getSelectedIds()).thenAccept(v -> {
            server.execute(() -> sendFullTreeToAll(server));
        });
    }

    public static void handleRequestFullTree(MinecraftServer server, ServerPlayer player) {
        if (player != null && player.hasPermissions(2)) {
            sendFullTreeToPlayer(server, player);
        }
    }

    private static void sendFullTreeToAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendFullTreeToPlayer(server, player);
        }
    }

    private static void sendFullTreeToPlayer(MinecraftServer server, ServerPlayer player) {
        Map<ResourceLocation, AdvancementProgress> progressMap = new HashMap<>();
        for (Advancement holder : server.getAdvancements().getAllAdvancements()) {
            AdvancementProgress prog = player.getAdvancements().getOrStartProgress(holder);
            if (prog.hasProgress()) {
                progressMap.put(holder.getId(), prog);
            }
        }

        player.connection.send(new ClientboundUpdateAdvancementsPacket(
                true,
                server.getAdvancements().getAllAdvancements(),
                Set.of(),
                progressMap
        ));
    }

    private static void ensurePackMetaExists(File datapackDir) throws Exception {
        File packMeta = new File(datapackDir, "pack.mcmeta");
        if (!packMeta.exists()) {
            JsonObject meta = new JsonObject();
            JsonObject pack = new JsonObject();
            pack.addProperty("pack_format", 15);
            pack.addProperty("description", "In-game edits from " + Constants.MOD_NAME);
            meta.add("pack", pack);
            try (FileWriter writer = new FileWriter(packMeta)) {
                writer.write(meta.toString());
            }
        }
    }

    public static void handleRewardClaim(MinecraftServer server, ServerPlayer player, ClaimRewardPayload payload) {
        Advancement holder = server.getAdvancements().getAdvancement(payload.advancementId());
        if (holder != null) {
            if (player.getAdvancements().getOrStartProgress(holder).isDone() &&
                    !RewardTrackerData.get(server).isClaimed(player.getUUID(), holder.getId())) {

                holder.getRewards().grant(player);
                RewardTrackerData.get(server).claim(player.getUUID(), holder.getId());
                RewardTrackerData.get(server).syncToPlayer(player);
            }
        }
    }
}