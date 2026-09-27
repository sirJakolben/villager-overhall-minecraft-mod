package com.villageroverhaul.passive;

import com.villageroverhaul.claim.StationFocus;
import com.villageroverhaul.core.state.VillagerState;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

/**
 * Which passive a villager works on, by profession - the work scan (freedom/VillagerWorkScan) and the
 * meter sizing (RestockService.meterPoints) only talk to this class. Librarian: book upgrades
 * (BookUpgradeWork), Mason: crushing (CrushingWork), Runesmith (Vanilla toolsmith): repairing
 * (RepairWork), Salvager (Vanilla armorer): smelting (SmeltingWork). Other professions have no passive yet: never
 * pending, nothing happens.
 */
public final class PassiveWork {

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
        Holder<VillagerProfession> profession = villager.getVillagerData().profession();
        if (profession.is(VillagerProfession.LIBRARIAN)) {
            return BookUpgradeWork.isPending(level, villager, state);
        }
        if (profession.is(VillagerProfession.MASON)) {
            return CrushingWork.isPending(level, villager, state);
        }
        if (profession.is(VillagerProfession.TOOLSMITH)) {
            return RepairWork.isPending(level, villager, state);
        }
        if (profession.is(VillagerProfession.ARMORER)) {
            return SmeltingWork.isPending(level, villager, state);
        }
        return false;
    }

    /** Runs on every work scan, also outside work time (workTime false) - see BookUpgradeWork.apply. */
    public static VillagerState apply(ServerLevel level, Villager villager, VillagerState state, boolean workTime) {
        Holder<VillagerProfession> profession = villager.getVillagerData().profession();
        if (profession.is(VillagerProfession.LIBRARIAN)) {
            return BookUpgradeWork.apply(level, villager, state, workTime);
        }
        if (profession.is(VillagerProfession.MASON)) {
            return CrushingWork.apply(level, villager, state, workTime);
        }
        if (profession.is(VillagerProfession.TOOLSMITH)) {
            return RepairWork.apply(level, villager, state, workTime);
        }
        if (profession.is(VillagerProfession.ARMORER)) {
            return SmeltingWork.apply(level, villager, state, workTime);
        }
        return state;
    }

    /**
     * How many passive steps a full, 100 % happy work day brings at this passive rank - sizes the passive
     * meter. Professions without a passive use the Librarian's table (their meter just fills and holds).
     */
    public static int stepsPerDay(Villager villager, int passiveRank) {
        if (villager.getVillagerData().profession().is(VillagerProfession.MASON)) {
            return CrushingWork.batchesPerDay(passiveRank);
        }
        if (villager.getVillagerData().profession().is(VillagerProfession.TOOLSMITH)) {
            return RepairWork.stepsPerDay(passiveRank);
        }
        if (villager.getVillagerData().profession().is(VillagerProfession.ARMORER)) {
            return SmeltingWork.stepsPerDay(passiveRank);
        }
        return BookUpgradeWork.upgradesPerDay(passiveRank);
    }
}
