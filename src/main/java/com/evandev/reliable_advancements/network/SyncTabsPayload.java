package com.evandev.reliable_advancements.network;

import com.evandev.reliable_advancements.reference.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record SyncTabsPayload(String jsonPayload) implements CustomPacketPayload {
    public static final Type<SyncTabsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "sync_tabs"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncTabsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(4194304), SyncTabsPayload::jsonPayload,
            SyncTabsPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
