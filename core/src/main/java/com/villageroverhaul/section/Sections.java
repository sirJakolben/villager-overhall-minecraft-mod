package com.villageroverhaul.section;

import com.villageroverhaul.api.SectionDefinition;
import com.villageroverhaul.api.StationDefinition;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Every registered section and station (api/ExtensionHooks - the core's own come from CoreSections). Filled from
 * mod constructors, possibly in parallel, and read-only after that; the per-profession views are built once on
 * first use, so no lookup here ever walks a list during gameplay.
 */
public final class Sections {

    private static final Map<Identifier, SectionDefinition> SECTIONS = new ConcurrentHashMap<>();
    private static final List<StationDefinition> STATIONS = new CopyOnWriteArrayList<>();
    private static final Map<ResourceKey<VillagerProfession>, List<StationDefinition>> STATIONS_BY_PROFESSION = new ConcurrentHashMap<>();
    private static volatile List<SectionDefinition> ordered;

    private Sections() {
    }

    public static void register(SectionDefinition section) {
        if (section.display() == SectionDefinition.Display.BADGE
                && SECTIONS.values().stream().anyMatch(other -> other.display() == SectionDefinition.Display.BADGE)) {
            throw new IllegalStateException("Only one BADGE section fits the screen - " + section.id() + " is the second");
        }
        if (SECTIONS.putIfAbsent(section.id(), section) != null) {
            throw new IllegalStateException("Section registered twice: " + section.id());
        }
        ordered = null;
    }

    public static void register(StationDefinition station) {
        STATIONS.add(station);
        STATIONS_BY_PROFESSION.clear();
    }

    public static Optional<SectionDefinition> get(Identifier id) {
        return Optional.ofNullable(SECTIONS.get(id));
    }

    /** All sections, by order (then id). */
    public static List<SectionDefinition> all() {
        List<SectionDefinition> result = ordered;
        if (result == null) {
            result = SECTIONS.values().stream()
                    .sorted(Comparator.comparingInt(SectionDefinition::order).thenComparing(section -> section.id().toString()))
                    .toList();
            ordered = result;
        }
        return result;
    }

    public static List<StationDefinition> stations() {
        return STATIONS;
    }

    /** The stations a profession has besides its Vanilla job site - empty for most professions. */
    public static List<StationDefinition> stationsOf(Holder<VillagerProfession> profession) {
        return profession.unwrapKey()
                .map(key -> STATIONS_BY_PROFESSION.computeIfAbsent(key,
                        k -> STATIONS.stream().filter(station -> station.profession().equals(k)).toList()))
                .orElse(List.of());
    }
}
