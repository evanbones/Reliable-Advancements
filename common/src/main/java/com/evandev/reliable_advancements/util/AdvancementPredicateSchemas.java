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
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.EntitySubPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

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

    private static Schema<ItemPredicate> createItemPredicateSchema(Schema<Object> intMinMax) {
        Schema<Object> itemHolderSet = holderSetSchema(Registries.ITEM);
        List<Schema.Field<ItemPredicate, ?>> itemFields = List.of(
                new Schema.Field<>("items", itemHolderSet, true, null),
                new Schema.Field<>("count", intMinMax, true, null),
                new Schema.Field<>("components", new Schema.MapOf<>(Schema.str(), new Schema.Opaque<>(null, null)), true, null),
                new Schema.Field<>("predicates", new Schema.MapOf<>(Schema.str(), new Schema.Opaque<>(null, null)), true, null),
                new Schema.Field<>("nbt", Schema.str(), true, null)
        );
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
        List<Schema.Field<Object, ?>> blockFields = List.of(
                new Schema.Field<>("blocks", blockHolderSet, true, null),
                new Schema.Field<>("state", new Schema.MapOf<>(Schema.str(), Schema.str()), true, null),
                new Schema.Field<>("nbt", Schema.str(), true, null)
        );
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

        List<Schema.Field<EntitySubPredicate, ?>> playerFields = List.of(
                new Schema.Field<>("level", intMinMax, true, null),
                new Schema.Field<>("gamemode", new Schema.Enum<>(List.of("survival", "creative", "adventure", "spectator"), Object::toString), true, null),
                new Schema.Field<>("stats", new Schema.ListOf<>(statSchema, 0, Integer.MAX_VALUE), true, null),
                new Schema.Field<>("recipes", new Schema.MapOf<>(new Schema.ResourceId(Registries.RECIPE), new Schema.Bool()), true, null),
                new Schema.Field<>("advancements", new Schema.MapOf<>(new Schema.ResourceId(Registries.ADVANCEMENT), advValueSchema), true, null),
                new Schema.Field<>("looking_at", entityPredicateRef, true, null)
        );
        Schema<EntitySubPredicate> playerSubPredicate = new Schema.Record<>(EntitySubPredicate.class, playerFields);
        registerVariant(subPredicateVariants, "minecraft:player", playerSubPredicate);

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
                new Schema.Field<>("is_captain", new Schema.Bool(), true, null),
                new Schema.Field<>("can_join_raid", new Schema.Bool(), true, null)
        );
        registerVariant(subPredicateVariants, "minecraft:raider", new Schema.Record<>(EntitySubPredicate.class, raiderFields));

        registerEnumVariant(subPredicateVariants, "minecraft:axolotl", List.of("lucy", "wild", "gold", "cyan", "blue"));
        registerEnumVariant(subPredicateVariants, "minecraft:boat", List.of("oak", "spruce", "birch", "jungle", "acacia", "cherry", "dark_oak", "mangrove", "bamboo"));
        registerEnumVariant(subPredicateVariants, "minecraft:fox", List.of("red", "snow"));
        registerEnumVariant(subPredicateVariants, "minecraft:mooshroom", List.of("red", "brown"));
        registerEnumVariant(subPredicateVariants, "minecraft:rabbit", List.of("brown", "white", "black", "white_splotched", "gold", "salt", "evil"));
        registerEnumVariant(subPredicateVariants, "minecraft:horse", List.of("white", "creamy", "chestnut", "brown", "black", "gray", "dark_brown"));
        registerEnumVariant(subPredicateVariants, "minecraft:llama", List.of("creamy", "white", "brown", "gray"));
        registerResourceVariant(subPredicateVariants, "minecraft:villager", Registries.VILLAGER_TYPE);
        registerEnumVariant(subPredicateVariants, "minecraft:parrot", List.of("red_blue", "blue", "green", "yellow_blue", "gray"));
        registerEnumVariant(subPredicateVariants, "minecraft:tropical_fish", List.of("kob", "sunstreak", "snooper", "dasher", "brinely", "spotty", "flopper", "stripey", "glitter", "blockfish", "betty", "clayfish"));
        registerResourceVariant(subPredicateVariants, "minecraft:painting", Registries.PAINTING_VARIANT);
        registerResourceVariant(subPredicateVariants, "minecraft:cat", Registries.CAT_VARIANT);
        registerResourceVariant(subPredicateVariants, "minecraft:frog", Registries.FROG_VARIANT);
        registerResourceVariant(subPredicateVariants, "minecraft:wolf", Registries.WOLF_VARIANT);

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
                new Schema.Field<>("is_on_fire", new Schema.Bool(), true, null),
                new Schema.Field<>("is_sneaking", new Schema.Bool(), true, null),
                new Schema.Field<>("is_sprinting", new Schema.Bool(), true, null),
                new Schema.Field<>("is_swimming", new Schema.Bool(), true, null),
                new Schema.Field<>("is_baby", new Schema.Bool(), true, null)
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

        List<Schema.Field<EntityPredicate, ?>> entityPredicateFields = List.of(
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
        );

        return new Schema.Record<>(EntityPredicate.class, entityPredicateFields);
    }

    private static <T> void registerVariant(Map<String, Schema<? extends T>> map, String name, Schema<? extends T> schema) {
        map.put(name, schema);
        if (name.startsWith("minecraft:")) {
            map.put(name.substring("minecraft:".length()), schema);
        }
    }

    private static void registerEnumVariant(Map<String, Schema<? extends EntitySubPredicate>> map, String name, List<String> options) {
        Schema<EntitySubPredicate> schema = new Schema.Record<>(EntitySubPredicate.class, List.of(
                new Schema.Field<>("variant", new Schema.Enum<>(options, Object::toString), false, null)
        ));
        registerVariant(map, name, schema);
    }

    private static void registerResourceVariant(Map<String, Schema<? extends EntitySubPredicate>> map, String name, ResourceKey<? extends Registry<?>> registry) {
        Schema<EntitySubPredicate> schema = new Schema.Record<>(EntitySubPredicate.class, List.of(
                new Schema.Field<>("variant", new Schema.ResourceId(registry), false, null)
        ));
        registerVariant(map, name, schema);
    }

    private static <T> Schema<T> holderSetSchema(ResourceKey<? extends Registry<?>> registry) {
        Schema<ResourceLocation> idSchema = new Schema.ResourceId(registry);
        Schema<ResourceLocation> tagSchema = new Schema.TagId(registry, true);
        Schema<List<ResourceLocation>> listSchema = new Schema.ListOf<>(idSchema, 0, Integer.MAX_VALUE);
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
