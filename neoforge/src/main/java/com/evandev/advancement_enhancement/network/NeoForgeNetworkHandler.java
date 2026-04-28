package com.evandev.advancement_enhancement.network;

import com.evandev.advancement_enhancement.reference.Constants;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NeoForgeNetworkHandler {
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Constants.MOD_ID).optional();

        registrar.playToServer(EditAdvancementPayload.TYPE, EditAdvancementPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> ServerAdvancementEditor.saveAdvancementEdit(context.player().getServer(), (ServerPlayer) context.player(), payload));
        });

        registrar.playToServer(RequestAdvancementJsonPayload.TYPE, RequestAdvancementJsonPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> ServerAdvancementEditor.handleJsonRequest(context.player().getServer(), (ServerPlayer) context.player(), payload));
        });

        registrar.playToServer(LinkAdvancementPayload.TYPE, LinkAdvancementPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> ServerAdvancementEditor.handleLinkAdvancement(context.player().getServer(), (ServerPlayer) context.player(), payload));
        });

        registrar.playToClient(AdvancementJsonPayload.TYPE, AdvancementJsonPayload.STREAM_CODEC, (payload, context) -> {
            context.enqueueWork(() -> ClientNetworkHandler.handleAdvancementJson(payload));
        });
    }
}