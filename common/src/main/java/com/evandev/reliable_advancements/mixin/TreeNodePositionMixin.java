package com.evandev.reliable_advancements.mixin;

import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.TreeNodePosition;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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
    private void reliable_advancements$filterLayoutChildren(AdvancementNode child, TreeNodePosition previousSibling, CallbackInfoReturnable<TreeNodePosition> cir) {
        if (child.parent() != null && !child.parent().equals(this.node)) {
            cir.setReturnValue(previousSibling);
        }
    }

    @Redirect(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/advancements/AdvancementNode;children()Ljava/lang/Iterable;")
    )
    private Iterable<AdvancementNode> reliable_advancements$sortChildrenForRoot(AdvancementNode node) {
        List<AdvancementNode> sorted = new ArrayList<>();
        node.children().forEach(sorted::add);
        sorted.sort(Comparator.comparing(n -> n.holder().id().toString()));
        return sorted;
    }

    @Redirect(
            method = "addChild",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/advancements/AdvancementNode;children()Ljava/lang/Iterable;")
    )
    private Iterable<AdvancementNode> reliable_advancements$sortChildrenForChild(AdvancementNode node) {
        List<AdvancementNode> sorted = new ArrayList<>();
        node.children().forEach(sorted::add);
        sorted.sort(Comparator.comparing(n -> n.holder().id().toString()));
        return sorted;
    }
}
