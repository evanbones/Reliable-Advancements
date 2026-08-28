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

    @Shadow
    @Final
    @Mutable
    private @Nullable AdvancementNode parent;

    @Unique
    private List<AdvancementNode> reliable_advancements$parents = null;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void reliable_advancements$initLinkedChildren(AdvancementHolder holder, AdvancementNode parent, CallbackInfo ci) {
        this.children = new LinkedHashSet<>();
    }

    @Override
    public List<AdvancementNode> reliable_advancements$getParents() {
        return Objects.requireNonNullElseGet(this.reliable_advancements$parents, () -> this.parent != null ? List.of(this.parent) : List.of());
    }

    @Override
    public void reliable_advancements$addParent(AdvancementNode parent) {
        if (this.reliable_advancements$parents == null) {
            this.reliable_advancements$parents = new ArrayList<>();
        }
        if (parent != null) {
            int existing = this.reliable_advancements$parents.indexOf(parent);
            if (existing >= 0) {
                this.reliable_advancements$parents.set(existing, parent);
            } else {
                this.reliable_advancements$parents.add(parent);
            }
            if (this.parent != null && this.parent.holder().id().equals(parent.holder().id())) {
                this.parent = parent;
            }
        }
    }

    @Override
    public void reliable_advancements$removeParent(AdvancementNode parent) {
        if (parent == null) return;
        if (this.reliable_advancements$parents == null) {
            this.reliable_advancements$parents = new ArrayList<>();
            if (this.parent != null && !this.parent.holder().id().equals(parent.holder().id())) {
                this.reliable_advancements$parents.add(this.parent);
            }
        } else {
            this.reliable_advancements$parents.removeIf(p -> p == null || p.holder().id().equals(parent.holder().id()));
        }
        if (this.parent != null && this.parent.holder().id().equals(parent.holder().id())) {
            this.parent = null;
        }
    }

    @Override
    public void reliable_advancements$removeChild(AdvancementNode child) {
        if (this.children != null && child != null) {
            this.children.removeIf(c -> c == null || c.holder().id().equals(child.holder().id()));
        }
    }
}
