package com.villageroverhaul.vanilla;

import com.villageroverhaul.data.ItemAmount;
import com.villageroverhaul.trade.ResolvedExchange;
import com.villageroverhaul.work.RestockService;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;
import java.util.Optional;

/**
 * Price steps of entries built from Vanilla trades (VanillaCatalog, Tweak-Werte.md). Once a section has unlocked
 * all its entries, EXTRA_RANKS more upgrades improve them - stage 0 applies from the start, each extra rank moves
 * one stage on (stage = rank - last unlock rank, 0..4):
 * - entries that give emeralds (Quests): the amount asked for, EMERALD_REWARD_PRICE_PERCENT of Vanilla's
 * - entries with an unstackable result (tools, armour, cakes ...): the emerald price, UNSTACKABLE_PRICE_PERCENT,
 *   and on the last stage only, 2 emeralds or less becomes 1
 * - entries with a stackable result: how many you get, STACKABLE_YIELD_PERCENT, at most one full stack
 * - every entry's max stock: STOCK_START_PERCENT of Vanilla's at stage 0, evenly up to STOCK_END_PERCENT
 *   at the last stage
 * Always rounded up, never below 1.
 */
public final class VanillaScaling {

    /** Upgrades after the last unlock that improve prices or yields. */
    public static final int EXTRA_RANKS = 4;
    private static final int[] EMERALD_REWARD_PRICE_PERCENT = {120, 100, 80, 60, 30};
    private static final int[] UNSTACKABLE_PRICE_PERCENT = {200, 150, 100, 80, 50};
    private static final int[] STACKABLE_YIELD_PERCENT = {50, 70, 90, 120, 150};
    /** Max stock at stage 0 and at the last stage, in % of Vanilla's max uses - linear in between. */
    private static final int STOCK_START_PERCENT = 50;
    private static final int STOCK_END_PERCENT = 300;
    /** On the last stage, an emerald price at or below this is 1 (2026-09-27: only then). */
    private static final int PRICE_ONE_AT_OR_BELOW = 2;

    private VanillaScaling() {
    }

    /** 0 while unlocking, then 1..EXTRA_RANKS for each upgrade past the last unlock rank. */
    public static int stage(int rank, int lastUnlockRank) {
        return Mth.clamp(rank - lastUnlockRank, 0, EXTRA_RANKS);
    }

    /** The entry at this stage, with its current stock (VillagerState.stock). */
    public static ResolvedExchange resolve(VanillaCatalog.Entry entry, int stage, Map<Identifier, Integer> stock) {
        int maxUses = maxUses(entry.maxUses(), stage);
        int usesRemaining = stock.getOrDefault(entry.id(), RestockService.initialStock(maxUses));
        ItemStack result = entry.result();
        if (result.is(Items.EMERALD)) {
            return new ResolvedExchange(entry.id(), emeraldRewardPrice(entry.costA(), 0), emeraldRewardPrice(entry.costA(), stage), entry.costB(),
                    result.getCount(), amount(result), usesRemaining, maxUses, Optional.of(result));
        }
        if (result.getMaxStackSize() <= 1) {
            return new ResolvedExchange(entry.id(), unstackablePrice(entry.costA(), 0), unstackablePrice(entry.costA(), stage),
                    entry.costB().map(cost -> unstackablePrice(cost, stage)), result.getCount(), amount(result), usesRemaining, maxUses, Optional.of(result));
        }
        ItemStack scaled = result.copyWithCount(stackableYield(result, stage));
        return new ResolvedExchange(entry.id(), entry.costA(), entry.costA(), entry.costB(), stackableYield(result, 0), amount(scaled),
                usesRemaining, maxUses, Optional.of(scaled));
    }

    /** How many of a stackable result one trade gives at this stage, at most one full stack. */
    private static int stackableYield(ItemStack result, int stage) {
        return Math.min(result.getMaxStackSize(), percentUp(result.getCount(), STACKABLE_YIELD_PERCENT[stage]));
    }

    public static int stageOf(VanillaCatalog.Catalog catalog, Identifier section, int rank) {
        return stage(rank, catalog.lastUnlockRank(section));
    }

    private static ItemAmount emeraldRewardPrice(ItemAmount vanilla, int stage) {
        return withCount(vanilla, percentUp(vanilla.count(), EMERALD_REWARD_PRICE_PERCENT[stage]));
    }

    /** The emerald cost of an unstackable trade at this stage; other costs stay as they are. */
    private static ItemAmount unstackablePrice(ItemAmount vanilla, int stage) {
        if (!vanilla.item().equals(Items.EMERALD)) {
            return vanilla;
        }
        int price = percentUp(vanilla.count(), UNSTACKABLE_PRICE_PERCENT[stage]);
        return withCount(vanilla, stage == EXTRA_RANKS && price <= PRICE_ONE_AT_OR_BELOW ? 1 : price);
    }

    /** Max stock at this stage (50 % .. 300 % of Vanilla, rounded up). */
    private static int maxUses(int vanillaMaxUses, int stage) {
        double percent = STOCK_START_PERCENT + (STOCK_END_PERCENT - STOCK_START_PERCENT) * stage / (double) EXTRA_RANKS;
        return Math.max(1, Mth.ceil(vanillaMaxUses * percent / 100.0));
    }

    private static int percentUp(int count, int percent) {
        return Math.max(1, Mth.ceil(count * percent / 100.0));
    }

    private static ItemAmount amount(ItemStack stack) {
        return new ItemAmount(stack.getItem(), stack.getCount());
    }

    private static ItemAmount withCount(ItemAmount amount, int count) {
        return new ItemAmount(amount.item(), count, amount.enchantment(), amount.mob());
    }
}
