package com.evandev.reliable_advancements.util;

import com.evandev.reliable_advancements.gui.tabs.RewardsTab;
import com.evandev.reliable_advancements.reference.Constants;
import net.mehvahdjukaar.codecui.Schema;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.codecui.internal.CuratedSchemas;
import net.mehvahdjukaar.codecui.internal.SchemaResolver;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

public class TriggerSchemaManager {
    private static final Map<String, Schema<?>> SCHEMA_CACHE = new ConcurrentHashMap<>();

    static {
        try {
            CuratedSchemas.bootstrap();
            AdvancementPredicateSchemas.registerAll();
            for (Registry<?> registry : BuiltInRegistries.REGISTRY) {
                registerDynamicRegistry(registry);
            }
        } catch (Throwable ignored) {
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <K> void registerDynamicRegistry(Registry<K> registry) {
        if (registry == null) return;
        Supplier<List<K>> keys = () -> {
            List<K> snapshot = new ArrayList<>();
            try {
                for (K v : registry) snapshot.add(v);
            } catch (Throwable ignored) {
            }
            return snapshot;
        };
        Function<K, String> nameOf = v -> {
            Identifier id = registry.getKey(v);
            return id != null ? id.toString() : String.valueOf(v);
        };

        Class<?> elemClass = null;
        for (K sample : registry) {
            if (sample != null) {
                elemClass = sample.getClass();
                break;
            }
        }
        if (elemClass != null) {
            SchemaCodecs.registerDispatchKeys((Class) elemClass, keys, nameOf);
            for (Class<?> iface : elemClass.getInterfaces()) {
                SchemaCodecs.registerDispatchKeys((Class) iface, keys, nameOf);
            }
            if (elemClass.getSuperclass() != null && elemClass.getSuperclass() != Object.class) {
                SchemaCodecs.registerDispatchKeys((Class) elemClass.getSuperclass(), keys, nameOf);
            }
        }
    }

    public static @Nullable Schema<?> getSchema(String triggerId) {
        if (triggerId == null || triggerId.trim().isEmpty()) return null;
        String trimmed = triggerId.trim();
        return SCHEMA_CACHE.computeIfAbsent(trimmed, idStr -> {
            try {
                Identifier id = Identifier.tryParse(idStr);
                if (id == null) return null;
                CriterionTrigger<?> trigger = BuiltInRegistries.TRIGGER_TYPES.getValue(id);
                if (trigger == null) return null;
                return SchemaResolver.get().resolve(trigger.codec());
            } catch (Exception e) {
                Constants.LOG.debug("Failed to resolve schema for trigger {}", idStr, e);
                return null;
            }
        });
    }

    public static List<String> getSuggestionsForSchema(@Nullable Schema<?> schema) {
        if (schema == null) return List.of();
        schema = unwrapRef(schema);

        if (schema instanceof Schema.ResourceId(ResourceKey<? extends Registry<?>> regKey)) {
            if (regKey == null) {
                return BuiltInRegistries.ITEM.keySet().stream().map(Identifier::toString).toList();
            }
            List<String> results = new ArrayList<>();
            try {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level != null) {
                    var lookup = mc.level.registryAccess().lookup(regKey);
                    if (lookup.isPresent()) {
                        lookup.get().listElementIds().map(k -> k.identifier().toString()).forEach(results::add);
                        return results;
                    }
                }
            } catch (Exception ignored) {
            }

            try {
                var builtIn = BuiltInRegistries.REGISTRY.getValue(regKey.identifier());
                if (builtIn != null) {
                    builtIn.keySet().stream().map(Identifier::toString).forEach(results::add);
                    return results;
                }
            } catch (Exception ignored) {
            }

            if (regKey.equals(Registries.ITEM)) {
                return BuiltInRegistries.ITEM.keySet().stream().map(Identifier::toString).toList();
            } else if (regKey.equals(Registries.BLOCK)) {
                return BuiltInRegistries.BLOCK.keySet().stream().map(Identifier::toString).toList();
            } else if (regKey.equals(Registries.ENTITY_TYPE)) {
                return BuiltInRegistries.ENTITY_TYPE.keySet().stream().map(Identifier::toString).toList();
            } else if (regKey.equals(Registries.MOB_EFFECT)) {
                return BuiltInRegistries.MOB_EFFECT.keySet().stream().map(Identifier::toString).toList();
            } else if (regKey.equals(Registries.LOOT_TABLE)) {
                return RewardsTab.getLootTableSuggestions();
            }
            return results;
        }

        if (schema instanceof Schema.TagId(ResourceKey<? extends Registry<?>> registry, boolean hashed)) {
            List<String> tags = new ArrayList<>();
            try {
                List<Identifier> tagIds = SchemaCodecs.availableTagIds(registry);
                for (Identifier id : tagIds) {
                    tags.add(hashed ? "#" + id.toString() : id.toString());
                }
            } catch (Exception ignored) {
            }

            if (tags.isEmpty() && registry != null) {
                try {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.level != null) {
                        var lookup = mc.level.registryAccess().lookup(registry);
                        lookup.ifPresent(objectRegistryLookup -> objectRegistryLookup.listTagIds().map(k -> (hashed ? "#" : "") + k.location()).forEach(tags::add));
                    }
                } catch (Exception ignored) {
                }
            }
            return tags;
        }

        if (schema instanceof Schema.Enum<?> e) {
            List<String> options = new ArrayList<>();
            for (Object opt : e.options()) {
                try {
                    @SuppressWarnings("unchecked")
                    String label = ((Schema.Enum<Object>) e).label().apply(opt);
                    if (label != null) options.add(label);
                } catch (Exception ignored) {
                }
            }
            return options;
        }

        return List.of();
    }

