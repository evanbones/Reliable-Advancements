package com.evandev.reliable_advancements.mixin;

import com.google.common.collect.ImmutableMap;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

@Mixin(ServerAdvancementManager.class)
public abstract class ServerAdvancementManagerMixin {

    @Shadow
    private Map<Identifier, AdvancementHolder> advancements;

    @Inject(
            method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/advancements/AdvancementTree;addAll(Ljava/util/Collection;)V"
            )
    )
    private void reliable_advancements$sortAdvancements(Map<Identifier, Advancement> preparations, ResourceManager manager, ProfilerFiller profiler, CallbackInfo ci) {
        Map<Identifier, AdvancementHolder> sortedMap = new LinkedHashMap<>();
        this.advancements.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Identifier::toString)))
                .forEach(e -> sortedMap.put(e.getKey(), e.getValue()));
        this.advancements = ImmutableMap.copyOf(sortedMap);
    }
}
