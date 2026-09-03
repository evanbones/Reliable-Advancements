package com.evandev.reliable_advancements.tabs;

import com.evandev.reliable_advancements.advancements.IMultiParentNode;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class TabResolver {
    public static final ResourceLocation DEFAULT_BACKGROUND =
            ResourceLocation.withDefaultNamespace("textures/gui/advancements/backgrounds/stone.png");

    private static final Map<String, Integer> VANILLA_TAB_ORDER = Map.of(
            "minecraft:story/root", 0,
            "minecraft:adventure/root", 1,
            "minecraft:husbandry/root", 2,
            "minecraft:nether/root", 3,
            "minecraft:end/root", 4
    );

    private TabResolver() {
    }

    public static int defaultIndex(ResourceLocation tabId) {
        return VANILLA_TAB_ORDER.getOrDefault(tabId.toString(), 5);
    }

    public static boolean declaresTab(AdvancementNode root) {
        Optional<DisplayInfo> display = root.advancement().display();
        return display.isPresent() && display.get().getBackground().isPresent();
    }

    public static List<ResolvedTab> resolve(AdvancementTree tree, TabStore store) {
        Map<ResourceLocation, List<AdvancementNode>> rootsByTab = new LinkedHashMap<>();
        Map<ResourceLocation, ResourceLocation> claimedBy = new HashMap<>();

        for (TabDefinition def : store.tabs()) {
            if (def.deleted) continue;
            rootsByTab.put(def.id, new ArrayList<>());
            for (ResourceLocation rootId : def.roots) {
                claimedBy.putIfAbsent(rootId, def.id);
            }
        }

        Set<ResourceLocation> placed = new LinkedHashSet<>();
        for (TabDefinition def : store.tabs()) {
            if (def.deleted) continue;
            for (ResourceLocation rootId : def.roots) {
                AdvancementNode node = tree.get(rootId);
                if (node != null && node.parent() == null && placed.add(rootId)) {
                    rootsByTab.get(def.id).add(node);
                }
            }
        }

        for (AdvancementNode root : tree.roots()) {
            ResourceLocation rootId = root.holder().id();
            if (placed.contains(rootId)) continue;

            ResourceLocation owner = claimedBy.get(rootId);
            if (owner != null) {
                rootsByTab.get(owner).add(root);
                placed.add(rootId);
            } else if (root.advancement().display().isPresent() && !store.isTabDeleted(rootId)) {
                rootsByTab.computeIfAbsent(rootId, k -> new ArrayList<>()).add(root);
                placed.add(rootId);
            }
        }

        List<ResolvedTab> resolved = new ArrayList<>();
        for (Map.Entry<ResourceLocation, List<AdvancementNode>> entry : rootsByTab.entrySet()) {
            ResourceLocation tabId = entry.getKey();
            List<AdvancementNode> roots = entry.getValue();
            roots.sort((r1, r2) -> {
                boolean match1 = r1.holder().id().equals(tabId);
                boolean match2 = r2.holder().id().equals(tabId);
                if (match1 != match2) return match1 ? -1 : 1;

                boolean decl1 = declaresTab(r1);
                boolean decl2 = declaresTab(r2);
                if (decl1 != decl2) return decl1 ? -1 : 1;

                return r1.holder().id().compareTo(r2.holder().id());
            });
            resolved.add(build(tabId, roots, store.tab(tabId), store.presentation(tabId)));
        }
        return resolved;
    }

    public static ResolvedTab describe(TabDefinition def, TabStore store) {
        return build(def.id, List.of(), def, store.presentation(def.id));
    }

    private static ResolvedTab build(ResourceLocation id, List<AdvancementNode> roots,
                                     @Nullable TabDefinition def, @Nullable TabStore.Presentation seen) {
        DisplayInfo source = null;
        for (AdvancementNode root : roots) {
            if (root.holder().id().equals(id) && root.advancement().display().isPresent()) {
                source = root.advancement().display().get();
                break;
            }
        }
        if (source == null) {
            for (AdvancementNode root : roots) {
                if (declaresTab(root)) {
                    source = root.advancement().display().get();
                    break;
                }
            }
        }
        if (source == null) {
            for (AdvancementNode root : roots) {
                Optional<DisplayInfo> display = root.advancement().display();
                if (display.isPresent()) {
                    source = display.get();
                    break;
                }
            }
        }

        Component title;
        if (def != null && def.title != null && !def.title.isEmpty()) {
            title = Component.literal(def.title);
        } else if (def != null && seen != null && seen.title() != null) {
            title = Component.literal(seen.title());
        } else if (source != null) {
            title = source.getTitle();
        } else if (seen != null && seen.title() != null) {
            title = Component.literal(seen.title());
        } else {
            title = Component.literal(id.getPath());
        }

        ItemStack icon;
        if (def != null && def.icon != null) {
            icon = new ItemStack(BuiltInRegistries.ITEM.getOptional(def.icon).orElse(Items.STONE));
        } else if (def != null && seen != null && seen.icon() != null) {
            icon = new ItemStack(BuiltInRegistries.ITEM.getOptional(seen.icon()).orElse(Items.STONE));
        } else if (source != null) {
            icon = source.getIcon();
        } else if (seen != null && seen.icon() != null) {
            icon = new ItemStack(BuiltInRegistries.ITEM.getOptional(seen.icon()).orElse(Items.STONE));
        } else {
            icon = new ItemStack(Items.STONE);
        }

        ResourceLocation background = null;
        if (def != null && def.background != null) {
            background = def.background;
        } else if (source != null && source.getBackground().isPresent()) {
            background = source.getBackground().get();
        } else {
            for (AdvancementNode root : roots) {
                Optional<DisplayInfo> display = root.advancement().display();
                if (display.isPresent() && display.get().getBackground().isPresent()) {
                    background = display.get().getBackground().get();
                    break;
                }
            }
        }
        if (background == null && seen != null && seen.background() != null) {
            background = seen.background();
        }
        if (background == null) {
            background = DEFAULT_BACKGROUND;
        }

        return new ResolvedTab(
                id,
                title,
                icon,
                background,
                def != null && def.staticBackground,
                def != null ? def.bgWidth : TabDefinition.DEFAULT_TILE,
                def != null ? def.bgHeight : TabDefinition.DEFAULT_TILE,
                def != null ? def.windowWidth : 0,
                def != null ? def.windowHeight : 0,
                def != null && def.index != null ? def.index : defaultIndex(id),
                def != null && def.backgroundRules != null ? def.backgroundRules : "[]",
                Collections.unmodifiableList(roots),
                def
        );
    }

    public static Map<ResourceLocation, ResourceLocation> advancementToTab(AdvancementTree tree, List<ResolvedTab> tabs) {
        Map<ResourceLocation, ResourceLocation> rootToTab = new HashMap<>();
        for (ResolvedTab tab : tabs) {
            for (AdvancementNode root : tab.roots()) {
                rootToTab.put(root.holder().id(), tab.id());
            }
        }

        Map<ResourceLocation, ResourceLocation> result = new HashMap<>();
        List<AdvancementNode> unplaced = new ArrayList<>();
        for (AdvancementNode node : tree.nodes()) {
            ResourceLocation tabId = rootToTab.get(node.root().holder().id());
            if (tabId != null) {
                result.put(node.holder().id(), tabId);
            } else {
                unplaced.add(node);
            }
        }

        unplaced.sort(Comparator.comparing(node -> node.holder().id()));
        boolean progressed;
        do {
            progressed = false;
            for (AdvancementNode node : unplaced) {
                if (result.containsKey(node.holder().id())) continue;
                for (AdvancementNode parent : IMultiParentNode.getParents(node)) {
                    ResourceLocation tabId = parent == null ? null : result.get(parent.holder().id());
                    if (tabId != null) {
                        result.put(node.holder().id(), tabId);
                        progressed = true;
                        break;
                    }
                }
            }
        } while (progressed);
        return result;
    }
}
