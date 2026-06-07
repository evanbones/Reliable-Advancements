package com.evandev.reliable_advancements.network;

import com.evandev.reliable_advancements.reference.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record LinkAdvancementPayload(Identifier childId,
                                     Identifier parentId) implements CustomPacketPayload {
    public static final Type<LinkAdvancementPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "link_advancement"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LinkAdvancementPayload> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, LinkAdvancementPayload::childId,
            Identifier.STREAM_CODEC, LinkAdvancementPayload::parentId,
            LinkAdvancementPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}