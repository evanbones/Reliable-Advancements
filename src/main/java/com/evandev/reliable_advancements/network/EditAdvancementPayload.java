package com.evandev.reliable_advancements.network;

import com.evandev.reliable_advancements.reference.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record EditAdvancementPayload(ResourceLocation advancementId, String jsonPayload, boolean isDelete) implements CustomPacketPayload {
    public static final Type<EditAdvancementPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "edit_advancement"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EditAdvancementPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, EditAdvancementPayload::advancementId,
            ByteBufCodecs.stringUtf8(1048576), EditAdvancementPayload::jsonPayload,
            ByteBufCodecs.BOOL, EditAdvancementPayload::isDelete,
            EditAdvancementPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}