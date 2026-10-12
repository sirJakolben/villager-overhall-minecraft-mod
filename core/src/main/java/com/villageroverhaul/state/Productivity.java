package com.villageroverhaul.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The work meters, one per section (keyed by section id), in work points - see RestockService. A meter fills
 * while the villager works at its section's station; what a full meter does is up to the section's logic
 * (a trade section refills its stock and drops back to 0, a passive holds until its next step). Meters never
 * reset by day.
 *
 * focus: the section the villager works on right now - it walks to that section's station; empty in the
 * morning, before it chose (StationFocus). refillRemainders keeps, per entry, the fraction of a use (in 1/100)
 * a refill step could not hand out as a whole use yet, so rounding never loses stock. releaseAtTick holds,
 * per section, when a meter held at full is released after a trade used that section's stock.
 */
public record Productivity(Map<Identifier, Integer> meters, Optional<Identifier> focus, Map<Identifier, Integer> refillRemainders,
                           Map<Identifier, Long> releaseAtTick) {

    public static final Productivity EMPTY = new Productivity(Map.of(), Optional.empty(), Map.of(), Map.of());

    public int meter(Identifier section) {
        return meters.getOrDefault(section, 0);
    }

    public Productivity withMeter(Identifier section, int points) {
        Map<Identifier, Integer> newMeters = new HashMap<>(meters);
        newMeters.put(section, points);
        return new Productivity(Map.copyOf(newMeters), focus, refillRemainders, releaseAtTick);
    }

    public Productivity withFocus(Optional<Identifier> section) {
        return new Productivity(meters, section, refillRemainders, releaseAtTick);
    }

    public Productivity withRefillRemainders(Map<Identifier, Integer> remainders) {
        return new Productivity(meters, focus, Map.copyOf(remainders), releaseAtTick);
    }

    public Productivity withReleaseAtTick(Map<Identifier, Long> releaseAt) {
        return new Productivity(meters, focus, refillRemainders, Map.copyOf(releaseAt));
    }

    public static final Codec<Productivity> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Identifier.CODEC, Codec.INT).fieldOf("meters").forGetter(Productivity::meters),
            Identifier.CODEC.optionalFieldOf("focus").forGetter(Productivity::focus),
            Codec.unboundedMap(Identifier.CODEC, Codec.INT).fieldOf("refill_remainders").forGetter(Productivity::refillRemainders),
            Codec.unboundedMap(Identifier.CODEC, Codec.LONG).fieldOf("release_at_tick").forGetter(Productivity::releaseAtTick)
    ).apply(instance, Productivity::new));

    public static final StreamCodec<ByteBuf, Productivity> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, ByteBufCodecs.VAR_INT), Productivity::meters,
            ByteBufCodecs.optional(Identifier.STREAM_CODEC), Productivity::focus,
            ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, ByteBufCodecs.VAR_INT), Productivity::refillRemainders,
            ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, ByteBufCodecs.VAR_LONG), Productivity::releaseAtTick,
            Productivity::new
    );
}
