package com.villageroverhaul.traderework.quest;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

/**
 * The quest section's rank ladder (reworked 2026-09-24, README.md): which quest slots a villager has and how
 * many quests per day it takes. All numbers: Obsidian Tweak-Werte.md.
 *
 * Slots have fixed indices so their state (QuestState) never shifts when a new slot unlocks: 0 and 1 draw from
 * the easy pool, 2 is the hard slot, 3 the permanent quest. The Veteran (the Vanilla weaponsmith, 2026-09-27)
 * has no permanent quest: on the permanent quest's rank it opens a second hard slot instead (slot 4, so slot 3
 * keeps meaning "permanent" everywhere), and its daily limit on the highest quest rank is
 * SECOND_HARD_SLOT_LIMIT_BONUS higher.
 */
public final class QuestSlots {

    /** An entry's quest pool (QuestEntry) - an entry without one is easy. */
    public static final String EASY = "easy";
    public static final String HARD = "hard";
    public static final String PERMANENT = "permanent";

    /** Quests per day, indexed by quest rank 0..9. */
    private static final int[] DAILY_LIMIT_BY_RANK = {1, 2, 3, 3, 4, 4, 4, 5, 6, 7};
    private static final int SECOND_EASY_SLOT_RANK = 3;
    private static final int HARD_SLOT_RANK = 5;
    private static final int PERMANENT_SLOT_RANK = 6;
    /** Extra quests per day on the highest quest rank for professions with a second hard slot. */
    private static final int SECOND_HARD_SLOT_LIMIT_BONUS = 3;

    public static final int HARD_SLOT = 2;
    public static final int PERMANENT_SLOT = 3;
    public static final int SECOND_HARD_SLOT = 4;

    public static final int SLOT_COUNT = 5;

    private QuestSlots() {
    }

    /** A second hard slot instead of the permanent quest - the Veteran. */
    public static boolean hasSecondHardSlot(Holder<VillagerProfession> profession) {
        return profession.is(VillagerProfession.WEAPONSMITH);
    }

    public static int dailyLimit(Villager villager, int questRank) {
        int rank = Math.min(Math.max(questRank, 0), DAILY_LIMIT_BY_RANK.length - 1);
        int bonus = rank == DAILY_LIMIT_BY_RANK.length - 1 && hasSecondHardSlot(villager.getVillagerData().profession())
                ? SECOND_HARD_SLOT_LIMIT_BONUS : 0;
        return DAILY_LIMIT_BY_RANK[rank] + bonus;
    }

    /** Whether this slot is unlocked at the given quest rank for this profession. */
    public static boolean isOpen(int slot, int questRank, Holder<VillagerProfession> profession) {
        return switch (slot) {
            case 0 -> true;
            case 1 -> questRank >= SECOND_EASY_SLOT_RANK;
            case HARD_SLOT -> questRank >= HARD_SLOT_RANK;
            case PERMANENT_SLOT -> questRank >= PERMANENT_SLOT_RANK && !hasSecondHardSlot(profession);
            case SECOND_HARD_SLOT -> questRank >= PERMANENT_SLOT_RANK && hasSecondHardSlot(profession);
            default -> false;
        };
    }

    public static String poolOf(int slot) {
        return switch (slot) {
            case HARD_SLOT, SECOND_HARD_SLOT -> HARD;
            case PERMANENT_SLOT -> PERMANENT;
            default -> EASY;
        };
    }
}
