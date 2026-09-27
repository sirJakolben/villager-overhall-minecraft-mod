package com.villageroverhaul.quest;

import com.villageroverhaul.core.QuestOffer;
import com.villageroverhaul.core.QuestProvider;
import com.villageroverhaul.core.ResolvedExchange;
import com.villageroverhaul.core.DayClock;
import com.villageroverhaul.core.VillagerStateAccess;
import com.villageroverhaul.core.state.QuestLog;
import com.villageroverhaul.core.state.VillagerState;
import com.villageroverhaul.progression.UpgradeGroup;
import com.villageroverhaul.trade.ExchangeExecutor;
import com.villageroverhaul.trade.ExchangeScaling;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Server-side quest turn-in and reroll, reusing ExchangeExecutor from Block C - see the shared-logic
 * note in README.md. Completing a slot rotates it to a new pool pick for free; rerolling
 * does the same but costs a per-slot cooldown that shrinks as the quest group's rank rises - see
 * README.md. Ticks stand in for the not-yet-built real day-boundary
 * trigger (Block F), the same way RestockService's manual call does for trades.
 */
public final class QuestActions {

    private static final long TICKS_PER_DAY = 24000L;
    private static final int BASE_REROLL_COOLDOWN_DAYS = 5;
    private static final int MAX_RANK_REROLL_COOLDOWN_DAYS = 1;
    private static final long HIDDEN_UNTIL_TOMORROW = Long.MAX_VALUE;
    /** Pause between turning in a quest and its slot showing the next one (2 s). */
    public static final int NEXT_QUEST_DELAY_TICKS = 40;

    public enum Result {
        SUCCESS, NO_OFFER, LIMIT_REACHED, CANNOT_AFFORD, ON_COOLDOWN
    }

    private static final QuestProvider PROVIDER = new QuestProviderImpl();

    private QuestActions() {
    }

    public static Result completeOffer(Villager villager, Player player, int slot) {
        Optional<QuestOffer> offer = findOffer(villager, slot);
        if (offer.isEmpty()) {
            return Result.NO_OFFER;
        }
        ResolvedExchange exchange = offer.get().exchange();
        if (exchange.usesRemaining() <= 0) {
            return Result.LIMIT_REACHED;
        }

        if (!ExchangeExecutor.canAfford(player, exchange.input(), exchange.secondInput())) {
            return Result.CANNOT_AFFORD;
        }
        ExchangeExecutor.execute(player, exchange.input(), exchange.secondInput(), exchange.output());
        recordCompletion(villager, slot);
        return Result.SUCCESS;
    }

    /**
     * Bookkeeping only (rotates the slot to its next pool pick) - no item movement. Used both by
     * completeOffer above (the debug-command path) and by VillagerMenu's real trade slots, where
     * the Slot machinery itself already moved the items - see ExchangeResultSlot.
     */
    public static void recordCompletion(Villager villager, int slot) {
        VillagerStateAccess access = VillagerStateAccess.of(villager);
        VillagerState state = access.getState();
        Map<Identifier, Integer> newRotations = new HashMap<>(state.questSlotRotations());
        Identifier key = QuestSlots.rotationKey(slot);

        // Count towards today's limit and hide the slot briefly before its next quest pops up - or, once
        // the limit is used up, until tomorrow; quests still shown stay completable. The permanent quest
        // neither rotates nor hides, it only counts - and greys out at the limit (QuestProviderImpl).
        QuestLog log = state.questLog();
        long today = villager.level() instanceof ServerLevel level ? DayClock.today(level) : log.day();
        int completed = log.day() == today ? log.completedToday() + 1 : 1;
        Map<Identifier, Long> hidden = new HashMap<>(log.hiddenSlots());
        if (!QuestSlots.isPermanent(slot)) {
            newRotations.merge(key, 1, Integer::sum);
            boolean limitReached = completed >= QuestSlots.dailyLimit(villager, state.ranks().quest());
            hidden.put(key, limitReached ? HIDDEN_UNTIL_TOMORROW : villager.level().getGameTime() + NEXT_QUEST_DELAY_TICKS);
        }
        access.setState(withQuestLog(withQuestSlots(state, newRotations, state.questSlotRerollAvailableAtTick()),
                new QuestLog(today, completed, hidden)));
    }

    /** True once today's quest limit (by quest rank, QuestSlots.dailyLimit) is used up - no new quests and no permanent quest until the next day. */
    public static boolean isLimitReached(Villager villager, VillagerState state) {
        return state.questLog().completedToday() >= QuestSlots.dailyLimit(villager, state.ranks().quest());
    }

