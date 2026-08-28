package com.evandev.reliable_advancements.mixin;

import com.evandev.reliable_advancements.advancements.IMultiParentNode;
import com.evandev.reliable_advancements.config.ModConfig;
import com.evandev.reliable_advancements.util.RewardTrackerData;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.advancements.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.advancements.AdvancementVisibilityEvaluator;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {

    @Shadow
    private ServerPlayer player;

    @Shadow
    private AdvancementTree tree;

    @Shadow
    @Final
    private Set<AdvancementNode> rootsToUpdate;

    @Shadow
    @Final
    private Set<AdvancementHolder> visible;

    @Shadow
    @Final
    private Set<AdvancementHolder> progressChanged;

    @Unique
    private static void reliable_advancements$collectAllRoots(AdvancementNode node, Set<AdvancementNode> roots, Set<AdvancementNode> visited) {
        if (node == null || !visited.add(node)) return;
        List<AdvancementNode> parents = IMultiParentNode.getParents(node);
        if (parents.isEmpty()) {
            roots.add(node);
        } else {
            for (AdvancementNode parent : parents) {
                if (parent != null) {
                    reliable_advancements$collectAllRoots(parent, roots, visited);
                }
            }
        }
    }

    @Shadow
    public abstract AdvancementProgress getOrStartProgress(AdvancementHolder advancement);

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

    @Inject(method = "markForVisibilityUpdate", at = @At("HEAD"), cancellable = true)
    private void reliable_advancements$markAllRootsForUpdate(AdvancementHolder advancement, CallbackInfo ci) {
        AdvancementNode advancementNode = this.tree.get(advancement);
        if (advancementNode != null) {
            Set<AdvancementNode> roots = new HashSet<>();
            reliable_advancements$collectAllRoots(advancementNode, roots, new HashSet<>());
            this.rootsToUpdate.addAll(roots);
            ci.cancel();
        }
    }

    @Inject(method = "updateTreeVisibility", at = @At("HEAD"), cancellable = true)
    private void reliable_advancements$updateTreeVisibilityMultiParent(
            AdvancementNode root,
            Set<AdvancementHolder> advancementOutput,
            Set<ResourceLocation> idOutput,
            CallbackInfo ci
    ) {
        AdvancementVisibilityEvaluator.evaluateVisibility(
                root,
                node -> this.getOrStartProgress(node.holder()).isDone(),
                (node, isVisible) -> {
                    AdvancementHolder holder = node.holder();
                    if (isVisible) {
                        if (this.visible.add(holder)) {
                            advancementOutput.add(holder);
                            this.progressChanged.add(holder);
                        }
                    } else {
                        if (!reliable_advancements$isNodeVisibleInAnyRoot(node, new HashSet<>())) {
                            if (this.visible.remove(holder)) {
                                idOutput.add(holder.id());
                            }
                        }
                    }
                }
        );
        ci.cancel();
    }

    @Unique
    private boolean reliable_advancements$isNodeVisibleInAnyRoot(AdvancementNode node, Set<AdvancementNode> visited) {
        if (node == null || !visited.add(node)) {
            return false;
        }

        if (this.getOrStartProgress(node.holder()).isDone()) {
            return true;
        }

        if (reliable_advancements$hasDoneDescendant(node, new HashSet<>())) {
            return true;
        }

        int maxDepth = ModConfig.get().visibilityDepth;

        return reliable_advancements$checkAncestorPathVisible(node, 0, maxDepth, new HashSet<>());
    }

    @Unique
    private boolean reliable_advancements$hasDoneDescendant(AdvancementNode node, Set<AdvancementNode> visited) {
        if (node == null || !visited.add(node)) return false;
        if (this.getOrStartProgress(node.holder()).isDone()) return true;

        for (AdvancementNode child : node.children()) {
            if (reliable_advancements$hasDoneDescendant(child, visited)) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private boolean reliable_advancements$checkAncestorPathVisible(AdvancementNode node, int depth, int maxDepth, Set<AdvancementNode> visited) {
        if (node == null || !visited.add(node)) {
            return false;
        }
        if (maxDepth >= 0 && depth >= maxDepth) {
            return false;
        }

        List<AdvancementNode> parents = IMultiParentNode.getParents(node);
        if (parents.isEmpty()) {
            return maxDepth < 0;
        }

        for (AdvancementNode parent : parents) {
            if (parent == null) continue;

            Optional<DisplayInfo> display = parent.advancement().display();
            if (display.isEmpty()) {
                continue;
            }

            boolean parentDone = this.getOrStartProgress(parent.holder()).isDone();
            if (parentDone) {
                return true;
            }

            if (display.get().isHidden()) {
                continue;
            }

            if (maxDepth < 0) {
                return true;
            }

            if (reliable_advancements$checkAncestorPathVisible(parent, depth + 1, maxDepth, visited)) {
                return true;
            }
        }

        return false;
    }
}