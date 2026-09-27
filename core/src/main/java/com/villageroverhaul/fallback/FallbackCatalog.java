package com.villageroverhaul.fallback;

import com.villageroverhaul.claim.ProfessionStations;
import com.villageroverhaul.data.ItemAmount;
import com.villageroverhaul.data.ItemExchange;
import com.villageroverhaul.data.ModDataPackRegistries;
import com.villageroverhaul.VillagerOverhaulMod;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The non-implementation rule (2026-09-27, villager-overhaul-projektrahmen.md): every profession we haven't
 * designed ourselves - Vanilla ones like the farmer, and any profession another mod adds - still runs on our
 * system, built from its own Vanilla trade sets:
 * - every trade of every Vanilla level (1-5, the whole pool, not Vanilla's random pick), in Vanilla order;
 *   a Vanilla villager-type restriction still applies
 * - a trade that exists in more than MAX_VARIANTS colour variants (16 wools, carpets, beds ...) keeps one
 *   of them, picked per villager
 * - trades that give emeralds become permanent quests, the rest are trades; the last (up to) four trades
 *   become the Masteries, all others the basic trades
 * - each rank of a group unlocks exactly the next one in that order - quest rank 0 / basic rank 0 the first,
 *   master rank 1 the first mastery - and the group's rank cap is its number of entries (FallbackRanks)
 * - prices, amounts and stock are Vanilla's own, rolled once (enchantments, dyes ...) with a random seeded
 *   by the villager and the trade, so every rebuild gives the same offer; nothing scales with rank
 *
 * The catalog is rebuilt from that seed on demand and kept in memory only; a datapack reload clears it.
 */
@EventBusSubscriber(modid = VillagerOverhaulMod.MODID)
public final class FallbackCatalog {

    /** More variants than this of one trade (only colours differ) → one of them. */
    public static final int MAX_VARIANTS = 3;
    /** Up to this many trades from the end become Masteries. */
    public static final int MASTERY_COUNT = 4;

    /** One entry: id (for uses and quest bookkeeping), the rolled costs, result and stock, and its unlock rank in its group. */
    public record Entry(Identifier id, ItemExchange.Tier tier, int unlockRank, ItemAmount costA, Optional<ItemAmount> costB,
                        ItemStack result, int maxUses) {
    }

    public record Catalog(List<Entry> quests, List<Entry> basic, List<Entry> master) {
        public static final Catalog EMPTY = new Catalog(List.of(), List.of(), List.of());

        /** Rank that unlocks the last quest / basic trade / mastery - the price steps start after it (FallbackScaling). */
        public int lastQuestRank() {
            return Math.max(0, quests.size() - 1);
        }

        public int lastBasicRank() {
            return Math.max(0, basic.size() - 1);
        }

        public int lastMasterRank() {
            return master.size();
        }

        public Optional<Entry> byId(Identifier id) {
            for (List<Entry> list : List.of(quests, basic, master)) {
                for (Entry entry : list) {
                    if (entry.id().equals(id)) {
                        return Optional.of(entry);
                    }
                }
            }
            return Optional.empty();
        }
    }

    private record CacheKey(UUID villager, Holder<VillagerProfession> profession, Object type) {
    }

    private static final Map<CacheKey, Catalog> CACHE = new ConcurrentHashMap<>();

    private FallbackCatalog() {
    }

    /**
     * Not designed by us: no own station entry, no own trade or quest files - and Vanilla (or its mod) gives
     * it trade sets. Works on both sides (the trade and quest registries are synced).
     */
    public static boolean isFallback(Villager villager) {
        Holder<VillagerProfession> profession = villager.getVillagerData().profession();
        if (ProfessionStations.of(profession).isPresent() || !hasTradeSets(profession.value())) {
            return false;
        }
        RegistryAccess registries = villager.level().registryAccess();
        for (var key : List.of(ModDataPackRegistries.TRADE, ModDataPackRegistries.QUEST)) {
            for (ItemExchange exchange : registries.lookupOrThrow(key)) {
                if (exchange.profession() == profession.value()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean hasTradeSets(VillagerProfession profession) {
        for (int level = 1; level <= 5; level++) {
            if (profession.getTrades(level) != null) {
                return true;
            }
        }
        return false;
    }

    /** Server only (Vanilla's trade sets are server data); EMPTY for designed professions or on the client. */
    public static Catalog of(Villager villager) {
        if (!(villager.level() instanceof ServerLevel level) || !isFallback(villager)) {
            return Catalog.EMPTY;
        }
        CacheKey key = new CacheKey(villager.getUUID(), villager.getVillagerData().profession(), villager.getVillagerData().type());
        return CACHE.computeIfAbsent(key, k -> build(level, villager));
    }

    @SubscribeEvent
    static void onDatapackSync(OnDatapackSyncEvent event) {
        CACHE.clear();
    }

    private record Rolled(Identifier id, int level, MerchantOffer offer) {
    }

    private static Catalog build(ServerLevel level, Villager villager) {
        VillagerProfession profession = villager.getVillagerData().profession().value();
        List<Rolled> rolled = new ArrayList<>();
        for (int vanillaLevel = 1; vanillaLevel <= 5; vanillaLevel++) {
            ResourceKey<TradeSet> setKey = profession.getTrades(vanillaLevel);
            Optional<TradeSet> set = setKey == null ? Optional.empty() : level.registryAccess().lookupOrThrow(Registries.TRADE_SET).getOptional(setKey);
            if (set.isEmpty()) {
                continue;
            }
            for (Holder<VillagerTrade> trade : set.get().getTrades()) {
                Identifier tradeId = trade.unwrapKey().map(ResourceKey::identifier)
                        .orElse(Identifier.fromNamespaceAndPath("unknown", "trade_" + rolled.size()));
                MerchantOffer offer = trade.value().getOffer(lootContext(level, villager, tradeId));
                if (offer != null) {
                    rolled.add(new Rolled(tradeId, vanillaLevel, offer));
                }
            }
        }
        List<Rolled> kept = pickVariants(rolled, villager);

        List<Rolled> questsRolled = new ArrayList<>();
        List<Rolled> tradesRolled = new ArrayList<>();
        for (Rolled r : kept) {
            (r.offer().getResult().is(Items.EMERALD) ? questsRolled : tradesRolled).add(r);
        }
        int masterCount = Math.min(MASTERY_COUNT, tradesRolled.size());
        List<Rolled> basicRolled = tradesRolled.subList(0, tradesRolled.size() - masterCount);
        List<Rolled> masterRolled = tradesRolled.subList(tradesRolled.size() - masterCount, tradesRolled.size());

        List<Entry> quests = new ArrayList<>();
        for (int i = 0; i < questsRolled.size(); i++) {
            quests.add(entry(questsRolled.get(i), ItemExchange.Tier.BASIC, i));
        }
        List<Entry> basic = new ArrayList<>();
        for (int i = 0; i < basicRolled.size(); i++) {
            basic.add(entry(basicRolled.get(i), ItemExchange.Tier.BASIC, i));
        }
        List<Entry> master = new ArrayList<>();
        for (int i = 0; i < masterRolled.size(); i++) {
            master.add(entry(masterRolled.get(i), ItemExchange.Tier.MASTER, i + 1));
        }
        return new Catalog(List.copyOf(quests), List.copyOf(basic), List.copyOf(master));
    }

    /** Same context Vanilla uses for villager trades, but with a random seeded by villager and trade - stable across rebuilds. */
    private static LootContext lootContext(ServerLevel level, Villager villager, Identifier tradeId) {
        long seed = villager.getUUID().getMostSignificantBits() ^ villager.getUUID().getLeastSignificantBits() ^ ((long) tradeId.hashCode() << 16);
        return new LootContext.Builder(
                new LootParams.Builder(level)
                        .withParameter(LootContextParams.ORIGIN, villager.position())
                        .withParameter(LootContextParams.THIS_ENTITY, villager)
                        .withParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED, Unit.INSTANCE)
                        .create(LootContextParamSets.VILLAGER_TRADE))
                .withOptionalRandomSeed(seed)
                .create(Optional.empty());
    }

    /**
     * Groups colour variants of one trade - same Vanilla level, same costs and result once the colour is
     * taken out of the item names - and keeps one per villager if a group has more than MAX_VARIANTS. The
     * kept one stays at the group's first position, so the Vanilla order holds.
     */
    private static List<Rolled> pickVariants(List<Rolled> rolled, Villager villager) {
        Map<String, List<Rolled>> groups = new LinkedHashMap<>();
        for (Rolled r : rolled) {
            groups.computeIfAbsent(groupKey(r), k -> new ArrayList<>()).add(r);
        }
        List<Rolled> kept = new ArrayList<>();
        for (Map.Entry<String, List<Rolled>> group : groups.entrySet()) {
            List<Rolled> members = group.getValue();
            if (members.size() > MAX_VARIANTS) {
                int pick = Math.floorMod(villager.getUUID().hashCode() * 31 + group.getKey().hashCode(), members.size());
                kept.add(members.get(pick));
            } else {
                kept.addAll(members);
            }
        }
        return kept;
    }

    /** Dye colour names, longest first - "light_gray" has to go before "gray" would leave "light_" behind. */
    private static final List<String> COLOUR_NAMES = java.util.Arrays.stream(DyeColor.values()).map(DyeColor::getName)
            .sorted(java.util.Comparator.comparingInt(String::length).reversed()).toList();

    private static String groupKey(Rolled r) {
        MerchantOffer offer = r.offer();
        return r.level() + "|" + colourless(offer.getItemCostA().item().value())
                + "|" + offer.getItemCostB().map(cost -> colourless(cost.item().value())).orElse("-")
                + "|" + colourless(offer.getResult().getItem());
    }

    /** The item's id with any dye colour taken out: black_wool, white_wool → _wool. */
    private static String colourless(Item item) {
        String name = BuiltInRegistries.ITEM.getKey(item).toString();
        for (String colour : COLOUR_NAMES) {
            name = name.replace(colour + "_", "").replace("_" + colour, "");
        }
        return name;
    }

    private static Entry entry(Rolled r, ItemExchange.Tier tier, int unlockRank) {
        MerchantOffer offer = r.offer();
        Identifier id = Identifier.fromNamespaceAndPath(VillagerOverhaulMod.MODID,
                "fallback/" + r.id().getNamespace() + "/" + r.id().getPath());
        return new Entry(id, tier, unlockRank, amount(offer.getItemCostA()), offer.getItemCostB().map(FallbackCatalog::amount),
                offer.getResult().copy(), offer.getMaxUses());
    }

    private static ItemAmount amount(ItemCost cost) {
        return new ItemAmount(cost.item().value(), cost.count());
    }
}
