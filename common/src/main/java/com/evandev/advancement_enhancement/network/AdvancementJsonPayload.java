package com.evandev.advancement_enhancement.network;

import com.evandev.advancement_enhancement.reference.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record AdvancementJsonPayload(ResourceLocation advancementId, String jsonPayload,
                                     String initialTab) implements CustomPacketPayload {
    public static final Type<AdvancementJsonPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "advancement_json"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AdvancementJsonPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, AdvancementJsonPayload::advancementId,
            ByteBufCodecs.stringUtf8(1048576), AdvancementJsonPayload::jsonPayload,
            ByteBufCodecs.STRING_UTF8, AdvancementJsonPayload::initialTab,
            AdvancementJsonPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}