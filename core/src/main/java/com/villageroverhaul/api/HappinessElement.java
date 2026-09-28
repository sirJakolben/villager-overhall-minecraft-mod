package com.villageroverhaul.api;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;

/**
 * An extra happiness element an extension adds (2026-09-28, e.g. the Trade Rework's villager statues), counted
 * exactly like the core's bed and meeting point (happiness/HappinessCalculator): observed today it gives its full
 * weight, then falls linearly to 0 % timerDays after the last observation, rounded up to 5 % steps. Register with
 * ExtensionHooks.registerHappinessElement.
 *
 * The villager work scan (work/VillagerWorkScan) asks observer at most once per scan and only until it said yes
 * that day - still, it runs per villager, so keep it cheap (a POI query, not a block scan).
 */
public record HappinessElement(Identifier id, int weight, int timerDays, Observer observer) {

    @FunctionalInterface
    public interface Observer {
        /** Server side, during the villager's scan: is the element there right now? */
        boolean observe(ServerLevel level, Villager villager);
    }
}
