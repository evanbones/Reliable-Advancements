package com.evandev.reliable_advancements.datagen;

import com.evandev.reliable_advancements.reference.Constants;
import com.google.gson.JsonObject;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.*;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class TriggerSchemaProvider implements DataProvider {
    private final PackOutput output;

    public TriggerSchemaProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput cache) {
        JsonObject root = new JsonObject();
        JsonObject triggers = new JsonObject();

        Map<CriterionTrigger<?>, Class<?>> manualMap = getManualMapping();
        for (CriterionTrigger<?> trigger : CriteriaTriggers.all()) {
            ResourceLocation id = trigger.getId();
            Class<?> recordClass = manualMap.get(trigger);

            if (recordClass != null) {
                JsonObject fields = new JsonObject();
                for (Field field : recordClass.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;
                    String fieldType = simplifyType(field.getGenericType());
                    String jsonKey = getJsonKey(field.getName());
                    fields.addProperty(jsonKey, fieldType);
                }
                triggers.add(id.toString(), fields);
            }
        }

        root.add("triggers", triggers);

        Path path = this.output.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
                .resolve(Constants.MOD_ID + "/trigger_schemas.json");
        return DataProvider.saveStable(cache, root, path);
    }

    private String getJsonKey(String fieldName) {
        return switch (fieldName) {
            case "entityPredicate" -> "entity";
            case "entityType" -> "type";
            case "playerPredicate" -> "player";
            case "locationPredicate" -> "location";
            case "killingBlow" -> "killing_blow";
            default -> fieldName.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
        };
    }

    private String simplifyType(Type type) {
        if (type instanceof Class<?> clazz) {
            if (clazz == String.class) return "string";
            if (clazz == Integer.class || clazz == int.class) return "integer";
            if (clazz == Boolean.class || clazz == boolean.class) return "boolean";
            if (clazz == Float.class || clazz == float.class || clazz == Double.class || clazz == double.class)
                return "float";
            if (clazz == ResourceLocation.class) return "resource_location";
            return "object";
        } else if (type instanceof ParameterizedType pType) {
            Class<?> rawType = (Class<?>) pType.getRawType();
            if (rawType == Optional.class) {
                return simplifyType(pType.getActualTypeArguments()[0]);
            } else if (rawType == List.class || rawType == Set.class) {
                return "list";
            } else if (rawType == Map.class) {
                return "object";
            }
        }
        return "object";
    }

    private Map<CriterionTrigger<?>, Class<?>> getManualMapping() {
        Map<CriterionTrigger<?>, Class<?>> map = new HashMap<>();
        map.put(CriteriaTriggers.IMPOSSIBLE, ImpossibleTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.PLAYER_KILLED_ENTITY, KilledTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.ENTITY_KILLED_PLAYER, KilledTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.ENTER_BLOCK, EnterBlockTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.INVENTORY_CHANGED, InventoryChangeTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.RECIPE_UNLOCKED, RecipeUnlockedTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.PLAYER_HURT_ENTITY, PlayerHurtEntityTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.ENTITY_HURT_PLAYER, EntityHurtPlayerTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.ENCHANTED_ITEM, EnchantedItemTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.FILLED_BUCKET, FilledBucketTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.BREWED_POTION, BrewedPotionTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.CONSTRUCT_BEACON, ConstructBeaconTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.USED_ENDER_EYE, UsedEnderEyeTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.SUMMONED_ENTITY, SummonedEntityTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.BRED_ANIMALS, BredAnimalsTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.LOCATION, PlayerTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.SLEPT_IN_BED, PlayerTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.CURED_ZOMBIE_VILLAGER, CuredZombieVillagerTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.TRADE, TradeTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.ITEM_DURABILITY_CHANGED, ItemDurabilityTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.LEVITATION, LevitationTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.CHANGED_DIMENSION, ChangeDimensionTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.TICK, PlayerTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.TAME_ANIMAL, TameAnimalTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.PLACED_BLOCK, ItemUsedOnLocationTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.CONSUME_ITEM, ConsumeItemTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.EFFECTS_CHANGED, EffectsChangedTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.USED_TOTEM, UsedTotemTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.NETHER_TRAVEL, DistanceTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.FISHING_ROD_HOOKED, FishingRodHookedTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.CHANNELED_LIGHTNING, ChanneledLightningTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.SHOT_CROSSBOW, ShotCrossbowTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.KILLED_BY_CROSSBOW, KilledByCrossbowTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.RAID_WIN, PlayerTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.BAD_OMEN, PlayerTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.HONEY_BLOCK_SLIDE, SlideDownBlockTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.BEE_NEST_DESTROYED, BeeNestDestroyedTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.TARGET_BLOCK_HIT, TargetBlockTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.ITEM_USED_ON_BLOCK, ItemUsedOnLocationTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.GENERATE_LOOT, LootTableTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.THROWN_ITEM_PICKED_UP_BY_ENTITY, PickedUpItemTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.THROWN_ITEM_PICKED_UP_BY_PLAYER, PickedUpItemTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.PLAYER_INTERACTED_WITH_ENTITY, PlayerInteractTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.START_RIDING_TRIGGER, StartRidingTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.LIGHTNING_STRIKE, LightningStrikeTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.USING_ITEM, UsingItemTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.FALL_FROM_HEIGHT, DistanceTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.RIDE_ENTITY_IN_LAVA_TRIGGER, DistanceTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.KILL_MOB_NEAR_SCULK_CATALYST, KilledTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.ALLAY_DROP_ITEM_ON_BLOCK, ItemUsedOnLocationTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.AVOID_VIBRATION, PlayerTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.RECIPE_CRAFTED, RecipeCraftedTrigger.TriggerInstance.class);
        return map;
    }

    @Override
    public @NotNull String getName() {
        return Constants.MOD_NAME + " Trigger Schemas";
    }
}