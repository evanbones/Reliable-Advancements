package com.evandev.reliable_advancements.mixin;

import org.spongepowered.asm.mixin.Mixin;

//? if <26.3 {
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
//?}

//? if >=26.1 {
/*import com.evandev.reliable_advancements.advancements.IMultiParentAdvancement;
import com.evandev.reliable_advancements.advancements.MultiParentHelper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
*///?}

//? if >=26.1 && <26.3 {
/*import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
*///?} else if >=26.3 {
/*import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Either;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
*///?}

/**
 * Reads multi-parent advancement JSON while it is decoded.
 */
//? if <26.3 {
@Mixin(SimpleJsonResourceReloadListener.class)
//?} else {
/*@Mixin(targets = "net.minecraft.resources.RegistryLoadTask$PendingRegistration")
*///?}
public abstract class AdvancementJsonParseMixin {
    //? if >=26.1 && <26.3 {
    /*@WrapOperation(
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
            List<ResourceLocation> parents = MultiParentHelper.parseParents(jsonObj);
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
    *///?} else if >=26.3 {
    /*@Unique
    private static final ThreadLocal<List<ResourceLocation>> reliable_advancements$pendingParents = new ThreadLocal<>();

    @ModifyExpressionValue(
            method = "loadFromResource",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/StrictJsonParser;parse(Ljava/io/Reader;)Lcom/google/gson/JsonElement;")
    )
    private static JsonElement reliable_advancements$prepareAdvancementJson(JsonElement json, @Local(argsOnly = true) ResourceKey<?> elementKey) {
        reliable_advancements$pendingParents.remove();
        if (!elementKey.isFor(Registries.ADVANCEMENT) || !json.isJsonObject()) return json;

        JsonObject jsonObj = json.getAsJsonObject();
        reliable_advancements$pendingParents.set(MultiParentHelper.parseParents(jsonObj));
        return MultiParentHelper.prepareJsonForCodec(jsonObj);
    }

    @Inject(method = "loadFromResource", at = @At("RETURN"))
    private static void reliable_advancements$applyParents(CallbackInfoReturnable<Either<?, Exception>> cir) {
        List<ResourceLocation> parents = reliable_advancements$pendingParents.get();
        reliable_advancements$pendingParents.remove();
        if (parents == null) return;
        cir.getReturnValue().ifLeft(parsed -> {
            if (parsed instanceof Advancement advancement) {
                IMultiParentAdvancement.setParents(advancement, parents);
            }
        });
    }
    *///?}
}
