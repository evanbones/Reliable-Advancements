package com.evandev.reliable_advancements.mixin;

import com.evandev.reliable_advancements.advancements.IAdvancementSyncListener;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientAdvancements.class)
public abstract class ClientAdvancementsMixin {

    @Shadow
    @Nullable
    private ClientAdvancements.Listener listener;

    @Inject(method = "update", at = @At("RETURN"))
    private void reliable_advancements$onUpdateComplete(ClientboundUpdateAdvancementsPacket packet, CallbackInfo ci) {
        if (this.listener instanceof IAdvancementSyncListener syncListener) {
            syncListener.onAdvancementSyncComplete();
        }
    }
}
