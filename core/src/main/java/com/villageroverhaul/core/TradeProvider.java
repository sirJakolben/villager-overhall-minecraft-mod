package com.villageroverhaul.core;

import net.minecraft.world.entity.npc.villager.Villager;

import java.util.List;

public interface TradeProvider {

    /** All trades currently unlocked for this villager, scaled to its rank. */
    List<ResolvedExchange> getAvailableTrades(Villager villager);
}
