package com.evandev.reliable_advancements.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record ResetTabPayload(List<ResourceLocation> advancementIds) {
    public static final ResourceLocation ID = new ResourceLocation(com.evandev.reliable_advancements.reference.Constants.MOD_ID, "reset_tab");

    public ResetTabPayload(FriendlyByteBuf buf) {
        this(buf.readList(FriendlyByteBuf::readResourceLocation));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeCollection(advancementIds, FriendlyByteBuf::writeResourceLocation);
    }
}