package com.villageroverhaul.progression;

import com.villageroverhaul.core.VillagerStateAccess;
import com.villageroverhaul.fallback.FallbackCatalog;
import com.villageroverhaul.fallback.RankCaps;
import com.villageroverhaul.core.state.GroupRanks;
import com.villageroverhaul.core.state.VillagerState;
import net.minecraft.world.entity.npc.villager.Villager;

/**
 * Level/XP and upgrade-point bookkeeping - see README.md (level
 * curve, max level and rank costs decided 2026-09-23, numbers still up for playtest tweaking).
 *
 * Level curve (reworked 2026-09-24): the step from level L to L+1 costs XP_BASE + XP_PER_LEVEL_SQUARED * L²
 * villager XP (400, 409, 436, ..., 3649) - flat early, steep late. At 100 % happiness (30 XP per work
 * check, ~60 checks a day, VillagerWorkScan) that is ~3 in-game days to level 9 (one group fully
 * unlocked) and ~17 days to level 20, the same as the old linear 600 + 100 * L curve.
 *
 * Ranks: costs per group live in UpgradeGroup - every group costs 9 points to unlock fully and 20 to
 * max out, and a villager earns exactly 20 points on the way to MAX_LEVEL (reworked 2026-09-24).
 * Master and Passive can only be upgraded once the villager has used that workstation (Stations).
 */
public final class ProgressionService {

    public static final int MAX_LEVEL = 20;
    private static final int XP_BASE = 400;
    private static final int XP_PER_LEVEL_SQUARED = 9;
    private static final int MAX_UNSPENT_POINTS = 5;

    /**
     * How many of our own levels map to one Vanilla merchant-tier name (Novice..Master, 1-5) -
     * placeholder like the rest of this class's numbers, see README.md
     * section 2 ("wir tweaken es ein bisschen" - level speed/cap still open).
     */
    private static final int LEVELS_PER_MERCHANT_TIER = 5;

    private ProgressionService() {
    }

    /** 1 (Novice) .. 5 (Master), for the "merchant.level.N" translation key. */
    public static int merchantTier(int level) {
        return Math.min(5, level / LEVELS_PER_MERCHANT_TIER + 1);
    }

