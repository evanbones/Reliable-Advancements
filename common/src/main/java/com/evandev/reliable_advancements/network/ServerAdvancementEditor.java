package com.evandev.reliable_advancements.network;

import com.evandev.reliable_advancements.mixin.ServerAdvancementManagerAccessor;
import com.evandev.reliable_advancements.platform.Services;
import com.evandev.reliable_advancements.reference.Constants;
import com.evandev.reliable_advancements.util.RewardTrackerData;
import com.google.gson.*;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ServerAdvancementEditor {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String EDITS_PACK_ID = "file/" + Constants.MOD_ID + "_edits";
    private static MinecraftServer packEnabledFor = null;

    public static void handleJsonRequest(MinecraftServer server, ServerPlayer player, RequestAdvancementJsonPayload payload) {
        if (player != null && !player.hasPermissions(2)) return;

        if (payload.advancementId().toString().equals("reliable_advancements:resync")) {
            player.getAdvancements().reload(server.getAdvancements());
            return;
        }

        AdvancementHolder holder = server.getAdvancements().get(payload.advancementId());
        if (holder != null) {
            RegistryOps<JsonElement> ops = server.registryAccess().createSerializationContext(JsonOps.INSTANCE);

            DataResult<JsonElement> encoded = Advancement.CODEC.encodeStart(ops, holder.value());
            if (encoded.error().isPresent()) {
                report(player, "Could not open " + payload.advancementId() + ": " + encoded.error().get().message());
                return;
            }

            JsonObject json = encoded.result().orElseThrow().getAsJsonObject();

            AdvancementJsonPayload response = new AdvancementJsonPayload(payload.advancementId(), json.toString(), payload.initialTab());
            Services.PLATFORM.sendAdvancementJsonToClient(player, response);
        }
    }

    public static void handleLinkAdvancement(MinecraftServer server, ServerPlayer player, LinkAdvancementPayload payload) {
        if (player != null && !player.hasPermissions(2)) return;

        AdvancementHolder holder = server.getAdvancements().get(payload.childId());
        if (holder != null) {
            RegistryOps<JsonElement> ops = server.registryAccess().createSerializationContext(JsonOps.INSTANCE);

            DataResult<JsonElement> encoded = Advancement.CODEC.encodeStart(ops, holder.value());
            if (encoded.error().isPresent()) {
                report(player, "Could not link " + payload.childId() + ": " + encoded.error().get().message());
                return;
            }

            JsonObject json = encoded.result().orElseThrow().getAsJsonObject();

            json.addProperty("parent", payload.parentId().toString());
            saveAdvancementEdit(server, player, new EditAdvancementPayload(payload.childId(), json.toString(), false));
        }
    }

    public static void saveAdvancementEdit(MinecraftServer server, ServerPlayer player, EditAdvancementPayload payload) {
        if (player != null && !player.hasPermissions(2)) return;

        File datapackDir = new File(server.getWorldPath(LevelResource.DATAPACK_DIR).toFile(), Constants.MOD_ID + "_edits");
        File dataDir = new File(datapackDir, "data/" + payload.advancementId().getNamespace() + "/advancement");
        File advFile = new File(dataDir, payload.advancementId().getPath() + ".json");

        if (payload.isDelete()) {
            if (advFile.exists() && !advFile.delete()) {
                report(player, "Could not reset " + payload.advancementId() + ": failed to delete " + advFile);
                return;
            }
            reloadWithEditsPack(server).thenRun(() -> server.execute(() -> sendFullTreeToAll(server)));
            return;
        }

        JsonObject advJson;
        try {
            advJson = JsonParser.parseString(payload.jsonPayload()).getAsJsonObject();
        } catch (Exception e) {
            report(player, "Could not save " + payload.advancementId() + ": malformed JSON (" + e.getMessage() + ")");
            return;
        }

        RegistryOps<JsonElement> ops = server.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        DataResult<Advancement> parsed = Advancement.CODEC.parse(ops, advJson);
        if (parsed.error().isPresent()) {
            report(player, "Could not save " + payload.advancementId() + ": " + parsed.error().get().message());
            return;
        }
        AdvancementHolder newHolder = new AdvancementHolder(payload.advancementId(), parsed.result().orElseThrow());

        try {
            File parentDir = advFile.getParentFile();
            if (!parentDir.isDirectory() && !parentDir.mkdirs()) {
                report(player, "Could not save " + payload.advancementId() + ": failed to create " + parentDir);
                return;
            }
            ensurePackMetaExists(datapackDir);
            try (FileWriter writer = new FileWriter(advFile, StandardCharsets.UTF_8)) {
                writer.write(GSON.toJson(advJson));
            }
        } catch (IOException e) {
            report(player, "Could not save " + payload.advancementId() + ": failed to write " + advFile + " (" + e.getMessage() + ")");
            return;
        }

        ensurePackEnabled(server, () -> {
            applyAdvancement(server, newHolder);
            sendFullTreeToAll(server);
        });
    }

    public static void handleResetTab(MinecraftServer server, ServerPlayer player, ResetTabPayload payload) {
        if (player != null && !player.hasPermissions(2)) return;
        File datapackDir = new File(server.getWorldPath(LevelResource.DATAPACK_DIR).toFile(), Constants.MOD_ID + "_edits");

        for (ResourceLocation id : payload.advancementIds()) {
            File advFile = new File(datapackDir, "data/" + id.getNamespace() + "/advancement/" + id.getPath() + ".json");
            if (advFile.exists() && !advFile.delete()) {
                report(player, "Could not reset " + id + ": failed to delete " + advFile);
            }
        }

        reloadWithEditsPack(server).thenRun(() -> server.execute(() -> sendFullTreeToAll(server)));
    }

    public static void handleRequestFullTree(MinecraftServer server, ServerPlayer player) {
        if (player != null && player.hasPermissions(2)) {
            sendFullTreeToPlayer(server, player);
        }
    }

    private static void applyAdvancement(MinecraftServer server, AdvancementHolder holder) {
        ServerAdvancementManagerAccessor manager = (ServerAdvancementManagerAccessor) server.getAdvancements();
        Map<ResourceLocation, AdvancementHolder> map = new HashMap<>(manager.getAdvancements());
        map.put(holder.id(), holder);
        manager.setAdvancements(map);

        AdvancementTree tree = new AdvancementTree();
        tree.addAll(map.values());
        manager.setTree(tree);
    }

    private static void ensurePackEnabled(MinecraftServer server, Runnable afterwards) {
        if (packEnabledFor == server) {
            afterwards.run();
            return;
        }

        List<String> ids = packIdsIncludingEdits(server);
        if (!ids.contains(EDITS_PACK_ID)) {
            afterwards.run();
            return;
        }
        packEnabledFor = server;

        if (server.getPackRepository().getSelectedIds().contains(EDITS_PACK_ID)) {
            afterwards.run();
            return;
        }

        server.reloadResources(ids).whenComplete((v, error) -> server.execute(() -> {
            if (error != null) {
                Constants.LOG.error("Failed to enable the {} datapack", EDITS_PACK_ID, error);
            }
            afterwards.run();
        }));
    }

    private static CompletableFuture<Void> reloadWithEditsPack(MinecraftServer server) {
        List<String> ids = packIdsIncludingEdits(server);
        if (ids.contains(EDITS_PACK_ID)) {
            packEnabledFor = server;
        }
        return server.reloadResources(ids);
    }

    private static List<String> packIdsIncludingEdits(MinecraftServer server) {
        PackRepository repository = server.getPackRepository();
        repository.reload();

        List<String> ids = new ArrayList<>(repository.getSelectedIds());
        if (!ids.contains(EDITS_PACK_ID) && repository.getAvailableIds().contains(EDITS_PACK_ID)) {
            ids.add(EDITS_PACK_ID);
        }
        return ids;
    }

    private static void report(ServerPlayer player, String message) {
        Constants.LOG.error(message);
        if (player != null) {
            player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.RED));
        }
    }

    private static void sendFullTreeToAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendFullTreeToPlayer(server, player);
        }
    }

    private static void sendFullTreeToPlayer(MinecraftServer server, ServerPlayer player) {
        Map<ResourceLocation, AdvancementProgress> progressMap = new HashMap<>();
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
                progressMap
        ));
    }

    private static void ensurePackMetaExists(File datapackDir) throws IOException {
        File packMeta = new File(datapackDir, "pack.mcmeta");
        if (!packMeta.exists()) {
            JsonObject meta = new JsonObject();
            JsonObject pack = new JsonObject();
            pack.addProperty("pack_format", 48);
            pack.addProperty("description", "In-game edits from " + Constants.MOD_NAME);
            meta.add("pack", pack);
            try (FileWriter writer = new FileWriter(packMeta, StandardCharsets.UTF_8)) {
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
