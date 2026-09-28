package com.villageroverhaul.vanilla;

import com.villageroverhaul.state.VillagerState;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Rank caps of a profession running on its Vanilla trades (VanillaCatalog): each rank unlocks the section's next
 * entry, rank 0 already the first, and after the last one VanillaScaling.EXTRA_RANKS more ranks improve prices
 * and stock. A section without entries can't be upgraded (cap 0).
 *
 * Upgrade cost: one shared price line over everything the villager can buy - every rank of every section,
 * unlocks and price steps together: the first upgrade costs FIRST_COST, the very last LAST_COST, linear in
 * between (rounded), and every upgrade in any section moves one step on - so even a profession with many trades
 * can be fully unlocked in reasonable time.
 */
public record VanillaRankCaps(Map<Identifier, Integer> caps) {

    /** Cost of the first and the very last upgrade (Tweak-Werte.md). */
    public static final int FIRST_COST = 1;
    public static final int LAST_COST = 5;

    public static VanillaRankCaps of(VanillaCatalog.Catalog catalog) {
        Map<Identifier, Integer> caps = new HashMap<>();
        catalog.sections().forEach((section, entries) ->
                caps.put(section, entries.isEmpty() ? 0 : catalog.lastUnlockRank(section) + VanillaScaling.EXTRA_RANKS));
        return new VanillaRankCaps(Map.copyOf(caps));
    }

    /** Points the section's next upgrade costs, or -1 once it is at its cap. */
    public int upgradeCost(Identifier section, VillagerState state) {
        int rank = state.rank(section);
        if (rank < 0 || rank >= caps.getOrDefault(section, 0)) {
            return -1;
        }
        int total = caps.values().stream().mapToInt(Integer::intValue).sum();
        int done = caps.keySet().stream().mapToInt(state::rank).sum();
        return total <= 1 ? LAST_COST : (int) Math.round(FIRST_COST + (LAST_COST - FIRST_COST) * done / (double) (total - 1));
    }
}
