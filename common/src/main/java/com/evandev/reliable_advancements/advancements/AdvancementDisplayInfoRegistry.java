package com.evandev.reliable_advancements.advancements;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class AdvancementDisplayInfoRegistry {
    private final Map<ResourceLocation, AdvancementDisplayInfo> registry;

    public AdvancementDisplayInfoRegistry(AdvancementNode advancementNode) {
        registry = new HashMap<>();
    }

    public AdvancementDisplayInfo get(AdvancementHolder advancementHolder) {
        return registry.getOrDefault(advancementHolder.id(), new AdvancementDisplayInfo(advancementHolder));
    }
}
