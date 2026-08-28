package com.evandev.reliable_advancements.util;

import com.evandev.reliable_advancements.network.SyncClaimedRewardsPayload;
import com.evandev.reliable_advancements.platform.Services;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

public class RewardTrackerData extends SavedData {
    private static final Codec<Set<Identifier>> CLAIM_SET_CODEC = Identifier.CODEC.listOf().xmap(HashSet::new, ArrayList::new);
    private static final Codec<RewardTrackerData> CODEC = RecordCodecBuilder.create(
            i -> i.group(
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, CLAIM_SET_CODEC)
                            .optionalFieldOf("claims", Map.of())
                            .forGetter(data -> data.claimedRewards)
            ).apply(i, RewardTrackerData::new)
    );
    public static final SavedDataType<RewardTrackerData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("reliable_advancements", "claims"), RewardTrackerData::new, CODEC, DataFixTypes.LEVEL
    );

    private final Map<UUID, Set<Identifier>> claimedRewards;

    public RewardTrackerData() {
        this(new HashMap<>());
    }

    private RewardTrackerData(Map<UUID, Set<Identifier>> claimedRewards) {
        this.claimedRewards = new HashMap<>(claimedRewards);
    }

    public static RewardTrackerData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean isClaimed(UUID player, Identifier advancement) {
        return claimedRewards.getOrDefault(player, Collections.emptySet()).contains(advancement);
    }

    public void claim(UUID player, Identifier advancement) {
        claimedRewards.computeIfAbsent(player, k -> new HashSet<>()).add(advancement);
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
}
