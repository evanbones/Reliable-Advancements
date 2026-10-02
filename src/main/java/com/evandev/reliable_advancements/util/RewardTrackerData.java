package com.evandev.reliable_advancements.util;

import com.evandev.reliable_advancements.network.SyncClaimedRewardsPayload;
import com.evandev.reliable_advancements.platform.Services;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

//? if <26.1 {
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;
//?} else {
/*import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedDataType;
*///?}

public class RewardTrackerData extends SavedData {
    //? if <26.1 {
    private static final String DATA_NAME = "reliable_advancements_claims";
    //?} else {
    /*private static final Codec<Set<ResourceLocation>> CLAIM_SET_CODEC = ResourceLocation.CODEC.listOf().xmap(HashSet::new, ArrayList::new);
    private static final Codec<RewardTrackerData> CODEC = RecordCodecBuilder.create(
            i -> i.group(
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, CLAIM_SET_CODEC)
                            .optionalFieldOf("claims", Map.of())
                            .forGetter(data -> data.claimedRewards)
            ).apply(i, RewardTrackerData::new)
    );
    public static final SavedDataType<RewardTrackerData> TYPE = new SavedDataType<>(
            ResourceLocation.fromNamespaceAndPath("reliable_advancements", "claims"), RewardTrackerData::new, CODEC, DataFixTypes.LEVEL
    );
    *///?}

    private final Map<UUID, Set<ResourceLocation>> claimedRewards;

    public RewardTrackerData() {
        this(new HashMap<>());
    }

    private RewardTrackerData(Map<UUID, Set<ResourceLocation>> claimedRewards) {
        this.claimedRewards = new HashMap<>(claimedRewards);
    }

    public static RewardTrackerData get(MinecraftServer server) {
        //? if <26.1 {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(RewardTrackerData::new, RewardTrackerData::load, null),
                DATA_NAME
        );
        //?} else {
        /*return server.overworld().getDataStorage().computeIfAbsent(TYPE);
        *///?}
    }

    public boolean isClaimed(UUID player, ResourceLocation advancement) {
        return claimedRewards.getOrDefault(player, Collections.emptySet()).contains(advancement);
    }

    public void claim(UUID player, ResourceLocation advancement) {
        claimedRewards.computeIfAbsent(player, k -> new HashSet<>()).add(advancement);
        this.setDirty();
    }

    public void unclaim(UUID player, ResourceLocation advancement) {
        if (claimedRewards.containsKey(player)) {
            claimedRewards.get(player).remove(advancement);
            this.setDirty();
        }
    }

    public void syncToPlayer(ServerPlayer player) {
        Set<ResourceLocation> claims = claimedRewards.getOrDefault(player.getUUID(), Collections.emptySet());
        Services.PLATFORM.sendClaimedRewardsSync(player, new SyncClaimedRewardsPayload(new ArrayList<>(claims)));
    }

    //? if <26.1 {
    public static RewardTrackerData load(CompoundTag tag, HolderLookup.Provider provider) {
        RewardTrackerData data = new RewardTrackerData();
        for (String key : tag.getAllKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                Set<ResourceLocation> claims = new HashSet<>();
                ListTag list = tag.getList(key, Tag.TAG_STRING);
                for (int i = 0; i < list.size(); i++) {
                    claims.add(ResourceLocation.parse(list.getString(i)));
                }
                data.claimedRewards.put(uuid, claims);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        for (Map.Entry<UUID, Set<ResourceLocation>> entry : claimedRewards.entrySet()) {
            ListTag list = new ListTag();
            for (ResourceLocation id : entry.getValue()) {
                list.add(StringTag.valueOf(id.toString()));
            }
            tag.put(entry.getKey().toString(), list);
        }
        return tag;
    }
    //?}
}
