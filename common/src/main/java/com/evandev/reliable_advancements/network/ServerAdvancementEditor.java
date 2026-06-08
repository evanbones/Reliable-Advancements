package com.evandev.reliable_advancements.network;

import com.evandev.reliable_advancements.mixin.ServerAdvancementManagerAccessor;
import com.evandev.reliable_advancements.platform.Services;
import com.evandev.reliable_advancements.reference.Constants;
import com.evandev.reliable_advancements.util.RewardTrackerData;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ServerAdvancementEditor {
    public static void handleJsonRequest(MinecraftServer server, ServerPlayer player, RequestAdvancementJsonPayload payload) {
        if (player != null && !player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) return;

        if (payload.advancementId().toString().equals("reliable_advancements:resync")) {
            player.getAdvancements().reload(server.getAdvancements());
            return;
        }

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
        if (player != null && !player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) return;

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
        if (player != null && !player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) return;
        try {
            File datapackDir = new File(server.getWorldPath(LevelResource.DATAPACK_DIR).toFile(), Constants.MOD_ID + "_edits");
            File dataDir = new File(datapackDir, "data/" + payload.advancementId().getNamespace() + "/advancement");
            File advFile = new File(dataDir, payload.advancementId().getPath() + ".json");

            if (payload.isDelete()) {
                if (advFile.exists()) advFile.delete();
            } else {
                advFile.getParentFile().mkdirs();
                ensurePackMetaExists(datapackDir);
                JsonObject advJson = JsonParser.parseString(payload.jsonPayload()).getAsJsonObject();
                try (FileWriter writer = new FileWriter(advFile)) {
                    writer.write(new Gson().newBuilder().setPrettyPrinting().create().toJson(advJson));
                }

                RegistryOps<JsonElement> ops = server.registryAccess().createSerializationContext(JsonOps.INSTANCE);
                Advancement newAdvancement = Advancement.CODEC.parse(ops, advJson).getOrThrow();
                AdvancementHolder newHolder = new AdvancementHolder(payload.advancementId(), newAdvancement);

                ServerAdvancementManagerAccessor manager = (ServerAdvancementManagerAccessor) server.getAdvancements();
                Map<Identifier, AdvancementHolder> map = new HashMap<>(manager.getAdvancements());
                map.put(payload.advancementId(), newHolder);
                manager.setAdvancements(map);

                AdvancementTree tree = new AdvancementTree();
                tree.addAll(map.values());
                manager.setTree(tree);

                sendFullTreeToAll(server);
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to save and persist advancement edit", e);
        }
    }

    public static void handleResetTab(MinecraftServer server, ServerPlayer player, ResetTabPayload payload) {
        if (player != null && !player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) return;
        File datapackDir = new File(server.getWorldPath(LevelResource.DATAPACK_DIR).toFile(), Constants.MOD_ID + "_edits");

        for (Identifier id : payload.advancementIds()) {
            File advFile = new File(datapackDir, "data/" + id.getNamespace() + "/advancement/" + id.getPath() + ".json");
            if (advFile.exists()) {
                advFile.delete();
            }
        }

        server.reloadResources(server.getPackRepository().getSelectedIds()).thenAccept(v -> {
            server.execute(() -> {
                sendFullTreeToAll(server);
            });
        });
    }

    public static void handleRequestFullTree(MinecraftServer server, ServerPlayer player) {
        if (player != null && player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            sendFullTreeToPlayer(server, player);
        }
    }

    private static void sendFullTreeToAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendFullTreeToPlayer(server, player);
        }
    }

    private static void sendFullTreeToPlayer(MinecraftServer server, ServerPlayer player) {
        Map<Identifier, AdvancementProgress> progressMap = new HashMap<>();
        for (AdvancementHolder holder : server.getAdvancements().getAllAdvancements()) {
            AdvancementProgress prog = player.getAdvancements().getOrStartProgress(holder);
            if (prog.hasProgress()) {
                progressMap.put(holder.id(), prog);
            }
        }

        player.connection.send(new ClientboundUpdateAdvancementsPacket(
                true,
                server.getAdvancements().getAllAdvancements(),
                Set.of(),
                progressMap,
                true
        ));
    }

    private static void ensurePackMetaExists(File datapackDir) throws Exception {
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
    }

    public static void handleRewardClaim(MinecraftServer server, ServerPlayer player, ClaimRewardPayload payload) {
        AdvancementHolder holder = server.getAdvancements().get(payload.advancementId());
        if (holder != null) {
            if (player.getAdvancements().getOrStartProgress(holder).isDone() &&
                    !RewardTrackerData.get(server).isClaimed(player.getUUID(), holder.id())) {

                holder.value().rewards().grant(player);
                RewardTrackerData.get(server).claim(player.getUUID(), holder.id());
                RewardTrackerData.get(server).syncToPlayer(player);
            }
        }
    }
}