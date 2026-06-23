package com.evandev.reliable_advancements.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record EditAdvancementPayload(ResourceLocation advancementId, String jsonPayload, boolean isDelete) {
    public static final ResourceLocation ID = new ResourceLocation(com.evandev.reliable_advancements.reference.Constants.MOD_ID, "edit_advancement");

    public EditAdvancementPayload(FriendlyByteBuf buf) {
        this(buf.readResourceLocation(), buf.readUtf(1048576), buf.readBoolean());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(advancementId);
        buf.writeUtf(jsonPayload, 1048576);
        buf.writeBoolean(isDelete);
    }
}