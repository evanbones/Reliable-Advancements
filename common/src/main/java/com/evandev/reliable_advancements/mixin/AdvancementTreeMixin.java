package com.evandev.reliable_advancements.mixin;

import com.evandev.reliable_advancements.advancements.IMultiParentAdvancement;
import com.evandev.reliable_advancements.advancements.IMultiParentNode;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(AdvancementTree.class)
public abstract class AdvancementTreeMixin {

    @Unique
    private final Map<Identifier, Set<Identifier>> reliable_advancements$childrenByParent = new HashMap<>();

    @Shadow
    @Final
    private Map<Identifier, AdvancementNode> nodes;

    @Shadow
    @Final
    private Set<AdvancementNode> roots;

    @Shadow
    @Final
    private Set<AdvancementNode> tasks;

    @Shadow
    private @Nullable AdvancementTree.Listener listener;

    @ModifyVariable(
            method = "addAll",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Collection<AdvancementHolder> sortAdvancementsForDeterministicTree(Collection<AdvancementHolder> advancements) {
        List<AdvancementHolder> sorted = new ArrayList<>(advancements);
        sorted.sort(Comparator.comparing(AdvancementHolder::id));
        return sorted;
    }

    @Inject(method = "tryInsert", at = @At("HEAD"))
    private void reliable_advancements$evictStaleNode(AdvancementHolder holder, CallbackInfoReturnable<Boolean> cir) {
        AdvancementNode stale = this.nodes.get(holder.id());
        if (stale == null) {
            return;
        }

        Optional<Identifier> primaryParent = holder.value().parent();
        if (primaryParent.isPresent() && this.nodes.get(primaryParent.get()) == null) {
            return;
        }

        for (AdvancementNode parent : new ArrayList<>(IMultiParentNode.getParents(stale))) {
            if (parent != null) {
                IMultiParentNode.removeChild(parent, stale);
                IMultiParentNode.removeParent(stale, parent);
                Set<Identifier> children = this.reliable_advancements$childrenByParent.get(parent.holder().id());
                if (children != null) {
                    children.remove(holder.id());
                }
            }
        }
        if (stale.parent() != null) {
            IMultiParentNode.removeChild(stale.parent(), stale);
        }

        this.roots.remove(stale);
        this.tasks.remove(stale);
        this.nodes.remove(holder.id());
    }

    @Inject(method = "clear", at = @At("HEAD"))
    private void reliable_advancements$onClear(CallbackInfo ci) {
        this.reliable_advancements$childrenByParent.clear();
    }

    @Inject(method = "tryInsert", at = @At("RETURN"))
    private void linkMultiParents(AdvancementHolder holder, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            AdvancementNode currentNode = this.nodes.get(holder.id());
            if (currentNode != null) {
                List<Identifier> parentIds = IMultiParentAdvancement.getParents(holder.value());
                for (Identifier parentId : parentIds) {
                    this.reliable_advancements$childrenByParent.computeIfAbsent(parentId, k -> new HashSet<>()).add(holder.id());
                    AdvancementNode parentNode = this.nodes.get(parentId);
                    if (parentNode != null) {
                        parentNode.addChild(currentNode);
                        IMultiParentNode.addParent(currentNode, parentNode);
                    }
                }

                Set<Identifier> childIds = this.reliable_advancements$childrenByParent.get(holder.id());
                if (childIds != null) {
                    for (Identifier childId : childIds) {
                        AdvancementNode childNode = this.nodes.get(childId);
                        if (childNode != null) {
                            currentNode.addChild(childNode);
                            IMultiParentNode.addParent(childNode, currentNode);
                        }
                    }
                }
            }
        }
    }

    @WrapOperation(
            method = "remove(Lnet/minecraft/advancements/AdvancementNode;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/advancements/AdvancementNode;children()Ljava/lang/Iterable;")
    )
    private Iterable<AdvancementNode> reliable_advancements$copyChildrenForSafeRemoval(AdvancementNode instance, Operation<Iterable<AdvancementNode>> original) {
        Iterable<AdvancementNode> children = original.call(instance);
        List<AdvancementNode> copy = new ArrayList<>();
        if (children != null) {
            for (AdvancementNode child : children) {
                if (child != null) {
                    copy.add(child);
                }
            }
        }
        return copy;
    }

    @Inject(method = "setListener", at = @At("HEAD"), cancellable = true)
    private void reliable_advancements$setListenerOrdered(AdvancementTree.Listener listener, CallbackInfo ci) {
        this.listener = listener;
        if (listener != null) {
            List<AdvancementNode> nonTabRoots = new ArrayList<>();

            for (AdvancementNode root : this.roots) {
                boolean isTabRoot = root.advancement().display().map(d -> d.getBackground().isPresent()).orElse(false);
                if (isTabRoot) {
                    listener.onAddAdvancementRoot(root);
                } else {
                    nonTabRoots.add(root);
                }
            }

            for (AdvancementNode node : nonTabRoots) {
                listener.onAddAdvancementTask(node);
            }

            for (AdvancementNode task : this.tasks) {
                listener.onAddAdvancementTask(task);
            }
        }
        ci.cancel();
    }

    @Inject(method = "remove(Lnet/minecraft/advancements/AdvancementNode;)V", at = @At("HEAD"))
    private void reliable_advancements$cleanMultiParentReferences(AdvancementNode node, CallbackInfo ci) {
        if (node != null) {
            List<AdvancementNode> parents = new ArrayList<>(IMultiParentNode.getParents(node));
            for (AdvancementNode parent : parents) {
                if (parent != null) {
                    IMultiParentNode.removeChild(parent, node);
                    IMultiParentNode.removeParent(node, parent);
                    Set<Identifier> children = this.reliable_advancements$childrenByParent.get(parent.holder().id());
                    if (children != null) {
                        children.remove(node.holder().id());
                    }
                }
            }
            this.reliable_advancements$childrenByParent.remove(node.holder().id());
        }
    }
}


