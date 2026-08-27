package com.evandev.reliable_advancements.mixin;

import com.evandev.reliable_advancements.advancements.IMultiParentAdvancement;
import com.evandev.reliable_advancements.advancements.MultiParentHelper;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
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
import java.util.List;
import java.util.Map;

@Mixin(ServerAdvancementManager.class)
public abstract class ServerAdvancementManagerMixin {

    @Shadow
    private Map<ResourceLocation, AdvancementHolder> advancements;

    @Inject(
            method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("HEAD")
    )
    private void reliable_advancements$prepareJson(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci) {
        for (Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
            if (entry.getValue() instanceof JsonObject jsonObj) {
                if (jsonObj.has("parent") && jsonObj.get("parent").isJsonArray()) {
                    JsonArray arr = jsonObj.getAsJsonArray("parent");
                    if (!jsonObj.has("parents")) {
                        jsonObj.add("parents", arr.deepCopy());
                    }
                    if (!arr.isEmpty() && arr.get(0).isJsonPrimitive()) {
                        jsonObj.addProperty("parent", arr.get(0).getAsString());
                    } else {
                        jsonObj.remove("parent");
                    }
                }
            }
        }
    }

    @Inject(
            method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/advancements/AdvancementTree;addAll(Ljava/util/Collection;)V"
            )
    )
    private void reliable_advancements$populateMultiParents(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci) {
        Map<ResourceLocation, AdvancementHolder> sortedMap = new LinkedHashMap<>();
        this.advancements.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(e -> sortedMap.put(e.getKey(), e.getValue()));
        this.advancements = ImmutableMap.copyOf(sortedMap);

        for (Map.Entry<ResourceLocation, AdvancementHolder> entry : this.advancements.entrySet()) {
            JsonElement jsonElement = map.get(entry.getKey());
            if (jsonElement instanceof JsonObject jsonObj) {
                List<ResourceLocation> parents = MultiParentHelper.parseParents(jsonObj);
                IMultiParentAdvancement.setParents(entry.getValue().value(), parents);
            }
        }
    }
}
