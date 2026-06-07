package com.evandev.reliable_advancements.network;

import com.evandev.reliable_advancements.reference.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record ClaimRewardPayload(Identifier advancementId) implements CustomPacketPayload {
    public static final Type<ClaimRewardPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "claim_reward"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClaimRewardPayload> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, ClaimRewardPayload::advancementId,
            ClaimRewardPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}