package com.evandev.reliable_advancements.network;

import com.evandev.reliable_advancements.reference.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record LinkAdvancementPayload(ResourceLocation childId,
                                     ResourceLocation parentId,
                                     boolean unlink) implements CustomPacketPayload {
    public static final Type<LinkAdvancementPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "link_advancement"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LinkAdvancementPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, LinkAdvancementPayload::childId,
            ResourceLocation.STREAM_CODEC, LinkAdvancementPayload::parentId,
            ByteBufCodecs.BOOL, LinkAdvancementPayload::unlink,
            LinkAdvancementPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}