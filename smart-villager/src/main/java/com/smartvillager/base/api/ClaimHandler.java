package com.smartvillager.base.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.stream.Stream;

/**
 * How a mod that gives villagers more than Vanilla's one job site takes part in the emerald claim (claim/ManualClaims). The claim itself
 * always handles Vanilla's job site memory and point-of-interest ticket; a handler adds its own bookkeeping on top (Progression: the
 * villager's stations). Register with SmartVillagerApi.registerClaimHandler.
 */
public interface ClaimHandler {

    /** The villager already holds the workplace at pos in its own records. */
    boolean owns(Villager villager, ServerLevel level, BlockPos pos);

    /**
     * The claim took the workplace at pos from this villager (it is known to have held it): drop it from the handler's records.
     * Returns true if the handler held it there.
     */
    boolean displace(Villager villager, ServerLevel level, BlockPos pos);

    /**
     * The villager takes the workplace at pos. Returns true if the handler did it all (ticket, job site memory, records) - the plain Vanilla
     * take is then skipped.
     */
    boolean take(Villager villager, ServerLevel level, BlockPos pos);

    /** The villager changes profession: let go of every workplace the handler tracks for it. */
    void releaseAll(Villager villager, ServerLevel level);

    /** Workplaces the handler tracks for the villager besides its job site, for the Trading Block's reach check. */
    Stream<net.minecraft.core.GlobalPos> workplaces(Villager villager);
}
