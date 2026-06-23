package com.evandev.reliable_advancements.network;

import com.evandev.reliable_advancements.reference.Constants;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ForgeNetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Constants.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;

        CHANNEL.registerMessage(id++, EditAdvancementPayload.class,
                EditAdvancementPayload::write,
                EditAdvancementPayload::new,
                (payload, contextSupplier) -> {
                    var context = contextSupplier.get();
                    context.enqueueWork(() -> ServerAdvancementEditor.saveAdvancementEdit(
                            context.getSender().getServer(), context.getSender(), payload
                    ));
                    context.setPacketHandled(true);
                }
        );

        CHANNEL.registerMessage(id++, RequestAdvancementJsonPayload.class,
                RequestAdvancementJsonPayload::write,
                RequestAdvancementJsonPayload::new,
                (payload, contextSupplier) -> {
                    var context = contextSupplier.get();
                    context.enqueueWork(() -> ServerAdvancementEditor.handleJsonRequest(
                            context.getSender().getServer(), context.getSender(), payload
                    ));
                    context.setPacketHandled(true);
                }
        );

        CHANNEL.registerMessage(id++, LinkAdvancementPayload.class,
                LinkAdvancementPayload::write,
                LinkAdvancementPayload::new,
                (payload, contextSupplier) -> {
                    var context = contextSupplier.get();
                    context.enqueueWork(() -> ServerAdvancementEditor.handleLinkAdvancement(
                            context.getSender().getServer(), context.getSender(), payload
                    ));
                    context.setPacketHandled(true);
                }
        );

        CHANNEL.registerMessage(id++, ClaimRewardPayload.class,
                ClaimRewardPayload::write,
                ClaimRewardPayload::new,
                (payload, contextSupplier) -> {
                    var context = contextSupplier.get();
                    context.enqueueWork(() -> ServerAdvancementEditor.handleRewardClaim(
                            context.getSender().getServer(), context.getSender(), payload
                    ));
                    context.setPacketHandled(true);
                }
        );

        CHANNEL.registerMessage(id++, RequestFullTreePayload.class,
                RequestFullTreePayload::write,
                RequestFullTreePayload::new,
                (payload, contextSupplier) -> {
                    var context = contextSupplier.get();
                    context.enqueueWork(() -> ServerAdvancementEditor.handleRequestFullTree(
                            context.getSender().getServer(), context.getSender()
                    ));
                    context.setPacketHandled(true);
                }
        );

        CHANNEL.registerMessage(id++, ResetTabPayload.class,
                ResetTabPayload::write,
                ResetTabPayload::new,
                (payload, contextSupplier) -> {
                    var context = contextSupplier.get();
                    context.enqueueWork(() -> ServerAdvancementEditor.handleResetTab(
                            context.getSender().getServer(), context.getSender(), payload
                    ));
                    context.setPacketHandled(true);
                }
        );

        CHANNEL.registerMessage(id++, AdvancementJsonPayload.class,
                AdvancementJsonPayload::write,
                AdvancementJsonPayload::new,
                (payload, contextSupplier) -> {
                    var context = contextSupplier.get();
                    context.enqueueWork(() -> ClientNetworkHandler.handleAdvancementJson(payload));
                    context.setPacketHandled(true);
                }
        );

        CHANNEL.registerMessage(id++, SyncClaimedRewardsPayload.class,
                SyncClaimedRewardsPayload::write,
                SyncClaimedRewardsPayload::new,
                (payload, contextSupplier) -> {
                    var context = contextSupplier.get();
                    context.enqueueWork(() -> ClientNetworkHandler.handleSyncClaimedRewards(payload));
                    context.setPacketHandled(true);
                }
        );
    }
}