    /**
     * Villager XP for the step from `level` to `level + 1`. Past MAX_LEVEL (only fallback professions get
     * there, see maxLevel) every step costs as much as the last one, 19 → 20 - the curve stops growing so
     * their many unlocks stay reachable (2026-09-27).
     */
    public static int xpToNextLevel(int level) {
        int curveLevel = Math.min(level, MAX_LEVEL - 1);
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

    /** Progress toward the next level-up, 0..1 - clamped for old saves that banked XP past the step while paused. */
    public static float xpFraction(VillagerState state, int maxLevel) {
        if (state.level() >= maxLevel) {
            return 1.0F;
        }
        return Math.min(1.0F, state.xp() / (float) xpToNextLevel(state.level()));
    }

    /**
     * A group shows (and can be upgraded) while the villager owns its station - lectern for Basic, master
     * and passive station - or already has a rank in it (decided 2026-09-24). Quests are always open.
     */
    public static boolean isGroupOpen(VillagerState state, UpgradeGroup group) {
        return switch (group) {
            case QUEST -> true;
            case BASIC_TRADE -> state.stations().basic().isPresent() || state.ranks().basicTrade() > 0;
            case MASTER_TRADE -> state.stations().master().isPresent() || state.ranks().masterTrade() > 0;
            case PASSIVE -> state.stations().passive().isPresent() || state.ranks().passive() > 0;
        };
    }

    /**
     * isGroupOpen for a real villager: a fallback profession (FallbackCatalog, 2026-09-27) has no master or
     * passive station - its Masteries are always open, its Passive never.
     */
    public static boolean isGroupOpen(Villager villager, VillagerState state, UpgradeGroup group) {
        if (FallbackCatalog.isFallback(villager)) {
            return switch (group) {
                case MASTER_TRADE -> true;
                case PASSIVE -> false;
                default -> isGroupOpen(state, group);
            };
        }
        return isGroupOpen(state, group);
    }

    /** Server: the villager's rank caps - its own for a fallback profession, RankCaps.NONE otherwise. */
    public static RankCaps rankCaps(Villager villager) {
        return FallbackCatalog.isFallback(villager) ? RankCaps.of(FallbackCatalog.of(villager)) : RankCaps.NONE;
    }

    /**
     * Adds unspent upgrade points directly, ignoring MAX_UNSPENT_POINTS - debug only (/vo grant_points).
     * Level and XP stay untouched; while above the cap, the villager simply doesn't level.
     */
    public static void grantPoints(VillagerStateAccess access, int amount) {
        VillagerState state = access.getState();
        access.setState(new VillagerState(
                state.level(), state.xp(), state.unspentUpgradePoints() + amount,
                state.ranks(), state.questSlotRotations(), state.tradeUsesRemaining(),
                state.dailyProductivity(), state.happiness(), state.lastProcessedDay(), state.questSlotRerollAvailableAtTick(), state.questLog(), state.stations()
        ));
    }

    /**
     * Highest level of this villager: MAX_LEVEL - except a fallback profession (FallbackCatalog, 2026-09-27),
     * which has no level cap, so its many unlocks can all be bought; it only pauses at the point cap.
     */
    public static int maxLevel(Villager villager) {
        return FallbackCatalog.isFallback(villager) ? Integer.MAX_VALUE : MAX_LEVEL;
    }

    /** False at the max level, or at the point cap with the XP bar already full - withXp would drop any XP then. */
    public static boolean canGainXp(VillagerState state, int maxLevel) {
        if (state.level() >= maxLevel) {
            return false;
        }
        return state.unspentUpgradePoints() < MAX_UNSPENT_POINTS || state.xp() < xpToNextLevel(state.level()) - 1;
    }

    /** Grants XP and stores the result - see withXp. */
    public static void grantXp(Villager villager, int amount) {
        VillagerStateAccess access = VillagerStateAccess.of(villager);
        access.setState(withXp(access.getState(), amount, maxLevel(villager)));
    }

    /**
     * Adds XP and levels up while XP allows - never past maxLevel (see maxLevel(Villager)) (further XP is dropped), and never
     * past MAX_UNSPENT_POINTS unspent upgrade points (XP then stops just below the next level until points are spent).
     */
    public static VillagerState withXp(VillagerState state, int amount, int maxLevel) {
        int level = state.level();
        if (level >= maxLevel) {
            return state;
        }
        int xp = state.xp() + amount;
        int unspentUpgradePoints = state.unspentUpgradePoints();

        while (level < maxLevel && xp >= xpToNextLevel(level) && unspentUpgradePoints < MAX_UNSPENT_POINTS) {
            xp -= xpToNextLevel(level);
            level++;
            unspentUpgradePoints++;
        }
        if (level >= maxLevel) {
            xp = 0;
        } else if (unspentUpgradePoints >= MAX_UNSPENT_POINTS) {
            // Paused at the point cap: XP stops one short of the next level, so a long-unvisited villager
            // never banks more than MAX_UNSPENT_POINTS - spending points doesn't trigger a burst of level-ups.
            xp = Math.min(xp, xpToNextLevel(level) - 1);
        }

        return new VillagerState(
                level, xp, unspentUpgradePoints,
                state.ranks(), state.questSlotRotations(), state.tradeUsesRemaining(),
                state.dailyProductivity(), state.happiness(), state.lastProcessedDay(), state.questSlotRerollAvailableAtTick(), state.questLog(), state.stations()
        );
    }

    /** Spends the next upgrade's cost in points on the given group's rank. Returns false if nothing was spent. */
    public static boolean investPoint(Villager villager, UpgradeGroup group) {
        VillagerStateAccess access = VillagerStateAccess.of(villager);
        VillagerState state = access.getState();
        int currentRank = rankOf(state.ranks(), group);
        int cost = rankCaps(villager).upgradeCost(group, state.ranks());
        if (cost < 0 || state.unspentUpgradePoints() < cost || !isGroupOpen(villager, state, group)) {
            return false;
        }

        access.setState(new VillagerState(
                state.level(), state.xp(), state.unspentUpgradePoints() - cost,
                withRank(state.ranks(), group, currentRank + 1),
                state.questSlotRotations(), state.tradeUsesRemaining(),
                state.dailyProductivity(), state.happiness(), state.lastProcessedDay(), state.questSlotRerollAvailableAtTick(), state.questLog(), state.stations()
        ));
        return true;
    }

    public static int rankOf(GroupRanks ranks, UpgradeGroup group) {
        return switch (group) {
            case QUEST -> ranks.quest();
            case BASIC_TRADE -> ranks.basicTrade();
            case MASTER_TRADE -> ranks.masterTrade();
            case PASSIVE -> ranks.passive();
        };
    }

    private static GroupRanks withRank(GroupRanks ranks, UpgradeGroup group, int newRank) {
        return switch (group) {
            case QUEST -> new GroupRanks(newRank, ranks.basicTrade(), ranks.masterTrade(), ranks.passive());
            case BASIC_TRADE -> new GroupRanks(ranks.quest(), newRank, ranks.masterTrade(), ranks.passive());
            case MASTER_TRADE -> new GroupRanks(ranks.quest(), ranks.basicTrade(), newRank, ranks.passive());
            case PASSIVE -> new GroupRanks(ranks.quest(), ranks.basicTrade(), ranks.masterTrade(), newRank);
        };
    }
}
