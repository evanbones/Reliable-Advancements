package com.evandev.reliable_advancements.util;

import com.evandev.reliable_advancements.gui.tabs.RewardsTab;
import com.evandev.reliable_advancements.reference.Constants;
import net.mehvahdjukaar.codecui.Schema;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.codecui.internal.CuratedSchemas;
import net.mehvahdjukaar.codecui.internal.SchemaResolver;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
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
            registerCustomAdvancementSchemas();
            for (Registry<?> registry : BuiltInRegistries.REGISTRY) {
                registerDynamicRegistry(registry);
            }
        } catch (Throwable ignored) {
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerCustomAdvancementSchemas() {
        try {
            Schema.Str str = new Schema.Str(0, Integer.MAX_VALUE, null);
            Schema.Bool bool = new Schema.Bool();
            Schema.IntRange intAll = new Schema.IntRange(Integer.MIN_VALUE, Integer.MAX_VALUE);
            Schema.DoubleRange doubleAll = new Schema.DoubleRange(-Double.MAX_VALUE, Double.MAX_VALUE);

            Schema<?> intBounds = Schema.anyOf(
                    Schema.option("exact", intAll),
                    Schema.option("range", new Schema.Record<>(Object.class, List.of(
                            new Schema.Field<>("min", intAll, true, null),
                            new Schema.Field<>("max", intAll, true, null)
                    )))
            );

            Schema<?> doubleBounds = Schema.anyOf(
                    Schema.option("exact", doubleAll),
                    Schema.option("range", new Schema.Record<>(Object.class, List.of(
                            new Schema.Field<>("min", doubleAll, true, null),
                            new Schema.Field<>("max", doubleAll, true, null)
                    )))
            );

            Schema<?> itemOrTag = Schema.anyOf(
                    Schema.option("id", new Schema.ResourceId(Registries.ITEM)),
                    Schema.option("tag", new Schema.TagId(Registries.ITEM, true)),
                    Schema.option("list", new Schema.ListOf<>(new Schema.ResourceId(Registries.ITEM), 1, Integer.MAX_VALUE))
            );

            Schema<?> itemSchema = new Schema.Record<>(ItemPredicate.class, List.of(
                    new Schema.Field<>("items", itemOrTag, true, null),
                    new Schema.Field<>("count", intBounds, true, null),
                    new Schema.Field<>("predicates", new Schema.MapOf<>(new Schema.ResourceId(null), str), true, null)
            ));

            Schema<?> biomeOrTag = Schema.anyOf(
                    Schema.option("id", new Schema.ResourceId(Registries.BIOME)),
                    Schema.option("tag", new Schema.TagId(Registries.BIOME, true)),
                    Schema.option("list", new Schema.ListOf<>(new Schema.ResourceId(Registries.BIOME), 1, Integer.MAX_VALUE))
            );
            Schema<?> structOrTag = Schema.anyOf(
                    Schema.option("id", new Schema.ResourceId(Registries.STRUCTURE)),
                    Schema.option("tag", new Schema.TagId(Registries.STRUCTURE, true))
            );

            Schema<?> locationSchema = new Schema.Record<>(LocationPredicate.class, List.of(
                    new Schema.Field<>("biomes", biomeOrTag, true, null),
                    new Schema.Field<>("structures", structOrTag, true, null),
                    new Schema.Field<>("dimension", new Schema.ResourceId(Registries.DIMENSION_TYPE), true, null),
                    new Schema.Field<>("position", new Schema.Record<>(Object.class, List.of(
                            new Schema.Field<>("x", doubleBounds, true, null),
                            new Schema.Field<>("y", doubleBounds, true, null),
                            new Schema.Field<>("z", doubleBounds, true, null)
                    )), true, null),
                    new Schema.Field<>("block", new Schema.Record<>(Object.class, List.of(
                            new Schema.Field<>("blocks", Schema.anyOf(Schema.option("id", new Schema.ResourceId(Registries.BLOCK)), Schema.option("tag", new Schema.TagId(Registries.BLOCK, true))), true, null),
                            new Schema.Field<>("state", new Schema.MapOf<>(str, str), true, null)
                    )), true, null),
                    new Schema.Field<>("fluid", new Schema.Record<>(Object.class, List.of(
                            new Schema.Field<>("fluids", Schema.anyOf(Schema.option("id", new Schema.ResourceId(Registries.FLUID)), Schema.option("tag", new Schema.TagId(Registries.FLUID, true))), true, null),
                            new Schema.Field<>("state", new Schema.MapOf<>(str, str), true, null)
                    )), true, null),
                    new Schema.Field<>("light", new Schema.Record<>(Object.class, List.of(
                            new Schema.Field<>("light", intBounds, true, null)
                    )), true, null),
                    new Schema.Field<>("smokey", bool, true, null),
                    new Schema.Field<>("can_see_sky", bool, true, null)
            ));

            Schema.Ref entityRef = new Schema.Ref<>();

            Schema<?> playerSubPredicate = new Schema.Record<>(Object.class, List.of(
                    new Schema.Field<>("looking_at", entityRef, true, null),
                    new Schema.Field<>("gamemode", new Schema.Enum<>(List.of("survival", "creative", "adventure", "spectator"), String::valueOf), true, null),
                    new Schema.Field<>("level", intBounds, true, null),
                    new Schema.Field<>("advancements", new Schema.MapOf<>(new Schema.ResourceId(Registries.ADVANCEMENT), bool), true, null),
                    new Schema.Field<>("recipes", new Schema.MapOf<>(new Schema.ResourceId(Registries.RECIPE), bool), true, null)
            ));

            LinkedHashMap<String, Schema<?>> subPredicates = new LinkedHashMap<>();
            subPredicates.put("minecraft:player", playerSubPredicate);
            subPredicates.put("minecraft:lightning", new Schema.Record<>(Object.class, List.of(
                    new Schema.Field<>("blocks_set_on_fire", intBounds, true, null),
                    new Schema.Field<>("entity_struck", entityRef, true, null)
            )));
            subPredicates.put("minecraft:raider", new Schema.Record<>(Object.class, List.of(
                    new Schema.Field<>("has_raid", bool, true, null),
                    new Schema.Field<>("is_captain", bool, true, null),
                    new Schema.Field<>("wave", intBounds, true, null)
            )));
            subPredicates.put("minecraft:sheep", new Schema.Record<>(Object.class, List.of(
                    new Schema.Field<>("sheared", bool, true, null)
            )));
            subPredicates.put("minecraft:wolf", new Schema.Record<>(Object.class, List.of(
                    new Schema.Field<>("variant", new Schema.ResourceId(null), true, null)
            )));
            subPredicates.put("minecraft:cat", new Schema.Record<>(Object.class, List.of(
                    new Schema.Field<>("variant", new Schema.ResourceId(null), true, null)
            )));
            subPredicates.put("minecraft:slime", new Schema.Record<>(Object.class, List.of(
                    new Schema.Field<>("size", intBounds, true, null)
            )));
            Schema.OneOf<?> entitySubPredicateSchema = new Schema.OneOf<>("type", subPredicates);

            Schema<?> entityTypeOrTag = Schema.anyOf(
                    Schema.option("id", new Schema.ResourceId(Registries.ENTITY_TYPE)),
                    Schema.option("tag", new Schema.TagId(Registries.ENTITY_TYPE, true))
            );

            Schema<?> entityPredicateSchema = new Schema.Record<>(EntityPredicate.class, List.of(
                    new Schema.Field<>("type", entityTypeOrTag, true, null),
                    new Schema.Field<>("distance", new Schema.Record<>(Object.class, List.of(
                            new Schema.Field<>("absolute", doubleBounds, true, null),
                            new Schema.Field<>("horizontal", doubleBounds, true, null),
                            new Schema.Field<>("x", doubleBounds, true, null),
                            new Schema.Field<>("y", doubleBounds, true, null),
                            new Schema.Field<>("z", doubleBounds, true, null)
                    )), true, null),
                    new Schema.Field<>("movement", new Schema.Record<>(Object.class, List.of(
                            new Schema.Field<>("x", doubleBounds, true, null),
                            new Schema.Field<>("y", doubleBounds, true, null),
                            new Schema.Field<>("z", doubleBounds, true, null),
                            new Schema.Field<>("speed", doubleBounds, true, null),
                            new Schema.Field<>("horizontal_speed", doubleBounds, true, null),
                            new Schema.Field<>("vertical_speed", doubleBounds, true, null),
                            new Schema.Field<>("fall_distance", doubleBounds, true, null)
                    )), true, null),
                    new Schema.Field<>("location", locationSchema, true, null),
                    new Schema.Field<>("stepping_on", locationSchema, true, null),
                    new Schema.Field<>("movement_affected_by", locationSchema, true, null),
                    new Schema.Field<>("effects", new Schema.MapOf<>(new Schema.ResourceId(Registries.MOB_EFFECT), new Schema.Record<>(Object.class, List.of(
                            new Schema.Field<>("amplifier", intBounds, true, null),
                            new Schema.Field<>("duration", intBounds, true, null),
                            new Schema.Field<>("ambient", bool, true, null),
                            new Schema.Field<>("visible", bool, true, null)
                    ))), true, null),
                    new Schema.Field<>("nbt", str, true, null),
                    new Schema.Field<>("flags", new Schema.Record<>(Object.class, List.of(
                            new Schema.Field<>("is_on_fire", bool, true, null),
                            new Schema.Field<>("is_crouching", bool, true, null),
                            new Schema.Field<>("is_sprinting", bool, true, null),
                            new Schema.Field<>("is_swimming", bool, true, null),
                            new Schema.Field<>("is_baby", bool, true, null)
                    )), true, null),
                    new Schema.Field<>("equipment", new Schema.Record<>(Object.class, List.of(
                            new Schema.Field<>("head", itemSchema, true, null),
                            new Schema.Field<>("chest", itemSchema, true, null),
                            new Schema.Field<>("legs", itemSchema, true, null),
                            new Schema.Field<>("feet", itemSchema, true, null),
                            new Schema.Field<>("mainhand", itemSchema, true, null),
                            new Schema.Field<>("offhand", itemSchema, true, null)
                    )), true, null),
                    new Schema.Field<>("type_specific", entitySubPredicateSchema, true, null),
                    new Schema.Field<>("vehicle", entityRef, true, null),
                    new Schema.Field<>("passenger", entityRef, true, null),
                    new Schema.Field<>("targeted_entity", entityRef, true, null),
                    new Schema.Field<>("team", str, true, null)
            ));

            entityRef.bind(entityPredicateSchema);

            Schema.Ref conditionRef = new Schema.Ref<>();

            LinkedHashMap<String, Schema<?>> conditionVariants = new LinkedHashMap<>();
            conditionVariants.put("minecraft:entity_properties", new Schema.Record<>(Object.class, List.of(
                    new Schema.Field<>("entity", new Schema.Enum<>(List.of("this", "killer", "direct_killer", "killer_player"), String::valueOf), false, "this"),
                    new Schema.Field<>("predicate", entityRef, true, null)
            )));
            conditionVariants.put("minecraft:location_check", new Schema.Record<>(Object.class, List.of(
                    new Schema.Field<>("predicate", locationSchema, true, null),
                    new Schema.Field<>("offset", new Schema.ListOf<>(intAll, 3, 3), true, null)
            )));
            conditionVariants.put("minecraft:match_tool", new Schema.Record<>(Object.class, List.of(
                    new Schema.Field<>("predicate", itemSchema, true, null)
            )));
            conditionVariants.put("minecraft:survives_explosion", new Schema.Record<>(Object.class, List.of()));
            conditionVariants.put("minecraft:weather_check", new Schema.Record<>(Object.class, List.of(
                    new Schema.Field<>("raining", bool, true, null),
                    new Schema.Field<>("thundering", bool, true, null)
            )));
            conditionVariants.put("minecraft:inverted", new Schema.Record<>(Object.class, List.of(
                    new Schema.Field<>("term", conditionRef, false, null)
            )));
            conditionVariants.put("minecraft:any_of", new Schema.Record<>(Object.class, List.of(
                    new Schema.Field<>("terms", new Schema.ListOf<>(conditionRef, 1, Integer.MAX_VALUE), false, null)
            )));
            conditionVariants.put("minecraft:all_of", new Schema.Record<>(Object.class, List.of(
                    new Schema.Field<>("terms", new Schema.ListOf<>(conditionRef, 1, Integer.MAX_VALUE), false, null)
            )));

            Schema.OneOf<?> conditionSchema = new Schema.OneOf<>("condition", conditionVariants);
            conditionRef.bind(conditionSchema);

            Schema<?> contextAwareSchema = new Schema.ListOf<>(conditionSchema, 0, Integer.MAX_VALUE);

            Schema<?> advancementEntityCodecSchema = Schema.anyOf(
                    Schema.option("conditions", contextAwareSchema),
                    Schema.option("direct", entityPredicateSchema)
            );

            SchemaCodecs.registerCompanion(EntityPredicate.ADVANCEMENT_CODEC, (Schema) advancementEntityCodecSchema);
            SchemaCodecs.registerCompanion(EntityPredicate.CODEC, (Schema) entityPredicateSchema);
            SchemaCodecs.registerCompanion(ItemPredicate.CODEC, (Schema) itemSchema);
            SchemaCodecs.registerCompanion(LocationPredicate.CODEC, (Schema) locationSchema);
            SchemaCodecs.registerCompanion(ContextAwarePredicate.CODEC, (Schema) contextAwareSchema);
            SchemaCodecs.registerCompanion(LootItemCondition.DIRECT_CODEC, (Schema) conditionSchema);
            SchemaCodecs.registerCompanion(LootItemCondition.TYPED_CODEC, (Schema) conditionSchema);
        } catch (Throwable t) {
            Constants.LOG.warn("Failed to register custom advancement schemas", t);
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
            ResourceLocation id = registry.getKey(v);
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
                ResourceLocation id = ResourceLocation.tryParse(idStr);
                if (id == null) return null;
                CriterionTrigger<?> trigger = BuiltInRegistries.TRIGGER_TYPES.get(id);
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
                return BuiltInRegistries.ITEM.keySet().stream().map(ResourceLocation::toString).toList();
            }
            List<String> results = new ArrayList<>();
            try {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level != null) {
                    var lookup = mc.level.registryAccess().lookup(regKey);
                    if (lookup.isPresent()) {
                        lookup.get().listElementIds().map(k -> k.location().toString()).forEach(results::add);
                        return results;
                    }
                }
            } catch (Exception ignored) {
            }

            try {
                var builtIn = BuiltInRegistries.REGISTRY.get(regKey.location());
                if (builtIn != null) {
                    builtIn.keySet().stream().map(ResourceLocation::toString).forEach(results::add);
                    return results;
                }
            } catch (Exception ignored) {
            }

            if (regKey.equals(Registries.ITEM)) {
                return BuiltInRegistries.ITEM.keySet().stream().map(ResourceLocation::toString).toList();
            } else if (regKey.equals(Registries.BLOCK)) {
                return BuiltInRegistries.BLOCK.keySet().stream().map(ResourceLocation::toString).toList();
            } else if (regKey.equals(Registries.ENTITY_TYPE)) {
                return BuiltInRegistries.ENTITY_TYPE.keySet().stream().map(ResourceLocation::toString).toList();
            } else if (regKey.equals(Registries.MOB_EFFECT)) {
                return BuiltInRegistries.MOB_EFFECT.keySet().stream().map(ResourceLocation::toString).toList();
            } else if (regKey.equals(Registries.LOOT_TABLE)) {
                return RewardsTab.getLootTableSuggestions();
            }
            return results;
        }

        if (schema instanceof Schema.TagId(ResourceKey<? extends Registry<?>> registry, boolean hashed)) {
            List<String> tags = new ArrayList<>();
            try {
                List<ResourceLocation> tagIds = SchemaCodecs.availableTagIds(registry);
                for (ResourceLocation id : tagIds) {
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
        if (schema instanceof Schema.ResourceId(ResourceKey<? extends Registry<?>> registry)) {
            return registry != null ? "\"minecraft:" + registry.location().getPath() + "\"" : "\"minecraft:stone\"";
        }
        if (schema instanceof Schema.TagId r) {
            return r.registry() != null ? "\"#" + r.registry().location() + "\"" : "\"#minecraft:items\"";
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