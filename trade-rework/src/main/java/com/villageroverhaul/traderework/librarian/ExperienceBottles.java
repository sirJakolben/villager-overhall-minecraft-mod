package com.villageroverhaul.traderework.librarian;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jspecify.annotations.Nullable;

/**
 * XP amounts and costs around the two experience bottles (Obsidian: Librarian.md, "XP abfüllen" and
 * "Villager-XP-Flasche").
 *
 * A Vanilla Bottle o' Enchanting holds a fixed XP_POINTS - exactly the points a player needs from
 * level 0 to level 5, returned 1:1 on impact no matter the thrower's level, replacing Vanilla's random
 * 3-11 (see ThrownExperienceBottleMixin). Vanilla's per-level cost below level 16 is 2 * level + 7,
 * so 7 + 9 + 11 + 13 + 15 = 55. Any trade that hands out Bottles o' Enchanting takes exactly that
 * much XP from the player per bottle, so bottling is lossless.
 *
 * A Villager Experience Bottle holds a fixed VILLAGER_BOTTLE_XP (2026-09-24): for an untrained villager that is
 * five levels (a full point buffer) plus a full XP bar - the point cap (ProgressionService.withXp) drops the rest.
 */
public final class ExperienceBottles {

    public static final int XP_POINTS = 55;
    private static final int VILLAGER_BOTTLE_XP = 2500;

    private ExperienceBottles() {
    }

    public static int villagerBottleXp() {
        return VILLAGER_BOTTLE_XP;
    }

    /** Player XP a trade costs on top of its items: XP_POINTS per Bottle o' Enchanting it gives out, 0 otherwise. */
    public static int playerXpCost(@Nullable MerchantOffer offer) {
        if (offer == null || !offer.getResult().is(Items.EXPERIENCE_BOTTLE)) {
            return 0;
        }
        return offer.getResult().getCount() * XP_POINTS;
    }

}
