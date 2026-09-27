package com.villageroverhaul.librarian;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.Optional;

/**
 * The Librarian's passive "book upgrade" (Obsidian Librarian.md, "Passive Ability"): +1 level on one
 * enchantment of a book in the enchantment station. Which books, which enchantment and how high it may
 * go is decided here; when an upgrade happens is the villager's side (passive/BookUpgradeWork).
 *
 * - Books: enchanted books only - the special books turn into enchanted books at the enchanting table (SpecialBookItem).
 * - Which enchantment: the lowest-level one that is still below its cap (decided 2026-09-26).
 * - Cap by passive rank: LEVEL_CAP_BY_RANK, never above the enchantment's Vanilla maximum; from
 *   OVERMAX_RANK on, Vanilla maximum + 1 - only for enchantments whose Vanilla maximum is above 1.
 */
public final class BookUpgrades {

    /** Highest level an upgrade reaches, per passive rank below OVERMAX_RANK (Tweak-Werte.md). */
    private static final int[] LEVEL_CAP_BY_RANK = {2, 3, 4, 5, 5, 5};
    public static final int OVERMAX_RANK = LEVEL_CAP_BY_RANK.length;

    private BookUpgrades() {
    }

    public static boolean isBook(ItemStack stack) {
        return stack.is(Items.ENCHANTED_BOOK);
    }

    public static int levelCap(Holder<Enchantment> enchantment, int passiveRank) {
        int vanillaMax = enchantment.value().getMaxLevel();
        if (passiveRank >= OVERMAX_RANK) {
            return vanillaMax > 1 ? vanillaMax + 1 : vanillaMax;
        }
        return Math.min(vanillaMax, LEVEL_CAP_BY_RANK[Math.max(0, passiveRank)]);
    }

    /** The enchantment the next upgrade of this book raises at this rank - empty if the book is done. */
    public static Optional<Holder<Enchantment>> nextUpgrade(ItemStack stack, int passiveRank) {
        if (!isBook(stack)) {
            return Optional.empty();
        }
        Holder<Enchantment> lowest = null;
        int lowestLevel = Integer.MAX_VALUE;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()) {
            int level = entry.getIntValue();
            if (level < levelCap(entry.getKey(), passiveRank) && level < lowestLevel) {
                lowest = entry.getKey();
                lowestLevel = level;
            }
        }
        return Optional.ofNullable(lowest);
    }

    /** Whether any rank could still upgrade this book - what the station lets in. */
    public static boolean isUpgradeable(ItemStack stack) {
        return nextUpgrade(stack, OVERMAX_RANK).isPresent();
    }

    public static ItemEnchantments upgrade(ItemStack stack, Holder<Enchantment> enchantment) {
        return EnchantmentHelper.updateEnchantments(stack, enchantments -> enchantments.set(enchantment, enchantments.getLevel(enchantment) + 1));
    }
}
