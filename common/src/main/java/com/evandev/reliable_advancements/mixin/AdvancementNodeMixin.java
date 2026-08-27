package com.evandev.reliable_advancements.mixin;

import com.evandev.reliable_advancements.advancements.IMultiParentNode;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(AdvancementNode.class)
public abstract class AdvancementNodeMixin implements IMultiParentNode {

    @Shadow
    @Final
    @Mutable
    private Set<AdvancementNode> children;
    @Unique
    private List<AdvancementNode> reliable_advancements$parents = null;

    @Shadow
    @Nullable
    public abstract AdvancementNode parent();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void reliable_advancements$initLinkedChildren(AdvancementHolder holder, AdvancementNode parent, CallbackInfo ci) {
        this.children = new LinkedHashSet<>();
    }

    @Override
    public List<AdvancementNode> reliable_advancements$getParents() {
        return Objects.requireNonNullElseGet(this.reliable_advancements$parents, () -> parent() != null ? List.of(parent()) : List.of());
    }

    @Override
    public void reliable_advancements$addParent(AdvancementNode parent) {
        if (this.reliable_advancements$parents == null) {
            this.reliable_advancements$parents = new ArrayList<>();
        }
        if (parent != null && !this.reliable_advancements$parents.contains(parent)) {
            this.reliable_advancements$parents.add(parent);
        }
    }

    @Override
    public void reliable_advancements$removeParent(AdvancementNode parent) {
        if (this.reliable_advancements$parents == null) {
            this.reliable_advancements$parents = new ArrayList<>();
            if (parent() != null && parent() != parent) {
                this.reliable_advancements$parents.add(parent());
            }
        } else {
            this.reliable_advancements$parents.remove(parent);
        }
    }

    @Override
    public void reliable_advancements$removeChild(AdvancementNode child) {
        if (this.children != null && child != null) {
            this.children.remove(child);
        }
    }
}
