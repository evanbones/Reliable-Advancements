package com.evandev.better_advancements.network;

import com.evandev.better_advancements.reference.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record RequestAdvancementJsonPayload(ResourceLocation advancementId,
                                            String initialTab) implements CustomPacketPayload {
    public static final Type<RequestAdvancementJsonPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "request_advancement_json"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestAdvancementJsonPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, RequestAdvancementJsonPayload::advancementId,
            ByteBufCodecs.STRING_UTF8, RequestAdvancementJsonPayload::initialTab,
            RequestAdvancementJsonPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}