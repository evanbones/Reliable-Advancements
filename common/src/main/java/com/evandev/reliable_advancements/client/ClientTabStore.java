package com.evandev.reliable_advancements.client;

import com.evandev.reliable_advancements.tabs.ResolvedTab;
import com.evandev.reliable_advancements.tabs.TabDefinition;
import com.evandev.reliable_advancements.tabs.TabResolver;
import com.evandev.reliable_advancements.tabs.TabStore;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ClientTabStore {
    private static TabStore store = new TabStore();

    private ClientTabStore() {
    }

    public static TabStore get() {
        return store;
    }

    public static void set(TabStore newStore) {
        store = newStore;
    }

    public static void clear() {
        store = new TabStore();
    }

    public static List<ResolvedTab> resolve(AdvancementTree tree) {
        return TabResolver.resolve(tree, store);
    }

    public static List<TabDefinition> restorable() {
        List<TabDefinition> restorable = new ArrayList<>();
        for (TabDefinition def : store.tabs()) {
            if (def.deleted && !def.permanentlyDeleted) restorable.add(def);
        }
        return restorable;
    }

    public static Map<ResourceLocation, TabStore.Deletion> restorableAdvancements(@Nullable ResourceLocation tabId) {
        Map<ResourceLocation, TabStore.Deletion> restorable = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, TabStore.Deletion> entry : store.deletedAdvancements().entrySet()) {
            if (TabStore.STANDALONE_DELETE.equals(entry.getValue().owner())) {
                if (java.util.Objects.equals(tabId, entry.getValue().tab())) {
                    restorable.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return restorable;
    }

    public static @Nullable int[] savedPosition(ResourceLocation tabId, ResourceLocation advancementId) {
        TabDefinition def = store.tab(tabId);
        return def == null ? null : def.positions.get(advancementId);
    }
}
