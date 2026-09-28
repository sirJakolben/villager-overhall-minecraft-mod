package com.villageroverhaul.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Everything our mod remembers about one villager - the single source of truth its offers are derived from
 * (trade/VillagerOffers). Immutable: every change goes through one of the with-methods and is stored with
 * VillagerStateAccess.setState.
 *
 * ranks: rank per section id (api/SectionDefinition), 0 when absent. stock: uses left per entry id (a trade
 * or quest of any section); an entry without a value starts at half its max (RestockService.initialStock).
 * workDay: the day whose work day has started (RestockService.startDay) - the first work scan of a new day
 * starts it.
 * State a section's own logic needs beyond this (the Trade Rework's quest slots) lives in that logic's own
 * attachment.
 */
public record VillagerState(
        int level,
        int xp,
        int unspentUpgradePoints,
        Map<Identifier, Integer> ranks,
        Map<Identifier, Integer> stock,
        Productivity productivity,
        Happiness happiness,
        long workDay,
        Stations stations
) {

    /** The state a villager gets before any progression, work, or happiness tracking has ever touched it. */
    public static VillagerState initial() {
        return new VillagerState(0, 0, 0, Map.of(), Map.of(), Productivity.EMPTY, Happiness.EMPTY, 0L, Stations.NONE);
    }

    /**
     * Back to a fresh start (/vo reset): level, XP, points, ranks, stock and meters as initial(). Happiness and
     * stations stay - they describe the villager's surroundings (bed, village, owned stations), not its progress.
     */
    public VillagerState withProgressReset() {
        return new VillagerState(0, 0, 0, Map.of(), Map.of(), Productivity.EMPTY, happiness, 0L, stations);
    }

    public int rank(Identifier section) {
        return ranks.getOrDefault(section, 0);
    }

    public VillagerState withProgress(int newLevel, int newXp, int newUnspentUpgradePoints) {
        return new VillagerState(newLevel, newXp, newUnspentUpgradePoints, ranks, stock, productivity, happiness, workDay, stations);
    }

    public VillagerState withRank(Identifier section, int rank) {
        Map<Identifier, Integer> newRanks = new HashMap<>(ranks);
        newRanks.put(section, rank);
        return new VillagerState(level, xp, unspentUpgradePoints, Map.copyOf(newRanks), stock, productivity, happiness, workDay, stations);
    }

    public VillagerState withStock(Map<Identifier, Integer> newStock) {
        return new VillagerState(level, xp, unspentUpgradePoints, ranks, Map.copyOf(newStock), productivity, happiness, workDay, stations);
    }

    public VillagerState withProductivity(Productivity newProductivity) {
        return new VillagerState(level, xp, unspentUpgradePoints, ranks, stock, newProductivity, happiness, workDay, stations);
    }

    public VillagerState withHappiness(Happiness newHappiness) {
        return new VillagerState(level, xp, unspentUpgradePoints, ranks, stock, productivity, newHappiness, workDay, stations);
    }

    public VillagerState withWorkDay(long day) {
        return new VillagerState(level, xp, unspentUpgradePoints, ranks, stock, productivity, happiness, day, stations);
    }

    public VillagerState withStations(Stations newStations) {
        return new VillagerState(level, xp, unspentUpgradePoints, ranks, stock, productivity, happiness, workDay, newStations);
    }

    private static final Codec<Map<Identifier, Integer>> ID_TO_INT = Codec.unboundedMap(Identifier.CODEC, Codec.INT);
    private static final StreamCodec<RegistryFriendlyByteBuf, Map<Identifier, Integer>> ID_TO_INT_STREAM =
            ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, ByteBufCodecs.VAR_INT);

    public static final MapCodec<VillagerState> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("level").forGetter(VillagerState::level),
            Codec.INT.fieldOf("xp").forGetter(VillagerState::xp),
            Codec.INT.fieldOf("unspent_upgrade_points").forGetter(VillagerState::unspentUpgradePoints),
            ID_TO_INT.fieldOf("ranks").forGetter(VillagerState::ranks),
            ID_TO_INT.fieldOf("stock").forGetter(VillagerState::stock),
            Productivity.CODEC.fieldOf("productivity").forGetter(VillagerState::productivity),
            Happiness.CODEC.fieldOf("happiness").forGetter(VillagerState::happiness),
            Codec.LONG.fieldOf("work_day").forGetter(VillagerState::workDay),
            Stations.CODEC.fieldOf("stations").forGetter(VillagerState::stations)
    ).apply(instance, VillagerState::new));

    public static final Codec<VillagerState> CODEC = MAP_CODEC.codec();

    public static final StreamCodec<RegistryFriendlyByteBuf, VillagerState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VillagerState::level,
            ByteBufCodecs.VAR_INT, VillagerState::xp,
            ByteBufCodecs.VAR_INT, VillagerState::unspentUpgradePoints,
            ID_TO_INT_STREAM, VillagerState::ranks,
            ID_TO_INT_STREAM, VillagerState::stock,
            Productivity.STREAM_CODEC, VillagerState::productivity,
            Happiness.STREAM_CODEC, VillagerState::happiness,
            ByteBufCodecs.VAR_LONG, VillagerState::workDay,
            Stations.STREAM_CODEC, VillagerState::stations,
            VillagerState::new
    );
}
