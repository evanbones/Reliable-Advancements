package com.evandev.reliable_advancements.util;

import com.evandev.reliable_advancements.reference.Constants;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapDecoder;
import com.mojang.serialization.codecs.OptionalFieldCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.codecui.Schema;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.codecui.SchemaHandler;
import net.minecraft.advancements.criterion.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public final class AdvancementPredicateSchemas {

    private static boolean registered = false;

    public static synchronized void registerAll() {
        if (registered) return;
        registered = true;

        try {
            registerRecordCodecHandler();

            Schema<Object> doubleMinMax = createDoubleMinMax();
            Schema<Object> intMinMax = createIntMinMax();

            Schema<ItemPredicate> itemPredicateSchema = createItemPredicateSchema(intMinMax);
            SchemaCodecs.registerCompanion(ItemPredicate.CODEC, itemPredicateSchema);

            Schema<LocationPredicate> locationPredicateSchema = createLocationPredicateSchema(doubleMinMax, intMinMax);
            SchemaCodecs.registerCompanion(LocationPredicate.CODEC, locationPredicateSchema);

            Schema.Ref<EntityPredicate> entityPredicateRef = new Schema.Ref<>();

            Schema<EntitySubPredicate> subPredicateOneOf = createEntitySubPredicateSchema(entityPredicateRef, intMinMax);
            SchemaCodecs.registerCompanion(EntitySubPredicate.CODEC, subPredicateOneOf);

            Schema<?> mobEffectsSchema = createMobEffectsSchema(intMinMax);

            Schema<EntityPredicate> entityPredicateSchema = createEntityPredicateSchema(
                    entityPredicateRef, doubleMinMax, intMinMax, itemPredicateSchema,
                    locationPredicateSchema, mobEffectsSchema, subPredicateOneOf
            );
            entityPredicateRef.bind(entityPredicateSchema);

            SchemaCodecs.registerCompanion(EntityPredicate.CODEC, entityPredicateSchema);

            Schema<LootItemCondition> lootConditionSchema = createLootItemConditionSchema(
                    entityPredicateRef, itemPredicateSchema
            );
            SchemaCodecs.registerCompanion(LootItemCondition.DIRECT_CODEC, lootConditionSchema);

            @SuppressWarnings({"unchecked", "rawtypes"})
            Schema<ContextAwarePredicate> contextAwareSchema =
                    (Schema) new Schema.ListOf<>(lootConditionSchema, 0, Integer.MAX_VALUE);
            SchemaCodecs.registerCompanion(ContextAwarePredicate.CODEC, contextAwareSchema);

            Schema<DamageSourcePredicate> damageSourcePredicateSchema =
                    createDamageSourcePredicateSchema(entityPredicateRef);
            SchemaCodecs.registerCompanion(DamageSourcePredicate.CODEC, damageSourcePredicateSchema);

            Schema<DamagePredicate> damagePredicateSchema =
                    createDamagePredicateSchema(entityPredicateRef, doubleMinMax, damageSourcePredicateSchema);
            SchemaCodecs.registerCompanion(DamagePredicate.CODEC, damagePredicateSchema);
        } catch (Throwable t) {
            Constants.LOG.error("Failed to register advancement predicate schemas", t);
        }
    }

    private static void registerRecordCodecHandler() {
        SchemaCodecs.registerHandler(new SchemaHandler() {
            @Override
            public Schema<?> tryResolve(Codec<?> codec, Resolver resolver) {
                return null;
            }

            @Override
            public Schema<?> tryResolveMap(MapCodec<?> codec, Resolver resolver) {
                if (codec instanceof OptionalFieldCodec<?> opt) {
                    try {
                        Field nameF = OptionalFieldCodec.class.getDeclaredField("name");
                        nameF.setAccessible(true);
                        String name = (String) nameF.get(opt);

                        Field elemF = OptionalFieldCodec.class.getDeclaredField("elementCodec");
                        elemF.setAccessible(true);
                        Codec<?> elem = (Codec<?>) elemF.get(opt);

                        if (name != null && elem != null) {
                            Schema<?> inner = resolver.resolve(elem);
                            return new Schema.Record<>(Object.class, List.of(new Schema.Field<>(name, inner, true, null)));
                        }
                    } catch (Throwable ignored) {
                    }
                }

                if (codec.getClass().getName().startsWith(RecordCodecBuilder.class.getName())) {
                    try {
                        List<Schema.Field<Object, ?>> fields = new ArrayList<>();
                        collectFieldsFromMapCodec(codec, fields, resolver, new HashSet<>(), 0);
                        if (!fields.isEmpty()) {
                            return new Schema.Record<>(Object.class, fields);
                        }
                    } catch (Throwable ignored) {
                    }
                }
                return null;
            }
        });
    }

    private static <T> Schema<T> createDoubleMinMax() {
        return Schema.anyOf(
                Schema.option("number", new Schema.DoubleRange(-Double.MAX_VALUE, Double.MAX_VALUE)),
                Schema.option("range", new Schema.Record<>(Object.class, List.of(
                        new Schema.Field<>("min", new Schema.DoubleRange(-Double.MAX_VALUE, Double.MAX_VALUE), true, null),
                        new Schema.Field<>("max", new Schema.DoubleRange(-Double.MAX_VALUE, Double.MAX_VALUE), true, null)
                )))
        );
    }

    private static <T> Schema<T> createIntMinMax() {
        return Schema.anyOf(
                Schema.option("number", new Schema.IntRange(Integer.MIN_VALUE, Integer.MAX_VALUE)),
                Schema.option("range", new Schema.Record<>(Object.class, List.of(
                        new Schema.Field<>("min", new Schema.IntRange(Integer.MIN_VALUE, Integer.MAX_VALUE), true, null),
                        new Schema.Field<>("max", new Schema.IntRange(Integer.MIN_VALUE, Integer.MAX_VALUE), true, null)
                )))
        );
    }

    private static <T> List<Schema.Field<T, ?>> dataComponentMatcherFields() {
        return List.of(
                new Schema.Field<>("components", new Schema.MapOf<>(Schema.str(), new Schema.Opaque<>(null, null)), true, null),
                new Schema.Field<>("predicates", new Schema.MapOf<>(Schema.str(), new Schema.Opaque<>(null, null)), true, null)
        );
    }

    private static Schema<ItemPredicate> createItemPredicateSchema(Schema<Object> intMinMax) {
        Schema<Object> itemHolderSet = holderSetSchema(Registries.ITEM);
        List<Schema.Field<ItemPredicate, ?>> itemFields = new ArrayList<>(List.of(
                new Schema.Field<>("items", itemHolderSet, true, null),
                new Schema.Field<>("count", intMinMax, true, null)
        ));
        itemFields.addAll(AdvancementPredicateSchemas.dataComponentMatcherFields());
        return new Schema.Record<>(ItemPredicate.class, itemFields);
    }

    private static Schema<LocationPredicate> createLocationPredicateSchema(Schema<Object> doubleMinMax, Schema<Object> intMinMax) {
        List<Schema.Field<Object, ?>> positionFields = List.of(
                new Schema.Field<>("x", doubleMinMax, true, null),
                new Schema.Field<>("y", doubleMinMax, true, null),
                new Schema.Field<>("z", doubleMinMax, true, null)
        );
        Schema<Object> positionSchema = new Schema.Record<>(Object.class, positionFields);

        Schema<Object> blockHolderSet = holderSetSchema(Registries.BLOCK);
        List<Schema.Field<Object, ?>> blockFields = new ArrayList<>(List.of(
                new Schema.Field<>("blocks", blockHolderSet, true, null),
                new Schema.Field<>("state", new Schema.MapOf<>(Schema.str(), Schema.str()), true, null),
                new Schema.Field<>("nbt", Schema.str(), true, null)
        ));
        blockFields.addAll(AdvancementPredicateSchemas.dataComponentMatcherFields());
        Schema<Object> blockPredicateSchema = new Schema.Record<>(Object.class, blockFields);

        Schema<Object> fluidHolderSet = holderSetSchema(Registries.FLUID);
        List<Schema.Field<Object, ?>> fluidFields = List.of(
                new Schema.Field<>("fluids", fluidHolderSet, true, null),
                new Schema.Field<>("state", new Schema.MapOf<>(Schema.str(), Schema.str()), true, null)
        );
        Schema<Object> fluidPredicateSchema = new Schema.Record<>(Object.class, fluidFields);

        List<Schema.Field<Object, ?>> lightFields = List.of(
                new Schema.Field<>("light", intMinMax, true, null)
        );
        Schema<Object> lightPredicateSchema = new Schema.Record<>(Object.class, lightFields);

        Schema<Object> biomeHolderSet = holderSetSchema(Registries.BIOME);
        Schema<Object> structureHolderSet = holderSetSchema(Registries.STRUCTURE);

        List<Schema.Field<LocationPredicate, ?>> locationFields = List.of(
                new Schema.Field<>("position", positionSchema, true, null),
                new Schema.Field<>("biomes", biomeHolderSet, true, null),
                new Schema.Field<>("structures", structureHolderSet, true, null),
                new Schema.Field<>("dimension", new Schema.ResourceId(Registries.DIMENSION), true, null),
                new Schema.Field<>("smokey", new Schema.Bool(), true, null),
                new Schema.Field<>("light", lightPredicateSchema, true, null),
                new Schema.Field<>("block", blockPredicateSchema, true, null),
                new Schema.Field<>("fluid", fluidPredicateSchema, true, null),
                new Schema.Field<>("can_see_sky", new Schema.Bool(), true, null)
        );
        return new Schema.Record<>(LocationPredicate.class, locationFields);
    }

    private static Schema<?> createMobEffectsSchema(Schema<Object> intMinMax) {
        List<Schema.Field<Object, ?>> effectInstFields = List.of(
                new Schema.Field<>("amplifier", intMinMax, true, null),
                new Schema.Field<>("duration", intMinMax, true, null),
                new Schema.Field<>("ambient", new Schema.Bool(), true, null),
                new Schema.Field<>("visible", new Schema.Bool(), true, null)
        );
        Schema<Object> effectInstSchema = new Schema.Record<>(Object.class, effectInstFields);
        return new Schema.MapOf<>(new Schema.ResourceId(Registries.MOB_EFFECT), effectInstSchema);
    }

    private static Schema<EntitySubPredicate> createEntitySubPredicateSchema(Schema.Ref<EntityPredicate> entityPredicateRef, Schema<Object> intMinMax) {
        Map<String, Schema<? extends EntitySubPredicate>> subPredicateVariants = new LinkedHashMap<>();

        List<Schema.Field<Object, ?>> statFields = List.of(
                new Schema.Field<>("type", new Schema.ResourceId(Registries.STAT_TYPE), false, null),
                new Schema.Field<>("stat", new Schema.ResourceId(null), false, null),
                new Schema.Field<>("value", intMinMax, false, null)
        );
        Schema<Object> statSchema = new Schema.Record<>(Object.class, statFields);

        Schema<Object> advValueSchema = Schema.anyOf(
                Schema.option("status", new Schema.Bool()),
                Schema.option("criteria", new Schema.MapOf<>(Schema.str(), new Schema.Bool()))
        );

        List<Schema.Field<Object, ?>> foodFields = List.of(
                new Schema.Field<>("level", intMinMax, true, null),
                new Schema.Field<>("saturation", createDoubleMinMax(), true, null)
        );
        Schema<Object> foodSchema = new Schema.Record<>(Object.class, foodFields);

        List<Schema.Field<Object, ?>> inputFields = List.of(
                new Schema.Field<>("forward", new Schema.Bool(), true, null),
                new Schema.Field<>("backward", new Schema.Bool(), true, null),
                new Schema.Field<>("left", new Schema.Bool(), true, null),
                new Schema.Field<>("right", new Schema.Bool(), true, null),
                new Schema.Field<>("jump", new Schema.Bool(), true, null),
                new Schema.Field<>("sneak", new Schema.Bool(), true, null),
                new Schema.Field<>("sprint", new Schema.Bool(), true, null)
        );
        Schema<Object> inputSchema = new Schema.Record<>(Object.class, inputFields);

        List<Schema.Field<EntitySubPredicate, ?>> playerFields = List.of(
                new Schema.Field<>("level", intMinMax, true, null),
                new Schema.Field<>("food", foodSchema, true, null),
                new Schema.Field<>("gamemode", new Schema.ListOf<>(new Schema.Enum<>(List.of("survival", "creative", "adventure", "spectator"), Object::toString), 0, Integer.MAX_VALUE), true, null),
                new Schema.Field<>("stats", new Schema.ListOf<>(statSchema, 0, Integer.MAX_VALUE), true, null),
                new Schema.Field<>("recipes", new Schema.MapOf<>(new Schema.ResourceId(Registries.RECIPE), new Schema.Bool()), true, null),
                new Schema.Field<>("advancements", new Schema.MapOf<>(new Schema.ResourceId(Registries.ADVANCEMENT), advValueSchema), true, null),
                new Schema.Field<>("looking_at", entityPredicateRef, true, null),
                new Schema.Field<>("input", inputSchema, true, null)
        );
        registerVariant(subPredicateVariants, "minecraft:player", new Schema.Record<>(EntitySubPredicate.class, playerFields));

        List<Schema.Field<EntitySubPredicate, ?>> lightningFields = List.of(
                new Schema.Field<>("blocks_set_on_fire", intMinMax, true, null),
                new Schema.Field<>("entity_struck", entityPredicateRef, true, null)
        );
        registerVariant(subPredicateVariants, "minecraft:lightning", new Schema.Record<>(EntitySubPredicate.class, lightningFields));

        List<Schema.Field<EntitySubPredicate, ?>> hookFields = List.of(
                new Schema.Field<>("in_open_water", new Schema.Bool(), true, null)
        );
        registerVariant(subPredicateVariants, "minecraft:fishing_hook", new Schema.Record<>(EntitySubPredicate.class, hookFields));

        List<Schema.Field<EntitySubPredicate, ?>> slimeFields = List.of(
                new Schema.Field<>("size", intMinMax, true, null)
        );
        registerVariant(subPredicateVariants, "minecraft:slime", new Schema.Record<>(EntitySubPredicate.class, slimeFields));

        List<Schema.Field<EntitySubPredicate, ?>> raiderFields = List.of(
                new Schema.Field<>("has_raid", new Schema.Bool(), true, null),
                new Schema.Field<>("is_captain", new Schema.Bool(), true, null)
        );
        registerVariant(subPredicateVariants, "minecraft:raider", new Schema.Record<>(EntitySubPredicate.class, raiderFields));

        List<Schema.Field<EntitySubPredicate, ?>> sheepFields = List.of(
                new Schema.Field<>("sheared", new Schema.Bool(), true, null)
        );
        registerVariant(subPredicateVariants, "minecraft:sheep", new Schema.Record<>(EntitySubPredicate.class, sheepFields));

        return new Schema.OneOf<>("type", subPredicateVariants);
    }

    private static Schema<EntityPredicate> createEntityPredicateSchema(
            Schema.Ref<EntityPredicate> entityPredicateRef,
            Schema<Object> doubleMinMax,
            Schema<Object> intMinMax,
            Schema<ItemPredicate> itemPredicateSchema,
            Schema<LocationPredicate> locationPredicateSchema,
            Schema<?> mobEffectsSchema,
            Schema<EntitySubPredicate> subPredicateOneOf
    ) {
        Schema<Object> entityTypeHolderSet = holderSetSchema(Registries.ENTITY_TYPE);

        List<Schema.Field<Object, ?>> distanceFields = List.of(
                new Schema.Field<>("x", doubleMinMax, true, null),
                new Schema.Field<>("y", doubleMinMax, true, null),
                new Schema.Field<>("z", doubleMinMax, true, null),
                new Schema.Field<>("horizontal", doubleMinMax, true, null),
                new Schema.Field<>("absolute", doubleMinMax, true, null)
        );
        Schema<Object> distancePredicateSchema = new Schema.Record<>(Object.class, distanceFields);

        List<Schema.Field<Object, ?>> movementFields = List.of(
                new Schema.Field<>("x", doubleMinMax, true, null),
                new Schema.Field<>("y", doubleMinMax, true, null),
                new Schema.Field<>("z", doubleMinMax, true, null),
                new Schema.Field<>("speed", doubleMinMax, true, null),
                new Schema.Field<>("horizontal_speed", doubleMinMax, true, null),
                new Schema.Field<>("vertical_speed", doubleMinMax, true, null),
                new Schema.Field<>("fall_distance", doubleMinMax, true, null)
        );
        Schema<Object> movementPredicateSchema = new Schema.Record<>(Object.class, movementFields);

        List<Schema.Field<Object, ?>> flagFields = List.of(
                new Schema.Field<>("is_on_ground", new Schema.Bool(), true, null),
                new Schema.Field<>("is_on_fire", new Schema.Bool(), true, null),
                new Schema.Field<>("is_sneaking", new Schema.Bool(), true, null),
                new Schema.Field<>("is_sprinting", new Schema.Bool(), true, null),
                new Schema.Field<>("is_swimming", new Schema.Bool(), true, null),
                new Schema.Field<>("is_flying", new Schema.Bool(), true, null),
                new Schema.Field<>("is_baby", new Schema.Bool(), true, null),
                new Schema.Field<>("is_in_water", new Schema.Bool(), true, null),
                new Schema.Field<>("is_fall_flying", new Schema.Bool(), true, null)
        );
        Schema<Object> flagsPredicateSchema = new Schema.Record<>(Object.class, flagFields);

        List<Schema.Field<Object, ?>> equipmentFields = List.of(
                new Schema.Field<>("head", itemPredicateSchema, true, null),
                new Schema.Field<>("chest", itemPredicateSchema, true, null),
                new Schema.Field<>("legs", itemPredicateSchema, true, null),
                new Schema.Field<>("feet", itemPredicateSchema, true, null),
                new Schema.Field<>("mainhand", itemPredicateSchema, true, null),
                new Schema.Field<>("offhand", itemPredicateSchema, true, null),
                new Schema.Field<>("body", itemPredicateSchema, true, null)
        );
        Schema<Object> equipmentPredicateSchema = new Schema.Record<>(Object.class, equipmentFields);

        List<Schema.Field<EntityPredicate, ?>> entityPredicateFields = new ArrayList<>(List.of(
                new Schema.Field<>("type", entityTypeHolderSet, true, null),
                new Schema.Field<>("distance", distancePredicateSchema, true, null),
                new Schema.Field<>("movement", movementPredicateSchema, true, null),
                new Schema.Field<>("location", locationPredicateSchema, true, null),
                new Schema.Field<>("stepping_on", locationPredicateSchema, true, null),
                new Schema.Field<>("movement_affected_by", locationPredicateSchema, true, null),
                new Schema.Field<>("effects", mobEffectsSchema, true, null),
                new Schema.Field<>("nbt", Schema.str(), true, null),
                new Schema.Field<>("flags", flagsPredicateSchema, true, null),
                new Schema.Field<>("equipment", equipmentPredicateSchema, true, null),
                new Schema.Field<>("type_specific", subPredicateOneOf, true, null),
                new Schema.Field<>("periodic_tick", new Schema.IntRange(1, Integer.MAX_VALUE), true, null),
                new Schema.Field<>("vehicle", entityPredicateRef, true, null),
                new Schema.Field<>("passenger", entityPredicateRef, true, null),
                new Schema.Field<>("targeted_entity", entityPredicateRef, true, null),
                new Schema.Field<>("team", Schema.str(), true, null),
                new Schema.Field<>("slots", new Schema.MapOf<>(Schema.str(), itemPredicateSchema), true, null)
        ));
        entityPredicateFields.addAll(AdvancementPredicateSchemas.dataComponentMatcherFields());

        return new Schema.Record<>(EntityPredicate.class, entityPredicateFields);
    }

    private static Schema<LootItemCondition> createLootItemConditionSchema(
            Schema.Ref<EntityPredicate> entityPredicateRef,
            Schema<ItemPredicate> itemPredicateSchema
    ) {
        Schema.Ref<LootItemCondition> conditionRef = new Schema.Ref<>();

        Schema<Object> numberProvider = Schema.anyOf(
                Schema.option("number", new Schema.DoubleRange(-Double.MAX_VALUE, Double.MAX_VALUE)),
                Schema.option("object", opaque())
        );

        Schema<Object> entityTargetEnum = new Schema.Enum<>(
                List.of("this", "attacker", "direct_attacker", "attacking_player", "target_entity", "interacting_entity"),
                Object::toString
        );

        Map<String, Schema<? extends LootItemCondition>> variants = new LinkedHashMap<>();

        registerVariant(variants, "minecraft:entity_properties", new Schema.Record<>(LootItemCondition.class, List.of(
                new Schema.Field<>("predicate", entityPredicateRef, true, null),
                new Schema.Field<>("entity", entityTargetEnum, false, null)
        )));

        registerVariant(variants, "minecraft:killed_by_player", new Schema.Record<>(LootItemCondition.class, List.of()));

        registerVariant(variants, "minecraft:inverted", new Schema.Record<>(LootItemCondition.class, List.of(
                new Schema.Field<>("term", conditionRef, false, null)
        )));

        registerVariant(variants, "minecraft:any_of", new Schema.Record<>(LootItemCondition.class, List.of(
                new Schema.Field<>("terms", new Schema.ListOf<>(conditionRef, 0, Integer.MAX_VALUE), false, null)
        )));

        registerVariant(variants, "minecraft:all_of", new Schema.Record<>(LootItemCondition.class, List.of(
                new Schema.Field<>("terms", new Schema.ListOf<>(conditionRef, 0, Integer.MAX_VALUE), false, null)
        )));

        registerVariant(variants, "minecraft:random_chance", new Schema.Record<>(LootItemCondition.class, List.of(
                new Schema.Field<>("chance", numberProvider, false, null)
        )));

        registerVariant(variants, "minecraft:match_tool", new Schema.Record<>(LootItemCondition.class, List.of(
                new Schema.Field<>("predicate", itemPredicateSchema, true, null)
        )));

        registerVariant(variants, "minecraft:entity_scores", new Schema.Record<>(LootItemCondition.class, List.of(
                new Schema.Field<>("scores", new Schema.MapOf<>(Schema.str(), opaque()), false, null),
                new Schema.Field<>("entity", entityTargetEnum, false, null)
        )));

        for (Identifier id : BuiltInRegistries.LOOT_CONDITION_TYPE.keySet()) {
            String name = id.toString();
            if (!variants.containsKey(name)) {
                registerVariant(variants, name, opaque());
            }
        }

        Schema<LootItemCondition> oneOf = new Schema.OneOf<>("condition", variants);
        conditionRef.bind(oneOf);
        return oneOf;
    }

    private static <T> Schema<T> opaque() {
        return new Schema.Opaque<>(null, null);
    }

    private static Schema<DamageSourcePredicate> createDamageSourcePredicateSchema(Schema.Ref<EntityPredicate> entityPredicateRef) {
        List<Schema.Field<Object, ?>> tagFields = List.of(
                new Schema.Field<>("id", new Schema.TagId(Registries.DAMAGE_TYPE, false), false, null),
                new Schema.Field<>("expected", new Schema.Bool(), false, null)
        );
        Schema<Object> tagPredicateSchema = new Schema.Record<>(Object.class, tagFields);

        List<Schema.Field<DamageSourcePredicate, ?>> fields = List.of(
                new Schema.Field<>("tags", new Schema.ListOf<>(tagPredicateSchema, 0, Integer.MAX_VALUE), true, null),
                new Schema.Field<>("direct_entity", entityPredicateRef, true, null),
                new Schema.Field<>("source_entity", entityPredicateRef, true, null),
                new Schema.Field<>("is_direct", new Schema.Bool(), true, null)
        );
        return new Schema.Record<>(DamageSourcePredicate.class, fields);
    }

    private static Schema<DamagePredicate> createDamagePredicateSchema(
            Schema.Ref<EntityPredicate> entityPredicateRef,
            Schema<Object> doubleMinMax,
            Schema<DamageSourcePredicate> damageSourcePredicateSchema
    ) {
        List<Schema.Field<DamagePredicate, ?>> fields = List.of(
                new Schema.Field<>("dealt", doubleMinMax, true, null),
                new Schema.Field<>("taken", doubleMinMax, true, null),
                new Schema.Field<>("source_entity", entityPredicateRef, true, null),
                new Schema.Field<>("blocked", new Schema.Bool(), true, null),
                new Schema.Field<>("type", damageSourcePredicateSchema, true, null)
        );
        return new Schema.Record<>(DamagePredicate.class, fields);
    }

    private static <T> void registerVariant(Map<String, Schema<? extends T>> map, String name, Schema<? extends T> schema) {
        map.put(name, schema);
        if (name.startsWith("minecraft:")) {
            map.put(name.substring("minecraft:".length()), schema);
        }
    }

    private static <T> Schema<T> holderSetSchema(ResourceKey<? extends Registry<?>> registry) {
        Schema<Identifier> idSchema = new Schema.ResourceId(registry);
        Schema<Identifier> tagSchema = new Schema.TagId(registry, true);
        Schema<List<Identifier>> listSchema = new Schema.ListOf<>(idSchema, 0, Integer.MAX_VALUE);
        return Schema.anyOf(
                Schema.option("id", idSchema),
                Schema.option("tag", tagSchema),
                Schema.option("list", listSchema)
        );
    }

    private static void collectFieldsFromMapCodec(
            Object node,
            List<Schema.Field<Object, ?>> fields,
            SchemaHandler.Resolver resolver,
            Set<Object> visited,
            int depth
    ) {
        if (node == null || depth > 20 || !visited.add(node)) return;

        if (node instanceof OptionalFieldCodec<?> opt) {
            try {
                Field nameF = OptionalFieldCodec.class.getDeclaredField("name");
                nameF.setAccessible(true);
                String name = (String) nameF.get(opt);

                Field elemF = OptionalFieldCodec.class.getDeclaredField("elementCodec");
                elemF.setAccessible(true);
                Codec<?> elem = (Codec<?>) elemF.get(opt);

                if (name != null && elem != null) {
                    Schema<?> inner = resolver.resolve(elem);
                    fields.add(new Schema.Field<>(name, inner, true, null));
                    return;
                }
            } catch (Throwable ignored) {
            }
        }

        if (node.getClass().getName().endsWith("FieldDecoder")) {
            try {
                Field nameF = node.getClass().getDeclaredField("name");
                nameF.setAccessible(true);
                String name = (String) nameF.get(node);

                Field elemF = node.getClass().getDeclaredField("elementCodec");
                elemF.setAccessible(true);
                Object elemObj = elemF.get(node);
                if (elemObj instanceof Codec<?> elem && name != null) {
                    Schema<?> inner = resolver.resolve(elem);
                    fields.add(new Schema.Field<>(name, inner, false, null));
                    return;
                }
            } catch (Throwable ignored) {
            }
        }

        for (Class<?> cls = node.getClass(); cls != null && cls != Object.class; cls = cls.getSuperclass()) {
            for (Field f : cls.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                f.setAccessible(true);
                try {
                    Object v = f.get(node);
                    if (v == null || v == node) continue;

                    if (v instanceof RecordCodecBuilder<?, ?> rcb) {
                        Field decF = RecordCodecBuilder.class.getDeclaredField("decoder");
                        decF.setAccessible(true);
                        Object dec = decF.get(rcb);
                        if (dec != null && !isPureUnitDecoder(dec)) {
                            collectFieldsFromMapCodec(dec, fields, resolver, visited, depth + 1);
                        }
                    } else if (v instanceof MapDecoder<?>) {
                        collectFieldsFromMapCodec(v, fields, resolver, visited, depth + 1);
                    }
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static boolean isPureUnitDecoder(Object dec) {
        String name = dec.getClass().getName();
        return name.contains("Decoder$5") || name.contains("UnitDecoder") || name.contains("Point") || name.contains("Stable");
    }
}
