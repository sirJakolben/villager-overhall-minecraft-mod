package com.smartvillager.base.api;

import net.minecraft.world.entity.npc.villager.Villager;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Where the mods above Smart Villager plug in. Register from a mod constructor; the mod declares ordering AFTER Smart Villager.
 *
 * - Stay put: a reason for a villager to stand still right now. Vanilla's place-bound behaviors (walk back to the job site, stroll around
 *   it, meet at the bell) pause while one holds, just as for a Trading Block call. Two kinds: always, and only during work.
 * - Claim handler: see ClaimHandler.
 */
public final class SmartVillagerApi {

    private static final List<Predicate<Villager>> STAY_PUT = new CopyOnWriteArrayList<>();
    private static final List<Predicate<Villager>> STAY_PUT_WHILE_WORKING = new CopyOnWriteArrayList<>();
    private static final List<ClaimHandler> CLAIM_HANDLERS = new CopyOnWriteArrayList<>();

    private SmartVillagerApi() {
    }

    /** A reason to stand still at any time of day (the Fisherman mid-cast). */
    public static void registerStayPut(Predicate<Villager> reason) {
        STAY_PUT.add(reason);
    }

    /** A reason to leave the job site's own behaviors alone, but only while working - not at the bell (working at another station). */
    public static void registerStayPutWhileWorking(Predicate<Villager> reason) {
        STAY_PUT_WHILE_WORKING.add(reason);
    }

    public static void registerClaimHandler(ClaimHandler handler) {
        CLAIM_HANDLERS.add(handler);
    }

    public static boolean isStayingPut(Villager villager) {
        for (Predicate<Villager> reason : STAY_PUT) {
            if (reason.test(villager)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isStayingPutWhileWorking(Villager villager) {
        if (isStayingPut(villager)) {
            return true;
        }
        for (Predicate<Villager> reason : STAY_PUT_WHILE_WORKING) {
            if (reason.test(villager)) {
                return true;
            }
        }
        return false;
    }

    public static List<ClaimHandler> claimHandlers() {
        return CLAIM_HANDLERS;
    }
}
