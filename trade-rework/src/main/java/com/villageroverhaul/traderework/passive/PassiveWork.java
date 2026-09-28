package com.villageroverhaul.traderework.passive;

import com.villageroverhaul.state.VillagerState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;

/**
 * One profession's passive - the work at its passive station (Librarian: book upgrades, Mason: crushing,
 * Runesmith: repairing, Salvager: smelting). PassiveLogic calls the one of the villager's profession.
 */
public interface PassiveWork {

    /** A step is due - full meter and work waiting in the station; the villager walks there first. */
    boolean isPending(ServerLevel level, Villager villager, VillagerState state);

    /** Every work scan, also outside work time (workTime false): a step when due, station bookkeeping always. */
    VillagerState apply(ServerLevel level, Villager villager, VillagerState state, boolean workTime);

    /** How many steps a full, 100 % happy work day brings at this passive rank - sizes the meter. */
    int stepsPerDay(int rank);
}
