package com.evandev.reliable_advancements.mixin;

import com.evandev.reliable_advancements.advancements.IMultiParentAdvancement;
import com.evandev.reliable_advancements.advancements.IMultiParentNode;
import com.evandev.reliable_advancements.reference.Constants;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.resources.ResourceLocation;
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
    private final Map<ResourceLocation, Set<ResourceLocation>> reliable_advancements$childrenByParent = new HashMap<>();
    @Unique
    private final Set<ResourceLocation> reliable_advancements$forcedRoots = new HashSet<>();
    @Shadow
    @Final
    private Map<ResourceLocation, AdvancementNode> nodes;
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

        Map<ResourceLocation, Optional<ResourceLocation>> pending = new LinkedHashMap<>();
        for (AdvancementHolder holder : sorted) {
            pending.put(holder.id(), holder.value().parent());
        }

        Set<ResourceLocation> grounded = new HashSet<>(this.nodes.keySet());
        boolean progressed;
        do {
            progressed = false;
            for (Map.Entry<ResourceLocation, Optional<ResourceLocation>> entry : pending.entrySet()) {
                if (grounded.contains(entry.getKey())) continue;
                Optional<ResourceLocation> parent = entry.getValue();
                if (parent.isEmpty() || grounded.contains(parent.get())) {
                    grounded.add(entry.getKey());
                    progressed = true;
                }
            }
        } while (progressed);

        this.reliable_advancements$forcedRoots.clear();
        while (grounded.size() < pending.size() + this.nodes.size()) {
            ResourceLocation candidate = null;
            for (Map.Entry<ResourceLocation, Optional<ResourceLocation>> entry : pending.entrySet()) {
                if (!grounded.contains(entry.getKey())) {
                    Optional<ResourceLocation> parent = entry.getValue();
                    if (parent.isPresent() && !pending.containsKey(parent.get()) && !this.nodes.containsKey(parent.get())) {
                        candidate = entry.getKey();
                        break;
                    }
                }
            }
            if (candidate == null) {
                for (ResourceLocation id : pending.keySet()) {
                    if (!grounded.contains(id)) {
                        candidate = id;
                        break;
                    }
                }
            }
            if (candidate == null) break;

            this.reliable_advancements$forcedRoots.add(candidate);
            Constants.LOG.warn("Advancement {} cannot reach a root through parent {}; loading it as a root of its own",
                    candidate, pending.get(candidate).orElse(null));
            grounded.add(candidate);

            do {
                progressed = false;
                for (Map.Entry<ResourceLocation, Optional<ResourceLocation>> entry : pending.entrySet()) {
                    if (grounded.contains(entry.getKey())) continue;
                    Optional<ResourceLocation> parent = entry.getValue();
                    if (parent.isPresent() && grounded.contains(parent.get())) {
                        grounded.add(entry.getKey());
                        progressed = true;
                    }
                }
            } while (progressed);
        }
        return sorted;
    }

    @Inject(method = "tryInsert", at = @At("HEAD"), cancellable = true)
    private void reliable_advancements$evictStaleNode(AdvancementHolder advancement, CallbackInfoReturnable<Boolean> cir) {
        boolean forceRoot = this.reliable_advancements$forcedRoots.contains(advancement.id());
        AdvancementNode stale = this.nodes.get(advancement.id());
        if (stale == null) {
            if (forceRoot) reliable_advancements$insertAsRoot(advancement, cir);
            return;
        }

        Optional<ResourceLocation> primaryParent = advancement.value().parent();
        if (!forceRoot && primaryParent.isPresent() && this.nodes.get(primaryParent.get()) == null) {
            return;
        }

        for (AdvancementNode parent : new ArrayList<>(IMultiParentNode.getParents(stale))) {
            if (parent != null) {
                IMultiParentNode.removeChild(parent, stale);
                IMultiParentNode.removeParent(stale, parent);
                Set<ResourceLocation> children = this.reliable_advancements$childrenByParent.get(parent.holder().id());
                if (children != null) {
                    children.remove(advancement.id());
                }
            }
        }
        if (stale.parent() != null) {
            IMultiParentNode.removeChild(stale.parent(), stale);
        }

        this.roots.remove(stale);
        this.tasks.remove(stale);
        this.nodes.remove(advancement.id());

        if (forceRoot) reliable_advancements$insertAsRoot(advancement, cir);
    }

    @Unique
    private void reliable_advancements$insertAsRoot(AdvancementHolder advancement, CallbackInfoReturnable<Boolean> cir) {
        AdvancementNode node = new AdvancementNode(advancement, null);
        this.nodes.put(advancement.id(), node);
        this.roots.add(node);
        if (this.listener != null) {
            this.listener.onAddAdvancementRoot(node);
        }
        reliable_advancements$linkMultiParents(advancement);
        cir.setReturnValue(true);
    }

    @Inject(method = "clear", at = @At("HEAD"))
    private void reliable_advancements$onClear(CallbackInfo ci) {
        this.reliable_advancements$childrenByParent.clear();
        this.reliable_advancements$forcedRoots.clear();
    }

    @Inject(method = "tryInsert", at = @At("RETURN"))
    private void linkMultiParents(AdvancementHolder advancement, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            reliable_advancements$linkMultiParents(advancement);
        }
    }

    @Unique
    private void reliable_advancements$linkMultiParents(AdvancementHolder advancement) {
        AdvancementNode currentNode = this.nodes.get(advancement.id());
        if (currentNode == null) return;
        boolean isForcedRoot = this.reliable_advancements$forcedRoots.contains(advancement.id());

        for (ResourceLocation parentId : IMultiParentAdvancement.getParents(advancement.value())) {
            if (isForcedRoot && advancement.value().parent().map(parentId::equals).orElse(false)) {
                continue;
            }

            this.reliable_advancements$childrenByParent.computeIfAbsent(parentId, k -> new HashSet<>()).add(advancement.id());
            AdvancementNode parentNode = this.nodes.get(parentId);
            if (parentNode != null) {
                if (reliable_advancements$isAncestor(currentNode, parentNode)) {
                    Constants.LOG.warn("Ignoring cyclic parent link: {} is already an ancestor of {}",
                            currentNode.holder().id(), parentNode.holder().id());
                    continue;
                }
                parentNode.addChild(currentNode);
                IMultiParentNode.addParent(currentNode, parentNode);
            }
        }

        Set<ResourceLocation> childIds = this.reliable_advancements$childrenByParent.get(advancement.id());
        if (childIds != null) {
            for (ResourceLocation childId : childIds) {
                AdvancementNode childNode = this.nodes.get(childId);
                if (childNode != null) {
                    if (reliable_advancements$isAncestor(childNode, currentNode)) {
                        Constants.LOG.warn("Ignoring cyclic child link: {} is already an ancestor of {}",
                                childNode.holder().id(), currentNode.holder().id());
                        continue;
                    }
                    currentNode.addChild(childNode);
                    IMultiParentNode.addParent(childNode, currentNode);
                }
            }
        }
    }

    @Unique
    private static boolean reliable_advancements$isAncestor(AdvancementNode potentialAncestor, AdvancementNode node) {
        if (potentialAncestor == null || node == null) return false;
        if (potentialAncestor.equals(node)) return true;

        Set<AdvancementNode> visited = new HashSet<>();
        Queue<AdvancementNode> queue = new ArrayDeque<>();
        queue.add(node);
        visited.add(node);

        while (!queue.isEmpty()) {
            AdvancementNode curr = queue.poll();
            for (AdvancementNode parent : IMultiParentNode.getParents(curr)) {
                if (parent == null) continue;
                if (parent.equals(potentialAncestor)) return true;
                if (visited.add(parent)) {
                    queue.add(parent);
                }
            }
            AdvancementNode primaryParent = curr.parent();
            if (primaryParent != null) {
                if (primaryParent.equals(potentialAncestor)) return true;
                if (visited.add(primaryParent)) {
                    queue.add(primaryParent);
                }
            }
        }
        return false;
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

    @Inject(method = "remove(Lnet/minecraft/advancements/AdvancementNode;)V", at = @At("HEAD"))
    private void reliable_advancements$cleanMultiParentReferences(AdvancementNode node, CallbackInfo ci) {
        if (node != null) {
            List<AdvancementNode> parents = new ArrayList<>(IMultiParentNode.getParents(node));
            for (AdvancementNode parent : parents) {
                if (parent != null) {
                    IMultiParentNode.removeChild(parent, node);
                    IMultiParentNode.removeParent(node, parent);
                    Set<ResourceLocation> children = this.reliable_advancements$childrenByParent.get(parent.holder().id());
                    if (children != null) {
                        children.remove(node.holder().id());
                    }
                }
            }
            this.reliable_advancements$childrenByParent.remove(node.holder().id());
        }
    }
}


