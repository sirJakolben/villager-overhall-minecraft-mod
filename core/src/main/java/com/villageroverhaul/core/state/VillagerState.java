package com.villageroverhaul.core.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

public record VillagerState(
        int level,
        int xp,
        int unspentUpgradePoints,
        GroupRanks ranks,
        Map<Identifier, Integer> questSlotRotations,
        Map<Identifier, Integer> tradeUsesRemaining,
        DailyProductivity dailyProductivity,
        Happiness happiness,
        long lastProcessedDay,
        Map<Identifier, Long> questSlotRerollAvailableAtTick,
        QuestLog questLog,
        Stations stations
) {

    /** The state a villager gets before any progression, work, or happiness tracking has ever touched it. */
    public static VillagerState initial() {
        return new VillagerState(
                0, 0, 0,
                GroupRanks.EMPTY,
                Map.of(),
                Map.of(),
                DailyProductivity.EMPTY,
                Happiness.EMPTY,
                0L,
                Map.of(),
                QuestLog.EMPTY,
                Stations.NONE
        );
    }

    public VillagerState withStations(Stations newStations) {
        return new VillagerState(
                level, xp, unspentUpgradePoints, ranks, questSlotRotations, tradeUsesRemaining,
                dailyProductivity, happiness, lastProcessedDay, questSlotRerollAvailableAtTick, questLog, newStations
        );
    }

    public static final MapCodec<VillagerState> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("level").forGetter(VillagerState::level),
            Codec.INT.fieldOf("xp").forGetter(VillagerState::xp),
            Codec.INT.fieldOf("unspent_upgrade_points").forGetter(VillagerState::unspentUpgradePoints),
            GroupRanks.CODEC.fieldOf("ranks").forGetter(VillagerState::ranks),
            Codec.unboundedMap(Identifier.CODEC, Codec.INT).fieldOf("quest_slot_rotations").forGetter(VillagerState::questSlotRotations),
            Codec.unboundedMap(Identifier.CODEC, Codec.INT).fieldOf("trade_uses_remaining").forGetter(VillagerState::tradeUsesRemaining),
            DailyProductivity.CODEC.fieldOf("daily_productivity").forGetter(VillagerState::dailyProductivity),
            Happiness.CODEC.fieldOf("happiness").forGetter(VillagerState::happiness),
            Codec.LONG.fieldOf("last_processed_day").forGetter(VillagerState::lastProcessedDay),
            Codec.unboundedMap(Identifier.CODEC, Codec.LONG).fieldOf("quest_slot_reroll_available_at_tick").forGetter(VillagerState::questSlotRerollAvailableAtTick),
            QuestLog.CODEC.optionalFieldOf("quest_log", QuestLog.EMPTY).forGetter(VillagerState::questLog),
            // Optional: saves from before the workstation rule load with Stations.NONE.
            Stations.CODEC.optionalFieldOf("workstations", Stations.NONE).forGetter(VillagerState::stations)
    ).apply(instance, VillagerState::new));

    public static final Codec<VillagerState> CODEC = MAP_CODEC.codec();

    public static final StreamCodec<RegistryFriendlyByteBuf, VillagerState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VillagerState::level,
            ByteBufCodecs.VAR_INT, VillagerState::xp,
            ByteBufCodecs.VAR_INT, VillagerState::unspentUpgradePoints,
            GroupRanks.STREAM_CODEC, VillagerState::ranks,
            ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, ByteBufCodecs.VAR_INT), VillagerState::questSlotRotations,
            ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, ByteBufCodecs.VAR_INT), VillagerState::tradeUsesRemaining,
            DailyProductivity.STREAM_CODEC, VillagerState::dailyProductivity,
            Happiness.STREAM_CODEC, VillagerState::happiness,
            ByteBufCodecs.VAR_LONG, VillagerState::lastProcessedDay,
            ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, ByteBufCodecs.VAR_LONG), VillagerState::questSlotRerollAvailableAtTick,
            QuestLog.STREAM_CODEC, VillagerState::questLog,
            Stations.STREAM_CODEC, VillagerState::stations,
            VillagerState::new
    );
}
