package com.smartvillager.progress.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The villager's workplaces by station id (api/StationDefinition; the Vanilla job site under
 * StationDefinition.JOB_SITE), each a reserved point of interest - see station/StationClaims. Vanilla's JOB_SITE
 * memory always points at one of them and is mirrored in here, because the brain isn't synced to the client.
 * Several sections may share a station; it is stored once.
 *
 * heldByStation: the villager's profession is being kept by a backup station - with two or more
 * stations and no experience yet, Vanilla would otherwise fire it the moment its job-site block breaks.
 */
public record Stations(Map<Identifier, GlobalPos> positions, boolean heldByStation) {

    public static final Stations NONE = new Stations(Map.of(), false);

    public Optional<GlobalPos> get(Identifier station) {
        return Optional.ofNullable(positions.get(station));
    }

    public Stations with(Identifier station, Optional<GlobalPos> pos) {
        Map<Identifier, GlobalPos> newPositions = new HashMap<>(positions);
        pos.ifPresentOrElse(p -> newPositions.put(station, p), () -> newPositions.remove(station));
        return new Stations(Map.copyOf(newPositions), heldByStation);
    }

    public Stations withHeldByStation(boolean held) {
        return new Stations(positions, held);
    }


    public int count() {
        return positions.size();
    }

    public static final Codec<Stations> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Identifier.CODEC, GlobalPos.CODEC).fieldOf("positions").forGetter(Stations::positions),
            Codec.BOOL.fieldOf("held_by_station").forGetter(Stations::heldByStation)
    ).apply(instance, Stations::new));

    public static final StreamCodec<ByteBuf, Stations> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, GlobalPos.STREAM_CODEC), Stations::positions,
            ByteBufCodecs.BOOL, Stations::heldByStation,
            Stations::new
    );
}
