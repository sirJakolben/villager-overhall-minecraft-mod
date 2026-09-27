package com.villageroverhaul.fallback;

import com.villageroverhaul.core.state.GroupRanks;
import com.villageroverhaul.progression.ProgressionService;
import com.villageroverhaul.progression.UpgradeGroup;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Rank caps of a fallback profession (FallbackCatalog, 2026-09-27): each rank unlocks the next entry - quest
 * and basic rank 0 already show the first, the master group starts empty at rank 0 - and after the last one
 * FallbackScaling.EXTRA_RANKS more ranks improve prices or yields. NONE: a designed profession, the fixed UpgradeGroup caps apply. Sent to the client with
 * the offers (VillagerOffersPayload) - only the server can roll the catalog.
 */
public record RankCaps(int quest, int basic, int master) {

    public static final RankCaps NONE = new RankCaps(-1, -1, -1);

    public static final StreamCodec<ByteBuf, RankCaps> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RankCaps::quest,
            ByteBufCodecs.VAR_INT, RankCaps::basic,
            ByteBufCodecs.VAR_INT, RankCaps::master,
            RankCaps::new
    );

    /** Last unlock rank plus FallbackScaling.EXTRA_RANKS price steps - for a group with entries at all. */
    public static RankCaps of(FallbackCatalog.Catalog catalog) {
        return new RankCaps(
                catalog.quests().isEmpty() ? 0 : catalog.lastQuestRank() + FallbackScaling.EXTRA_RANKS,
                catalog.basic().isEmpty() ? 0 : catalog.lastBasicRank() + FallbackScaling.EXTRA_RANKS,
                catalog.master().isEmpty() ? 0 : catalog.lastMasterRank() + FallbackScaling.EXTRA_RANKS);
    }

    public boolean isFallback() {
        return quest >= 0;
    }

    /** Highest rank of the group; -1 = use the group's own cap. Passive never opens for a fallback profession. */
    public int capOf(UpgradeGroup group) {
        if (!isFallback()) {
            return -1;
        }
        return switch (group) {
            case QUEST -> quest;
            case BASIC_TRADE -> basic;
            case MASTER_TRADE -> master;
            case PASSIVE -> 0;
        };
    }

    /**
     * Points the next upgrade of group costs, or -1 once maxed. A designed profession uses the group's own cost
     * table. A fallback profession (2026-09-27) has one shared price line over everything it can buy - all
     * quest, basic and master ranks together, unlocks and price steps: the first upgrade costs FIRST_COST, the
     * very last LAST_COST, linear in between (rounded), and every upgrade in any group moves one step on - so
     * even a profession with many trades can be fully unlocked in reasonable time.
     */
    public int upgradeCost(UpgradeGroup group, GroupRanks ranks) {
        int currentRank = ProgressionService.rankOf(ranks, group);
        int cap = capOf(group);
        if (cap < 0) {
            return group.upgradeCost(currentRank);
        }
        if (currentRank < 0 || currentRank >= cap) {
            return -1;
        }
        int total = quest + basic + master;
        int done = ranks.quest() + ranks.basicTrade() + ranks.masterTrade();
        return total <= 1 ? LAST_COST : (int) Math.round(FIRST_COST + (LAST_COST - FIRST_COST) * done / (double) (total - 1));
    }

    /** Cost of a fallback profession's first and very last upgrade (Tweak-Werte.md). */
    public static final int FIRST_COST = 1;
    public static final int LAST_COST = 5;
}
