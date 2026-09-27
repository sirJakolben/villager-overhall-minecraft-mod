package com.villageroverhaul.passive;

import com.villageroverhaul.api.ExtensionHooks;
import com.villageroverhaul.claim.StationFocus;
import com.villageroverhaul.core.state.VillagerState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;

/**
 * Which passive a villager works on, by profession - the work scan (freedom/VillagerWorkScan) and the
 * meter sizing (RestockService.meterPoints) only talk to this class. The passives themselves come from an
 * extension (api/ExtensionHooks.registerPassive - Trade Rework: Librarian book upgrades, Mason crushing,
 * Runesmith repairing, Salvager smelting). A profession without one is never pending and nothing happens.
 */
public final class PassiveWork {

    /**
     * Steps per day for a profession without a passive - only sizes a meter that just fills and holds (the
     * non-implementation rule hides the passive group anyway). Equal to the Librarian's rank-0 value.
     */
    private static final int NO_PASSIVE_STEPS_PER_DAY = 1;

    private PassiveWork() {
    }

    /**
     * The villager works at its passive station right now (its focus, StationFocus.workStation) - passive
     * steps and their sounds happen only then (2026-09-26), not just because the station stands next to
     * the workplace it actually works at.
     */
    public static boolean worksAtPassiveStation(Villager villager, VillagerState state) {
        return state.stations().passive().isPresent() && StationFocus.workStation(villager, state).equals(state.stations().passive());
    }

    /** A passive step is due - the focus goes to the passive station (RestockService.chooseFocus). */
    public static boolean isPending(ServerLevel level, Villager villager, VillagerState state) {
        return ExtensionHooks.passive(villager.getVillagerData().profession())
                .map(passive -> passive.isPending(level, villager, state))
                .orElse(false);
    }

    /** Runs on every work scan, also outside work time (workTime false). */
    public static VillagerState apply(ServerLevel level, Villager villager, VillagerState state, boolean workTime) {
        return ExtensionHooks.passive(villager.getVillagerData().profession())
                .map(passive -> passive.apply(level, villager, state, workTime))
                .orElse(state);
    }

    /** How many passive steps a full, 100 % happy work day brings at this passive rank - sizes the passive meter. */
    public static int stepsPerDay(Villager villager, int passiveRank) {
        return ExtensionHooks.passive(villager.getVillagerData().profession())
                .map(passive -> passive.stepsPerDay(passiveRank))
                .orElse(NO_PASSIVE_STEPS_PER_DAY);
    }
}
