package com.villageroverhaul.trade;

import com.villageroverhaul.api.SectionDefinition;
import com.villageroverhaul.data.ItemAmount;
import com.villageroverhaul.data.ItemExchange;

/**
 * Base-to-Max per rank, rounded to whole numbers: up to the section's fullUnlockRank everything stays at Base
 * (those ranks unlock entries instead), then values move linearly to Max at the section's max rank. A cheaper
 * price shows up as a Vanilla-style discount (struck-through Base price), see VillagerOffers.toOffer.
 */
public final class ExchangeScaling {

    private ExchangeScaling() {
    }

    public static ItemAmount scaleInput(ItemExchange exchange, int rank, SectionDefinition section) {
        return scaleAmount(exchange.baseInput(), exchange.maxInput(), rank, section);
    }

    public static ItemAmount scaleOutput(ItemExchange exchange, int rank, SectionDefinition section) {
        return scaleAmount(exchange.baseOutput(), exchange.maxOutput(), rank, section);
    }

    public static int scaleMaxUses(ItemExchange exchange, int rank, SectionDefinition section) {
        return scale(exchange.baseMaxUses(), exchange.maxMaxUses(), rank, section);
    }

    /** Base up to the section's fullUnlockRank, then linear to Max at its max rank. */
    public static int scale(int base, int max, int rank, SectionDefinition section) {
        int start = section.fullUnlockRank();
        return lerp(base, max, rank - start, section.maxRank() - start);
    }

    private static ItemAmount scaleAmount(ItemAmount base, ItemAmount max, int rank, SectionDefinition section) {
        return new ItemAmount(base.item(), scale(base.count(), max.count(), rank, section), base.enchantment(), base.mob());
    }

    public static int lerp(int base, int max, int rank, int maxRank) {
        if (maxRank <= 0) {
            return base;
        }
        int clampedRank = Math.min(Math.max(rank, 0), maxRank);
        return Math.round(base + (max - base) * (clampedRank / (float) maxRank));
    }
}
