package com.evandev.reliable_advancements.network;

import com.evandev.reliable_advancements.reference.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record RequestAdvancementJsonPayload(ResourceLocation advancementId, String initialTab) {
    public static final ResourceLocation ID = new ResourceLocation(Constants.MOD_ID, "request_advancement_json");

    public RequestAdvancementJsonPayload(FriendlyByteBuf buf) {
        this(buf.readResourceLocation(), buf.readUtf());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(advancementId);
        buf.writeUtf(initialTab);
    }
}