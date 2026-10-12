package com.villageroverhaul.vanilla;

import com.villageroverhaul.VillagerOverhaulMod;
import com.villageroverhaul.data.ItemAmount;
import com.villageroverhaul.section.CoreSections;
import com.villageroverhaul.section.SectionEntries;
import com.villageroverhaul.section.Sections;
import net.minecraft.core.Holder;
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The entries of a profession nobody designed - Vanilla ones like the farmer, and any profession another mod
 * adds - built from its own Vanilla trade sets. This is what the core does on its own for every profession; a
 * profession with data-pack entries or stations of its own (the Trade Rework's) doesn't use it.
 * - every trade of every Vanilla level (1-5, the whole pool, not Vanilla's random pick), in Vanilla order;
 *   a Vanilla villager-type restriction still applies
 * - a trade that exists in more than MAX_VARIANTS colour variants (16 wools, carpets, beds ...) keeps one
 *   of them, picked per villager
 * - trades that give emeralds go into the Quests section, the rest into Trades (CoreSections); both restock
 * - each rank of a section unlocks exactly its next entry, rank 0 the first; after the last one
 *   VanillaScaling.EXTRA_RANKS more ranks improve prices and stock (VanillaRankCaps)
 * - prices, amounts and stock are Vanilla's own, rolled once (enchantments, dyes ...) with a random seeded
 *   by the villager and the trade, so every rebuild gives the same offer
 *
 * The catalog is rebuilt from that seed on demand and kept in memory only; a datapack reload clears it.
 */
@EventBusSubscriber(modid = VillagerOverhaulMod.MODID)
public final class VanillaCatalog {

    /** More variants than this of one trade (only colours differ) → one of them. */
    public static final int MAX_VARIANTS = 3;

    /** One entry: id (its stock key), the rolled costs, result and stock, and its unlock rank in its section. */
    public record Entry(Identifier id, int unlockRank, ItemAmount costA, Optional<ItemAmount> costB, ItemStack result, int maxUses) {
    }

    /** Entries per section id, each list in unlock order. */
    public record Catalog(Map<Identifier, List<Entry>> sections) {
        public static final Catalog EMPTY = new Catalog(Map.of());

        public List<Entry> entries(Identifier section) {
            return sections.getOrDefault(section, List.of());
        }

        /** Rank that unlocks the section's last entry - the price steps start after it (VanillaScaling). */
        public int lastUnlockRank(Identifier section) {
            return Math.max(0, entries(section).size() - 1);
        }
    }

    private record CacheKey(UUID villager, Holder<VillagerProfession> profession, Object type) {
    }

    private static final Map<CacheKey, Catalog> CACHE = new ConcurrentHashMap<>();

    private VanillaCatalog() {
    }

    /**
     * Nobody designed the profession: no stations and no data-pack entries of its own - and Vanilla (or its mod)
     * gives it trade sets.
     */
    public static boolean usedBy(Villager villager) {
        Holder<VillagerProfession> profession = villager.getVillagerData().profession();
        return Sections.stationsOf(profession).isEmpty()
                && !SectionEntries.hasAny(villager.level().registryAccess(), profession.value())
                && hasTradeSets(profession.value());
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
        if (!(villager.level() instanceof ServerLevel level) || !usedBy(villager)) {
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

        List<Entry> quests = new ArrayList<>();
        List<Entry> trades = new ArrayList<>();
        for (Rolled r : pickVariants(rolled, villager)) {
            List<Entry> section = r.offer().getResult().is(Items.EMERALD) ? quests : trades;
            section.add(entry(r, section.size()));
        }
        return new Catalog(Map.of(CoreSections.QUESTS, List.copyOf(quests), CoreSections.TRADES, List.copyOf(trades)));
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
    private static final List<String> COLOUR_NAMES = Arrays.stream(DyeColor.values()).map(DyeColor::getName)
            .sorted(Comparator.comparingInt(String::length).reversed()).toList();

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

    private static Entry entry(Rolled r, int unlockRank) {
        MerchantOffer offer = r.offer();
        Identifier id = Identifier.fromNamespaceAndPath(VillagerOverhaulMod.MODID,
                "vanilla/" + r.id().getNamespace() + "/" + r.id().getPath());
        return new Entry(id, unlockRank, amount(offer.getItemCostA()), offer.getItemCostB().map(VanillaCatalog::amount),
                offer.getResult().copy(), offer.getMaxUses());
    }

    private static ItemAmount amount(ItemCost cost) {
        return new ItemAmount(cost.item().value(), cost.count());
    }
}
