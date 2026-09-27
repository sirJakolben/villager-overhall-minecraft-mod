package com.villageroverhaul.progression;

/**
 * The four upgrade groups (README.md, reworked 2026-09-24). Each rank
 * up to fullUnlockRank unlocks something, prices stay at Base; the ranks after it only scale Base to
 * Max. Both halves cost the same in every group: 9 points to unlock everything, 11 more to max out -
 * so 20 points buy either one maxed group, or two fully unlocked ones. Master trades (2026-09-26): 7 ranks,
 * ranks 1-4 unlock the Librarian's four special books.
 */
public enum UpgradeGroup {
    QUEST(new int[]{1, 1, 1, 2, 2, 2, 3, 3, 5}, 6),
    BASIC_TRADE(new int[]{1, 1, 1, 2, 2, 2, 3, 3, 5}, 6),
    MASTER_TRADE(new int[]{2, 2, 2, 3, 3, 3, 5}, 4),
    PASSIVE(new int[]{3, 3, 3, 3, 3, 5}, 3);

    private final int[] upgradeCosts;
    private final int fullUnlockRank;

    UpgradeGroup(int[] upgradeCosts, int fullUnlockRank) {
        this.upgradeCosts = upgradeCosts;
        this.fullUnlockRank = fullUnlockRank;
    }

    public int maxRank() {
        return upgradeCosts.length;
    }

    /** Points the upgrade from currentRank to currentRank + 1 costs, or -1 once the group is maxed. */
    public int upgradeCost(int currentRank) {
        return currentRank >= 0 && currentRank < upgradeCosts.length ? upgradeCosts[currentRank] : -1;
    }

    /** From this rank on everything in the group is unlocked; Base-to-Max scaling runs from here to maxRank. */
    public int fullUnlockRank() {
        return fullUnlockRank;
    }
}
