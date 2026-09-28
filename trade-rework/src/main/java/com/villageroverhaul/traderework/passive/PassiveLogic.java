package com.villageroverhaul.traderework.passive;

import com.villageroverhaul.api.SectionDefinition;
import com.villageroverhaul.api.SectionLogic;
import com.villageroverhaul.state.VillagerState;
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

    @Override
    public int meterPoints(Villager villager, VillagerState state, SectionDefinition section) {
        int steps = work(villager).map(work -> work.stepsPerDay(state.rank(section.id()))).orElse(NO_WORK_STEPS_PER_DAY);
        return RestockService.FULL_DAY_POINTS / steps;
    }

    @Override
    public boolean isPending(ServerLevel level, Villager villager, VillagerState state, SectionDefinition section) {
        return work(villager).map(work -> work.isPending(level, villager, state)).orElse(false);
    }

    @Override
    public VillagerState onWorkScan(ServerLevel level, Villager villager, VillagerState state, SectionDefinition section, boolean workTime) {
        return work(villager).map(work -> work.apply(level, villager, state, workTime)).orElse(state);
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

    /** After a step: the meter starts over. */
    public static VillagerState emptyMeter(VillagerState state) {
        return state.withProductivity(state.productivity().withMeter(TradeReworkSections.PASSIVE.id(), 0));
    }
}
