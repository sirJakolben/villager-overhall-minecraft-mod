package com.villageroverhaul.trade;

import com.villageroverhaul.api.ExtensionHooks;
import com.villageroverhaul.client.ui.VillagerMenu;
import com.villageroverhaul.core.QuestOffer;
import com.villageroverhaul.core.ResolvedExchange;
import com.villageroverhaul.data.ExplorerMap;
import com.villageroverhaul.data.ItemExchange;
import com.villageroverhaul.data.ModDataPackRegistries;
import com.villageroverhaul.mixin.VillagerAccessor;
import com.villageroverhaul.network.VillagerOffersPayload;
import com.villageroverhaul.progression.ProgressionService;
import com.villageroverhaul.quest.QuestProviderImpl;
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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Projects our VillagerState (the single source of truth for uses, ranks, quest rotations) onto the
 * villager's own Vanilla MerchantOffers list. Keeping our trades in that exact list is what lets
 * Vanilla's own features act on them unchanged: Hero of the Village discounts (updateSpecialPrices),
 * the villager holding offer items (ShowTradesToPlayer), the "no offers" head shake, MerchantContainer's
 * out-of-stock check and the whole result-slot/shift-click machinery.
 *
 * The list is a derived view, never the other way round: rank changes rescale cost/output/max uses,
 * which MerchantOffer can't express (those fields are final), so the view is rebuilt on every state
 * change - see AttachmentVillagerStateAccess.setState. Order is fixed: quests, then basic trades, then
 * master trades, each in unlock order - VillagerOffersPayload carries the section sizes to the client.
 *
 * priceMultiplier is 0, which disables Vanilla's gossip-reputation discount and demand surcharge
 * while leaving Hero of the Village (based only on the base cost) intact. xp is 0, so Vanilla's
 * trade XP never levels the villager - leveling goes through our own work-based progression.
 */
public final class VillagerOffers {

    public sealed interface Source permits TradeSource, QuestSource {
    }

    public record TradeSource(Identifier tradeId) implements Source {
    }

    public record QuestSource(int slot) implements Source {
    }

    private record Built(MerchantOffers offers, List<Source> sources, int questCount, int basicCount) {
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
        List<Integer> questSlots = built.sources().stream()
                .filter(source -> source instanceof QuestSource)
                .map(source -> ((QuestSource) source).slot())
                .toList();
        PacketDistributor.sendToPlayer(player, new VillagerOffersPayload(
                menu.containerId, villager.getOffers(), questSlots, built.basicCount(), ProgressionService.rankCaps(villager)));
    }

    /**
     * Which trade/quest a live offer object stands for, by identity (MerchantOffer has no id). Valid
     * because the live list is always rebuilt from the current state, so rebuilding the sources from
     * that same state yields the same order.
     */
    public static Optional<Source> sourceOf(Villager villager, MerchantOffer offer) {
        int index = villager.getOffers().indexOf(offer);
        List<Source> sources = build(villager).sources();
        return index >= 0 && index < sources.size() ? Optional.of(sources.get(index)) : Optional.empty();
    }

    private static Built build(Villager villager) {
        MerchantOffers offers = new MerchantOffers();
        List<Source> sources = new ArrayList<>();

        List<QuestOffer> quests = new QuestProviderImpl().getCurrentOffers(villager);
        for (QuestOffer quest : quests) {
            // No uses left = today's quest limit is used up, the row shows greyed out.
            ItemStack questResult = quest.exchange().resultStack().map(ItemStack::copy).orElseGet(() -> quest.exchange().output().toStack(villager.registryAccess()));
            offers.add(toOffer(villager, quest.exchange(), questResult, 1 - quest.exchange().usesRemaining(), 1));
            sources.add(new QuestSource(quest.slot()));
        }

        List<ResolvedExchange> trades = new ArrayList<>(new TradeProviderImpl().getAvailableTrades(villager));
        // Stable sort: only moves master trades behind basic ones, keeping TradeProviderImpl's unlock order within each.
        trades.sort(Comparator.comparing((ResolvedExchange e) -> e.tier() == ItemExchange.Tier.MASTER));
        Registry<ItemExchange> registry = villager.level().registryAccess().lookupOrThrow(ModDataPackRegistries.TRADE);
        int basicCount = 0;
        for (ResolvedExchange trade : trades) {
            int uses = Math.max(0, trade.maxUses() - trade.usesRemaining());
            ItemStack result = trade.resultStack().map(ItemStack::copy).orElseGet(() -> trade.output().toStack(villager.registryAccess()));
            Optional<ExplorerMap> explorerMap = registry.getOptional(trade.id()).flatMap(ItemExchange::explorerMap);
            if (explorerMap.isPresent()) {
                // Explorer map: the real map once found, a placeholder before, sold out if nothing was found.
                ExtensionHooks.MapOutput map = ExtensionHooks.explorerMap(villager, trade.id(), explorerMap.get());
                result = map.stack();
                if (map.soldOut()) {
                    uses = trade.maxUses();
                }
            }
            offers.add(toOffer(villager, trade, result, uses, trade.maxUses()));
            sources.add(new TradeSource(trade.id()));
            if (trade.tier() == ItemExchange.Tier.BASIC) {
                basicCount++;
            }
        }
        return new Built(offers, sources, quests.size(), basicCount);
    }

    /**
     * The rank discount (2026-09-24) is Vanilla's own special price: the base cost is the unscaled Base
     * price and the difference to the scaled price goes into specialPriceDiff, so the price shows
     * struck through like a cured-zombie discount. Hero of the Village then adds on top in
     * updateSpecialPrices. Vanilla resets specialPriceDiff to 0 when trading stops, which is why the
     * list is rebuilt each time trading starts (VillagerTradingMixin).
     */
    private static MerchantOffer toOffer(Villager villager, ResolvedExchange exchange, ItemStack result, int uses, int maxUses) {
        MerchantOffer offer = new MerchantOffer(
                RequiredEnchantmentCost.of(exchange.basePrice(), villager.registryAccess()),
                exchange.secondInput().map(second -> new ItemCost(second.item(), second.count())),
                result,
                uses,
                maxUses,
                0,
                0.0F
        );
        offer.setSpecialPriceDiff(exchange.input().count() - exchange.basePrice().count());
        return offer;
    }
}
