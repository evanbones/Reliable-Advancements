package com.evandev.reliable_advancements.mixin.accessor;

import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.TreeNodePosition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TreeNodePosition.class)
public interface TreeNodePositionAccessor {
    @Accessor("node")
    AdvancementNode reliable_advancements$getNode();

    @Accessor("parent")
    TreeNodePosition reliable_advancements$getParent();
}
