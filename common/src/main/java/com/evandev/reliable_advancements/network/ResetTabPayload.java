package com.evandev.reliable_advancements.network;

import com.evandev.reliable_advancements.reference.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record ResetTabPayload(Identifier rootAdvancementId, List<Identifier> advancementIds) implements CustomPacketPayload {
    public static final Type<ResetTabPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "reset_tab"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ResetTabPayload> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, ResetTabPayload::rootAdvancementId,
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), ResetTabPayload::advancementIds,
            ResetTabPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
