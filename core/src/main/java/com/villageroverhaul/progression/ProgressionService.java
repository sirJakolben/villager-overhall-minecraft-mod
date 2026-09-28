package com.villageroverhaul.progression;

import com.villageroverhaul.api.SectionDefinition;
import com.villageroverhaul.section.Sections;
import com.villageroverhaul.section.VillagerSections;
import com.villageroverhaul.state.VillagerState;
import com.villageroverhaul.state.VillagerStateAccess;
import com.villageroverhaul.vanilla.VanillaCatalog;
import com.villageroverhaul.vanilla.VanillaRankCaps;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.Optional;

/**
 * Level/XP and upgrade-point bookkeeping - see README.md (numbers still up for playtest tweaking).
 *
 * Level curve (reworked 2026-09-24): the step from level L to L+1 costs XP_BASE + XP_PER_LEVEL_SQUARED * L²
 * villager XP (400, 409, 436, ..., 3649) - flat early, steep late - and from CURVE_END_LEVEL on every step
 * costs as much as the last one. At 100 % happiness (30 XP per work check, ~60 checks a day,
 * VillagerWorkScan) that is ~3 in-game days to level 9 and ~17 days to level 20.
 *
 * No level cap, for every profession (2026-09-28: the core's rule replaced the Trade Rework's level-20 cap) -
 * a villager only pauses at the point cap.
 *
 * Ranks: every level brings one upgrade point, spent on a section's rank (investPoint). The price of each
 * rank-up comes from the section (SectionDefinition.upgradeCost), or - for a profession running on its Vanilla
 * trades - from one shared price line (VanillaRankCaps).
 */
public final class ProgressionService {

    /** From this level on the curve stops growing. */
    public static final int CURVE_END_LEVEL = 20;
    private static final int XP_BASE = 400;
    private static final int XP_PER_LEVEL_SQUARED = 9;
    private static final int MAX_UNSPENT_POINTS = 5;

    /** How many of our own levels map to one Vanilla merchant-tier name (Novice..Master, 1-5). */
    private static final int LEVELS_PER_MERCHANT_TIER = 5;

    private ProgressionService() {
    }

    /** 1 (Novice) .. 5 (Master), for the "merchant.level.N" translation key. */
    public static int merchantTier(int level) {
        return Math.min(5, level / LEVELS_PER_MERCHANT_TIER + 1);
    }

    /**
     * Villager XP for the step from `level` to `level + 1`. Past CURVE_END_LEVEL every step costs as much as the
     * last one, 19 → 20 - the curve stops growing so late unlocks stay reachable (2026-09-27).
     */
    public static int xpToNextLevel(int level) {
        int curveLevel = Math.min(level, CURVE_END_LEVEL - 1);
        return XP_BASE + XP_PER_LEVEL_SQUARED * curveLevel * curveLevel;
    }

    /** Villager XP needed to go from one level to another - the single place the level curve is read from outside. */
    public static int xpBetweenLevels(int fromLevel, int toLevel) {
        int total = 0;
        for (int level = fromLevel; level < toLevel; level++) {
            total += xpToNextLevel(level);
        }
        return total;
    }

    /** Progress toward the next level-up, 0..1. */
    public static float xpFraction(VillagerState state) {
        return Math.min(1.0F, state.xp() / (float) xpToNextLevel(state.level()));
    }

    /** False at the point cap with the XP bar already full - withXp would drop any XP then. */
    public static boolean canGainXp(VillagerState state) {
        return state.unspentUpgradePoints() < MAX_UNSPENT_POINTS || state.xp() < xpToNextLevel(state.level()) - 1;
    }

    /** Grants XP and stores the result - see withXp. */
    public static void grantXp(Villager villager, int amount) {
        VillagerStateAccess access = VillagerStateAccess.of(villager);
        access.setState(withXp(access.getState(), amount));
    }

    /**
     * Adds unspent upgrade points directly, ignoring MAX_UNSPENT_POINTS - debug only (/vo grant_points).
     * Level and XP stay untouched; while above the cap, the villager simply doesn't level.
     */
    public static void grantPoints(VillagerStateAccess access, int amount) {
        VillagerState state = access.getState();
        access.setState(state.withProgress(state.level(), state.xp(), state.unspentUpgradePoints() + amount));
    }

    /**
     * Adds XP and levels up while XP allows - never past MAX_UNSPENT_POINTS unspent upgrade points (XP then stops
     * just below the next level until points are spent).
     */
    public static VillagerState withXp(VillagerState state, int amount) {
        int level = state.level();
        int xp = state.xp() + amount;
        int unspentUpgradePoints = state.unspentUpgradePoints();

        while (xp >= xpToNextLevel(level) && unspentUpgradePoints < MAX_UNSPENT_POINTS) {
            xp -= xpToNextLevel(level);
            level++;
            unspentUpgradePoints++;
        }
        if (unspentUpgradePoints >= MAX_UNSPENT_POINTS) {
            // Paused at the point cap: XP stops one short of the next level, so a long-unvisited villager
            // never banks more than MAX_UNSPENT_POINTS - spending points doesn't trigger a burst of level-ups.
            xp = Math.min(xp, xpToNextLevel(level) - 1);
        }
        return state.withProgress(level, xp, unspentUpgradePoints);
    }

    /** Points the section's next rank costs for this villager, or -1 once it is maxed. */
    public static int upgradeCost(Villager villager, VillagerState state, SectionDefinition section) {
        if (VanillaCatalog.usedBy(villager)) {
            return VanillaRankCaps.of(VanillaCatalog.of(villager)).upgradeCost(section.id(), state);
        }
        return section.upgradeCost(state.rank(section.id()));
    }

    public enum InvestResult {
        UPGRADED, UNKNOWN_SECTION, NOT_OPEN, MAXED, NOT_ENOUGH_POINTS
    }

    /** Spends the next rank's cost in points on the section - only one the villager has, and only while it is open. */
    public static InvestResult investPoint(Villager villager, Identifier sectionId) {
        VillagerStateAccess access = VillagerStateAccess.of(villager);
        VillagerState state = access.getState();
        Optional<SectionDefinition> section = Sections.get(sectionId).filter(VillagerSections.of(villager)::contains);
        if (section.isEmpty()) {
            return InvestResult.UNKNOWN_SECTION;
        }
        if (!VillagerSections.isOpen(state, section.get())) {
            return InvestResult.NOT_OPEN;
        }
        int cost = upgradeCost(villager, state, section.get());
        if (cost < 0) {
            return InvestResult.MAXED;
        }
        if (state.unspentUpgradePoints() < cost) {
            return InvestResult.NOT_ENOUGH_POINTS;
        }
        access.setState(state.withProgress(state.level(), state.xp(), state.unspentUpgradePoints() - cost)
                .withRank(sectionId, state.rank(sectionId) + 1));
        return InvestResult.UPGRADED;
    }
}
