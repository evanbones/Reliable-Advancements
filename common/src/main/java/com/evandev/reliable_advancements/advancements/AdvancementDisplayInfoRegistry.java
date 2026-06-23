package com.evandev.reliable_advancements.advancements;

import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class AdvancementDisplayInfoRegistry {
    private final Map<ResourceLocation, AdvancementDisplayInfo> registry;

    public AdvancementDisplayInfoRegistry(Advancement advancement) {
        registry = new HashMap<>();
    }

    public AdvancementDisplayInfo get(Advancement advancement) {
        return registry.getOrDefault(advancement.getId(), new AdvancementDisplayInfo(advancement));
    }
}
