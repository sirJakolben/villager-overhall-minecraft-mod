package com.smartvillager.professions.mapping;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;

/**
 * One datapack file in data/smartvillager_professions/smartvillager_professions/map_pool/ (2026-10-10): structures that belong to the adventure or the
 * challenge pool, each entry a structure, a list or a #tag with a weight - a heavier entry is drawn more often. The Cartographer's
 * and the Veteran's passive stations draw the target of their next map from their pool (MapTargets). Any number of files may feed one
 * pool, so other mods ship their own. A structure no file mentions still joins a pool by default (MapTargets.fallbackKind).
 */
public record MapPool(Kind kind, List<Entry> entries) {

    public enum Kind implements StringRepresentable {
        ADVENTURE("adventure"),
        CHALLENGE("challenge");

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);
        private final String id;

        Kind(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    /** The entry is drawn as one unit: its map points to the nearest structure of any in the set. */
    public record Entry(HolderSet<Structure> structures, int weight) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                RegistryCodecs.homogeneousList(Registries.STRUCTURE).fieldOf("structures").forGetter(Entry::structures),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("weight", 1).forGetter(Entry::weight)
        ).apply(instance, Entry::new));
    }

    public static final Codec<MapPool> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Kind.CODEC.fieldOf("pool").forGetter(MapPool::kind),
            Entry.CODEC.listOf().fieldOf("entries").forGetter(MapPool::entries)
    ).apply(instance, MapPool::new));
}
