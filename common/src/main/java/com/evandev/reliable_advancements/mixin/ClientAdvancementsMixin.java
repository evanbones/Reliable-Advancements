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

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import java.util.HashSet;
import java.util.Set;

@Mixin(ClientAdvancements.class)
public abstract class ClientAdvancementsMixin {

    @Shadow
    @Final
    private AdvancementTree tree;

    @Shadow
    @Nullable
    private ClientAdvancements.Listener listener;

    @Inject(method = "update", at = @At("HEAD"))
    private void reliable_advancements$cleanUpdatedNodesBeforeAdd(ClientboundUpdateAdvancementsPacket packet, CallbackInfo ci) {
        if (!packet.shouldReset() && !packet.getAdded().isEmpty()) {
            Set<ResourceLocation> existingIds = new HashSet<>();
            for (AdvancementHolder holder : packet.getAdded()) {
                if (this.tree.get(holder.id()) != null) {
                    existingIds.add(holder.id());
                }
            }
            if (!existingIds.isEmpty()) {
                this.tree.remove(existingIds);
            }
        }
    }

    @Inject(method = "update", at = @At("RETURN"))
    private void reliable_advancements$onUpdateComplete(ClientboundUpdateAdvancementsPacket packet, CallbackInfo ci) {
        if (this.listener instanceof IAdvancementSyncListener syncListener) {
            syncListener.onAdvancementSyncComplete();
        }
    }
}
