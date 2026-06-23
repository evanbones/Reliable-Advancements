package com.evandev.reliable_advancements.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record SyncClaimedRewardsPayload(List<ResourceLocation> claimedIds) {
    public static final ResourceLocation ID = new ResourceLocation(com.evandev.reliable_advancements.reference.Constants.MOD_ID, "sync_claimed_rewards");

    public SyncClaimedRewardsPayload(FriendlyByteBuf buf) {
        this(buf.readList(FriendlyByteBuf::readResourceLocation));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeCollection(claimedIds, FriendlyByteBuf::writeResourceLocation);
    }
}