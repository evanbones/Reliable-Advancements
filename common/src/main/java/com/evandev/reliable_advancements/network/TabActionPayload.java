package com.evandev.reliable_advancements.network;

import com.evandev.reliable_advancements.reference.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record TabActionPayload(Action action, ResourceLocation tabId,
                               String jsonPayload) implements CustomPacketPayload {
    public static final Type<TabActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "tab_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TabActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(Action::byId, Action::ordinal), TabActionPayload::action,
            ResourceLocation.STREAM_CODEC, TabActionPayload::tabId,
            ByteBufCodecs.stringUtf8(1048576), TabActionPayload::jsonPayload,
            TabActionPayload::new
    );

    public static TabActionPayload of(Action action, ResourceLocation tabId) {
        return new TabActionPayload(action, tabId, "");
    }

    public static TabActionPayload addRoot(ResourceLocation tabId, ResourceLocation advancementId) {
        return new TabActionPayload(Action.ADD_ROOT, tabId, advancementId.toString());
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum Action {
        SAVE,
        SET_POSITIONS,
        ADD_ROOT,
        DELETE,
        RESTORE,
        RESET_TO_VANILLA,
        MIGRATE_CLIENT_LAYOUT,
        CACHE_PRESENTATION,
        PERMANENT_DELETE;

        private static final Action[] VALUES = values();

        public static Action byId(int id) {
            return id >= 0 && id < VALUES.length ? VALUES[id] : SAVE;
        }
    }
}
