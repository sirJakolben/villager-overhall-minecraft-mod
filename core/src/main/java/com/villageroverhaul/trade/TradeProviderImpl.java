package com.villageroverhaul.trade;

import com.villageroverhaul.core.ResolvedExchange;
import com.villageroverhaul.core.TradeProvider;
import com.villageroverhaul.core.VillagerStateAccess;
import com.villageroverhaul.core.state.VillagerState;
import com.villageroverhaul.data.ItemAmount;
import com.villageroverhaul.data.ItemExchange;
import com.villageroverhaul.data.ModDataPackRegistries;
import com.villageroverhaul.fallback.FallbackCatalog;
import com.villageroverhaul.fallback.FallbackScaling;
import com.villageroverhaul.progression.ProgressionService;
import com.villageroverhaul.progression.UpgradeGroup;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TradeProviderImpl implements TradeProvider {

    /**
     * A fallback profession's unlocked trades (fallback/FallbackCatalog): basic trades up to the basic rank
     * (once the basic group is open), Masteries up to the master rank - Vanilla's own rolled prices and
     * results, no scaling.
     */
    public static List<FallbackCatalog.Entry> unlockedFallbackTrades(Villager villager, VillagerState state) {
        FallbackCatalog.Catalog catalog = FallbackCatalog.of(villager);
        List<FallbackCatalog.Entry> unlocked = new ArrayList<>();
        if (ProgressionService.isGroupOpen(villager, state, UpgradeGroup.BASIC_TRADE)) {
            catalog.basic().stream().filter(e -> e.unlockRank() <= state.ranks().basicTrade()).forEach(unlocked::add);
        }
        catalog.master().stream().filter(e -> e.unlockRank() <= state.ranks().masterTrade()).forEach(unlocked::add);
        return unlocked;
    }

    @Override
    public List<ResolvedExchange> getAvailableTrades(Villager villager) {
        VillagerState state = VillagerStateAccess.of(villager).getState();
        if (FallbackCatalog.isFallback(villager)) {
            FallbackCatalog.Catalog catalog = FallbackCatalog.of(villager);
            List<ResolvedExchange> resolved = new ArrayList<>();
            for (FallbackCatalog.Entry entry : unlockedFallbackTrades(villager, state)) {
                // Price steps after the group's last unlock (FallbackScaling): unstackable results get cheaper, stackable ones bigger.
                int stage = FallbackScaling.tradeStage(entry, catalog, state.ranks());
                int maxUses = FallbackScaling.tradeMaxUses(entry.maxUses(), stage);
                boolean scalesPrice = FallbackScaling.scalesPrice(entry.result());
                ItemStack result = scalesPrice ? entry.result() : FallbackScaling.tradeYield(entry.result(), stage);
                resolved.add(new ResolvedExchange(entry.id(), entry.tier(),
                        scalesPrice ? FallbackScaling.tradePrice(entry.costA(), 0) : entry.costA(),
                        scalesPrice ? FallbackScaling.tradePrice(entry.costA(), stage) : entry.costA(),
                        scalesPrice ? FallbackScaling.tradePrice(entry.costB(), stage) : entry.costB(),
                        new ItemAmount(result.getItem(), result.getCount()),
                        state.tradeUsesRemaining().getOrDefault(entry.id(), RestockService.initialUsesRemaining(maxUses)),
                        maxUses, Optional.of(result)));
            }
            return resolved;
        }
        VillagerProfession profession = villager.getVillagerData().profession().value();
        Registry<ItemExchange> registry = villager.level().registryAccess().lookupOrThrow(ModDataPackRegistries.TRADE);
        Map<Identifier, Integer> usesRemaining = state.tradeUsesRemaining();

        List<Map.Entry<ResourceKey<ItemExchange>, ItemExchange>> unlocked = new ArrayList<>();
        for (Map.Entry<ResourceKey<ItemExchange>, ItemExchange> entry : registry.entrySet()) {
            ItemExchange exchange = entry.getValue();
            if (exchange.profession() == profession && exchange.isTradeUnlocked(state)) {
                unlocked.add(entry);
            }
        }
        // Unlock order (rank, then id within one rank), so the list reads chronologically: each
        // upgrade appends its new trades at the bottom instead of mixing them in by registry order.
        unlocked.sort(Comparator.comparingInt((Map.Entry<ResourceKey<ItemExchange>, ItemExchange> e) -> e.getValue().unlockRank())
                .thenComparing(e -> e.getKey().identifier().toString()));

        List<ResolvedExchange> resolved = new ArrayList<>();
        for (Map.Entry<ResourceKey<ItemExchange>, ItemExchange> entry : unlocked) {
            Identifier id = entry.getKey().identifier();
            ItemExchange exchange = entry.getValue();

            int rank = exchange.tradeGroupRank(state);
            int maxUses = ExchangeScaling.scaleMaxUses(exchange, rank, exchange.tradeGroup());
            resolved.add(new ResolvedExchange(
                    id,
                    exchange.tier(),
                    exchange.baseInput(),
                    ExchangeScaling.scaleInput(exchange, rank, exchange.tradeGroup()),
                    exchange.secondInput(),
                    ExchangeScaling.scaleOutput(exchange, rank, exchange.tradeGroup()),
                    usesRemaining.getOrDefault(id, RestockService.initialUsesRemaining(maxUses)),
                    maxUses
            ));
        }
        return resolved;
    }
}
