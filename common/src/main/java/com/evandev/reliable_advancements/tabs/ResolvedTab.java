package com.evandev.reliable_advancements.tabs;

import net.minecraft.advancements.AdvancementNode;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record ResolvedTab(
        ResourceLocation id,
        Component title,
        ItemStack icon,
        ResourceLocation background,
        boolean staticBackground,
        int bgWidth,
        int bgHeight,
        int windowWidth,
        int windowHeight,
        int index,
        String backgroundRules,
        List<AdvancementNode> roots,
        @Nullable TabDefinition definition
) {
    public @Nullable AdvancementNode primaryRoot() {
        if (roots.isEmpty()) return null;
        for (AdvancementNode root : roots) {
            if (root.holder().id().equals(id)) {
                return root;
            }
        }
        for (AdvancementNode root : roots) {
            if (TabResolver.declaresTab(root)) {
                return root;
            }
        }
        return roots.getFirst();
    }
}
