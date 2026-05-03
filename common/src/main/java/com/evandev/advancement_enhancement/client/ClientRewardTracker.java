package com.evandev.advancement_enhancement.client;

import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

public class ClientRewardTracker {
    public static final Set<ResourceLocation> CLAIMED = new HashSet<>();

    public static boolean isClaimed(ResourceLocation id) {
        return CLAIMED.contains(id);
    }
}