package com.evandev.advancement_enhancement.network;

import com.evandev.advancement_enhancement.reference.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record LinkAdvancementPayload(ResourceLocation childId,
                                     ResourceLocation parentId) implements CustomPacketPayload {
    public static final Type<LinkAdvancementPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "link_advancement"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LinkAdvancementPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, LinkAdvancementPayload::childId,
            ResourceLocation.STREAM_CODEC, LinkAdvancementPayload::parentId,
            LinkAdvancementPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}