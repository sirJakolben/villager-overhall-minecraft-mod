package com.villageroverhaul.api;

import com.villageroverhaul.trade.ResolvedExchange;

/**
 * One row of a section: the exchange it offers, scaled and with its stock, and a slot only the section's own
 * logic interprets (the Trade Rework's quest slot; NO_SLOT for a plain trade). The slot travels to the client
 * with the row (network/VillagerOffersPayload) and comes back with row actions (network/SectionActionPayload).
 */
public record SectionOffer(ResolvedExchange exchange, int slot) {

    public static final int NO_SLOT = -1;

    public static SectionOffer of(ResolvedExchange exchange) {
        return new SectionOffer(exchange, NO_SLOT);
    }
}
