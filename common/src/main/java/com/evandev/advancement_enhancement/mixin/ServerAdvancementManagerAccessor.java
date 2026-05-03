package com.evandev.advancement_enhancement.mixin;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerAdvancementManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ServerAdvancementManager.class)
public interface ServerAdvancementManagerAccessor {
    @Accessor("advancements")
    Map<ResourceLocation, AdvancementHolder> getAdvancements();

    @Accessor("advancements")
    void setAdvancements(Map<ResourceLocation, AdvancementHolder> advancements);

    @Accessor("tree")
    void setTree(AdvancementTree tree);
}