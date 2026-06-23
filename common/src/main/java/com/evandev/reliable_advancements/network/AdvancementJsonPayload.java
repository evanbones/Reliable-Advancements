package com.evandev.reliable_advancements.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record AdvancementJsonPayload(ResourceLocation advancementId, String jsonPayload, String initialTab) {
    public static final ResourceLocation ID = new ResourceLocation(com.evandev.reliable_advancements.reference.Constants.MOD_ID, "advancement_json");

    public AdvancementJsonPayload(FriendlyByteBuf buf) {
        this(buf.readResourceLocation(), buf.readUtf(1048576), buf.readUtf());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(advancementId);
        buf.writeUtf(jsonPayload, 1048576);
        buf.writeUtf(initialTab);
    }
}