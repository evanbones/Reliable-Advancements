package com.evandev.reliable_advancements.mixin;

import com.evandev.reliable_advancements.advancements.IMultiParentAdvancement;
import com.evandev.reliable_advancements.advancements.IMultiParentNode;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(AdvancementTree.class)
public abstract class AdvancementTreeMixin {

    @Shadow
    @Final
    private Map<ResourceLocation, AdvancementNode> nodes;

    @ModifyVariable(
            method = "addAll",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Collection<AdvancementHolder> sortAdvancementsForDeterministicTree(Collection<AdvancementHolder> advancements) {
        List<AdvancementHolder> sorted = new ArrayList<>(advancements);
        sorted.sort(Comparator.comparing(a -> a.id().toString()));
        return sorted;
    }

    @Inject(method = "tryInsert", at = @At("RETURN"))
    private void linkMultiParents(AdvancementHolder advancement, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            AdvancementNode currentNode = this.nodes.get(advancement.id());
            if (currentNode != null) {
                List<ResourceLocation> parentIds = IMultiParentAdvancement.getParents(advancement.value());
                for (ResourceLocation parentId : parentIds) {
                    AdvancementNode parentNode = this.nodes.get(parentId);
                    if (parentNode != null) {
                        parentNode.addChild(currentNode);
                        IMultiParentNode.addParent(currentNode, parentNode);
                    }
                }

                List<AdvancementNode> existingNodes = new ArrayList<>(this.nodes.values());
                existingNodes.sort(Comparator.comparing(n -> n.holder().id().toString()));
                for (AdvancementNode existingNode : existingNodes) {
                    List<ResourceLocation> existingParents = IMultiParentAdvancement.getParents(existingNode.advancement());
                    if (existingParents.contains(advancement.id())) {
                        currentNode.addChild(existingNode);
                        IMultiParentNode.addParent(existingNode, currentNode);
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

    @Inject(method = "remove(Lnet/minecraft/advancements/AdvancementNode;)V", at = @At("HEAD"))
    private void reliable_advancements$cleanMultiParentReferences(AdvancementNode node, CallbackInfo ci) {
        if (node != null) {
            List<AdvancementNode> parents = new ArrayList<>(IMultiParentNode.getParents(node));
            for (AdvancementNode parent : parents) {
                if (parent != null) {
                    IMultiParentNode.removeChild(parent, node);
                    IMultiParentNode.removeParent(node, parent);
                }
            }
        }
    }
}


