package com.villageroverhaul.station;

import com.villageroverhaul.api.SectionDefinition;
import com.villageroverhaul.section.Sections;
import com.villageroverhaul.section.VillagerSections;
import com.villageroverhaul.state.VillagerState;
import com.villageroverhaul.state.VillagerStateAccess;
import com.villageroverhaul.work.RestockService;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Optional;

/**
 * Which section the villager works on, and so at which of its stations it works right now.
 * - Morning (no focus yet, see RestockService.startDay): the owned station with the highest morning priority
 *   (ProfessionStations). There it chooses its focus (choose).
 * - Afterwards: the station of the section it focuses on.
 * If that is Vanilla's job site, Vanilla's own work behaviors handle it; any other station is an "away"
 * station, handled by WorkAtStation while the job-site-bound Vanilla behaviors pause.
 */
public final class StationFocus {

    private StationFocus() {
    }

    /** The section the villager focuses on, while it still owns that section's station. */
    public static Optional<SectionDefinition> focusSection(Villager villager, VillagerState state) {
        return state.productivity().focus()
                .flatMap(Sections::get)
                .filter(section -> VillagerSections.stationOf(state, section).isPresent());
    }

    public static Optional<GlobalPos> workStation(Villager villager, VillagerState state) {
        Optional<GlobalPos> station = focusSection(villager, state).flatMap(section -> VillagerSections.stationOf(state, section));
        if (station.isEmpty()) {
            Holder<VillagerProfession> profession = villager.getVillagerData().profession();
            station = state.stations().positions().entrySet().stream()
                    .max(Comparator.comparingInt((Entry<Identifier, GlobalPos> owned) -> ProfessionStations.morningPriority(profession, owned.getKey())))
                    .map(Entry::getValue);
        }
        return station.filter(pos -> pos.dimension() == villager.level().dimension());
    }

    /** The villager works at this station of its own right now - its focus is there (or it is the morning station). */
    public static boolean worksAt(Villager villager, VillagerState state, Identifier station) {
        Optional<GlobalPos> pos = state.stations().get(station);
        return pos.isPresent() && workStation(villager, state).equals(pos);
    }

    public static Optional<GlobalPos> awayStation(Villager villager) {
        return awayStation(villager, VillagerStateAccess.of(villager).getState());
    }

    /** The station it works at, unless that is Vanilla's job site (then Vanilla's work behaviors run as usual). */
    public static Optional<GlobalPos> awayStation(Villager villager, VillagerState state) {
        Optional<GlobalPos> jobSite = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
        return workStation(villager, state).filter(pos -> !Optional.of(pos).equals(jobSite));
    }

    /**
     * Where to work (decided 2026-09-24), only among workable sections - a section without its station can't be
     * worked on:
     * 0. Work is waiting at a section's station (SectionLogic.isPending - a passive's full meter and a book to
     *    upgrade): that section first, since that work happens only standing right at the station.
     * 1. Real shortage: the section that misses the most stock, until it is restocked or its meter holds full.
     * 2. Otherwise meters that aren't full: the emptiest first, and it stays there until that meter is full.
     * 3. Otherwise everything is full: it idles (and still levels) at the section it has the highest rank in.
     * Ties go by morning priority of the stations, then section order. A real shortage always interrupts a
     * meter pre-fill.
     */
    public static Optional<SectionDefinition> choose(ServerLevel level, Villager villager, VillagerState state, List<SectionDefinition> workable) {
        Holder<VillagerProfession> profession = villager.getVillagerData().profession();
        List<SectionDefinition> candidates = workable.stream()
                .sorted(Comparator.comparingInt((SectionDefinition section) -> -ProfessionStations.morningPriority(profession, section.station().orElseThrow()))
                        .thenComparingInt(SectionDefinition::order))
                .toList();
        for (SectionDefinition section : candidates) {
            if (section.logic().isPending(level, villager, state, section)) {
                return Optional.of(section);
            }
        }

        Map<SectionDefinition, Integer> missing = new HashMap<>();
        SectionDefinition shortage = null;
        for (SectionDefinition section : candidates) {
            int gap = section.logic().missingStock(villager, state, section);
            missing.put(section, gap);
            if (gap > 0 && !RestockService.isMeterFull(villager, state, section) && (shortage == null || gap > missing.get(shortage))) {
                shortage = section;
            }
        }
        Optional<SectionDefinition> current = focusSection(villager, state).filter(candidates::contains);
        if (current.isPresent() && !RestockService.isMeterFull(villager, state, current.get())
                && (missing.get(current.get()) > 0 || shortage == null)) {
            return current;
        }
        if (shortage != null) {
            return Optional.of(shortage);
        }

        Optional<SectionDefinition> emptiest = candidates.stream()
                .filter(section -> !RestockService.isMeterFull(villager, state, section))
                .min(Comparator.comparingInt(section -> state.productivity().meter(section.id())));
        if (emptiest.isPresent()) {
            return emptiest;
        }
        return candidates.stream().max(Comparator.comparingInt(section -> state.rank(section.id())));
    }
}
