package com.evandev.reliable_advancements.advancements;

import net.minecraft.advancements.AdvancementNode;

import java.util.List;

public interface IMultiParentNode {
    static IMultiParentNode of(AdvancementNode node) {
        return (IMultiParentNode) node;
    }

    static List<AdvancementNode> getParents(AdvancementNode node) {
        if (node == null) return List.of();
        try {
            return ((IMultiParentNode) node).reliable_advancements$getParents();
        } catch (ClassCastException e) {
            return node.parent() != null ? List.of(node.parent()) : List.of();
        }
    }

    static void addParent(AdvancementNode node, AdvancementNode parent) {
        if (node != null && parent != null) {
            try {
                ((IMultiParentNode) node).reliable_advancements$addParent(parent);
            } catch (ClassCastException ignored) {
            }
        }
    }

    static void removeParent(AdvancementNode node, AdvancementNode parent) {
        if (node != null && parent != null) {
            try {
                ((IMultiParentNode) node).reliable_advancements$removeParent(parent);
            } catch (ClassCastException ignored) {
            }
        }
    }

    static void removeChild(AdvancementNode node, AdvancementNode child) {
        if (node != null && child != null) {
            try {
                ((IMultiParentNode) node).reliable_advancements$removeChild(child);
            } catch (ClassCastException ignored) {
            }
        }
    }

    List<AdvancementNode> reliable_advancements$getParents();

    void reliable_advancements$addParent(AdvancementNode parent);

    void reliable_advancements$removeParent(AdvancementNode parent);

    void reliable_advancements$removeChild(AdvancementNode child);
}
