package com.evandev.reliable_advancements.mixin;

import com.evandev.reliable_advancements.advancements.IMultiParentAdvancement;
import com.evandev.reliable_advancements.advancements.MultiParentHelper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(SimpleJsonResourceReloadListener.class)
public abstract class SimpleJsonResourceReloadListenerMixin {

    @WrapOperation(
            method = "scanDirectory(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/resources/FileToIdConverter;Lcom/mojang/serialization/DynamicOps;Lcom/mojang/serialization/Codec;Ljava/util/Map;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/serialization/Codec;parse(Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;"
            )
    )
    private static <T> DataResult<T> reliable_advancements$parseMultiParentAdvancement(
            Codec<T> codec, DynamicOps<Object> ops, Object input, Operation<DataResult<T>> original) {
        if (codec == Advancement.CODEC && input instanceof JsonElement json && json.isJsonObject()) {
            JsonObject jsonObj = json.getAsJsonObject();
            List<Identifier> parents = MultiParentHelper.parseParents(jsonObj);
            JsonObject prepared = MultiParentHelper.prepareJsonForCodec(jsonObj);

            DataResult<T> result = original.call(codec, ops, prepared);
            result.result().ifPresent(parsed -> {
                if (parsed instanceof Advancement advancement) {
                    IMultiParentAdvancement.setParents(advancement, parents);
                }
            });
            return result;
        }

        return original.call(codec, ops, input);
    }
}
