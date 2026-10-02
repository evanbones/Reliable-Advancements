package com.evandev.reliable_advancements.mixin.accessor;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerAdvancementManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ServerAdvancementManager.class)
public interface ServerAdvancementManagerAccessor {
    @Accessor("advancements")
    Map<ResourceLocation, AdvancementHolder> getAdvancements();

    //? if >=26.3 {
    /*@Mutable
    *///?}
    @Accessor("advancements")
    void setAdvancements(Map<ResourceLocation, AdvancementHolder> advancements);

    //? if >=26.3 {
    /*@Mutable
    *///?}
    @Accessor("tree")
    void setTree(AdvancementTree tree);
}