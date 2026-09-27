package com.villageroverhaul.trade;

import com.villageroverhaul.core.ResolvedExchange;
import com.villageroverhaul.core.VillagerStateAccess;
import com.villageroverhaul.core.state.DailyProductivity;
import com.villageroverhaul.core.state.VillagerState;
import com.villageroverhaul.data.ItemAmount;
import com.villageroverhaul.data.ItemExchange;
import com.villageroverhaul.data.ModDataPackRegistries;
import com.villageroverhaul.fallback.FallbackCatalog;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Server-side trade execution - the shared core that Block D (quests) reuses, see QuestActions. */
public final class TradeActions {

    public enum Result {
        SUCCESS, UNKNOWN_TRADE, NOT_UNLOCKED, NO_USES_LEFT, CANNOT_AFFORD
    }

    private TradeActions() {
    }

    public static Result executeTrade(Villager villager, Player player, Identifier tradeId) {
        Registry<ItemExchange> registry = villager.level().registryAccess().lookupOrThrow(ModDataPackRegistries.TRADE);
        Optional<ItemExchange> maybeExchange = registry.getOptional(ResourceKey.create(ModDataPackRegistries.TRADE, tradeId));
        if (maybeExchange.isEmpty()) {
            return Result.UNKNOWN_TRADE;
        }
        ItemExchange exchange = maybeExchange.get();

        VillagerStateAccess access = VillagerStateAccess.of(villager);
        VillagerState state = access.getState();
        if (exchange.profession() != villager.getVillagerData().profession().value() || !exchange.isTradeUnlocked(state)) {
            return Result.NOT_UNLOCKED;
        }

        if (currentUsesRemaining(villager, tradeId) <= 0) {
            return Result.NO_USES_LEFT;
        }

        int rank = exchange.tradeGroupRank(state);
        ItemAmount input = ExchangeScaling.scaleInput(exchange, rank, exchange.tradeGroup());
        ItemAmount output = ExchangeScaling.scaleOutput(exchange, rank, exchange.tradeGroup());

        if (!ExchangeExecutor.canAfford(player, input, exchange.secondInput())) {
            return Result.CANNOT_AFFORD;
        }
        ExchangeExecutor.execute(player, input, exchange.secondInput(), output);
        recordUse(villager, tradeId);
        return Result.SUCCESS;
    }

    /**
     * Bookkeeping only (decrements today's remaining uses) - no item movement. Used both by
     * executeTrade above (the debug-command path) and by real trades in VillagerMenu, where
     * Vanilla's MerchantResultSlot already moved the items - see TradeEvents.
     */
    public static void recordUse(Villager villager, Identifier tradeId) {
        VillagerStateAccess access = VillagerStateAccess.of(villager);
        VillagerState state = access.getState();
        int usesRemaining = currentUsesRemaining(villager, tradeId);
        Map<Identifier, Integer> newUses = new HashMap<>(state.tradeUsesRemaining());
        newUses.put(tradeId, Math.max(0, usesRemaining - 1));
        // A held full meter of this trade's category refills shortly after the trade, see RestockService.
        DailyProductivity meters = villager.level().registryAccess().lookupOrThrow(ModDataPackRegistries.TRADE)
                .getOptional(ResourceKey.create(ModDataPackRegistries.TRADE, tradeId))
                .map(ItemExchange::tier)
                .or(() -> FallbackCatalog.of(villager).byId(tradeId).map(FallbackCatalog.Entry::tier))
                .map(tier -> RestockService.scheduleRelease(state.dailyProductivity(), tier, villager.level().getGameTime()))
                .orElse(state.dailyProductivity());
        access.setState(new VillagerState(
                state.level(), state.xp(), state.unspentUpgradePoints(), state.ranks(),
                state.questSlotRotations(), newUses, meters, state.happiness(),
                state.lastProcessedDay(), state.questSlotRerollAvailableAtTick(), state.questLog(), state.stations()
        ));
    }

    /** Resolved through TradeProviderImpl so a never-restocked trade gets the same initial stock everywhere. */
    private static int currentUsesRemaining(Villager villager, Identifier tradeId) {
        return new TradeProviderImpl().getAvailableTrades(villager).stream()
                .filter(trade -> trade.id().equals(tradeId))
                .findFirst()
                .map(ResolvedExchange::usesRemaining)
                .orElse(0);
    }
}
