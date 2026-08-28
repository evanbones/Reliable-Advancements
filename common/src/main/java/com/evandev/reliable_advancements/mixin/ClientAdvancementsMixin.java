package com.evandev.reliable_advancements.mixin;

import com.evandev.reliable_advancements.advancements.IAdvancementSyncListener;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(ClientAdvancements.class)
public abstract class ClientAdvancementsMixin {

    @Shadow
    @Nullable
    private ClientAdvancements.Listener listener;

    @WrapOperation(
            method = "update",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    )
    private Object reliable_advancements$captureOldProgress(
            Map<?, ?> map,
            Object key,
            Object value,
            Operation<Object> original,
            @Share("wasAlreadyDone") LocalBooleanRef wasAlreadyDone
    ) {
        Object oldProgress = map.get(key);
        wasAlreadyDone.set(oldProgress instanceof AdvancementProgress prog && prog.isDone());
        return original.call(map, key, value);
    }

    @WrapOperation(
            method = "update",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/advancements/AdvancementProgress;isDone()Z")
    )
    private boolean reliable_advancements$suppressDuplicateToast(
            AdvancementProgress instance,
            Operation<Boolean> original,
            @Share("wasAlreadyDone") LocalBooleanRef wasAlreadyDone
    ) {
        boolean isDone = original.call(instance);
        return isDone && !wasAlreadyDone.get();
    }

    @Inject(method = "update", at = @At("RETURN"))
    private void reliable_advancements$onUpdateComplete(ClientboundUpdateAdvancementsPacket packet, CallbackInfo ci) {
        if (this.listener instanceof IAdvancementSyncListener syncListener) {
            syncListener.onAdvancementSyncComplete();
        }
    }
}
