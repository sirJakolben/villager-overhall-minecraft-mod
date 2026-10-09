package com.villageroverhaul.traderework.passive;

import com.villageroverhaul.api.SectionDefinition;
import com.villageroverhaul.api.SectionLogic;
import com.villageroverhaul.state.VillagerState;
import com.villageroverhaul.state.VillagerStateAccess;
import com.villageroverhaul.station.StationFocus;
import com.villageroverhaul.traderework.TradeReworkSections;
import com.villageroverhaul.work.RestockService;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

import java.util.Map;
import java.util.Optional;

/**
 * The Passive section's logic: no rows, no restock - its meter fills while the villager works at its passive
 * station and holds at full until the profession's PassiveWork spends it on a step. The meter is sized so a
 * full, happy work day fills it exactly stepsPerDay times (a Librarian's 1 book upgrade on rank 0 means 6000
 * points, a Mason's 8 batches 750).
 */
public record PassiveLogic(Map<ResourceKey<VillagerProfession>, PassiveWork> works) implements SectionLogic {

    /** Steps per day for a profession without a passive - only sizes a meter nobody reads. */
    private static final int NO_WORK_STEPS_PER_DAY = 1;

    /** Points of one carried unit on a carrying passive's bar. */
    public static final int UNIT_POINTS = 100;

    @Override
    public int meterPoints(Villager villager, VillagerState state, SectionDefinition section) {
        int capacity = work(villager).map(work -> work.capacity(state.rank(section.id()))).orElse(0);
        if (capacity > 0) {
            return capacity * UNIT_POINTS;
        }
        int steps = work(villager).map(work -> work.stepsPerDay(state.rank(section.id()))).orElse(NO_WORK_STEPS_PER_DAY);
        return RestockService.FULL_DAY_POINTS / steps;
    }

    @Override
    public boolean isPending(ServerLevel level, Villager villager, VillagerState state, SectionDefinition section) {
        return work(villager).map(work -> work.isPending(level, villager, state)).orElse(false);
    }

    @Override
    public VillagerState onWorkScan(ServerLevel level, Villager villager, VillagerState state, SectionDefinition section, boolean workTime) {
        Optional<PassiveWork> work = work(villager);
        VillagerState updated = work.map(w -> w.apply(level, villager, state, workTime)).orElse(state);
        return work.isPresent() && work.get().capacity(rank(updated)) > 0 ? withLoadMeter(villager, updated) : updated;
    }

    /** A carrying passive keeps its bar itself (its load); working at the station does not fill it, and the focus does not pick it by its bar. */
    @Override
    public boolean fillsFromWork(Villager villager, VillagerState state, SectionDefinition section) {
        return work(villager).map(work -> work.capacity(state.rank(section.id())) <= 0).orElse(true);
    }

    @Override
    public Optional<GlobalPos> workPlace(Villager villager, VillagerState state, SectionDefinition section) {
        return work(villager).flatMap(work -> work.workPlace(villager, state));
    }

    /** /vo reset: the baskets' counters start over with everything else. */
    @Override
    public void onReset(Villager villager, SectionDefinition section) {
        PassiveCounters.set(villager, PassiveCounters.EMPTY);
    }

    private Optional<PassiveWork> work(Villager villager) {
        return villager.getVillagerData().profession().unwrapKey().map(works::get);
    }

    // Shared by every PassiveWork.

    public static int rank(VillagerState state) {
        return state.rank(TradeReworkSections.PASSIVE.id());
    }

    public static Optional<GlobalPos> station(VillagerState state) {
        return state.stations().get(TradeReworkSections.PASSIVE_STATION);
    }

    public static boolean isMeterFull(Villager villager, VillagerState state) {
        return RestockService.isMeterFull(villager, state, TradeReworkSections.PASSIVE);
    }

    /**
     * The villager works at its passive station right now (its focus, StationFocus.workStation) - passive steps
     * and their sounds happen only then (2026-09-26), not just because the station stands next to the station it
     * actually works at.
     */
    public static boolean worksAtStation(Villager villager, VillagerState state) {
        return StationFocus.worksAt(villager, state, TradeReworkSections.PASSIVE_STATION);
    }

    /** The bar of a carrying passive: the load, in UNIT_POINTS per unit. */
    public static VillagerState withLoadMeter(Villager villager, VillagerState state) {
        int load = Math.min(PassiveCounters.of(villager).total(), PassiveCounters.capacity(rank(state)));
        return state.withProductivity(state.productivity().withMeter(TradeReworkSections.PASSIVE.id(), load * UNIT_POINTS));
    }

    /** Brings the bar up to date right away, outside a work scan (a harvest, a catch). */
    public static void syncLoadMeter(Villager villager) {
        VillagerStateAccess access = VillagerStateAccess.of(villager);
        VillagerState state = access.getState();
        VillagerState updated = withLoadMeter(villager, state);
        if (!updated.equals(state)) {
            access.setState(updated);
        }
    }

    /** After a step: the meter starts over. */
    public static VillagerState emptyMeter(VillagerState state) {
        return state.withProductivity(state.productivity().withMeter(TradeReworkSections.PASSIVE.id(), 0));
    }
}
