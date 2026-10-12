package com.villageroverhaul.traderework.cavemap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.villageroverhaul.traderework.TradeReworkMod;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.saveddata.maps.MapId;

import java.util.Arrays;
import java.util.BitSet;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * What a cave map remembers about its pixels beyond the colour (2026-10-11), saved with the world next to the map itself (one file per map, like the Vanilla
 * map data) so a restart or a reload changes nothing:
 * - floors: per pixel the Y of the floor block it shows (NO_FLOOR = not coloured yet) - the rule which ray may redraw it (CaveMapDrawer)
 * - structure: the pixels that show a structure block and are locked
 */
public final class CaveMapMemory extends SavedData {

    public static final int SIZE = 128 * 128;
    public static final int NO_FLOOR = Integer.MAX_VALUE;

    public final int[] floors;
    public final BitSet structure;

    public CaveMapMemory() {
        floors = new int[SIZE];
        Arrays.fill(floors, NO_FLOOR);
        structure = new BitSet(SIZE);
    }

    private CaveMapMemory(IntStream savedFloors, LongStream savedStructure) {
        this();
        int[] values = savedFloors.toArray();
        System.arraycopy(values, 0, floors, 0, Math.min(values.length, SIZE));
        structure.or(BitSet.valueOf(savedStructure.toArray()));
    }

    public static final Codec<CaveMapMemory> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT_STREAM.fieldOf("floors").forGetter(memory -> IntStream.of(memory.floors)),
            Codec.LONG_STREAM.fieldOf("structure").forGetter(memory -> LongStream.of(memory.structure.toLongArray()))
    ).apply(instance, CaveMapMemory::new));

    private static SavedDataType<CaveMapMemory> type(MapId id) {
        return new SavedDataType<>(Identifier.fromNamespaceAndPath(TradeReworkMod.MODID, "cave_map_" + id.key()), CaveMapMemory::new, CODEC);
    }

    /** The memory of this map - loaded from the world's data, or empty for a map seen for the first time. Call setDirty after a change. */
    public static CaveMapMemory of(ServerLevel level, MapId id) {
        return level.getServer().getDataStorage().computeIfAbsent(type(id));
    }

    /** Stores a memory built elsewhere (a zoomed map) as that map's. */
    public static void put(ServerLevel level, MapId id, CaveMapMemory memory) {
        level.getServer().getDataStorage().set(type(id), memory);
        memory.setDirty();
    }
}
