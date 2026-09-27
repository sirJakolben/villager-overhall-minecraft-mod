package com.villageroverhaul.trade;

import com.villageroverhaul.core.VillagerStateAccess;
import com.villageroverhaul.core.state.DailyProductivity;
import com.villageroverhaul.core.state.StationSlot;
import com.villageroverhaul.core.state.Stations;
import com.villageroverhaul.core.state.VillagerState;
import com.villageroverhaul.data.ItemExchange;
import com.villageroverhaul.data.ModDataPackRegistries;
import com.villageroverhaul.fallback.FallbackCatalog;
import com.villageroverhaul.fallback.FallbackScaling;
import com.villageroverhaul.passive.PassiveWork;
import com.villageroverhaul.progression.ProgressionService;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Restock through productivity meters (README.md, reworked
 * 2026-09-23). All numbers: Obsidian Tweak-Werte.md.
 *
 * - Every work check earns work points: HAPPINESS_FLOOR at 0 % happiness up to 100 at 100 %.
 * - They go into the meter of the category the villager focuses on. A full meter (METER_POINTS) adds
 *   REFILL_PERCENT of each trade's max stock to every unlocked trade of that category (capped at the
 *   max) and drops back to 0. A 100 % happy villager working a full day (EXPECTED_WORK_CHECKS_PER_DAY)
 *   on one category fills it from empty to full. Every refill gives at least MIN_REFILL_USES; fractions
 *   of a use are kept per trade and added to the next refill, so rounding loses nothing.
 * - If the category has nothing missing when its meter fills, the meter holds at full. It is released
 *   RELEASE_DELAY_TICKS after the next trade of that category; as a fallback for stock missing without
 *   a trade (e.g. a higher max after a rank-up), each morning also releases held meters.
 * - Focus: which of its (up to three) workplaces the villager works at - real shortage first, then the
 *   emptiest meter, then idling at its highest-ranked workplace; see chooseFocus. Each day starts
 *   without a focus, at the morning workplace (StationFocus).
 */
public final class RestockService {

    public static final int REFILL_PERCENT = 25;
    public static final int MIN_REFILL_USES = 1;
    /** Pause between a trade and the release of a held full meter of its category (2 s). */
    public static final int RELEASE_DELAY_TICKS = 40;
    public static final int EXPECTED_WORK_CHECKS_PER_DAY = 60;
    public static final int HAPPINESS_FLOOR = 50;
    private static final int POINTS_PER_HAPPY_CHECK = 100;
    public static final int METER_POINTS = EXPECTED_WORK_CHECKS_PER_DAY * POINTS_PER_HAPPY_CHECK * REFILL_PERCENT / 100;
    /** Points a 100 % happy villager earns in one full work day at one workplace. */
    private static final int FULL_DAY_POINTS = EXPECTED_WORK_CHECKS_PER_DAY * POINTS_PER_HAPPY_CHECK;
    /** Remainders are stored in 1/REMAINDER_SCALE of a use. */
    private static final int REMAINDER_SCALE = 100;

    /** An unlocked trade with its current scaled max and stock. */
    private record Stock(Identifier id, ItemExchange.Tier tier, int maxUses, int current) {
    }

    private RestockService() {
    }

    /**
     * Stock of a trade that has never been restocked since it unlocked (no entry in
     * tradeUsesRemaining yet): half its current max, rounded up so a max of 1 still starts usable.
     * Applied lazily on read instead of written at unlock time, because unlocking is derived from
     * ranks and has no event of its own.
     */
    public static int initialUsesRemaining(int maxUses) {
        return (maxUses + 1) / 2;
    }

    /**
     * Size of a workplace's meter: METER_POINTS for the trade meters; the passive meter is sized so a
     * full, 100 % happy work day at the passive station fills it exactly as often as the profession's
     * passive has steps per day (PassiveWork.stepsPerDay) - a Librarian's 1 book upgrade on rank 0 means
     * 6000 points, a Mason's 8 batches 750. A meter above its size (after a rank-up) counts as full.
     */
    public static int meterPoints(Villager villager, StationSlot slot, VillagerState state) {
        if (slot != StationSlot.PASSIVE) {
            return METER_POINTS;
        }
        return FULL_DAY_POINTS / PassiveWork.stepsPerDay(villager, state.ranks().passive());
    }