    public static Schema<?> unwrapRef(Schema<?> schema) {
        return unwrapRef(schema, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    public static Schema<?> unwrapRef(Schema<?> schema, Set<Schema<?>> visited) {
        if (schema instanceof Schema.Ref<?> ref) {
            if (!visited.add(schema)) return schema;
            Schema<?> target = ref.target();
            return target != null ? unwrapRef(target, visited) : schema;
        }
        return schema;
    }

    public static String generateTemplateJson(@Nullable Schema<?> schema, int indentLevel) {
        return generateTemplateJson(schema, indentLevel, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    public static String generateTemplateJson(@Nullable Schema<?> schema, int indentLevel, Set<Schema<?>> visited) {
        if (schema == null || !visited.add(schema) || indentLevel > 4) return "{}";
        schema = unwrapRef(schema);
        String indent = "  ".repeat(indentLevel);
        String innerIndent = "  ".repeat(indentLevel + 1);

        if (schema instanceof Schema.Bool) return "false";
        if (schema instanceof Schema.IntRange r) return String.valueOf(Math.max(0, r.min()));
        if (schema instanceof Schema.FloatRange || schema instanceof Schema.DoubleRange || schema instanceof Schema.LongRange)
            return "0";
        if (schema instanceof Schema.Str) return "\"\"";
        if (schema instanceof Schema.ResourceId resourceId) {
            List<String> suggestions = getSuggestionsForSchema(resourceId);
            if (!suggestions.isEmpty()) return "\"" + suggestions.getFirst() + "\"";
            return "\"minecraft:stone\"";
        }
        if (schema instanceof Schema.TagId r) {
            return r.registry() != null ? "\"#" + r.registry().identifier() + "\"" : "\"#minecraft:items\"";
        }
        if (schema instanceof Schema.Enum<?> e) {
            return !e.options().isEmpty() ? "\"" + e.options().getFirst().toString().toLowerCase() + "\"" : "\"\"";
        }
        if (schema instanceof Schema.ListOf<?> l) {
            String elem = generateTemplateJson(l.element(), indentLevel + 1);
            if (elem.contains("\n")) {
                return "[\n" + innerIndent + elem.replace("\n", "\n" + innerIndent) + "\n" + indent + "]";
            }
            return "[ " + elem + " ]";
        }
        if (schema instanceof Schema.Record<?> rec) {
            StringBuilder sb = new StringBuilder("{\n");
            int count = 0;
            for (Schema.Field<?, ?> f : rec.fields()) {
                if (count++ > 0) sb.append(",\n");
                sb.append(innerIndent).append("\"").append(f.name()).append("\": ");
                String val = generateTemplateJson(f.schema(), indentLevel + 1);
                if (val.contains("\n")) {
                    sb.append(val.replace("\n", "\n" + innerIndent));
                } else {
                    sb.append(val);
                }
            }
            sb.append("\n").append(indent).append("}");
            return sb.toString();
        }
        return "{}";
    }
}
