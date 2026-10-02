package com.evandev.reliable_advancements.neoforge;

//? if neoforge {
import com.evandev.reliable_advancements.neoforge.client.NeoForgeClientSetup;
import com.evandev.reliable_advancements.network.ServerAdvancementEditor;
import com.evandev.reliable_advancements.reference.Constants;
import com.evandev.reliable_advancements.tabs.ServerTabManager;
import com.evandev.reliable_advancements.util.RewardTrackerData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@Mod(Constants.MOD_ID)
public class ReliableAdvancementsNeoForge {
    public ReliableAdvancementsNeoForge(ModContainer container, IEventBus modEventBus) {
        modEventBus.addListener(NeoForgeNetworkHandler::register);
        NeoForge.EVENT_BUS.addListener(this::onPlayerJoin);
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);

        //? if <26.1 {
        if (FMLLoader.getDist() == Dist.CLIENT) {
        //?} else {
        /*if (FMLLoader.getCurrent().getDist() == Dist.CLIENT) {
        *///?}
            NeoForgeClientSetup.init(container);
        }
    }

    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            RewardTrackerData.get(serverPlayer.level().getServer()).syncToPlayer(serverPlayer);
            ServerTabManager.syncToPlayer(serverPlayer);
        }
    }

    private void onServerStarted(ServerStartedEvent event) {
        ServerAdvancementEditor.reapplyAllEdits(event.getServer());
    }
}
//?}
