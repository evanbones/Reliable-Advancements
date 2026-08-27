package com.evandev.reliable_advancements.network;

import com.evandev.reliable_advancements.config.ModConfig;
import com.evandev.reliable_advancements.mixin.ServerAdvancementManagerAccessor;
import com.evandev.reliable_advancements.platform.Services;
import com.evandev.reliable_advancements.reference.Constants;
import com.evandev.reliable_advancements.util.RewardTrackerData;
import com.google.gson.*;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class ServerAdvancementEditor {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String EDITS_DIR_NAME = Constants.MOD_ID + "_edits";
    private static Map<ResourceLocation, AdvancementHolder> baseAdvancements = new HashMap<>();

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

        File advFile = editFile(server, payload.advancementId());

        if (payload.isDelete()) {
            deleteEditFile(server, payload.advancementId(), player);
            server.reloadResources(server.getPackRepository().getSelectedIds());
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
            if (ModConfig.get().storeAdvancementEditsAsDatapack) {
                ensurePackMetaExists(configuredEditsRoot(server).toFile());
            }
            try (FileWriter writer = new FileWriter(advFile, StandardCharsets.UTF_8)) {
                writer.write(GSON.toJson(advJson));
            }
        } catch (IOException e) {
            report(player, "Could not save " + payload.advancementId() + ": failed to write " + advFile + " (" + e.getMessage() + ")");
            return;
        }

        applyAdvancements(server, List.of(newHolder));
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.getAdvancements().reload(server.getAdvancements());
            p.getAdvancements().flushDirty(p);
        }
        sendFullTreeToAll(server);
    }

    public static void handleResetTab(MinecraftServer server, ServerPlayer player, ResetTabPayload payload) {
        if (player != null && !player.hasPermissions(2)) return;

        ResourceLocation tabRootId = payload.rootAdvancementId();
        Set<ResourceLocation> idsToDelete = new HashSet<>();
        if (tabRootId != null) {
            idsToDelete.add(tabRootId);
        }
        if (payload.advancementIds() != null) {
            idsToDelete.addAll(payload.advancementIds());
        }

        if (tabRootId != null) {
            if (baseAdvancements != null && !baseAdvancements.isEmpty()) {
                AdvancementTree baseTree = new AdvancementTree();
                baseTree.addAll(baseAdvancements.values());
                for (AdvancementHolder holder : baseAdvancements.values()) {
                    AdvancementNode node = baseTree.get(holder.id());
                    if (node != null && node.root().holder().id().equals(tabRootId)) {
                        idsToDelete.add(holder.id());
                    }
                }
            }

            for (AdvancementHolder holder : server.getAdvancements().getAllAdvancements()) {
                AdvancementNode node = server.getAdvancements().tree().get(holder.id());
                if (node != null && node.root().holder().id().equals(tabRootId)) {
                    idsToDelete.add(holder.id());
                }
            }
        }

        if (!idsToDelete.isEmpty()) {
            collectTabEditsFromDisk(server, idsToDelete);
        }

        for (ResourceLocation id : idsToDelete) {
            deleteEditFile(server, id, player);
        }

        server.reloadResources(server.getPackRepository().getSelectedIds());
    }

    private static void collectTabEditsFromDisk(MinecraftServer server, Set<ResourceLocation> idsToDelete) {
        Map<ResourceLocation, ResourceLocation> editParents = new HashMap<>();
        for (Path root : editsRoots(server)) {
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
                            try {
                                JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
                                if (json.has("parent")) {
                                    ResourceLocation parent = ResourceLocation.tryParse(json.get("parent").getAsString());
                                    if (parent != null) {
                                        editParents.put(id, parent);
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    } catch (IOException ignored) {
                    }
                }
            } catch (IOException ignored) {
            }
        }

        boolean added;
        do {
            added = false;
            for (Map.Entry<ResourceLocation, ResourceLocation> entry : editParents.entrySet()) {
                if (idsToDelete.contains(entry.getValue()) && idsToDelete.add(entry.getKey())) {
                    added = true;
                }
            }
        } while (added);
    }

    public static void handleRequestFullTree(MinecraftServer server, ServerPlayer player) {
        if (player != null && player.hasPermissions(2)) {
            sendFullTreeToPlayer(server, player);
        }
    }

    public static void reapplyAllEdits(MinecraftServer server) {
        ServerAdvancementManagerAccessor manager = (ServerAdvancementManagerAccessor) server.getAdvancements();
        baseAdvancements = new HashMap<>(manager.getAdvancements());

        RegistryOps<JsonElement> ops = server.registryAccess().createSerializationContext(JsonOps.INSTANCE);

        Map<ResourceLocation, AdvancementHolder> edits = new LinkedHashMap<>();
        for (Path root : editsRoots(server)) {
            Path dataDir = root.resolve("data");
            if (!Files.isDirectory(dataDir)) continue;

            try (Stream<Path> namespaces = Files.list(dataDir)) {
                for (Path namespaceDir : (Iterable<Path>) namespaces.filter(Files::isDirectory)::iterator) {
                    Path advDir = namespaceDir.resolve("advancement");
                    if (!Files.isDirectory(advDir)) continue;
                    collectEdits(ops, namespaceDir.getFileName().toString(), advDir, edits);
                }
            } catch (IOException e) {
                Constants.LOG.error("Failed to read saved advancement edits under {}", dataDir, e);
            }
        }

        if (!edits.isEmpty()) {
            applyAdvancements(server, edits.values());
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.getAdvancements().reload(server.getAdvancements());
            player.getAdvancements().flushDirty(player);
            RewardTrackerData.get(server).syncToPlayer(player);
        }
        sendFullTreeToAll(server);
    }

    private static void collectEdits(RegistryOps<JsonElement> ops, String namespace, Path advDir, Map<ResourceLocation, AdvancementHolder> out) {
        try (Stream<Path> files = Files.walk(advDir)) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".json"))::iterator) {
                String path = advDir.relativize(file).toString().replace(File.separatorChar, '/');
                path = path.substring(0, path.length() - ".json".length());
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, path);

                if (out.containsKey(id)) continue;

                try {
                    JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
                    DataResult<Advancement> parsed = Advancement.CODEC.parse(ops, json);
                    parsed.result().ifPresentOrElse(
                            advancement -> out.put(id, new AdvancementHolder(id, advancement)),
                            () -> Constants.LOG.error("Could not reapply saved edit for {}: {}", id,
                                    parsed.error().map(DataResult.Error::message).orElse("unknown error"))
                    );
                } catch (Exception e) {
                    Constants.LOG.error("Could not reapply saved edit for {}", id, e);
                }
            }
        } catch (IOException e) {
            Constants.LOG.error("Failed to read saved advancement edits under {}", advDir, e);
        }
    }

    private static void applyAdvancements(MinecraftServer server, Collection<AdvancementHolder> holders) {
        ServerAdvancementManagerAccessor manager = (ServerAdvancementManagerAccessor) server.getAdvancements();
        Map<ResourceLocation, AdvancementHolder> map = new HashMap<>(manager.getAdvancements());
        for (AdvancementHolder holder : holders) {
            map.put(holder.id(), holder);
        }
        manager.setAdvancements(map);

        AdvancementTree tree = new AdvancementTree();
        tree.addAll(map.values());
        manager.setTree(tree);
    }

    private static Path globalConfigEditsRoot() {
        return Services.PLATFORM.getConfigDirectory().resolve(EDITS_DIR_NAME);
    }

    private static Path configuredEditsRoot(MinecraftServer server) {
        if (!server.isDedicatedServer() && ModConfig.get().storeAdvancementEditsGlobally) {
            return globalConfigEditsRoot();
        }
        if (ModConfig.get().storeAdvancementEditsAsDatapack) {
            return server.getWorldPath(LevelResource.DATAPACK_DIR).resolve(EDITS_DIR_NAME);
        }
        return server.getWorldPath(LevelResource.ROOT).resolve(Constants.MOD_ID).resolve("edits");
    }

    private static List<Path> editsRoots(MinecraftServer server) {
        Path worldEdits = server.getWorldPath(LevelResource.ROOT).resolve(Constants.MOD_ID).resolve("edits");
        Path legacyDatapack = server.getWorldPath(LevelResource.DATAPACK_DIR).resolve(EDITS_DIR_NAME);
        Path globalConfig = globalConfigEditsRoot();

        List<Path> roots = new ArrayList<>();
        roots.add(worldEdits);
        if (!legacyDatapack.equals(worldEdits)) roots.add(legacyDatapack);
        if (!globalConfig.equals(worldEdits) && !globalConfig.equals(legacyDatapack)) roots.add(globalConfig);
        return roots;
    }

    private static File editFile(MinecraftServer server, ResourceLocation advancementId) {
        return editFileIn(configuredEditsRoot(server), advancementId);
    }

    private static File editFileIn(Path root, ResourceLocation advancementId) {
        File dataDir = new File(root.toFile(), "data/" + advancementId.getNamespace() + "/advancement");
        return new File(dataDir, advancementId.getPath() + ".json");
    }

    private static boolean deleteEditFile(MinecraftServer server, ResourceLocation advancementId, ServerPlayer player) {
        boolean deletedAny = false;
        for (Path root : editsRoots(server)) {
            File file = editFileIn(root, advancementId);
            if (file.exists()) {
                if (file.delete()) {
                    deletedAny = true;
                } else {
                    report(player, "Could not reset " + advancementId + ": failed to delete " + file);
                }
            }
        }
        return deletedAny;
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
