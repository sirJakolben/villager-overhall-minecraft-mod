package com.villageroverhaul.claim;

import com.villageroverhaul.core.VillagerStateAccess;
import com.villageroverhaul.core.state.StationSlot;
import com.villageroverhaul.core.state.Stations;
import com.villageroverhaul.core.state.VillagerState;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.Arrays;
import java.util.Optional;

/**
 * Block W: which of its workplaces the villager works at right now.
 * - Morning (no focus yet, see RestockService.startDay): its master station, else basic (lectern), else
 *   passive - the StationSlot order. There it chooses its focus (RestockService.chooseFocus).
 * - Afterwards: the workplace of its focus.
 * If that is Vanilla's job site, Vanilla's own work behaviors handle it; any other workplace is an
 * "away" station, handled by WorkAtStation while the lectern-bound Vanilla behaviors pause.
 */
public final class StationFocus {

    private StationFocus() {
    }

    public static Optional<GlobalPos> workStation(Villager villager, VillagerState state) {
        Stations stations = state.stations();
        Optional<StationSlot> slot = state.dailyProductivity().focusSlot().filter(focus -> stations.get(focus).isPresent());
        if (slot.isEmpty()) {
            slot = Arrays.stream(StationSlot.values()).filter(morning -> stations.get(morning).isPresent()).findFirst();
        }
        return slot.flatMap(stations::get).filter(pos -> pos.dimension() == villager.level().dimension());
    }

    public static Optional<GlobalPos> awayStation(Villager villager) {
        return awayStation(villager, VillagerStateAccess.of(villager).getState());
    }

    /** The workplace it works at, unless that is Vanilla's job site (then Vanilla's work behaviors run as usual). */
    public static Optional<GlobalPos> awayStation(Villager villager, VillagerState state) {
        Optional<GlobalPos> jobSite = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
        return workStation(villager, state).filter(pos -> !Optional.of(pos).equals(jobSite));
    }
}