    /**
     * The villager's focus is a trade workplace (master or basic, not the passive one) whose meter isn't
     * full yet - it is filling a bar there. Decides whether the villager makes its own work sound
     * (mixin/VillagerWorkSoundMixin).
     */
    public static boolean isFillingTradeMeter(Villager villager, VillagerState state) {
        Optional<StationSlot> focus = state.dailyProductivity().focusSlot();
        return focus.isPresent() && focus.get() != StationSlot.PASSIVE && state.stations().get(focus.get()).isPresent()
                && state.dailyProductivity().of(focus.get()) < meterPoints(villager, focus.get(), state);
    }

    /** Points one work check earns: HAPPINESS_FLOOR at 0 % happiness, POINTS_PER_HAPPY_CHECK at 100 %. */
    public static int workPoints(int happinessPercent) {
        return HAPPINESS_FLOOR + (POINTS_PER_HAPPY_CHECK - HAPPINESS_FLOOR) * happinessPercent / 100;
    }

    private static List<Stock> unlockedStock(Villager villager, VillagerState state) {
        List<Stock> stock = new ArrayList<>();
        if (FallbackCatalog.isFallback(villager)) {
            FallbackCatalog.Catalog catalog = FallbackCatalog.of(villager);
            for (FallbackCatalog.Entry entry : TradeProviderImpl.unlockedFallbackTrades(villager, state)) {
                int maxUses = FallbackScaling.tradeMaxUses(entry.maxUses(), FallbackScaling.tradeStage(entry, catalog, state.ranks()));
                stock.add(new Stock(entry.id(), entry.tier(), maxUses,
                        state.tradeUsesRemaining().getOrDefault(entry.id(), initialUsesRemaining(maxUses))));
            }
            return stock;
        }
        VillagerProfession profession = villager.getVillagerData().profession().value();
        for (var entry : villager.level().registryAccess().lookupOrThrow(ModDataPackRegistries.TRADE).entrySet()) {
            ItemExchange exchange = entry.getValue();
            if (exchange.profession() != profession || !exchange.isTradeUnlocked(state)) {
                continue;
            }
            Identifier id = entry.getKey().identifier();
            int maxUses = ExchangeScaling.scaleMaxUses(exchange, exchange.tradeGroupRank(state), exchange.tradeGroup());
            int current = state.tradeUsesRemaining().getOrDefault(id, initialUsesRemaining(maxUses));
            stock.add(new Stock(id, exchange.tier(), maxUses, current));
        }
        return stock;
    }

    /** Per unlocked category, the largest "max minus current" of its trades - 0 means the category is full. */
    public static Map<ItemExchange.Tier, Integer> gaps(Villager villager, VillagerState state) {
        return gapsOf(unlockedStock(villager, state));
    }

    private static Map<ItemExchange.Tier, Integer> gapsOf(List<Stock> stock) {
        Map<ItemExchange.Tier, Integer> gaps = new EnumMap<>(ItemExchange.Tier.class);
        for (Stock s : stock) {
            gaps.merge(s.tier(), Math.max(0, s.maxUses() - s.current()), Math::max);
        }
        return gaps;
    }

    /** First scan of a new day: releases held full meters of categories missing stock again, and drops the focus. */
    public static VillagerState startDay(Villager villager, VillagerState state, long today) {
        List<Stock> stock = unlockedStock(villager, state);
        Map<ItemExchange.Tier, Integer> gaps = gapsOf(stock);
        VillagerState updated = state;
        for (ItemExchange.Tier tier : gaps.keySet()) {
            if (gaps.get(tier) > 0 && updated.dailyProductivity().of(tier) >= METER_POINTS) {
                updated = refill(updated, stock, tier);
            }
        }
        return withMeters(updated, updated.dailyProductivity().withFocus(Optional.empty()), today);
    }

    /**
     * After a trade of this category: if its meter is held at full, schedule the release
     * RELEASE_DELAY_TICKS from now - it then refills right away instead of waiting for the morning.
     */
    public static DailyProductivity scheduleRelease(DailyProductivity meters, ItemExchange.Tier tier, long gameTime) {
        if (meters.of(tier) < METER_POINTS) {
            return meters;
        }
        Map<String, Long> releaseAt = new HashMap<>(meters.releaseAtTick());
        releaseAt.put(tier.getSerializedName(), gameTime + RELEASE_DELAY_TICKS);
        return meters.withReleaseAtTick(releaseAt);
    }

