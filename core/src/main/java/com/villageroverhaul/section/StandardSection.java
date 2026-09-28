package com.villageroverhaul.section;

import com.villageroverhaul.api.SectionDefinition;
import com.villageroverhaul.api.SectionOffer;
import com.villageroverhaul.data.ItemExchange;
import com.villageroverhaul.state.VillagerState;
import com.villageroverhaul.state.VillagerStateAccess;
import com.villageroverhaul.trade.ExchangeScaling;
import com.villageroverhaul.trade.ResolvedExchange;
import com.villageroverhaul.vanilla.VanillaCatalog;
import com.villageroverhaul.vanilla.VanillaScaling;
import com.villageroverhaul.work.RestockService;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The standard section behavior behind SectionLogic's defaults: every entry up to the section's rank is a trade
 * with its own stock; a trade uses one stock, a full meter refills it (RestockService). Entries come from the data
 * pack, or - for a profession nobody designed - from its Vanilla trades (VanillaCatalog).
 */
public final class StandardSection {

    private StandardSection() {
    }

    public static List<SectionOffer> offers(Villager villager, VillagerState state, SectionDefinition section) {
        int rank = state.rank(section.id());
        if (VanillaCatalog.usedBy(villager)) {
            VanillaCatalog.Catalog catalog = VanillaCatalog.of(villager);
            int stage = VanillaScaling.stageOf(catalog, section.id(), rank);
            return catalog.entries(section.id()).stream()
                    .filter(entry -> entry.unlockRank() <= rank)
                    .map(entry -> SectionOffer.of(VanillaScaling.resolve(entry, stage, state.stock())))
                    .toList();
        }
        return SectionEntries.of(villager.level().registryAccess(), villager.getVillagerData().profession().value(), section.id()).stream()
                .filter(entry -> entry.exchange().unlockRank() <= rank)
                .map(entry -> SectionOffer.of(resolve(entry.id(), entry.exchange(), rank, section, state.stock())))
                .toList();
    }

    /** Ids of every entry of every section the villager has, locked or not - what its stock may refer to. */
    public static Set<Identifier> entryIds(Villager villager) {
        Set<Identifier> ids = new HashSet<>();
        boolean vanilla = VanillaCatalog.usedBy(villager);
        for (SectionDefinition section : VillagerSections.of(villager)) {
            if (vanilla) {
                VanillaCatalog.of(villager).entries(section.id()).forEach(entry -> ids.add(entry.id()));
            } else {
                SectionEntries.of(villager.level().registryAccess(), villager.getVillagerData().profession().value(), section.id())
                        .forEach(entry -> ids.add(entry.id()));
            }
        }
        return ids;
    }

    /** A data-pack entry scaled to the section's rank, with its current stock. */
    public static ResolvedExchange resolve(Identifier id, ItemExchange exchange, int rank, SectionDefinition section, Map<Identifier, Integer> stock) {
        int maxUses = ExchangeScaling.scaleMaxUses(exchange, rank, section);
        return new ResolvedExchange(
                id,
                exchange.baseInput(),
                ExchangeScaling.scaleInput(exchange, rank, section),
                exchange.secondInput(),
                ExchangeScaling.scaleOutput(exchange, rank, section),
                stock.getOrDefault(id, RestockService.initialStock(maxUses)),
                maxUses
        );
    }

    /** One stock used; a meter held at full is released shortly after (RestockService.scheduleRelease). */
    public static void recordUse(Villager villager, SectionDefinition section, SectionOffer offer) {
        VillagerStateAccess access = VillagerStateAccess.of(villager);
        VillagerState state = access.getState();
        Map<Identifier, Integer> stock = new HashMap<>(state.stock());
        stock.put(offer.exchange().id(), Math.max(0, offer.exchange().usesRemaining() - 1));
        VillagerState used = state.withStock(stock);
        access.setState(used.withProductivity(RestockService.scheduleRelease(villager, used, section)));
    }

    /** The largest "max minus current" of the section's trades - 0 when nothing is missing. */
    public static int missingStock(Villager villager, VillagerState state, SectionDefinition section) {
        int gap = 0;
        for (SectionOffer offer : section.logic().offers(villager, state, section)) {
            gap = Math.max(gap, offer.exchange().maxUses() - offer.exchange().usesRemaining());
        }
        return gap;
    }

    public static VillagerState refill(Villager villager, VillagerState state, SectionDefinition section) {
        return RestockService.refill(state, section, section.logic().offers(villager, state, section));
    }
}
