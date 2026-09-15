package com.evandev.reliable_advancements.mixin;

import com.evandev.reliable_advancements.mixin.accessor.TreeNodePositionAccessor;
import com.evandev.reliable_advancements.reference.Constants;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.TreeNodePosition;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(TreeNodePosition.class)
public abstract class TreeNodePositionMixin {

    @Final
    @Shadow
    private AdvancementNode node;

    @Inject(method = "addChild", at = @At("HEAD"), cancellable = true)
    private void reliable_advancements$filterLayoutChildren(AdvancementNode child, TreeNodePosition previousSibling, CallbackInfoReturnable<TreeNodePosition> cir) {
        if (!reliable_advancements$isLayoutChild(child, this.node)) {
            cir.setReturnValue(previousSibling);
            return;
        }

        for (TreeNodePosition p = (TreeNodePosition) (Object) this; p != null; p = ((TreeNodePositionAccessor) p).reliable_advancements$getParent()) {
            if (((TreeNodePositionAccessor) p).reliable_advancements$getNode().equals(child)) {
                Constants.LOG.error("Cycle detected in advancement tree layout between {} and {}", this.node.holder().id(), child.holder().id());
                cir.setReturnValue(previousSibling);
                return;
            }
        }
    }

    @Unique
    private static boolean reliable_advancements$isLayoutChild(AdvancementNode child, AdvancementNode expectedParent) {
        if (child == null || expectedParent == null) return false;
        AdvancementNode p = child.parent();
        if (p == null) return false;

        Set<AdvancementNode> seen = new HashSet<>();
        while (p != null && p.advancement().display().isEmpty()) {
            if (!seen.add(p)) return false;
            p = p.parent();
        }
        return Objects.equals(p, expectedParent);
    }

    @WrapOperation(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/advancements/AdvancementNode;children()Ljava/lang/Iterable;")
    )
    private Iterable<AdvancementNode> reliable_advancements$sortChildrenForRoot(AdvancementNode node, Operation<Iterable<AdvancementNode>> original) {
        List<AdvancementNode> sorted = new ArrayList<>();
        original.call(node).forEach(sorted::add);
        sorted.sort(Comparator.comparing(n -> n.holder().id()));
        return sorted;
    }

    @WrapOperation(
            method = "addChild",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/advancements/AdvancementNode;children()Ljava/lang/Iterable;")
    )
    private Iterable<AdvancementNode> reliable_advancements$sortChildrenForChild(AdvancementNode node, Operation<Iterable<AdvancementNode>> original) {
        List<AdvancementNode> sorted = new ArrayList<>();
        original.call(node).forEach(sorted::add);
        sorted.sort(Comparator.comparing(n -> n.holder().id()));
        return sorted;
    }
}
