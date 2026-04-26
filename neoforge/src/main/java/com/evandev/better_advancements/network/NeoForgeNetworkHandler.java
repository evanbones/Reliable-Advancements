package com.evandev.better_advancements.network;

import com.evandev.better_advancements.network.EditAdvancementPayload;
import com.evandev.better_advancements.network.ServerAdvancementEditor;
import com.evandev.better_advancements.reference.Constants;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Constants.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class NeoForgeNetworkHandler {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1.0").optional();

        registrar.playToServer(
                EditAdvancementPayload.TYPE,
                EditAdvancementPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        MinecraftServer server = context.player().getServer();
                        if (server != null && server.getPlayerList().isOp(context.player().getGameProfile())) {
                            ServerAdvancementEditor.saveAdvancementEdit(server, payload);
                        }
                    });
                }
        );
    }
}