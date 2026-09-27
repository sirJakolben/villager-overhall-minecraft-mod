package com.villageroverhaul.core;

/** One of a villager's simultaneously active quest slots and its current pool pick - see QuestProvider. */
public record QuestOffer(int slot, ResolvedExchange exchange) {
}
