package com.evandev.reliable_advancements.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record ClaimRewardPayload(ResourceLocation advancementId) {
    public static final ResourceLocation ID = new ResourceLocation(com.evandev.reliable_advancements.reference.Constants.MOD_ID, "claim_reward");

    public ClaimRewardPayload(FriendlyByteBuf buf) {
        this(buf.readResourceLocation());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(advancementId);
    }
}