package com.evandev.reliable_advancements.util;

import com.evandev.reliable_advancements.network.SyncClaimedRewardsPayload;
import com.evandev.reliable_advancements.platform.Services;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class RewardTrackerData extends SavedData {
    private static final String DATA_NAME = "reliable_advancements_claims";
    private final Map<UUID, Set<ResourceLocation>> claimedRewards = new HashMap<>();

    public static RewardTrackerData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                RewardTrackerData::load,
                RewardTrackerData::new,
                DATA_NAME
        );
    }

    public static RewardTrackerData load(CompoundTag tag) {
        RewardTrackerData data = new RewardTrackerData();
        for (String key : tag.getAllKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                Set<ResourceLocation> claims = new HashSet<>();
                ListTag list = tag.getList(key, Tag.TAG_STRING);
                for (int i = 0; i < list.size(); i++) {
                    claims.add(new ResourceLocation(list.getString(i)));
                }
                data.claimedRewards.put(uuid, claims);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return data;
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

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        for (Map.Entry<UUID, Set<ResourceLocation>> entry : claimedRewards.entrySet()) {
            ListTag list = new ListTag();
            for (ResourceLocation id : entry.getValue()) {
                list.add(StringTag.valueOf(id.toString()));
            }
            tag.put(entry.getKey().toString(), list);
        }
        return tag;
    }
}