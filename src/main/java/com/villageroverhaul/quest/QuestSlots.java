package com.villageroverhaul.quest;

import com.villageroverhaul.VillagerOverhaulMod;
import com.villageroverhaul.data.ItemExchange;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

/**
 * The quest group's rank ladder (reworked 2026-09-24, README.md):
 * which quest slots a villager has and how many quests per day it takes. All numbers: Obsidian
 * Tweak-Werte.md.
 *
 * Slots have fixed indices so their state keys (quest_slot_rotations etc.) never shift when a new
 * slot unlocks: 0 and 1 draw from the easy pool, 2 is the hard slot, 3 the permanent quest.
 *
 * The Veteran (Vanilla weaponsmith, 2026-09-27) has no permanent quest: on the permanent quest's rank he
 * opens a second hard slot instead (slot 4, so slot 3 keeps meaning "permanent" everywhere), and his daily
 * limit on the highest quest rank is EXTRA_HARD_LIMIT_BONUS higher.
 */
public final class QuestSlots {

    /** Quests per day, indexed by quest rank 0..9. */
    private static final int[] DAILY_LIMIT_BY_RANK = {1, 2, 3, 3, 4, 4, 4, 5, 6, 7};
    private static final int SECOND_EASY_SLOT_RANK = 3;
    private static final int HARD_SLOT_RANK = 5;
    private static final int PERMANENT_SLOT_RANK = 6;
    /** Extra quests per day on the highest quest rank for professions with a second hard slot. */
    private static final int EXTRA_HARD_LIMIT_BONUS = 3;

    public static final int HARD_SLOT = 2;
    public static final int PERMANENT_SLOT = 3;
    public static final int SECOND_HARD_SLOT = 4;

    public static final int SLOT_COUNT = 5;

    /**
     * A fallback profession's quests (fallback/FallbackCatalog, 2026-09-27) take slots from here on, one per
     * quest: all of them are permanent quests - never rotate, no reroll, grey out at the daily limit.
     */
    public static final int FALLBACK_SLOT_BASE = 100;

    /** The permanent quest, or any fallback quest. */
    public static boolean isPermanent(int slot) {
        return slot == PERMANENT_SLOT || slot >= FALLBACK_SLOT_BASE;
    }

    private QuestSlots() {
    }

    /** Veteran: a second hard slot instead of the permanent quest. */
    public static boolean hasSecondHardSlot(Holder<VillagerProfession> profession) {
        return profession.is(VillagerProfession.WEAPONSMITH);
    }

    public static int dailyLimit(Villager villager, int questRank) {
        int rank = Math.min(Math.max(questRank, 0), DAILY_LIMIT_BY_RANK.length - 1);
        int bonus = rank == DAILY_LIMIT_BY_RANK.length - 1 && hasSecondHardSlot(villager.getVillagerData().profession())
                ? EXTRA_HARD_LIMIT_BONUS : 0;
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

    public static ItemExchange.QuestPool poolOf(int slot) {
        return switch (slot) {
            case HARD_SLOT, SECOND_HARD_SLOT -> ItemExchange.QuestPool.HARD;
            case PERMANENT_SLOT -> ItemExchange.QuestPool.PERMANENT;
            default -> ItemExchange.QuestPool.EASY;
        };
    }

    public static Identifier rotationKey(int slot) {
        return Identifier.fromNamespaceAndPath(VillagerOverhaulMod.MODID, "quest_slot_" + slot);
    }
}
