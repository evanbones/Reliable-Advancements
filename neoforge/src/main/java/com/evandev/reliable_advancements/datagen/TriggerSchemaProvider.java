package com.evandev.reliable_advancements.datagen;

import com.evandev.reliable_advancements.reference.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.RecordComponent;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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
        for (Map.Entry<ResourceKey<CriterionTrigger<?>>, CriterionTrigger<?>> entry : BuiltInRegistries.TRIGGER_TYPES.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            CriterionTrigger<?> trigger = entry.getValue();
            Class<?> recordClass = manualMap.get(trigger);

            if (recordClass != null && recordClass.isRecord()) {
                JsonArray fields = new JsonArray();
                Arrays.stream(recordClass.getRecordComponents())
                        .map(RecordComponent::getName)
                        .map(this::getJsonKey)
                        .sorted()
                        .forEach(fields::add);
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
            case "beesInside" -> "num_bees_inside";
            default -> fieldName.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
        };
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
        map.put(CriteriaTriggers.RAID_OMEN, PlayerTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.HONEY_BLOCK_SLIDE, SlideDownBlockTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.BEE_NEST_DESTROYED, BeeNestDestroyedTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.TARGET_BLOCK_HIT, TargetBlockTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.ITEM_USED_ON_BLOCK, ItemUsedOnLocationTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.DEFAULT_BLOCK_USE, DefaultBlockInteractionTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.ANY_BLOCK_USE, AnyBlockInteractionTrigger.TriggerInstance.class);
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
        map.put(CriteriaTriggers.CRAFTER_RECIPE_CRAFTED, RecipeCraftedTrigger.TriggerInstance.class);
        map.put(CriteriaTriggers.FALL_AFTER_EXPLOSION, FallAfterExplosionTrigger.TriggerInstance.class);
        return map;
    }

    @Override
    public @NotNull String getName() {
        return Constants.MOD_NAME + " Trigger Schemas";
    }
}