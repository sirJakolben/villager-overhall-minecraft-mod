package com.villageroverhaul.fallback;

import com.villageroverhaul.core.state.GroupRanks;
import com.villageroverhaul.data.ItemAmount;
import com.villageroverhaul.data.ItemExchange;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;

/**
 * Price steps of a fallback profession (FallbackCatalog, 2026-09-27, Tweak-Werte.md). Once a group has
 * unlocked all its entries, EXTRA_RANKS more upgrades improve them - stage 0 applies from the start, each
 * extra rank moves one stage on (stage = rank - last unlock rank, 0..4):
 * - quests: the amount asked for, QUEST_PRICE_PERCENT of Vanilla's
 * - trades with an unstackable result (tools, armour, cakes ...): the emerald price, TRADE_PRICE_PERCENT,
 *   and on the last stage only, 2 emeralds or less becomes 1
 * - trades with a stackable result: how many you get, TRADE_YIELD_PERCENT, at most one full stack
 * - every trade's max stock (restock cap): STOCK_START_PERCENT of Vanilla's at stage 0, evenly up to STOCK_END_PERCENT
 *   at the last stage
 * Always rounded up, never below 1.
 */
public final class FallbackScaling {

    /** Upgrades after the last unlock that improve prices or yields. */
    public static final int EXTRA_RANKS = 4;
    private static final int[] QUEST_PRICE_PERCENT = {120, 100, 80, 60, 30};
    private static final int[] TRADE_PRICE_PERCENT = {200, 150, 100, 80, 50};
    private static final int[] TRADE_YIELD_PERCENT = {50, 70, 90, 120, 150};
    /** Max stock at stage 0 and at the last stage, in % of Vanilla's max uses - linear in between. */
    private static final int STOCK_START_PERCENT = 50;
    private static final int STOCK_END_PERCENT = 300;
    /** On the last stage, an emerald price at or below this is 1 (2026-09-27: only then). */
    private static final int PRICE_ONE_AT_OR_BELOW = 2;

    private FallbackScaling() {
    }

    /** 0 while unlocking, then 1..EXTRA_RANKS for each upgrade past the last unlock rank. */
    public static int stage(int rank, int lastUnlockRank) {
        return Mth.clamp(rank - lastUnlockRank, 0, EXTRA_RANKS);
    }

    public static ItemAmount questPrice(ItemAmount vanilla, int stage) {
        return withCount(vanilla, percentUp(vanilla.count(), QUEST_PRICE_PERCENT[stage]));
    }

    public static boolean scalesPrice(ItemStack result) {
        return result.getMaxStackSize() <= 1;
    }

    /** The emerald cost of an unstackable trade at this stage; other costs stay as they are. */
    public static ItemAmount tradePrice(ItemAmount vanilla, int stage) {
        if (!vanilla.item().equals(Items.EMERALD)) {
            return vanilla;
        }
        int price = percentUp(vanilla.count(), TRADE_PRICE_PERCENT[stage]);
        return withCount(vanilla, stage == EXTRA_RANKS && price <= PRICE_ONE_AT_OR_BELOW ? 1 : price);
    }

    public static Optional<ItemAmount> tradePrice(Optional<ItemAmount> vanilla, int stage) {
        return vanilla.map(amount -> tradePrice(amount, stage));
    }

    /** Max stock of a trade at this stage (50 % .. 300 % of Vanilla, rounded up). */
    public static int tradeMaxUses(int vanillaMaxUses, int stage) {
        double percent = STOCK_START_PERCENT + (STOCK_END_PERCENT - STOCK_START_PERCENT) * stage / (double) EXTRA_RANKS;
        return Math.max(1, Mth.ceil(vanillaMaxUses * percent / 100.0));
    }

    /** Stage of a trade entry: its group's rank against that group's last unlock rank. */
    public static int tradeStage(FallbackCatalog.Entry entry, FallbackCatalog.Catalog catalog, GroupRanks ranks) {
        return entry.tier() == ItemExchange.Tier.MASTER
                ? stage(ranks.masterTrade(), catalog.lastMasterRank())
                : stage(ranks.basicTrade(), catalog.lastBasicRank());
    }

    /** The result of a stackable trade at this stage - more or fewer, at most one full stack. */
    public static ItemStack tradeYield(ItemStack vanilla, int stage) {
        int count = Math.min(vanilla.getMaxStackSize(), percentUp(vanilla.getCount(), TRADE_YIELD_PERCENT[stage]));
        return vanilla.copyWithCount(count);
    }

    private static int percentUp(int count, int percent) {
        return Math.max(1, Mth.ceil(count * percent / 100.0));
    }

    private static ItemAmount withCount(ItemAmount amount, int count) {
        return new ItemAmount(amount.item(), count, amount.enchantment(), amount.mob());
    }
}
