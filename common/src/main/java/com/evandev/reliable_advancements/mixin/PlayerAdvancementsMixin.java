package com.evandev.reliable_advancements.mixin;

import com.evandev.reliable_advancements.config.ModConfig;
import com.evandev.reliable_advancements.util.RewardTrackerData;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {

    @Shadow
    private ServerPlayer player;

    @Inject(method = "reload", at = @At("HEAD"))
    private void reliable_advancements$saveBeforeReload(ServerAdvancementManager manager, CallbackInfo ci) {
        ((PlayerAdvancements) (Object) this).save();
    }

    @WrapOperation(method = "award", at = @At(value = "INVOKE", target = "Lnet/minecraft/advancements/AdvancementRewards;grant(Lnet/minecraft/server/level/ServerPlayer;)V"))
    private void interceptRewardGrant(AdvancementRewards rewards, ServerPlayer player, Operation<Void> original) {
        if (!ModConfig.get().requireRewardClaiming) {
            original.call(rewards, player);
        }
    }

    @Inject(method = "revoke", at = @At("RETURN"))
    private void onRevoke(AdvancementHolder advancement, String criterionKey, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            PlayerAdvancements advancements = (PlayerAdvancements) (Object) this;
            if (!advancements.getOrStartProgress(advancement).isDone()) {
                RewardTrackerData.get(this.player.server).unclaim(this.player.getUUID(), advancement.id());
                RewardTrackerData.get(this.player.server).syncToPlayer(this.player);
            }
        }
    }
}