    /**
     * Shows hidden slots whose pause is over and resets the count on a new day. Offers are a pure view
     * of the state, so this is what makes them reappear: called every tick while someone trades
     * (VillagerMenu.broadcastChanges), when the trade screen opens, and by the periodic work scan.
     */
    public static void updateQuestLog(Villager villager) {
        if (!(villager.level() instanceof ServerLevel level)) {
            return;
        }
        VillagerStateAccess access = VillagerStateAccess.of(villager);
        VillagerState state = access.getState();
        QuestLog log = state.questLog();
        long now = level.getGameTime();
        boolean slotDue = log.hiddenSlots().values().stream().anyMatch(readyAt -> readyAt <= now);
        boolean newDay = log.completedToday() > 0 && log.day() != DayClock.today(level);
        // A quest rank-up can raise today's limit after it was reached - slots waiting for tomorrow come back.
        boolean limitRaised = !isLimitReached(villager, state) && log.hiddenSlots().containsValue(HIDDEN_UNTIL_TOMORROW);
        if (!slotDue && !newDay && !limitRaised) {
            return;
        }
        boolean releaseWaiting = newDay || limitRaised;
        Map<Identifier, Long> hidden = new HashMap<>(log.hiddenSlots());
        hidden.values().removeIf(readyAt -> readyAt <= now || (releaseWaiting && readyAt == HIDDEN_UNTIL_TOMORROW));
        access.setState(withQuestLog(state, new QuestLog(log.day(), newDay ? 0 : log.completedToday(), hidden)));
    }

    public static Result reroll(Villager villager, int slot) {
        VillagerStateAccess access = VillagerStateAccess.of(villager);
        VillagerState state = access.getState();
        if (QuestSlots.isPermanent(slot) || findOffer(villager, slot).isEmpty()) {
            return Result.NO_OFFER;
        }

        Identifier key = QuestSlots.rotationKey(slot);
        long now = villager.level().getGameTime();
        long availableAt = state.questSlotRerollAvailableAtTick().getOrDefault(key, 0L);
        if (now < availableAt) {
            return Result.ON_COOLDOWN;
        }

        int cooldownDays = rerollCooldownDays(state);

        Map<Identifier, Integer> newRotations = new HashMap<>(state.questSlotRotations());
        newRotations.merge(key, 1, Integer::sum);
        Map<Identifier, Long> newRerollTimes = new HashMap<>(state.questSlotRerollAvailableAtTick());
        newRerollTimes.put(key, now + cooldownDays * TICKS_PER_DAY);
        access.setState(withQuestSlots(state, newRotations, newRerollTimes));
        return Result.SUCCESS;
    }

    /** Full reroll cooldown at the villager's current quest rank: BASE_REROLL_COOLDOWN_DAYS down to MAX_RANK_REROLL_COOLDOWN_DAYS. */
    public static int rerollCooldownDays(VillagerState state) {
        return ExchangeScaling.lerp(
                BASE_REROLL_COOLDOWN_DAYS, MAX_RANK_REROLL_COOLDOWN_DAYS, state.ranks().quest(), UpgradeGroup.QUEST.maxRank()
        );
    }

    /** Ticks remaining until this slot's reroll button is available again, 0 if it already is. */
    public static long rerollCooldownTicksRemaining(VillagerState state, Villager villager, int slot) {
        long availableAt = state.questSlotRerollAvailableAtTick().getOrDefault(QuestSlots.rotationKey(slot), 0L);
        return Math.max(0L, availableAt - villager.level().getGameTime());
    }

    private static Optional<QuestOffer> findOffer(Villager villager, int slot) {
        List<QuestOffer> offers = PROVIDER.getCurrentOffers(villager);
        return offers.stream().filter(o -> o.slot() == slot).findFirst();
    }

    private static VillagerState withQuestSlots(VillagerState state, Map<Identifier, Integer> rotations, Map<Identifier, Long> rerollTimes) {
        return new VillagerState(
                state.level(), state.xp(), state.unspentUpgradePoints(), state.ranks(),
                rotations, state.tradeUsesRemaining(), state.dailyProductivity(), state.happiness(),
                state.lastProcessedDay(), rerollTimes, state.questLog(), state.stations()
        );
    }

    private static VillagerState withQuestLog(VillagerState state, QuestLog log) {
        return new VillagerState(
                state.level(), state.xp(), state.unspentUpgradePoints(), state.ranks(),
                state.questSlotRotations(), state.tradeUsesRemaining(), state.dailyProductivity(), state.happiness(),
                state.lastProcessedDay(), state.questSlotRerollAvailableAtTick(), log, state.stations()
        );
    }
}
