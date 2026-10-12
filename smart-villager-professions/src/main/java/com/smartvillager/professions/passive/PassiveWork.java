package com.smartvillager.professions.passive;

import com.smartvillager.progress.state.VillagerState;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.Optional;

/**
 * One profession's passive - the work at its passive station (Librarian: book upgrades, Mason: crushing,
 * Runesmith: repairing, Goldsmith: smelting). PassiveLogic calls the one of the villager's profession.
 */
public interface PassiveWork {

    /** A step is due - full meter and work waiting in the station; the villager walks there first. */
    boolean isPending(ServerLevel level, Villager villager, VillagerState state);

    /** Every work scan, also outside work time (workTime false): a step when due, station bookkeeping always. */
    VillagerState apply(ServerLevel level, Villager villager, VillagerState state, boolean workTime);

    /** How many steps a full, 100 % happy work day brings at this passive rank - sizes the meter. */
    int stepsPerDay(int rank);

    /** More than 1: a step takes this many full work days (the map drawing) - the meter is that many times bigger. Default: stepsPerDay decides. */
    default int daysPerStep(int passiveRank) {
        return 1;
    }

    /**
     * More than 0: a carrying passive (CarryingWork) - the passive bar then shows how full the villager's load is, out of this many units at this
     * passive rank, instead of filling from work (PassiveLogic).
     */
    default int capacity(int passiveRank) {
        return 0;
    }

    /** A short errand instead of the station (SectionLogic.workPlace) - the Fisherman's walk to the water. */
    default Optional<GlobalPos> workPlace(Villager villager, VillagerState state) {
        return Optional.empty();
    }
}
