package com.evandev.reliable_advancements.network;

import com.evandev.reliable_advancements.reference.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record AdvancementBatchPayload(Op op, List<ResourceLocation> advancementIds) implements CustomPacketPayload {
    public static final Type<AdvancementBatchPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "advancement_batch"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AdvancementBatchPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(Op::byId, Op::ordinal), AdvancementBatchPayload::op,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), AdvancementBatchPayload::advancementIds,
            AdvancementBatchPayload::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum Op {
        DELETE,
        RESET_TO_VANILLA,
        RESTORE,
        PERMANENT_DELETE;

        private static final Op[] VALUES = values();

        public static Op byId(int id) {
            return id >= 0 && id < VALUES.length ? VALUES[id] : DELETE;
        }
    }
}
