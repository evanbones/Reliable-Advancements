package com.evandev.reliable_advancements.advancements;

import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public interface IMultiParentAdvancement {
    static IMultiParentAdvancement of(Advancement advancement) {
        return (IMultiParentAdvancement) (Object) advancement;
    }

    static List<ResourceLocation> getParents(Advancement advancement) {
        if (advancement == null) return List.of();
        try {
            return ((IMultiParentAdvancement) (Object) advancement).reliable_advancements$getParents();
        } catch (ClassCastException e) {
            return advancement.parent().map(List::of).orElse(List.of());
        }
    }

    static void setParents(Advancement advancement, List<ResourceLocation> parents) {
        if (advancement != null) {
            try {
                ((IMultiParentAdvancement) (Object) advancement).reliable_advancements$setParents(parents);
            } catch (ClassCastException ignored) {
            }
        }
    }

    List<ResourceLocation> reliable_advancements$getParents();

    void reliable_advancements$setParents(List<ResourceLocation> parents);
}
