package com.evandev.reliable_advancements.mixin;

import com.evandev.reliable_advancements.advancements.IMultiParentNode;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.AdvancementCommands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mixin(AdvancementCommands.class)
public abstract class AdvancementCommandsMixin {

    @Inject(method = "getAdvancements", at = @At("HEAD"), cancellable = true)
    private static void reliable_advancements$getAdvancementsMultiParent(
            CommandContext<CommandSourceStack> context,
            AdvancementHolder target,
            @Coerce Object mode,
            CallbackInfoReturnable<List<AdvancementHolder>> cir
    ) {
        AdvancementTree advancementTree = context.getSource().getServer().getAdvancements().tree();
        AdvancementNode advancementNode = advancementTree.get(target);
        if (advancementNode == null) {
            cir.setReturnValue(List.of(target));
            return;
        }

        String modeName = mode instanceof Enum<?> e ? e.name() : "";
        boolean includeParents = "UNTIL".equals(modeName) || "THROUGH".equals(modeName) || "EVERYTHING".equals(modeName);
        boolean includeChildren = "FROM".equals(modeName) || "THROUGH".equals(modeName) || "EVERYTHING".equals(modeName);

        List<AdvancementHolder> list = new ArrayList<>();
        Set<AdvancementNode> visited = new HashSet<>();

        if (includeParents) {
            reliable_advancements$addParents(advancementNode, list, visited);
        }

        if (visited.add(advancementNode)) {
            list.add(target);
        }

        if (includeChildren) {
            reliable_advancements$addChildren(advancementNode, list, visited);
        }

        cir.setReturnValue(list);
    }

    @Unique
    private static void reliable_advancements$addParents(
            AdvancementNode node,
            List<AdvancementHolder> output,
            Set<AdvancementNode> visited
    ) {
        List<AdvancementNode> parents = IMultiParentNode.getParents(node);
        for (AdvancementNode parent : parents) {
            if (parent != null && visited.add(parent)) {
                reliable_advancements$addParents(parent, output, visited);
                output.add(parent.holder());
            }
        }
    }

    @Unique
    private static void reliable_advancements$addChildren(
            AdvancementNode node,
            List<AdvancementHolder> output,
            Set<AdvancementNode> visited
    ) {
        for (AdvancementNode child : node.children()) {
            if (visited.add(child)) {
                output.add(child.holder());
                reliable_advancements$addChildren(child, output, visited);
            }
        }
    }
}
