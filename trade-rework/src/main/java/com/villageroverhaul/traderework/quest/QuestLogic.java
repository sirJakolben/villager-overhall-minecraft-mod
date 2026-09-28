package com.villageroverhaul.traderework.quest;

import com.villageroverhaul.api.SectionDefinition;
import com.villageroverhaul.api.SectionLogic;
import com.villageroverhaul.api.SectionOffer;
import com.villageroverhaul.data.ItemAmount;
import com.villageroverhaul.data.ItemExchange;
import com.villageroverhaul.section.SectionEntries;
import com.villageroverhaul.state.VillagerState;
import com.villageroverhaul.state.VillagerStateAccess;
import com.villageroverhaul.trade.ExchangeScaling;
import com.villageroverhaul.trade.ResolvedExchange;
import com.villageroverhaul.work.DayClock;
import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The Trade Rework's quests: instead of listing every entry, each open slot (QuestSlots) shows one entry of its
 * pool, picked deterministically from the villager's UUID, the slot index and that slot's rotation counter
 * (QuestState) - never stored, recomputed on every access. The permanent slot always shows the profession's first
 * permanent quest and never rotates. What a permanent quest asks for never appears in the easy or hard pool
 * (2026-09-26, permanentItems / allowedVariants).
 *
 * Completing a slot rotates it to its next pick after a short pause (NEXT_QUEST_DELAY_TICKS); every quest counts
 * toward today's limit. Once it is used up, completed rotating slots stay empty until tomorrow while the ones
 * still shown stay completable (soft limit, 2026-09-23); only the permanent quest, which never leaves, is greyed
 * out (no uses left). A reroll (the row's button, action REROLL) rotates a slot for free but starts a per-slot
 * cooldown that shrinks as the quest rank rises. Quests don't restock - their meter stays unused.
 */
public final class QuestLogic implements SectionLogic {

    /** The row action of the reroll button (network/SectionActionPayload). */
    public static final int REROLL = 0;

    private static final long TICKS_PER_DAY = 24000L;
    private static final int BASE_REROLL_COOLDOWN_DAYS = 5;
    private static final int MAX_RANK_REROLL_COOLDOWN_DAYS = 1;
    /** Pause between turning in a quest and its slot showing the next one (2 s). */
    private static final int NEXT_QUEST_DELAY_TICKS = 40;

    @Override
    public List<SectionOffer> offers(Villager villager, VillagerState state, SectionDefinition section) {
        int rank = state.rank(section.id());
        QuestState quests = QuestState.of(villager);
        List<SectionEntries.Entry> entries = SectionEntries.of(villager.level().registryAccess(), villager.getVillagerData().profession().value(), section.id());
        Set<Item> permanentItems = permanentItems(entries);
        Map<String, List<SectionEntries.Entry>> pools = new HashMap<>();
        for (SectionEntries.Entry entry : entries) {
            ItemExchange exchange = entry.exchange();
            String pool = poolOf(exchange);
            if (exchange.unlockRank() > rank || !pool.equals(QuestSlots.PERMANENT) && allowedVariants(exchange, permanentItems).isEmpty()) {
                continue;
            }
            pools.computeIfAbsent(pool, p -> new ArrayList<>()).add(entry);
        }

        boolean limitReached = isLimitReached(villager, state, section);
        List<SectionOffer> offers = new ArrayList<>(QuestSlots.SLOT_COUNT);
        Map<String, Set<Integer>> usedIndices = new HashMap<>();
        for (int slot = 0; slot < QuestSlots.SLOT_COUNT; slot++) {
            String poolName = QuestSlots.poolOf(slot);
            List<SectionEntries.Entry> pool = pools.getOrDefault(poolName, List.of());
            if (!QuestSlots.isOpen(slot, rank, villager.getVillagerData().profession()) || pool.isEmpty()) {
                continue;
            }

            int index = 0;
            int seed = 0;
            if (!poolName.equals(QuestSlots.PERMANENT)) {
                Set<Integer> used = usedIndices.computeIfAbsent(poolName, p -> new HashSet<>());
                seed = villager.getUUID().hashCode() + slot * 1_000_003 + quests.rotation(slot);
                index = Math.floorMod(seed, pool.size());
                for (int attempt = 0; attempt < pool.size() && used.contains(index); attempt++) {
                    index = Math.floorMod(index + 1, pool.size());
                }
                used.add(index);
            }
            if (quests.isPaused(slot)) {
                continue; // just turned in - the next quest pops up after a short pause, see refresh (a paused slot still reserves its pick)
            }

            SectionEntries.Entry picked = pool.get(index);
            ItemExchange exchange = picked.exchange();
            Item inputItem = pickVariant(exchange, seed, permanentItems);
            Optional<ResourceKey<Enchantment>> inputEnchantment = pickEnchantmentVariant(exchange, seed);
            offers.add(new SectionOffer(new ResolvedExchange(
                    picked.id(),
                    new ItemAmount(inputItem, exchange.baseInput().count(), inputEnchantment, exchange.baseInput().mob()),
                    new ItemAmount(inputItem, ExchangeScaling.scaleInput(exchange, rank, section).count(), inputEnchantment, exchange.baseInput().mob()),
                    exchange.secondInput(),
                    exchange.baseOutput().count(),
                    ExchangeScaling.scaleOutput(exchange, rank, section),
                    limitReached && poolName.equals(QuestSlots.PERMANENT) ? 0 : 1, 1
            ), slot));
        }
        return offers;
    }

    /**
     * After a turn-in: counts toward today's limit and pauses the slot briefly before its next quest pops up - or,
     * once the limit is used up, until tomorrow. The permanent quest neither rotates nor pauses, it only counts -
     * and greys out at the limit.
     */
    @Override
    public void onUse(Villager villager, SectionDefinition section, SectionOffer offer) {
        QuestState quests = QuestState.of(villager);
        long today = villager.level() instanceof ServerLevel level ? DayClock.today(level) : quests.day();
        int completed = quests.day() == today ? quests.completedToday() + 1 : 1;
        Map<Integer, Long> paused = new HashMap<>(quests.pausedUntil());
        int slot = offer.slot();
        if (slot != QuestSlots.PERMANENT_SLOT) {
            quests = quests.withRotated(slot);
            boolean limitReached = completed >= QuestSlots.dailyLimit(villager, rank(villager, section));
            paused.put(slot, limitReached ? QuestState.PAUSED_UNTIL_TOMORROW : villager.level().getGameTime() + NEXT_QUEST_DELAY_TICKS);
        }
        QuestState.set(villager, quests.withLog(today, completed, paused));
    }

    /** Quests don't restock. */
    @Override
    public int missingStock(Villager villager, VillagerState state, SectionDefinition section) {
        return 0;
    }

    /**
     * Shows paused slots whose pause is over and resets the count on a new day - also when a quest rank-up raised
     * today's limit after it was reached, the slots waiting for tomorrow come back.
     */
    @Override
    public void refresh(Villager villager, SectionDefinition section) {
        if (!(villager.level() instanceof ServerLevel level)) {
            return;
        }
        QuestState quests = QuestState.of(villager);
        long now = level.getGameTime();
        boolean slotDue = quests.pausedUntil().values().stream().anyMatch(readyAt -> readyAt <= now);
        boolean newDay = quests.completedToday() > 0 && quests.day() != DayClock.today(level);
        boolean limitRaised = quests.completedToday() < QuestSlots.dailyLimit(villager, rank(villager, section))
                && quests.pausedUntil().containsValue(QuestState.PAUSED_UNTIL_TOMORROW);
        if (!slotDue && !newDay && !limitRaised) {
            return;
        }
        boolean releaseWaiting = newDay || limitRaised;
        Map<Integer, Long> paused = new HashMap<>(quests.pausedUntil());
        paused.values().removeIf(readyAt -> readyAt <= now || (releaseWaiting && readyAt == QuestState.PAUSED_UNTIL_TOMORROW));
        QuestState.set(villager, quests.withLog(quests.day(), newDay ? 0 : quests.completedToday(), paused));
    }

    /** Reroll: rotates the slot for free, then its button waits rerollCooldownDays. */
    @Override
    public void onAction(Villager villager, ServerPlayer player, SectionDefinition section, int slot, int action) {
        VillagerState state = VillagerStateAccess.of(villager).getState();
        if (action != REROLL || slot == QuestSlots.PERMANENT_SLOT
                || offers(villager, state, section).stream().noneMatch(offer -> offer.slot() == slot)
                || rerollCooldownTicks(QuestState.of(villager), villager, slot) > 0) {
            return;
        }
        long readyAt = villager.level().getGameTime() + rerollCooldownDays(state.rank(section.id()), section) * TICKS_PER_DAY;
        QuestState.set(villager, QuestState.of(villager).withRotated(slot).withRerollReadyAt(slot, readyAt));
    }

    /** Fresh slots: first quests again, no reroll cooldown, no turn-ins today. */
    @Override
    public void onReset(Villager villager, SectionDefinition section) {
        QuestState.set(villager, QuestState.EMPTY);
    }

    @Override
    public List<String> describe(Villager villager, VillagerState state, SectionDefinition section) {
        QuestState quests = QuestState.of(villager);
        long today = villager.level() instanceof ServerLevel level ? DayClock.today(level) : quests.day();
        int doneToday = quests.day() == today ? quests.completedToday() : 0;
        return List.of("Completed today: " + doneToday + "/" + QuestSlots.dailyLimit(villager, state.rank(section.id()))
                + ", slots pausing: " + quests.pausedUntil().keySet() + ", reroll ready at: " + quests.rerollReadyAt());
    }

    private static int rank(Villager villager, SectionDefinition section) {
        return VillagerStateAccess.of(villager).getState().rank(section.id());
    }

    private static boolean isLimitReached(Villager villager, VillagerState state, SectionDefinition section) {
        return QuestState.of(villager).completedToday() >= QuestSlots.dailyLimit(villager, state.rank(section.id()));
    }

    /** Full reroll cooldown at this quest rank: BASE_REROLL_COOLDOWN_DAYS down to MAX_RANK_REROLL_COOLDOWN_DAYS. */
    public static int rerollCooldownDays(int rank, SectionDefinition section) {
        return ExchangeScaling.lerp(BASE_REROLL_COOLDOWN_DAYS, MAX_RANK_REROLL_COOLDOWN_DAYS, rank, section.maxRank());
    }

    /** Ticks until this slot's reroll button is ready again, 0 if it already is. */
    public static long rerollCooldownTicks(QuestState quests, Villager villager, int slot) {
        return Math.max(0L, quests.rerollReadyAt().getOrDefault(slot, 0L) - villager.level().getGameTime());
    }

    private static String poolOf(ItemExchange exchange) {
        return exchange.pool().orElse(QuestSlots.EASY);
    }

    /**
     * A category quest (input_variants) asks for one item of its category, chosen from the same seed as
     * the slot's pick - so it stays put until the slot rotates. Scrambled so it doesn't move in lockstep
     * with the pool index.
     */
    private static Item pickVariant(ItemExchange exchange, int seed, Set<Item> permanentItems) {
        List<Item> variants = poolOf(exchange).equals(QuestSlots.PERMANENT)
                ? (exchange.inputVariants().isEmpty() ? List.of(exchange.baseInput().item()) : exchange.inputVariants())
                : allowedVariants(exchange, permanentItems);
        return variants.get(Math.floorMod(HashCommon.murmurHash3(seed), variants.size()));
    }

    /**
     * Rule (2026-09-26): what a profession's permanent quest asks for never shows up in its easy or hard
     * pool. The items its permanent quests ask for (base input and any variants).
     */
    private static Set<Item> permanentItems(List<SectionEntries.Entry> entries) {
        Set<Item> items = new HashSet<>();
        for (SectionEntries.Entry entry : entries) {
            if (poolOf(entry.exchange()).equals(QuestSlots.PERMANENT)) {
                items.add(entry.exchange().baseInput().item());
                items.addAll(entry.exchange().inputVariants());
            }
        }
        return items;
    }

    /**
     * The items a rotating quest may ask for: its variants (or its base input) minus the permanent quest's
     * items. A single-item quest for a permanent item is left out entirely; a category quest just loses
     * that variant.
     */
    private static List<Item> allowedVariants(ItemExchange exchange, Set<Item> permanentItems) {
        List<Item> variants = exchange.inputVariants().isEmpty() ? List.of(exchange.baseInput().item()) : exchange.inputVariants();
        return variants.stream().filter(item -> !permanentItems.contains(item)).toList();
    }

    /** Same for input_enchantment_variants - scrambled once more so it doesn't move in lockstep with pickVariant. */
    private static Optional<ResourceKey<Enchantment>> pickEnchantmentVariant(ItemExchange exchange, int seed) {
        List<ResourceKey<Enchantment>> variants = exchange.inputEnchantmentVariants();
        if (variants.isEmpty()) {
            return exchange.baseInput().enchantment();
        }
        return Optional.of(variants.get(Math.floorMod(HashCommon.murmurHash3(HashCommon.murmurHash3(seed)), variants.size())));
    }
}
