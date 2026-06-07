package com.evandev.reliable_advancements.client;

import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.Set;

public class ClientRewardTracker {
    public static final Set<Identifier> CLAIMED = new HashSet<>();

    public static boolean isClaimed(Identifier id) {
        return CLAIMED.contains(id);
    }
}