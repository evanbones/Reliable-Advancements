package com.evandev.reliable_advancements.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record LinkAdvancementPayload(ResourceLocation childId, ResourceLocation parentId) {
    public static final ResourceLocation ID = new ResourceLocation(com.evandev.reliable_advancements.reference.Constants.MOD_ID, "link_advancement");

    public LinkAdvancementPayload(FriendlyByteBuf buf) {
        this(buf.readResourceLocation(), buf.readResourceLocation());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(childId);
        buf.writeResourceLocation(parentId);
    }
}