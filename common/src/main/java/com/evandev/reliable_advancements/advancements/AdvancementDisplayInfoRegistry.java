package com.evandev.reliable_advancements.advancements;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class AdvancementDisplayInfoRegistry {
    private final Map<ResourceLocation, AdvancementDisplayInfo> registry = new HashMap<>();

    public AdvancementDisplayInfo get(AdvancementHolder advancementHolder) {
        return registry.computeIfAbsent(advancementHolder.id(), id -> new AdvancementDisplayInfo(advancementHolder));
    }
}
