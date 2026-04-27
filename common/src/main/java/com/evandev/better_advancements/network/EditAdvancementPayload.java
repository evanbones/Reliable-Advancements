package com.evandev.better_advancements.network;

import com.evandev.better_advancements.reference.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record EditAdvancementPayload(ResourceLocation advancementId, String title, String description, String iconId,
                                     String parentId) implements CustomPacketPayload {
    public static final Type<EditAdvancementPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "edit_advancement"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EditAdvancementPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, EditAdvancementPayload::advancementId,
            ByteBufCodecs.STRING_UTF8, EditAdvancementPayload::title,
            ByteBufCodecs.STRING_UTF8, EditAdvancementPayload::description,
            ByteBufCodecs.STRING_UTF8, EditAdvancementPayload::iconId,
            ByteBufCodecs.STRING_UTF8, EditAdvancementPayload::parentId,
            EditAdvancementPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}