    /**
     * Releases held meters whose delay after a trade is over: refills the category if it is missing
     * stock. Called every tick while someone trades (VillagerMenu.broadcastChanges) and by the work scan.
     */
    public static void releaseDue(Villager villager) {
        VillagerStateAccess access = VillagerStateAccess.of(villager);
        VillagerState state = access.getState();
        Map<String, Long> releaseAt = state.dailyProductivity().releaseAtTick();
        long now = villager.level().getGameTime();
        if (releaseAt.isEmpty() || releaseAt.values().stream().noneMatch(tick -> tick <= now)) {
            return;
        }
        List<Stock> stock = unlockedStock(villager, state);
        Map<ItemExchange.Tier, Integer> gaps = gapsOf(stock);
        Map<String, Long> remaining = new HashMap<>(releaseAt);
        VillagerState updated = state;
        for (ItemExchange.Tier tier : ItemExchange.Tier.values()) {
            Long tick = releaseAt.get(tier.getSerializedName());
            if (tick == null || tick > now) {
                continue;
            }
            remaining.remove(tier.getSerializedName());
            if (gaps.getOrDefault(tier, 0) > 0 && updated.dailyProductivity().of(tier) >= METER_POINTS) {
                updated = refill(updated, stock, tier);
            }
        }
        access.setState(withMeters(updated, updated.dailyProductivity().withReleaseAtTick(remaining), updated.lastProcessedDay()));
    }

    /**
     * One work check at the workplace of the current focus: re-picks the focus if needed (chooseFocus),
     * fills that workplace's meter, and refills its trades or holds when the meter is full.
     */
    public static VillagerState withWork(Villager villager, VillagerState state, int points, boolean passivePending) {
        List<Stock> stock = unlockedStock(villager, state);
        Map<ItemExchange.Tier, Integer> gaps = gapsOf(stock);
        DailyProductivity meters = state.dailyProductivity();

        Optional<StationSlot> focus = chooseFocus(villager, meters.focusSlot(), gaps, meters, state, passivePending);
        VillagerState updated = withMeters(state, meters.withFocus(focus), state.lastProcessedDay());
        if (focus.isEmpty()) {
            return updated;
        }

        StationSlot slot = focus.get();
        int filled = Math.min(meterPoints(villager, slot, updated), updated.dailyProductivity().of(slot) + points);
        updated = withMeters(updated, updated.dailyProductivity().with(slot, filled), updated.lastProcessedDay());
        if (slot.tier() != null && filled >= METER_POINTS && gaps.getOrDefault(slot.tier(), 0) > 0) {
            updated = refill(updated, stock, slot.tier());
        }
        return updated;
    }

    /**
     * Where to work (decided 2026-09-24), only among workplaces the villager owns - a category without
     * its workplace can't be worked on:
     * 0. A passive step is due (full passive meter and work waiting in the station - passive/PassiveWork,
     *    e.g. a book to upgrade or blocks to crush):
     *    the passive station first, since the upgrade happens only standing right at it (2026-09-26).
     * 1. Real shortage: the workplace whose trades miss the most (largest gap), until that category is
     *    restocked or its meter holds full.
     * 2. Otherwise meters that aren't full: the emptiest first, and it stays there until that meter is full.
     * 3. Otherwise everything is full: it idles (and still levels) at the workplace of the group it has
     *    the highest rank in; a missing workplace just means the next highest.
     * Ties go by morning order (master, basic, passive). A real shortage always interrupts a meter pre-fill.
     */
    public static Optional<StationSlot> chooseFocus(Villager villager, Optional<StationSlot> current, Map<ItemExchange.Tier, Integer> gaps,
                                                    DailyProductivity meters, VillagerState state, boolean passivePending) {
        Stations stations = state.stations();
        if (passivePending && stations.passive().isPresent()) {
            return Optional.of(StationSlot.PASSIVE);
        }
        StationSlot shortage = null;
        for (StationSlot slot : StationSlot.values()) {
            if (stations.get(slot).isPresent() && gapOf(slot, gaps) > 0 && meters.of(slot) < meterPoints(villager, slot, state)
                    && (shortage == null || gapOf(slot, gaps) > gapOf(shortage, gaps))) {
                shortage = slot;
            }
        }
        if (current.isPresent() && stations.get(current.get()).isPresent() && meters.of(current.get()) < meterPoints(villager, current.get(), state)) {
            if (gapOf(current.get(), gaps) > 0 || shortage == null) {
                return current;
            }
        }
        if (shortage != null) {
            return Optional.of(shortage);
        }

        StationSlot emptiest = null;
        for (StationSlot slot : StationSlot.values()) {
            if (stations.get(slot).isPresent() && meters.of(slot) < meterPoints(villager, slot, state)
                    && (emptiest == null || meters.of(slot) < meters.of(emptiest))) {
                emptiest = slot;
            }
        }
        if (emptiest != null) {
            return Optional.of(emptiest);
        }

        StationSlot highestRank = null;
        for (StationSlot slot : StationSlot.values()) {
            if (stations.get(slot).isPresent() && (highestRank == null || rankOf(slot, state) > rankOf(highestRank, state))) {
                highestRank = slot;
            }
        }
        return Optional.ofNullable(highestRank);
    }

