package com.villageroverhaul.core.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.villageroverhaul.data.ItemExchange;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The productivity meters per category, in work points (README.md,
 * reworked 2026-09-23). A meter fills while the villager works on its category; when full, the
 * category's trades refill a step and the meter drops back to 0 - or it holds at full while the
 * category has nothing missing. Meters never reset by day.
 *
 * focus is the workplace the villager currently works at (NO_FOCUS or a StationSlot ordinal), see
 * RestockService. refillRemainders keeps, per trade, the fraction of a use (in 1/100) that a refill
 * step could not hand out as a whole use yet, so rounding never loses stock. releaseAtTick holds, per
 * category (Tier serialized name), when a meter held at full is released after a trade used its stock.
 * The passive meter fills at the passive station and is spent by the passive ability (passive/BookUpgradeWork).
 */
public record DailyProductivity(int passive, int basic, int master, int focus, Map<Identifier, Integer> refillRemainders,
                                Map<String, Long> releaseAtTick) {

    public static final int NO_FOCUS = -1;
    public static final DailyProductivity EMPTY = new DailyProductivity(0, 0, 0, NO_FOCUS, Map.of(), Map.of());

    public int of(ItemExchange.Tier tier) {
        return tier == ItemExchange.Tier.MASTER ? master : basic;
    }

    public DailyProductivity with(ItemExchange.Tier tier, int points) {
        return tier == ItemExchange.Tier.MASTER
                ? new DailyProductivity(passive, basic, points, focus, refillRemainders, releaseAtTick)
                : new DailyProductivity(passive, points, master, focus, refillRemainders, releaseAtTick);
    }

    public int of(StationSlot slot) {
        return slot == StationSlot.PASSIVE ? passive : of(slot.tier());
    }

    public DailyProductivity with(StationSlot slot, int points) {
        return slot == StationSlot.PASSIVE
                ? new DailyProductivity(points, basic, master, focus, refillRemainders, releaseAtTick)
                : with(slot.tier(), points);
    }

    /** The workplace the villager currently works at; empty in the morning, before it chose (see StationFocus). */
    public Optional<StationSlot> focusSlot() {
        return focus < 0 || focus >= StationSlot.values().length ? Optional.empty() : Optional.of(StationSlot.values()[focus]);
    }

    public DailyProductivity withFocus(Optional<StationSlot> slot) {
        return new DailyProductivity(passive, basic, master, slot.map(Enum::ordinal).orElse(NO_FOCUS), refillRemainders, releaseAtTick);
    }

    public DailyProductivity withRefillRemainders(Map<Identifier, Integer> remainders) {
        return new DailyProductivity(passive, basic, master, focus, remainders, releaseAtTick);
    }

    public DailyProductivity withReleaseAtTick(Map<String, Long> releaseAt) {
        return new DailyProductivity(passive, basic, master, focus, refillRemainders, releaseAt);
    }

    public static final Codec<DailyProductivity> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("passive").forGetter(DailyProductivity::passive),
            Codec.INT.fieldOf("basic").forGetter(DailyProductivity::basic),
            Codec.INT.fieldOf("master").forGetter(DailyProductivity::master),
            Codec.INT.optionalFieldOf("focus", NO_FOCUS).forGetter(DailyProductivity::focus),
            Codec.unboundedMap(Identifier.CODEC, Codec.INT).optionalFieldOf("refill_remainders", Map.of()).forGetter(DailyProductivity::refillRemainders),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("release_at_tick", Map.of()).forGetter(DailyProductivity::releaseAtTick)
    ).apply(instance, DailyProductivity::new));

    public static final StreamCodec<ByteBuf, DailyProductivity> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DailyProductivity::passive,
            ByteBufCodecs.VAR_INT, DailyProductivity::basic,
            ByteBufCodecs.VAR_INT, DailyProductivity::master,
            ByteBufCodecs.VAR_INT, DailyProductivity::focus,
            ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, ByteBufCodecs.VAR_INT), DailyProductivity::refillRemainders,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_LONG), DailyProductivity::releaseAtTick,
            DailyProductivity::new
    );
}
