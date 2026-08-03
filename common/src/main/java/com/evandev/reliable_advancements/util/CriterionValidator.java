package com.evandev.reliable_advancements.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.*;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class CriterionValidator {

    private static final Result OK = new Result(Map.of(), null);

    private CriterionValidator() {
    }

    public static Result validate(String triggerId, JsonObject conditions) {
        DynamicOps<JsonElement> ops = ops();
        return ops != null ? validate(triggerId, conditions, ops) : OK;
    }

    public static Result validate(String triggerId, JsonObject conditions, DynamicOps<JsonElement> ops) {
        Codec<?> codec = codecFor(triggerId);
        if (codec == null) return OK;

        String wholeError = errorOf(codec, ops, conditions);
        if (wholeError == null) return OK;

        Map<String, String> blamed = errorOf(codec, ops, new JsonObject()) == null
                ? blameIndividually(codec, ops, conditions)
                : blameByRemoval(codec, ops, conditions, wholeError);

        return blamed.isEmpty() ? new Result(Map.of(), wholeError) : new Result(blamed, null);
    }

    private static Map<String, String> blameIndividually(Codec<?> codec, DynamicOps<JsonElement> ops, JsonObject conditions) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : conditions.entrySet()) {
            JsonObject single = new JsonObject();
            single.add(entry.getKey(), entry.getValue());

            String error = errorOf(codec, ops, single);
            if (error != null) errors.put(entry.getKey(), error);
        }
        return errors;
    }

    private static Map<String, String> blameByRemoval(Codec<?> codec, DynamicOps<JsonElement> ops, JsonObject conditions, String wholeError) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (String key : conditions.keySet()) {
            JsonObject without = conditions.deepCopy();
            without.remove(key);

            if (errorOf(codec, ops, without) == null) errors.put(key, wholeError);
        }
        if (!errors.isEmpty()) return errors;

        String missingFieldsError = errorOf(codec, ops, new JsonObject());
        for (Map.Entry<String, JsonElement> entry : conditions.entrySet()) {
            JsonObject single = new JsonObject();
            single.add(entry.getKey(), entry.getValue());

            String error = errorOf(codec, ops, single);
            if (error != null && !error.equals(missingFieldsError)) errors.put(entry.getKey(), error);
        }
        return errors;
    }

    private static @Nullable String errorOf(Codec<?> codec, DynamicOps<JsonElement> ops, JsonObject json) {
        try {
            return codec.parse(ops, json).error().map(DataResult.Error::message).orElse(null);
        } catch (Exception e) {
            return e.getMessage() != null ? e.getMessage() : e.toString();
        }
    }

    private static @Nullable Codec<?> codecFor(String triggerId) {
        if (triggerId == null) return null;

        ResourceLocation id = ResourceLocation.tryParse(triggerId.trim());
        if (id == null) return null;

        CriterionTrigger<?> trigger = BuiltInRegistries.TRIGGER_TYPES.get(id);
        return trigger != null ? trigger.codec() : null;
    }

    private static @Nullable DynamicOps<JsonElement> ops() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level != null ? opsFor(minecraft.level.registryAccess()) : null;
    }

    public static DynamicOps<JsonElement> opsFor(HolderLookup.Provider registries) {
        return RegistryOps.create(JsonOps.INSTANCE, new PermissiveLookup(registries));
    }

    private static <T> RegistryOps.RegistryInfo<T> placeholderFor(ResourceKey<? extends Registry<?>> registryKey) {
        @SuppressWarnings("unchecked")
        MappedRegistry<T> registry = new MappedRegistry<>((ResourceKey<? extends Registry<T>>) registryKey, Lifecycle.experimental());

        return new RegistryOps.RegistryInfo<>(registry.holderOwner(), registry.createRegistrationLookup(), Lifecycle.experimental());
    }

    public record Result(Map<String, String> fieldErrors, @Nullable String generalError) {
        public boolean isValid() {
            return fieldErrors.isEmpty() && generalError == null;
        }
    }

    private record PermissiveLookup(HolderLookup.Provider registries,
                                    Map<ResourceKey<? extends Registry<?>>, RegistryOps.RegistryInfo<?>> placeholders)
            implements RegistryOps.RegistryInfoLookup {

        PermissiveLookup(HolderLookup.Provider registries) {
            this(registries, new ConcurrentHashMap<>());
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> Optional<RegistryOps.RegistryInfo<T>> lookup(ResourceKey<? extends Registry<? extends T>> registryKey) {
            Optional<RegistryOps.RegistryInfo<T>> known = registries.lookup(registryKey)
                    .map(RegistryOps.RegistryInfo::fromRegistryLookup);
            if (known.isPresent()) return known;

            return Optional.of((RegistryOps.RegistryInfo<T>) placeholders.computeIfAbsent(registryKey, CriterionValidator::placeholderFor));
        }
    }
}
