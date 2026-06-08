package com.evandev.reliable_advancements.util;

import com.evandev.reliable_advancements.network.SyncClaimedRewardsPayload;
import com.evandev.reliable_advancements.platform.Services;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class RewardTrackerData extends SavedData {
    public static final Codec<RewardTrackerData> CODEC = CompoundTag.CODEC.xmap(
            RewardTrackerData::load,
            RewardTrackerData::save
    );
    private static final Identifier DATA_ID = Identifier.parse("minecraft:reliable_advancements_claims");
    public static final SavedDataType<RewardTrackerData> TYPE = new SavedDataType<>(
            DATA_ID,
            RewardTrackerData::new,
            CODEC,
            null
    );

    private final Map<UUID, Set<Identifier>> claimedRewards = new HashMap<>();

    public static RewardTrackerData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public static RewardTrackerData load(CompoundTag tag) {
        RewardTrackerData data = new RewardTrackerData();
        for (String key : tag.keySet()) {
            try {
                UUID uuid = UUID.fromString(key);
                Set<Identifier> claims = new HashSet<>();

                Tag t = tag.get(key);
                if (t instanceof ListTag list) {
                    for (Tag item : list) {
                        item.asString().ifPresent(s -> claims.add(Identifier.parse(s)));
                    }
                }
                data.claimedRewards.put(uuid, claims);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return data;
    }

    public boolean isClaimed(UUID player, Identifier advancement) {
        return claimedRewards.getOrDefault(player, Collections.emptySet()).contains(advancement);
    }

    public void claim(UUID player, Identifier advancement) {
        claimedRewards.computeIfAbsent(player, _ -> new HashSet<>()).add(advancement);
        this.setDirty();
    }

    public void unclaim(UUID player, Identifier advancement) {
        if (claimedRewards.containsKey(player)) {
            claimedRewards.get(player).remove(advancement);
            this.setDirty();
        }
    }

    public void syncToPlayer(ServerPlayer player) {
        Set<Identifier> claims = claimedRewards.getOrDefault(player.getUUID(), Collections.emptySet());
        Services.PLATFORM.sendClaimedRewardsSync(player, new SyncClaimedRewardsPayload(new ArrayList<>(claims)));
    }

    public @NotNull CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<UUID, Set<Identifier>> entry : claimedRewards.entrySet()) {
            ListTag list = new ListTag();
            for (Identifier id : entry.getValue()) {
                list.add(StringTag.valueOf(id.toString()));
            }
            tag.put(entry.getKey().toString(), list);
        }
        return tag;
    }
}