package com.evandev.reliable_advancements.mixin;

import com.evandev.reliable_advancements.advancements.AdvancementVisibilityRule;
import com.evandev.reliable_advancements.config.ModConfig;
import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.server.advancements.AdvancementVisibilityEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.function.Predicate;

@Mixin(AdvancementVisibilityEvaluator.class)
public abstract class AdvancementVisibilityEvaluatorMixin {

    @Inject(method = "evaluateVisibility(Lnet/minecraft/advancements/AdvancementNode;Ljava/util/function/Predicate;Lnet/minecraft/server/advancements/AdvancementVisibilityEvaluator$Output;)V", at = @At("HEAD"), cancellable = true)
    private static void reliable_advancements$evaluateVisibilityDag(
            AdvancementNode advancement,
            Predicate<AdvancementNode> isDone,
            AdvancementVisibilityEvaluator.Output output,
            CallbackInfo ci
    ) {
        AdvancementNode root = advancement.root();

        Set<AdvancementNode> reachableNodes = new LinkedHashSet<>();
        reliable_advancements$collectReachable(root, reachableNodes);

        Map<AdvancementNode, Boolean> hasDoneDescendant = new HashMap<>();
        for (AdvancementNode node : reachableNodes) {
            reliable_advancements$computeDoneDescendants(node, isDone, hasDoneDescendant, new HashSet<>());
        }

        Set<AdvancementNode> pathVisibleNodes = new HashSet<>();
        int maxDepth = ModConfig.get().visibilityDepth;

        if (maxDepth < 0) {
            reliable_advancements$evaluateUnlimitedVisibility(
                    root,
                    hasDoneDescendant,
                    pathVisibleNodes,
                    new HashSet<>()
            );
        } else if (maxDepth > 0) {
            Stack<AdvancementVisibilityRule> stack = new ObjectArrayList<>();
            int stackInitSize = Math.max(maxDepth, 2);
            for (int i = 0; i <= stackInitSize; i++) {
                stack.push(AdvancementVisibilityRule.NO_CHANGE);
            }

            reliable_advancements$evaluatePathVisibility(
                    root,
                    stack,
                    maxDepth,
                    hasDoneDescendant,
                    pathVisibleNodes,
                    new HashSet<>()
            );
        }

        for (AdvancementNode node : reachableNodes) {
            boolean isVisible = Boolean.TRUE.equals(hasDoneDescendant.get(node)) || pathVisibleNodes.contains(node);
            output.accept(node, isVisible);
        }

        ci.cancel();
    }

    @Unique
    private static void reliable_advancements$collectReachable(AdvancementNode node, Set<AdvancementNode> reachable) {
        if (node == null || !reachable.add(node)) {
            return;
        }
        for (AdvancementNode child : node.children()) {
            reliable_advancements$collectReachable(child, reachable);
        }
    }

    @Unique
    private static boolean reliable_advancements$computeDoneDescendants(
            AdvancementNode node,
            Predicate<AdvancementNode> predicate,
            Map<AdvancementNode, Boolean> hasDoneDescendant,
            Set<AdvancementNode> visiting
    ) {
        if (hasDoneDescendant.containsKey(node)) {
            return hasDoneDescendant.get(node);
        }

        if (!visiting.add(node)) {
            return false;
        }

        boolean done = predicate.test(node);
        for (AdvancementNode child : node.children()) {
            done |= reliable_advancements$computeDoneDescendants(child, predicate, hasDoneDescendant, visiting);
        }

        visiting.remove(node);
        hasDoneDescendant.put(node, done);
        return done;
    }

    @Unique
    private static void reliable_advancements$evaluateUnlimitedVisibility(
            AdvancementNode node,
            Map<AdvancementNode, Boolean> hasDoneDescendant,
            Set<AdvancementNode> pathVisibleNodes,
            Set<AdvancementNode> activePath
    ) {
        if (node == null || !activePath.add(node)) {
            return;
        }

        boolean isDoneOrDescendantDone = Boolean.TRUE.equals(hasDoneDescendant.get(node));
        AdvancementVisibilityRule rule = reliable_advancements$getRule(node.advancement(), isDoneOrDescendantDone);

        if (rule != AdvancementVisibilityRule.HIDE) {
            pathVisibleNodes.add(node);
            for (AdvancementNode child : node.children()) {
                reliable_advancements$evaluateUnlimitedVisibility(child, hasDoneDescendant, pathVisibleNodes, activePath);
            }
        }

        activePath.remove(node);
    }

    @Unique
    private static void reliable_advancements$evaluatePathVisibility(
            AdvancementNode node,
            Stack<AdvancementVisibilityRule> stack,
            int maxDepth,
            Map<AdvancementNode, Boolean> hasDoneDescendant,
            Set<AdvancementNode> pathVisibleNodes,
            Set<AdvancementNode> activePath
    ) {
        if (node == null || !activePath.add(node)) {
            return;
        }

        boolean isDoneOrDescendantDone = Boolean.TRUE.equals(hasDoneDescendant.get(node));
        AdvancementVisibilityRule rule = reliable_advancements$getRule(node.advancement(), isDoneOrDescendantDone);
        stack.push(rule);

        if (reliable_advancements$isUnfinishedNodeVisible(stack, maxDepth)) {
            pathVisibleNodes.add(node);
        }

        for (AdvancementNode child : node.children()) {
            reliable_advancements$evaluatePathVisibility(child, stack, maxDepth, hasDoneDescendant, pathVisibleNodes, activePath);
        }

        stack.pop();
        activePath.remove(node);
    }

    @Unique
    private static AdvancementVisibilityRule reliable_advancements$getRule(Advancement advancement, boolean alwaysShow) {
        Optional<DisplayInfo> optional = advancement.display();
        if (optional.isEmpty()) {
            return AdvancementVisibilityRule.HIDE;
        } else if (alwaysShow) {
            return AdvancementVisibilityRule.SHOW;
        } else {
            return optional.get().isHidden() ? AdvancementVisibilityRule.HIDE : AdvancementVisibilityRule.NO_CHANGE;
        }
    }

    @Unique
    private static boolean reliable_advancements$isUnfinishedNodeVisible(Stack<AdvancementVisibilityRule> visibilityRules, int maxDepth) {
        for (int i = 0; i <= maxDepth; i++) {
            AdvancementVisibilityRule rule = visibilityRules.peek(i);
            if (rule == AdvancementVisibilityRule.SHOW) {
                return true;
            }
            if (rule == AdvancementVisibilityRule.HIDE) {
                return false;
            }
        }
        return false;
    }
}
