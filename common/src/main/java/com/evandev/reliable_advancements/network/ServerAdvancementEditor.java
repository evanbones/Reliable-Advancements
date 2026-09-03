package com.evandev.reliable_advancements.network;

import com.evandev.reliable_advancements.advancements.IMultiParentAdvancement;
import com.evandev.reliable_advancements.advancements.MultiParentHelper;
import com.evandev.reliable_advancements.config.ModConfig;
import com.evandev.reliable_advancements.mixin.accessor.PlayerAdvancementsAccessor;
import com.evandev.reliable_advancements.mixin.accessor.ServerAdvancementManagerAccessor;
import com.evandev.reliable_advancements.platform.Services;
import com.evandev.reliable_advancements.reference.Constants;
import com.evandev.reliable_advancements.tabs.*;
import com.evandev.reliable_advancements.util.RewardTrackerData;
import com.google.gson.*;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

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

    private static Map<ResourceLocation, AdvancementHolder> baseAdvancements(MinecraftServer server) {
        if (baseAdvancements.isEmpty()) {
            baseAdvancements = new HashMap<>(((ServerAdvancementManagerAccessor) server.getAdvancements()).getAdvancements());
        }
        return baseAdvancements;
    }

    public static void handleJsonRequest(MinecraftServer server, ServerPlayer player, RequestAdvancementJsonPayload payload) {
        if (player != null && !player.hasPermissions(2)) return;

        if (payload.advancementId().equals(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "resync"))) {
            player.getAdvancements().reload(server.getAdvancements());
            player.getAdvancements().flushDirty(player);
            ServerTabManager.syncToPlayer(player);
            return;
        }

        AdvancementHolder holder = server.getAdvancements().get(payload.advancementId());
        if (holder == null) return;

        JsonObject json = encode(server, player, holder);
        if (json == null) return;

        Services.PLATFORM.sendAdvancementJsonToClient(player,
                new AdvancementJsonPayload(payload.advancementId(), json.toString(), payload.initialTab()));
    }

    public static void handleSyncRequest(@Nullable ServerPlayer player, RequestSyncPayload payload) {
        if (player == null) return;
        Services.PLATFORM.sendSyncComplete(player, new SyncCompletePayload(payload.token()));
    }

    public static void handleRequestFullTree(MinecraftServer server, ServerPlayer player) {
        if (player != null && player.hasPermissions(2)) {
            ServerTabManager.syncToPlayer(player);
            sendFullTreeToPlayer(server, player);
        }
    }

    private static @Nullable JsonObject encode(MinecraftServer server, @Nullable ServerPlayer player, AdvancementHolder holder) {
        RegistryOps<JsonElement> ops = server.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        DataResult<JsonElement> encoded = Advancement.CODEC.encodeStart(ops, holder.value());
        if (encoded.error().isPresent()) {
            report(player, "Could not read " + holder.id() + ": " + encoded.error().get().message());
            return null;
        }
        JsonObject json = encoded.result().orElseThrow().getAsJsonObject();
        MultiParentHelper.applyParentsToJson(json, IMultiParentAdvancement.getParents(holder.value()));
        return json;
    }

    public static void handleLinkAdvancement(MinecraftServer server, ServerPlayer player, LinkAdvancementPayload payload) {
        if (player != null && !player.hasPermissions(2)) return;

        AdvancementHolder holder = server.getAdvancements().get(payload.childId());
        if (holder == null) return;

        JsonObject json = encode(server, player, holder);
        if (json == null) return;

        List<ResourceLocation> parents = new ArrayList<>(IMultiParentAdvancement.getParents(holder.value()));
        if (payload.unlink()) {
            parents.remove(payload.parentId());
        } else if (!parents.contains(payload.parentId())) {
            parents.add(payload.parentId());
        }

        MultiParentHelper.applyParentsToJson(json, parents, choosePrimaryParent(server, holder, parents));
        saveAdvancementEdit(server, player, new EditAdvancementPayload(payload.childId(), json.toString(), false));
    }

    private static @Nullable ResourceLocation choosePrimaryParent(MinecraftServer server, AdvancementHolder holder, List<ResourceLocation> parents) {
        if (parents.isEmpty()) return null;

        AdvancementNode currentNode = server.getAdvancements().tree().get(holder);
        ResourceLocation currentTabRoot = currentNode != null ? currentNode.root().holder().id() : null;
        if (currentTabRoot != null) {
            for (ResourceLocation parentId : parents) {
                AdvancementNode parentNode = server.getAdvancements().tree().get(parentId);
                if (parentNode != null && parentNode.root().holder().id().equals(currentTabRoot)) {
                    return parentId;
                }
            }
        }
        return parents.getFirst();
    }

    public static void saveAdvancementEdit(MinecraftServer server, ServerPlayer player, EditAdvancementPayload payload) {
        if (player != null && !player.hasPermissions(2)) return;

        if (payload.isDelete()) {
            resetAdvancementsToVanilla(server, player, List.of(payload.advancementId()));
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
        DataResult<Advancement> parsed = Advancement.CODEC.parse(ops, MultiParentHelper.prepareJsonForCodec(advJson));
        if (parsed.error().isPresent()) {
            report(player, "Could not save " + payload.advancementId() + ": " + parsed.error().get().message());
            return;
        }

        Advancement adv = parsed.result().orElseThrow();
        List<ResourceLocation> parsedParents = MultiParentHelper.parseParents(advJson);
        if (!parsedParents.isEmpty() && advJson.has("display") && advJson.get("display").isJsonObject()) {
            advJson.getAsJsonObject("display").remove("background");
        }
        IMultiParentAdvancement.setParents(adv, parsedParents);
        if (!writeEditFile(server, player, payload.advancementId(), advJson)) return;

        if (ServerTabManager.store().isAdvancementDeleted(payload.advancementId())) {
            ServerTabManager.store().clearAdvancementDeletion(payload.advancementId());
            saveTabs(server);
        }

        AdvancementHolder previous = server.getAdvancements().get(payload.advancementId());
        Set<ResourceLocation> seeds = new LinkedHashSet<>();
        seeds.add(payload.advancementId());
        if (previous != null) previous.value().parent().ifPresent(seeds::add);
        adv.parent().ifPresent(seeds::add);
        pinCurrentLayout(server, seeds);

        rebuildServerAdvancements(server);
    }

    public static void handleAdvancementBatch(MinecraftServer server, ServerPlayer player, AdvancementBatchPayload payload) {
        if (player != null && !player.hasPermissions(2)) return;

        if (payload.op() == AdvancementBatchPayload.Op.RESET_TO_VANILLA) {
            resetAdvancementsToVanilla(server, player, payload.advancementIds());
            return;
        }
        if (payload.op() == AdvancementBatchPayload.Op.RESTORE) {
            restoreAdvancements(server, player, payload.advancementIds());
            return;
        }
        if (payload.op() == AdvancementBatchPayload.Op.PERMANENT_DELETE) {
            permanentlyDeleteAdvancements(server, player, payload.advancementIds());
            return;
        }

        Set<ResourceLocation> doomed = new LinkedHashSet<>();
        for (ResourceLocation id : payload.advancementIds()) {
            if (server.getAdvancements().get(id) != null) doomed.add(id);
        }
        if (doomed.isEmpty()) return;

        List<ResolvedTab> tabs = TabResolver.resolve(server.getAdvancements().tree(), ServerTabManager.store());
        Map<ResourceLocation, ResourceLocation> advancementToTab =
                TabResolver.advancementToTab(server.getAdvancements().tree(), tabs);

        pinCurrentLayout(server, doomed);
        severLinksInto(server, player, doomed);

        TabStore store = ServerTabManager.store();
        for (ResourceLocation id : doomed) {
            store.markAdvancementDeleted(id, tombstone(server, id, TabStore.STANDALONE_DELETE, advancementToTab.get(id)));
        }

        for (ResolvedTab tab : tabs) {
            if (tab.roots().isEmpty()) continue;
            if (tab.roots().stream().allMatch(root -> doomed.contains(root.holder().id()))) {
                materialise(store, tab);
            }
        }

        saveTabs(server);
        rebuildServerAdvancements(server);
    }

    private static void pinCurrentLayout(MinecraftServer server, Set<ResourceLocation> seeds) {
        AdvancementTree tree = server.getAdvancements().tree();
        TabStore store = ServerTabManager.store();
        List<ResolvedTab> tabs = TabResolver.resolve(tree, store);
        Map<ResourceLocation, ResourceLocation> advancementToTab = TabResolver.advancementToTab(tree, tabs);

        Set<ResourceLocation> affected = new HashSet<>();
        for (ResourceLocation id : seeds) {
            ResourceLocation tabId = advancementToTab.get(id);
            if (tabId != null) affected.add(tabId);
        }
        if (affected.isEmpty()) return;

        Map<ResourceLocation, ResolvedTab> tabsById = new HashMap<>();
        for (ResolvedTab tab : tabs) tabsById.put(tab.id(), tab);

        boolean dirty = false;

        for (Map.Entry<ResourceLocation, ResourceLocation> entry : advancementToTab.entrySet()) {
            ResolvedTab tab = affected.contains(entry.getValue()) ? tabsById.get(entry.getValue()) : null;
            if (tab == null) continue;

            AdvancementHolder holder = server.getAdvancements().get(entry.getKey());
            if (holder == null || holder.value().display().isEmpty()) continue;

            TabDefinition def = materialise(store, tab);
            if (def.positions.containsKey(entry.getKey())) continue;

            DisplayInfo display = holder.value().display().get();
            def.positions.put(entry.getKey(), new int[]{
                    Mth.floor(display.getX() * TabDefinition.PIXELS_PER_COLUMN),
                    Mth.floor(display.getY() * TabDefinition.PIXELS_PER_ROW)
            });
            dirty = true;
        }
        if (dirty) saveTabs(server);
    }

    private static void severLinksInto(MinecraftServer server, @Nullable ServerPlayer player,
                                       Set<ResourceLocation> doomed) {
        for (AdvancementHolder holder : new ArrayList<>(server.getAdvancements().getAllAdvancements())) {
            if (doomed.contains(holder.id())) continue;

            List<ResourceLocation> parents = new ArrayList<>(IMultiParentAdvancement.getParents(holder.value()));
            Optional<ResourceLocation> primary = holder.value().parent();
            boolean touched = parents.removeIf(doomed::contains);

            ResourceLocation newPrimary = primary.orElse(null);
            if (newPrimary != null && doomed.contains(newPrimary)) {
                touched = true;
                newPrimary = parents.isEmpty() ? null : parents.getFirst();
            }
            if (!touched) continue;

            JsonObject json = encode(server, player, holder);
            if (json == null) continue;
            MultiParentHelper.applyParentsToJson(json, parents, newPrimary);
            writeEditFile(server, player, holder.id(), json);
        }
    }

    private static TabDefinition materialise(TabStore store, ResolvedTab tab) {
        TabDefinition def = store.getOrCreate(tab.id());
        for (AdvancementNode root : tab.roots()) {
            if (!def.roots.contains(root.holder().id())) def.roots.add(root.holder().id());
        }
        if (def.background == null) def.background = tab.background();
        if (!def.staticBackground && tab.staticBackground()) def.staticBackground = tab.staticBackground();
        if (def.bgWidth == TabDefinition.DEFAULT_TILE && tab.bgWidth() != TabDefinition.DEFAULT_TILE)
            def.bgWidth = tab.bgWidth();
        if (def.bgHeight == TabDefinition.DEFAULT_TILE && tab.bgHeight() != TabDefinition.DEFAULT_TILE)
            def.bgHeight = tab.bgHeight();
        if (def.windowWidth == 0 && tab.windowWidth() > 0) def.windowWidth = tab.windowWidth();
        if (def.windowHeight == 0 && tab.windowHeight() > 0) def.windowHeight = tab.windowHeight();
        if (def.index == null) def.index = tab.index();
        if ((def.backgroundRules == null || def.backgroundRules.equals("[]")) && tab.backgroundRules() != null && !tab.backgroundRules().equals("[]")) {
            def.backgroundRules = tab.backgroundRules();
        }
        return def;
    }

    private static TabStore.Deletion tombstone(MinecraftServer server, ResourceLocation id,
                                               ResourceLocation owner, @Nullable ResourceLocation tab) {
        AdvancementHolder holder = server.getAdvancements().get(id);
        Optional<DisplayInfo> display = holder == null ? Optional.empty() : holder.value().display();
        if (display.isEmpty()) return new TabStore.Deletion(owner, tab, null, null);

        RegistryOps<JsonElement> ops = server.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        String title = ComponentSerialization.CODEC.encodeStart(ops, display.get().getTitle())
                .result().map(JsonElement::toString).orElse(null);
        return new TabStore.Deletion(owner, tab, title,
                BuiltInRegistries.ITEM.getKey(display.get().getIcon().getItem()));
    }

    private static void restoreAdvancements(MinecraftServer server, @Nullable ServerPlayer player,
                                            List<ResourceLocation> ids) {
        TabStore store = ServerTabManager.store();

        Map<ResourceLocation, TabStore.Deletion> restored = new LinkedHashMap<>();
        for (ResourceLocation id : ids) {
            TabStore.Deletion deletion = store.deletedAdvancements().get(id);
            if (deletion == null) continue;
            restored.put(id, deletion);
            store.clearAdvancementDeletion(id);
        }
        if (restored.isEmpty()) return;

        rebuildAndApplyServerTree(server);

        boolean severed = false;
        for (ResourceLocation id : restored.keySet()) {
            if (server.getAdvancements().get(id) != null) continue;

            AdvancementHolder holder = baseAdvancements(server).get(id);
            JsonObject json = holder == null ? readEditFile(server, id) : encode(server, player, holder);
            if (json == null) continue;

            MultiParentHelper.applyParentsToJson(json, List.of(), null);
            if (!writeEditFile(server, player, id, json)) continue;
            severed = true;
        }
        if (severed) rebuildAndApplyServerTree(server);

        AdvancementTree tree = server.getAdvancements().tree();
        for (Map.Entry<ResourceLocation, TabStore.Deletion> entry : restored.entrySet()) {
            AdvancementNode node = tree.get(entry.getKey());
            ResourceLocation tabId = entry.getValue().tab();
            if (node == null || node.parent() != null || tabId == null || store.isTabDeleted(tabId)) continue;

            TabDefinition def = store.getOrCreate(tabId);
            if (!def.roots.contains(entry.getKey())) def.roots.add(entry.getKey());
        }

        saveTabs(server);
        rebuildServerAdvancements(server);
    }

    public static void handleTabAction(MinecraftServer server, ServerPlayer player, TabActionPayload payload) {
        if (player != null && !player.hasPermissions(2)) return;

        switch (payload.action()) {
            case SAVE -> saveTabDefinition(server, player, payload);
            case SET_POSITIONS -> setTabPositions(server, payload);
            case ADD_ROOT -> addTabRoot(server, payload);
            case DELETE -> deleteTab(server, payload.tabId());
            case RESTORE -> restoreTab(server, payload.tabId());
            case RESET_TO_VANILLA -> resetTabToVanilla(server, player, payload.tabId());
            case MIGRATE_CLIENT_LAYOUT -> migrateClientLayout(server, payload);
            case CACHE_PRESENTATION -> cacheTabPresentation(server, payload);
            case PERMANENT_DELETE -> permanentlyDeleteTab(server, player, payload.tabId());
        }
    }

    private static void cacheTabPresentation(MinecraftServer server, TabActionPayload payload) {
        JsonObject json;
        try {
            json = JsonParser.parseString(payload.jsonPayload()).getAsJsonObject();
        } catch (Exception e) {
            return;
        }

        TabStore store = ServerTabManager.store();
        boolean changed = false;
        for (String key : json.keySet()) {
            ResourceLocation tabId = ResourceLocation.tryParse(key);
            if (tabId == null || !json.get(key).isJsonObject()) continue;

            JsonObject entry = json.getAsJsonObject(key);
            if (!entry.has("title")) continue;
            ResourceLocation icon = entry.has("icon") ? ResourceLocation.tryParse(entry.get("icon").getAsString()) : null;
            ResourceLocation background = entry.has("background") ? ResourceLocation.tryParse(entry.get("background").getAsString()) : null;
            changed |= store.cachePresentation(tabId, new TabStore.Presentation(entry.get("title").getAsString(), icon, background));
        }
        if (changed) saveTabs(server);
    }

    private static void saveTabDefinition(MinecraftServer server, @Nullable ServerPlayer player, TabActionPayload payload) {
        JsonObject json;
        try {
            json = JsonParser.parseString(payload.jsonPayload()).getAsJsonObject();
        } catch (Exception e) {
            report(player, "Could not save tab " + payload.tabId() + ": malformed JSON (" + e.getMessage() + ")");
            return;
        }

        TabDefinition incoming = TabDefinition.fromJson(payload.tabId(), json);
        TabDefinition existing = ServerTabManager.store().tab(payload.tabId());
        if (existing != null) {
            incoming.positions.putAll(existing.positions);
            if (incoming.roots.isEmpty()) incoming.roots.addAll(existing.roots);
        }
        incoming.deleted = false;
        ServerTabManager.store().put(incoming);

        saveTabs(server);
        ServerTabManager.syncToAll(server);
    }

    private static void setTabPositions(MinecraftServer server, TabActionPayload payload) {
        JsonObject json;
        try {
            json = JsonParser.parseString(payload.jsonPayload()).getAsJsonObject();
        } catch (Exception e) {
            return;
        }

        TabDefinition def = ServerTabManager.store().getOrCreate(payload.tabId());
        for (String key : json.keySet()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id == null || !json.get(key).isJsonArray()) continue;
            JsonArray pair = json.getAsJsonArray(key);
            if (pair.size() >= 2) {
                def.positions.put(id, new int[]{pair.get(0).getAsInt(), pair.get(1).getAsInt()});
            }
        }

        saveTabs(server);
        ServerTabManager.syncToAll(server);
    }

    private static void addTabRoot(MinecraftServer server, TabActionPayload payload) {
        ResourceLocation advancementId = ResourceLocation.tryParse(payload.jsonPayload());
        if (advancementId == null) return;

        TabStore store = ServerTabManager.store();
        for (TabDefinition other : store.tabs()) {
            if (!other.id.equals(payload.tabId())) other.roots.remove(advancementId);
        }

        TabDefinition def = store.getOrCreate(payload.tabId());
        if (!def.roots.contains(advancementId)) def.roots.add(advancementId);

        saveTabs(server);
        rebuildServerAdvancements(server);
    }

    private static void deleteTab(MinecraftServer server, ResourceLocation tabId) {
        List<ResolvedTab> tabs = TabResolver.resolve(server.getAdvancements().tree(), ServerTabManager.store());
        ResolvedTab target = tabs.stream().filter(tab -> tab.id().equals(tabId)).findFirst().orElse(null);

        TabStore store = ServerTabManager.store();
        TabDefinition def = target != null ? materialise(store, target) : store.getOrCreate(tabId);

        if (target != null) {
            Set<ResourceLocation> rootIds = new LinkedHashSet<>();
            for (AdvancementNode root : target.roots()) rootIds.add(root.holder().id());

            Map<ResourceLocation, AdvancementHolder> current =
                    ((ServerAdvancementManagerAccessor) server.getAdvancements()).getAdvancements();
            for (ResourceLocation id : ServerTabManager.collectSubtrees(current, rootIds)) {
                store.markAdvancementDeleted(id, tombstone(server, id, tabId, tabId));
            }
        }

        def.deleted = true;
        saveTabs(server);
        rebuildServerAdvancements(server);
    }

    private static void restoreTab(MinecraftServer server, ResourceLocation tabId) {
        TabStore store = ServerTabManager.store();
        TabDefinition def = store.tab(tabId);
        if (def == null) return;

        def.deleted = false;
        store.clearDeletionsOwnedBy(tabId);

        saveTabs(server);
        rebuildServerAdvancements(server);
    }

    private static void resetTabToVanilla(MinecraftServer server, @Nullable ServerPlayer player, ResourceLocation tabId) {
        Set<ResourceLocation> ids = new LinkedHashSet<>();

        List<ResolvedTab> tabs = TabResolver.resolve(server.getAdvancements().tree(), ServerTabManager.store());
        Map<ResourceLocation, ResourceLocation> advancementToTab =
                TabResolver.advancementToTab(server.getAdvancements().tree(), tabs);
        for (Map.Entry<ResourceLocation, ResourceLocation> entry : advancementToTab.entrySet()) {
            if (entry.getValue().equals(tabId)) ids.add(entry.getKey());
        }

        AdvancementTree baseTree = new AdvancementTree();
        baseTree.addAll(baseAdvancements(server).values());
        for (AdvancementHolder holder : baseAdvancements(server).values()) {
            AdvancementNode node = baseTree.get(holder.id());
            if (node != null && node.root().holder().id().equals(tabId)) {
                ids.add(holder.id());
            }
        }

        TabStore store = ServerTabManager.store();
        TabDefinition def = store.tab(tabId);
        if (def != null) {
            ids.addAll(def.roots);
            ids.addAll(def.positions.keySet());
        }
        for (Map.Entry<ResourceLocation, TabStore.Deletion> entry : store.deletedAdvancements().entrySet()) {
            if (tabId.equals(entry.getValue().owner()) || tabId.equals(entry.getValue().tab())) ids.add(entry.getKey());
        }

        collectEditDescendants(server, ids);

        for (ResourceLocation id : ids) {
            deleteEditFile(server, id, player);
            store.clearAdvancementDeletion(id);
        }
        store.remove(tabId);

        saveTabs(server);
        rebuildServerAdvancements(server);
    }

    private static void permanentlyDeleteTab(MinecraftServer server, @Nullable ServerPlayer player, ResourceLocation tabId) {
        if (player != null && !player.hasPermissions(2)) return;

        TabStore store = ServerTabManager.store();
        Map<ResourceLocation, AdvancementHolder> base = baseAdvancements(server);

        Set<ResourceLocation> ids = new LinkedHashSet<>();
        TabDefinition def = store.tab(tabId);
        if (def != null) {
            ids.addAll(def.roots);
            ids.addAll(def.positions.keySet());
        }
        for (Map.Entry<ResourceLocation, TabStore.Deletion> entry : store.deletedAdvancements().entrySet()) {
            if (tabId.equals(entry.getValue().owner()) || tabId.equals(entry.getValue().tab())) {
                ids.add(entry.getKey());
            }
        }

        collectEditDescendants(server, ids);

        for (ResourceLocation id : ids) {
            deleteEditFile(server, id, player);
            if (base.containsKey(id)) {
                TabStore.Deletion existing = store.deletedAdvancements().get(id);
                store.markAdvancementDeleted(id, new TabStore.Deletion(
                        TabStore.PERMANENT_DELETE,
                        existing != null ? existing.tab() : null,
                        null,
                        null
                ));
            } else {
                store.clearAdvancementDeletion(id);
            }
        }

        if (base.containsKey(tabId)) {
            TabDefinition targetDef = def != null ? def : store.getOrCreate(tabId);
            targetDef.deleted = true;
            targetDef.permanentlyDeleted = true;
        } else {
            store.remove(tabId);
        }

        saveTabs(server);
        rebuildServerAdvancements(server);
    }

    private static void migrateClientLayout(MinecraftServer server, TabActionPayload payload) {
        JsonObject json;
        try {
            json = JsonParser.parseString(payload.jsonPayload()).getAsJsonObject();
        } catch (Exception e) {
            return;
        }

        TabStore store = ServerTabManager.store();
        boolean changed = false;

        if (json.has("tab_properties") && json.get("tab_properties").isJsonObject()) {
            JsonObject tabProperties = json.getAsJsonObject("tab_properties");
            for (String key : tabProperties.keySet()) {
                ResourceLocation tabId = ResourceLocation.tryParse(key);
                if (tabId == null || !tabProperties.get(key).isJsonObject()) continue;
                if (store.tab(tabId) != null) continue;
                ServerTabManager.applyLegacyProperties(store.getOrCreate(tabId), tabProperties.getAsJsonObject(key));
                changed = true;
            }
        }

        if (json.has("positions") && json.get("positions").isJsonObject()) {
            List<ResolvedTab> tabs = TabResolver.resolve(server.getAdvancements().tree(), store);
            Map<ResourceLocation, ResourceLocation> advancementToTab =
                    TabResolver.advancementToTab(server.getAdvancements().tree(), tabs);

            JsonObject positions = json.getAsJsonObject("positions");
            for (String key : positions.keySet()) {
                ResourceLocation id = ResourceLocation.tryParse(key);
                if (id == null || !positions.get(key).isJsonArray()) continue;
                ResourceLocation tabId = advancementToTab.get(id);
                if (tabId == null) continue;

                TabDefinition def = store.getOrCreate(tabId);
                if (def.positions.containsKey(id)) continue;
                JsonArray pair = positions.getAsJsonArray(key);
                if (pair.size() >= 2) {
                    def.positions.put(id, new int[]{pair.get(0).getAsInt(), pair.get(1).getAsInt()});
                    changed = true;
                }
            }
        }

        if (changed) {
            saveTabs(server);
            ServerTabManager.syncToAll(server);
        }
    }

    public static void resetAdvancementsToVanilla(MinecraftServer server, @Nullable ServerPlayer player, Collection<ResourceLocation> ids) {
        if (player != null && !player.hasPermissions(2)) return;

        TabStore store = ServerTabManager.store();
        for (ResourceLocation id : ids) {
            deleteEditFile(server, id, player);
            store.clearAdvancementDeletion(id);

            TabDefinition def = store.tab(id);
            if (def != null && def.deleted) def.deleted = false;
            for (TabDefinition tab : store.tabs()) {
                tab.positions.remove(id);
                tab.roots.remove(id);
            }
        }

        saveTabs(server);
        rebuildServerAdvancements(server);
    }

    public static void permanentlyDeleteAdvancements(MinecraftServer server, @Nullable ServerPlayer player, Collection<ResourceLocation> ids) {
        if (player != null && !player.hasPermissions(2)) return;

        TabStore store = ServerTabManager.store();
        Map<ResourceLocation, AdvancementHolder> base = baseAdvancements(server);

        for (ResourceLocation id : ids) {
            deleteEditFile(server, id, player);

            for (TabDefinition tab : store.tabs()) {
                tab.positions.remove(id);
                tab.roots.remove(id);
            }

            if (base.containsKey(id)) {
                TabStore.Deletion existing = store.deletedAdvancements().get(id);
                store.markAdvancementDeleted(id, new TabStore.Deletion(
                        TabStore.PERMANENT_DELETE,
                        existing != null ? existing.tab() : null,
                        null,
                        null
                ));
            } else {
                store.clearAdvancementDeletion(id);
                TabDefinition def = store.tab(id);
                if (def != null && def.deleted) def.deleted = false;
            }
        }

        saveTabs(server);
        rebuildServerAdvancements(server);
    }

    public static void reapplyAllEdits(MinecraftServer server) {
        ServerAdvancementManagerAccessor manager = (ServerAdvancementManagerAccessor) server.getAdvancements();
        baseAdvancements = new HashMap<>(manager.getAdvancements());
        ServerTabManager.load(editsRoots(server), configuredEditsRoot(server));
        rebuildServerAdvancements(server);
    }

    public static void rebuildServerAdvancements(MinecraftServer server) {
        rebuildAndApplyServerTree(server);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.getAdvancements().reload(server.getAdvancements());
            player.getAdvancements().flushDirty(player);
            RewardTrackerData.get(server).syncToPlayer(player);
        }
        ServerTabManager.syncToAll(server);
        sendFullTreeToAll(server);
    }

    public static void rebuildAndApplyServerTree(MinecraftServer server) {
        ServerAdvancementManagerAccessor manager = (ServerAdvancementManagerAccessor) server.getAdvancements();

        AdvancementTree outgoing = server.getAdvancements().tree();
        List<ResolvedTab> tabsBefore = TabResolver.resolve(outgoing, ServerTabManager.store());
        Map<ResourceLocation, ResourceLocation> whereTheyWere = TabResolver.advancementToTab(outgoing, tabsBefore);

        Map<ResourceLocation, AdvancementHolder> current = new LinkedHashMap<>(baseAdvancements(server));

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

        current.putAll(edits);
        ServerTabManager.applyTombstones(current);
        applyTreeAndPositions(manager, current);
        reconcileOrphanRoots(server, tabsBefore, whereTheyWere);
    }

    private static void reconcileOrphanRoots(MinecraftServer server, List<ResolvedTab> tabsBefore,
                                             Map<ResourceLocation, ResourceLocation> whereTheyWere) {
        TabStore store = ServerTabManager.store();

        Set<ResourceLocation> claimed = new HashSet<>();
        for (TabDefinition def : store.tabs()) {
            if (!def.deleted) claimed.addAll(def.roots);
        }

        Map<ResourceLocation, ResolvedTab> tabsById = new HashMap<>();
        for (ResolvedTab tab : tabsBefore) tabsById.put(tab.id(), tab);

        boolean dirty = false;
        for (AdvancementNode root : server.getAdvancements().tree().roots()) {
            ResourceLocation id = root.holder().id();
            if (claimed.contains(id) || TabResolver.declaresTab(root)) continue;

            ResourceLocation wasId = whereTheyWere.get(id);
            if (wasId == null || wasId.equals(id)) continue;

            ResolvedTab was = tabsById.get(wasId);
            if (was == null || store.isTabDeleted(was.id())) continue;

            TabDefinition def = materialise(store, was);
            if (def.roots.contains(id)) continue;
            def.roots.add(id);
            dirty = true;
        }
        if (dirty) saveTabs(server);
    }

    public static void applyTreeAndPositions(ServerAdvancementManagerAccessor manager, Map<ResourceLocation, AdvancementHolder> map) {
        manager.setAdvancements(map);

        AdvancementTree tree = new AdvancementTree();
        List<AdvancementHolder> sorted = new ArrayList<>(map.values());
        sorted.sort(Comparator.comparing(AdvancementHolder::id));
        tree.addAll(sorted);

        for (AdvancementNode root : tree.roots()) {
            if (root.holder().value().display().isPresent()) {
                TreeNodePosition.run(root);
            }
        }

        manager.setTree(tree);
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
                    JsonObject prepared = MultiParentHelper.prepareJsonForCodec(json);
                    DataResult<Advancement> parsed = Advancement.CODEC.parse(ops, prepared);
                    parsed.result().ifPresentOrElse(
                            advancement -> {
                                List<ResourceLocation> parents = MultiParentHelper.parseParents(json);
                                IMultiParentAdvancement.setParents(advancement, parents);
                                out.put(id, new AdvancementHolder(id, advancement));
                            },
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

    private static void collectEditDescendants(MinecraftServer server, Set<ResourceLocation> ids) {
        Map<ResourceLocation, List<ResourceLocation>> editParents = new HashMap<>();

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
                                editParents.put(id, MultiParentHelper.parseParents(json));
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
            for (Map.Entry<ResourceLocation, List<ResourceLocation>> entry : editParents.entrySet()) {
                if (ids.contains(entry.getKey())) continue;
                for (ResourceLocation parent : entry.getValue()) {
                    if (ids.contains(parent) && ids.add(entry.getKey())) {
                        added = true;
                        break;
                    }
                }
            }
        } while (added);
    }

    private static void saveTabs(MinecraftServer server) {
        ServerTabManager.save(configuredEditsRoot(server));
    }

    private static Path globalConfigEditsRoot() {
        return Services.PLATFORM.getConfigDirectory().resolve(EDITS_DIR_NAME);
    }

    public static Path configuredEditsRoot(MinecraftServer server) {
        if (!server.isDedicatedServer() && ModConfig.get().storeAdvancementEditsGlobally) {
            return globalConfigEditsRoot();
        }
        if (ModConfig.get().storeAdvancementEditsAsDatapack) {
            return server.getWorldPath(LevelResource.DATAPACK_DIR).resolve(EDITS_DIR_NAME);
        }
        return server.getWorldPath(LevelResource.ROOT).resolve(Constants.MOD_ID).resolve("edits");
    }

    public static List<Path> editsRoots(MinecraftServer server) {
        Path worldEdits = server.getWorldPath(LevelResource.ROOT).resolve(Constants.MOD_ID).resolve("edits");
        Path legacyDatapack = server.getWorldPath(LevelResource.DATAPACK_DIR).resolve(EDITS_DIR_NAME);
        Path globalConfig = globalConfigEditsRoot();

        List<Path> roots = new ArrayList<>();
        roots.add(worldEdits);
        if (!legacyDatapack.equals(worldEdits)) roots.add(legacyDatapack);
        if (!globalConfig.equals(worldEdits) && !globalConfig.equals(legacyDatapack)) roots.add(globalConfig);
        return roots;
    }

    private static @Nullable JsonObject readEditFile(MinecraftServer server, ResourceLocation advancementId) {
        for (Path root : editsRoots(server)) {
            File file = editFileIn(root, advancementId);
            if (!file.isFile()) continue;
            try {
                return JsonParser.parseString(Files.readString(file.toPath())).getAsJsonObject();
            } catch (Exception e) {
                Constants.LOG.error("Failed to read saved edit for {}", advancementId, e);
            }
        }
        return null;
    }

    private static File editFileIn(Path root, ResourceLocation advancementId) {
        File dataDir = new File(root.toFile(), "data/" + advancementId.getNamespace() + "/advancement");
        return new File(dataDir, advancementId.getPath() + ".json");
    }

    private static boolean writeEditFile(MinecraftServer server, @Nullable ServerPlayer player, ResourceLocation id, JsonObject json) {
        File advFile = editFileIn(configuredEditsRoot(server), id);
        try {
            File parentDir = advFile.getParentFile();
            if (!parentDir.isDirectory() && !parentDir.mkdirs()) {
                report(player, "Could not save " + id + ": failed to create " + parentDir);
                return false;
            }
            if (ModConfig.get().storeAdvancementEditsAsDatapack) {
                ensurePackMetaExists(configuredEditsRoot(server).toFile());
            }
            try (FileWriter writer = new FileWriter(advFile, StandardCharsets.UTF_8)) {
                writer.write(GSON.toJson(json));
            }
            return true;
        } catch (IOException e) {
            report(player, "Could not save " + id + ": failed to write " + advFile + " (" + e.getMessage() + ")");
            return false;
        }
    }

    private static void deleteEditFile(MinecraftServer server, ResourceLocation advancementId, @Nullable ServerPlayer player) {
        for (Path root : editsRoots(server)) {
            File file = editFileIn(root, advancementId);
            if (file.exists() && !file.delete()) {
                report(player, "Could not reset " + advancementId + ": failed to delete " + file);
            }
        }
    }

    private static void ensurePackMetaExists(File datapackDir) throws IOException {
        File packMeta = new File(datapackDir, "pack.mcmeta");
        if (packMeta.exists()) return;

        JsonObject meta = new JsonObject();
        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", 48);
        pack.addProperty("description", "In-game edits from " + Constants.MOD_NAME);
        meta.add("pack", pack);
        try (FileWriter writer = new FileWriter(packMeta, StandardCharsets.UTF_8)) {
            writer.write(meta.toString());
        }
    }

    private static void report(@Nullable ServerPlayer player, String message) {
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
        Map<AdvancementHolder, AdvancementProgress> playerProgress = ((PlayerAdvancementsAccessor) player.getAdvancements()).getProgress();
        for (AdvancementHolder holder : server.getAdvancements().getAllAdvancements()) {
            AdvancementProgress prog = playerProgress.get(holder);
            if (prog != null && prog.hasProgress()) {
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

    public static void handleRewardClaim(MinecraftServer server, ServerPlayer player, ClaimRewardPayload payload) {
        AdvancementHolder holder = server.getAdvancements().get(payload.advancementId());
        if (holder == null) return;

        if (player.getAdvancements().getOrStartProgress(holder).isDone()
                && !RewardTrackerData.get(server).isClaimed(player.getUUID(), holder.id())) {
            holder.value().rewards().grant(player);
            RewardTrackerData.get(server).claim(player.getUUID(), holder.id());
            RewardTrackerData.get(server).syncToPlayer(player);
        }
    }
}
