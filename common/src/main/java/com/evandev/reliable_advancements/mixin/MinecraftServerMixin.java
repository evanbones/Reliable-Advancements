package com.evandev.reliable_advancements.mixin;

import com.evandev.reliable_advancements.network.ServerAdvancementEditor;
import com.evandev.reliable_advancements.reference.Constants;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.CompletableFuture;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @ModifyReturnValue(method = "reloadResources", at = @At("RETURN"))
    private CompletableFuture<Void> reliable_advancements$reapplyEditsAfterReload(CompletableFuture<Void> original) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        return original.whenComplete((ignored, error) -> server.execute(() -> {
            if (error != null) {
                Constants.LOG.error("Resource reload failed; reapplying saved advancement edits anyway", error);
            }
            ServerAdvancementEditor.reapplyAllEdits(server);
        }));
    }
}
