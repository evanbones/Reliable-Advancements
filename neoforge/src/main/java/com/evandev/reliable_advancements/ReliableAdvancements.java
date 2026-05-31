package com.evandev.reliable_advancements;

import com.evandev.reliable_advancements.client.ClientSetup;
import com.evandev.reliable_advancements.network.NeoForgeNetworkHandler;
import com.evandev.reliable_advancements.reference.Constants;
import com.evandev.reliable_advancements.util.RewardTrackerData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@Mod(Constants.MOD_ID)
public class ReliableAdvancements {
    public ReliableAdvancements(ModContainer container, IEventBus modEventBus) {
        modEventBus.addListener(NeoForgeNetworkHandler::register);
        NeoForge.EVENT_BUS.addListener(this::onPlayerJoin);

        if (FMLLoader.getDist() == Dist.CLIENT) {
            ClientSetup.init(container);
        }
    }

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            RewardTrackerData.get(serverPlayer.server).syncToPlayer(serverPlayer);
        }
    }
}