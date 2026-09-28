package com.villageroverhaul.trade;

import com.villageroverhaul.api.ExtensionHooks;
import com.villageroverhaul.api.SectionDefinition;
import com.villageroverhaul.api.SectionOffer;
import com.villageroverhaul.data.ExplorerMap;
import com.villageroverhaul.data.ItemExchange;
import com.villageroverhaul.data.ModDataPackRegistries;
import com.villageroverhaul.menu.VillagerMenu;
import com.villageroverhaul.mixin.VillagerAccessor;
import com.villageroverhaul.network.VillagerOffersPayload;
import com.villageroverhaul.progression.ProgressionService;
import com.villageroverhaul.section.RowView;
import com.villageroverhaul.section.SectionView;
import com.villageroverhaul.section.VillagerSections;
import com.villageroverhaul.state.VillagerState;
import com.villageroverhaul.state.VillagerStateAccess;
import com.villageroverhaul.work.RestockService;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Projects our VillagerState (the single source of truth for stock and ranks) and the section logics onto the
 * villager's own Vanilla MerchantOffers list. Keeping our offers in that exact list is what lets Vanilla's own
 * features act on them unchanged: Hero of the Village discounts (updateSpecialPrices), the villager holding
 * offer items (ShowTradesToPlayer), the "no offers" head shake, MerchantContainer's out-of-stock check and the
 * whole result-slot/shift-click machinery.
 *
 * The list is a derived view, never the other way round: rank changes rescale cost/output/max uses, which
 * MerchantOffer can't express (those fields are final), so the view is rebuilt on every state change - see
 * AttachmentVillagerStateAccess.setState. Order: the open sections in section order, each with its logic's
 * rows - VillagerOffersPayload carries the sections to the client.
 *
 * priceMultiplier is 0, which disables Vanilla's gossip-reputation discount and demand surcharge while leaving
 * Hero of the Village (based only on the base cost) intact. xp is 0, so Vanilla's trade XP never levels the
 * villager - leveling goes through our own work-based progression.
 */
public final class VillagerOffers {

    /** Which section and row a live offer stands for. */
    public record Source(SectionDefinition section, SectionOffer offer) {
    }

    private record Built(MerchantOffers offers, List<Source> sources, List<SectionView> sections) {
    }

    private VillagerOffers() {
    }

    /** Replaces the target list's contents with our offers - used by the updateTrades mixin. */
    public static void fill(Villager villager, MerchantOffers target) {
        target.clear();
        target.addAll(build(villager).offers());
    }

    /**
     * Rebuilds the live list in place after a state change. While someone is trading, Vanilla's own
     * updateSpecialPrices is re-run for them - the fresh offers start without a discount, and this
     * recomputes Hero of the Village against the (possibly rescaled) new base cost, exactly as
     * startTrading did. Then the open menu re-resolves its active offer and the client gets the list.
     */
    public static void refresh(Villager villager) {
        if (villager.level().isClientSide()) {
            return;
        }
        fill(villager, villager.getOffers());

        if (villager.getTradingPlayer() instanceof ServerPlayer player) {
            ((VillagerAccessor) villager).villageroverhaul$updateSpecialPrices(player);
            if (player.containerMenu instanceof VillagerMenu menu && menu.villager() == villager) {
                menu.refreshActiveOffer();
                sendTo(player, menu, villager);
            }
        }
    }

    public static void sendTo(ServerPlayer player, VillagerMenu menu, Villager villager) {
        Built built = build(villager);
        List<RowView> rows = built.sources().stream()
                .map(source -> new RowView(source.offer().slot(), source.offer().exchange().baseOutputCount()))
                .toList();
        PacketDistributor.sendToPlayer(player, new VillagerOffersPayload(menu.containerId, villager.getOffers(), built.sections(), rows));
    }

    /**
     * Which section row a live offer object stands for, by identity (MerchantOffer has no id). Valid because the
     * live list is always rebuilt from the current state, so rebuilding the sources from that same state yields
     * the same order.
     */
    public static Optional<Source> sourceOf(Villager villager, MerchantOffer offer) {
        int index = villager.getOffers().indexOf(offer);
        List<Source> sources = build(villager).sources();
        return index >= 0 && index < sources.size() ? Optional.of(sources.get(index)) : Optional.empty();
    }

    /** The current row offering this entry, in any open section. */
    public static Optional<Source> sourceOf(Villager villager, Identifier entryId) {
        return build(villager).sources().stream().filter(source -> source.offer().exchange().id().equals(entryId)).findFirst();
    }

    private static Built build(Villager villager) {
        VillagerState state = VillagerStateAccess.of(villager).getState();
        MerchantOffers offers = new MerchantOffers();
        List<Source> sources = new ArrayList<>();
        List<SectionView> views = new ArrayList<>();
        Registry<ItemExchange> registry = villager.level().registryAccess().lookupOrThrow(ModDataPackRegistries.EXCHANGE);

        for (SectionDefinition section : VillagerSections.open(villager, state)) {
            List<SectionOffer> rows = section.display() == SectionDefinition.Display.LIST
                    ? section.logic().offers(villager, state, section)
                    : List.of();
            for (SectionOffer row : rows) {
                offers.add(toOffer(villager, registry, row.exchange()));
                sources.add(new Source(section, row));
            }
            boolean showsMeter = section.meterVisible() && VillagerSections.stationOf(state, section).isPresent();
            views.add(new SectionView(section.id(), rows.size(), ProgressionService.upgradeCost(villager, state, section),
                    showsMeter ? RestockService.meterPoints(villager, state, section) : 0));
        }
        return new Built(offers, sources, views);
    }

    /**
     * The rank discount (2026-09-24) is Vanilla's own special price: the base cost is the unscaled Base
     * price and the difference to the scaled price goes into specialPriceDiff, so the price shows
     * struck through like a cured-zombie discount. Hero of the Village then adds on top in
     * updateSpecialPrices. Vanilla resets specialPriceDiff to 0 when trading stops, which is why the
     * list is rebuilt each time trading starts (VillagerTradingMixin).
     *
     * An explorer-map entry shows the real map once found, a placeholder before, and is sold out if nothing
     * was found (api/ExtensionHooks.explorerMap).
     */
    private static MerchantOffer toOffer(Villager villager, Registry<ItemExchange> registry, ResolvedExchange exchange) {
        int uses = Math.max(0, exchange.maxUses() - exchange.usesRemaining());
        ItemStack result = exchange.resultStack().map(ItemStack::copy).orElseGet(() -> exchange.output().toStack(villager.registryAccess()));
        Optional<ExplorerMap> explorerMap = registry.getOptional(exchange.id()).flatMap(ItemExchange::explorerMap);
        if (explorerMap.isPresent()) {
            ExtensionHooks.MapOutput map = ExtensionHooks.explorerMap(villager, exchange.id(), explorerMap.get());
            result = map.stack();
            if (map.soldOut()) {
                uses = exchange.maxUses();
            }
        }
        MerchantOffer offer = new MerchantOffer(
                RequiredEnchantmentCost.of(exchange.basePrice(), villager.registryAccess()),
                exchange.secondInput().map(second -> new ItemCost(second.item(), second.count())),
                result,
                uses,
                exchange.maxUses(),
                0,
                0.0F
        );
        offer.setSpecialPriceDiff(exchange.input().count() - exchange.basePrice().count());
        return offer;
    }
}
