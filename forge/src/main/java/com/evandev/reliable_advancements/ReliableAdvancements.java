package com.evandev.reliable_advancements;

import com.evandev.reliable_advancements.client.ClientSetup;
import com.evandev.reliable_advancements.network.ForgeNetworkHandler;
import com.evandev.reliable_advancements.reference.Constants;
import com.evandev.reliable_advancements.util.RewardTrackerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;

@Mod(Constants.MOD_ID)
public class ReliableAdvancements {
    public ReliableAdvancements() {
        ForgeNetworkHandler.register();
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerJoin);

        if (FMLLoader.getDist() == Dist.CLIENT) {
            ClientSetup.init();
        }
    }

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            RewardTrackerData.get(serverPlayer.server).syncToPlayer(serverPlayer);
        }
    }
}