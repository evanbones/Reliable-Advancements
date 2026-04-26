package com.evandev.better_advancements.platform;

import com.evandev.better_advancements.network.EditAdvancementPayload;
import com.evandev.better_advancements.platform.services.IEventHelper;
import com.evandev.better_advancements.platform.services.IPlatformHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.file.Path;

public class NeoForgePlatformHelper implements IPlatformHelper {
    private final NeoForgeEventHelper eventHelper = new NeoForgeEventHelper();
    private final NeoForgeAdvancementVisitor advancementVisitor = new NeoForgeAdvancementVisitor();

    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }

    @Override
    public Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public boolean isPhysicalClient() {
        return FMLLoader.getDist() == Dist.CLIENT;
    }

    @Override
    public IEventHelper getEventHelper() {
        return eventHelper;
    }

    @Override
    public NeoForgeAdvancementVisitor getAdvancementVisitor() {
        return advancementVisitor;
    }

    @Override
    public boolean canSendAdvancementEdit() {
        return true;
    }

    @Override
    public void sendAdvancementEdit(EditAdvancementPayload payload) {
        PacketDistributor.sendToServer(payload);
    }
}