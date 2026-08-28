package com.evandev.reliable_advancements.mixin.accessor;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerAdvancementManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ServerAdvancementManager.class)
public interface ServerAdvancementManagerAccessor {
    @Accessor("advancements")
    Map<Identifier, AdvancementHolder> getAdvancements();

    @Accessor("advancements")
    void setAdvancements(Map<Identifier, AdvancementHolder> advancements);

    @Accessor("tree")
    void setTree(AdvancementTree tree);
}
