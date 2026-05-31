package com.evandev.reliable_advancements.network;

import com.evandev.reliable_advancements.reference.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record RequestFullTreePayload() implements CustomPacketPayload {
    public static final Type<RequestFullTreePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "request_full_tree"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestFullTreePayload> STREAM_CODEC = StreamCodec.unit(new RequestFullTreePayload());

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}