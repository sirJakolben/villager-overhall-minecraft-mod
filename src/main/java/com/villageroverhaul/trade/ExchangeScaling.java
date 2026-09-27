package com.villageroverhaul.trade;

import com.villageroverhaul.data.ItemAmount;
import com.villageroverhaul.data.ItemExchange;
import com.villageroverhaul.progression.UpgradeGroup;

/**
 * Base-to-Max per rank, rounded to whole numbers - see README.md.
 * Reworked 2026-09-24: up to the group's fullUnlockRank everything stays at Base (those ranks unlock
 * things instead), then values move linearly to Max at the group's maxRank. A cheaper price shows up
 * as a Vanilla-style discount (struck-through Base price), see VillagerOffers.toOffer.
 */
public final class ExchangeScaling {

    private ExchangeScaling() {
    }

    public static ItemAmount scaleInput(ItemExchange exchange, int rank, UpgradeGroup group) {
        return scaleAmount(exchange.baseInput(), exchange.maxInput(), rank, group);
    }

    public static ItemAmount scaleOutput(ItemExchange exchange, int rank, UpgradeGroup group) {
        return scaleAmount(exchange.baseOutput(), exchange.maxOutput(), rank, group);
    }

    public static int scaleMaxUses(ItemExchange exchange, int rank, UpgradeGroup group) {
        return scale(exchange.baseMaxUses(), exchange.maxMaxUses(), rank, group);
    }

    /** Base up to the group's fullUnlockRank, then linear to Max at its maxRank. */
    public static int scale(int base, int max, int rank, UpgradeGroup group) {
        int start = group.fullUnlockRank();
        return lerp(base, max, rank - start, group.maxRank() - start);
    }

    private static ItemAmount scaleAmount(ItemAmount base, ItemAmount max, int rank, UpgradeGroup group) {
        return new ItemAmount(base.item(), scale(base.count(), max.count(), rank, group), base.enchantment(), base.mob());
    }

    public static int lerp(int base, int max, int rank, int maxRank) {
        if (maxRank <= 0) {
            return base;
        }
        int clampedRank = Math.min(Math.max(rank, 0), maxRank);
        return Math.round(base + (max - base) * (clampedRank / (float) maxRank));
    }
}