    private static int gapOf(StationSlot slot, Map<ItemExchange.Tier, Integer> gaps) {
        return slot.tier() == null ? 0 : gaps.getOrDefault(slot.tier(), 0);
    }

    private static int rankOf(StationSlot slot, VillagerState state) {
        return ProgressionService.rankOf(state.ranks(), slot.group());
    }

    /** Adds REFILL_PERCENT of each trade's max to every trade of the category, keeping the fractions, and empties its meter. */
    private static VillagerState refill(VillagerState state, List<Stock> stock, ItemExchange.Tier tier) {
        Map<Identifier, Integer> uses = new HashMap<>(state.tradeUsesRemaining());
        Map<Identifier, Integer> remainders = new HashMap<>(state.dailyProductivity().refillRemainders());
        for (Stock s : stock) {
            if (s.tier() != tier) {
                continue;
            }
            // At least MIN_REFILL_USES per refill; a use handed out early is paid for from later fractions.
            int scaled = remainders.getOrDefault(s.id(), 0) + s.maxUses() * REFILL_PERCENT * REMAINDER_SCALE / 100;
            int added = Math.max(MIN_REFILL_USES, scaled / REMAINDER_SCALE);
            uses.put(s.id(), Math.min(s.maxUses(), s.current() + added));
            remainders.put(s.id(), Math.max(0, scaled - added * REMAINDER_SCALE));
        }
        DailyProductivity meters = state.dailyProductivity().with(tier, 0).withRefillRemainders(remainders);
        return new VillagerState(
                state.level(), state.xp(), state.unspentUpgradePoints(), state.ranks(),
                state.questSlotRotations(), uses, meters, state.happiness(),
                state.lastProcessedDay(), state.questSlotRerollAvailableAtTick(), state.questLog(), state.stations()
        );
    }

    private static VillagerState withMeters(VillagerState state, DailyProductivity meters, long lastProcessedDay) {
        return new VillagerState(
                state.level(), state.xp(), state.unspentUpgradePoints(), state.ranks(),
                state.questSlotRotations(), state.tradeUsesRemaining(), meters, state.happiness(),
                lastProcessedDay, state.questSlotRerollAvailableAtTick(), state.questLog(), state.stations()
        );
    }

    /** Debug: fills every unlocked trade to its max right away (/vo restock). */
    public static void restock(Villager villager) {
        VillagerStateAccess access = VillagerStateAccess.of(villager);
        VillagerState state = access.getState();

        Map<Identifier, Integer> newUses = new HashMap<>(state.tradeUsesRemaining());
        for (Stock stock : unlockedStock(villager, state)) {
            newUses.put(stock.id(), stock.maxUses());
        }

        access.setState(new VillagerState(
                state.level(), state.xp(), state.unspentUpgradePoints(), state.ranks(),
                state.questSlotRotations(), newUses, state.dailyProductivity(), state.happiness(),
                state.lastProcessedDay(), state.questSlotRerollAvailableAtTick(), state.questLog(), state.stations()
        ));
    }
}
