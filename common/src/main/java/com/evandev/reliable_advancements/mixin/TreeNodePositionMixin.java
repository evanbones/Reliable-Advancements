package com.evandev.reliable_advancements.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.TreeNodePosition;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Mixin(TreeNodePosition.class)
public abstract class TreeNodePositionMixin {

    @Final
    @Shadow
    private AdvancementNode node;

    @Inject(method = "addChild", at = @At("HEAD"), cancellable = true)
    private void reliable_advancements$filterLayoutChildren(AdvancementNode node, TreeNodePosition previous, CallbackInfoReturnable<TreeNodePosition> cir) {
        if (node.parent() != null && !node.parent().equals(this.node)) {
            cir.setReturnValue(previous);
        }
    }

    @WrapOperation(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/advancements/AdvancementNode;children()Ljava/lang/Iterable;")
    )
    private Iterable<AdvancementNode> reliable_advancements$sortChildrenForRoot(AdvancementNode node, Operation<Iterable<AdvancementNode>> original) {
        List<AdvancementNode> sorted = new ArrayList<>();
        original.call(node).forEach(sorted::add);
        sorted.sort(Comparator.comparing(n -> n.holder().id().toString()));
        return sorted;
    }

    @WrapOperation(
            method = "addChild",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/advancements/AdvancementNode;children()Ljava/lang/Iterable;")
    )
    private Iterable<AdvancementNode> reliable_advancements$sortChildrenForChild(AdvancementNode node, Operation<Iterable<AdvancementNode>> original) {
        List<AdvancementNode> sorted = new ArrayList<>();
        original.call(node).forEach(sorted::add);
        sorted.sort(Comparator.comparing(n -> n.holder().id().toString()));
        return sorted;
    }
}
