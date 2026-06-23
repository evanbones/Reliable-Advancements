package com.evandev.reliable_advancements.mixin;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementList;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.Set;

@Mixin(AdvancementList.class)
public interface AdvancementListAccessor {
    @Accessor("advancements")
    Map<ResourceLocation, Advancement> getAdvancements();

    @Accessor("roots")
    Set<Advancement> getRoots();
}
