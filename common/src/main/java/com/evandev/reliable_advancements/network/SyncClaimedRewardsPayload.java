package com.evandev.reliable_advancements.network;

import com.evandev.reliable_advancements.reference.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record SyncClaimedRewardsPayload(List<Identifier> claimedIds) implements CustomPacketPayload {
    public static final Type<SyncClaimedRewardsPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "sync_claimed_rewards"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncClaimedRewardsPayload> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), SyncClaimedRewardsPayload::claimedIds,
            SyncClaimedRewardsPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}