package com.villageroverhaul.quest;

import com.villageroverhaul.core.QuestOffer;
import com.villageroverhaul.core.QuestProvider;
import com.villageroverhaul.core.ResolvedExchange;
import com.villageroverhaul.core.VillagerStateAccess;
import com.villageroverhaul.core.state.VillagerState;
import com.villageroverhaul.data.ItemAmount;
import com.villageroverhaul.data.ItemExchange;
import com.villageroverhaul.data.ModDataPackRegistries;
import com.villageroverhaul.fallback.FallbackCatalog;
import com.villageroverhaul.fallback.FallbackScaling;
import com.villageroverhaul.progression.UpgradeGroup;
import com.villageroverhaul.trade.ExchangeScaling;
import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Picks each of the villager's open quest slots deterministically from UUID, slot index and that
 * slot's rotation counter - never stored, recomputed on every access. Which slots are open and which
 * pool each draws from: QuestSlots. The permanent slot always shows the profession's first permanent
 * quest and never rotates. What a permanent quest asks for never appears in the easy or hard pool
 * (2026-09-26, permanentItems / allowedVariants).
 *
 * Slots in their post-completion pause are left out (QuestLog, QuestActions); a paused slot still
 * reserves its pick, so the others don't shift. Every quest counts toward today's limit. Once it is used
 * up, completed rotating slots stay empty until tomorrow while the ones still shown stay completable
 * (soft limit, 2026-09-23); only the permanent quest, which never leaves, is greyed out (no uses left).
 */
public class QuestProviderImpl implements QuestProvider {

    @Override
    public List<QuestOffer> getCurrentOffers(Villager villager) {
        VillagerState state = VillagerStateAccess.of(villager).getState();
        VillagerProfession profession = villager.getVillagerData().profession().value();
        int rank = state.ranks().quest();
        if (FallbackCatalog.isFallback(villager)) {
            return fallbackOffers(villager, state, rank);
        }

        Registry<ItemExchange> registry = villager.level().registryAccess().lookupOrThrow(ModDataPackRegistries.QUEST);
        Set<Item> permanentItems = permanentItems(registry, profession);
        Map<ItemExchange.QuestPool, List<Map.Entry<Identifier, ItemExchange>>> pools = new EnumMap<>(ItemExchange.QuestPool.class);
        for (Map.Entry<ResourceKey<ItemExchange>, ItemExchange> entry : registry.entrySet()) {
            ItemExchange exchange = entry.getValue();
            if (exchange.profession() != profession || exchange.unlockRank() > rank
                    || exchange.questPool() != ItemExchange.QuestPool.PERMANENT && allowedVariants(exchange, permanentItems).isEmpty()) {
                continue;
            }
            pools.computeIfAbsent(exchange.questPool(), pool -> new ArrayList<>()).add(Map.entry(entry.getKey().identifier(), exchange));
        }
        pools.values().forEach(pool -> pool.sort(Map.Entry.comparingByKey()));

        boolean limitReached = QuestActions.isLimitReached(villager, state);
        List<QuestOffer> offers = new ArrayList<>(QuestSlots.SLOT_COUNT);
        Map<ItemExchange.QuestPool, Set<Integer>> usedIndices = new EnumMap<>(ItemExchange.QuestPool.class);
        for (int slot = 0; slot < QuestSlots.SLOT_COUNT; slot++) {
            ItemExchange.QuestPool poolType = QuestSlots.poolOf(slot);
            List<Map.Entry<Identifier, ItemExchange>> pool = pools.getOrDefault(poolType, List.of());
            if (!QuestSlots.isOpen(slot, rank, villager.getVillagerData().profession()) || pool.isEmpty()) {
                continue;
            }

            int index = 0;
            int seed = 0;
            if (poolType != ItemExchange.QuestPool.PERMANENT) {
                Set<Integer> used = usedIndices.computeIfAbsent(poolType, p -> new HashSet<>());
                int rotation = state.questSlotRotations().getOrDefault(QuestSlots.rotationKey(slot), 0);
                seed = villager.getUUID().hashCode() + slot * 1_000_003 + rotation;
                index = Math.floorMod(seed, pool.size());
                for (int attempt = 0; attempt < pool.size() && used.contains(index); attempt++) {
                    index = Math.floorMod(index + 1, pool.size());
                }
                used.add(index);
            }
            if (state.questLog().hiddenSlots().containsKey(QuestSlots.rotationKey(slot))) {
                continue; // just turned in - the next quest pops up after a short pause, see QuestActions
            }

            Map.Entry<Identifier, ItemExchange> picked = pool.get(index);
            ItemExchange exchange = picked.getValue();
            Item inputItem = pickVariant(exchange, seed, permanentItems);
            Optional<ResourceKey<Enchantment>> inputEnchantment = pickEnchantmentVariant(exchange, seed);
            offers.add(new QuestOffer(slot, new ResolvedExchange(
                    picked.getKey(),
                    exchange.tier(),
                    new ItemAmount(inputItem, exchange.baseInput().count(), inputEnchantment, exchange.baseInput().mob()),
                    new ItemAmount(inputItem, ExchangeScaling.scaleInput(exchange, rank, UpgradeGroup.QUEST).count(), inputEnchantment, exchange.baseInput().mob()),
                    exchange.secondInput(),
                    ExchangeScaling.scaleOutput(exchange, rank, UpgradeGroup.QUEST),
                    limitReached && poolType == ItemExchange.QuestPool.PERMANENT ? 0 : 1, 1
            )));
        }
        return offers;
    }

    /**
     * A fallback profession (fallback/FallbackCatalog, 2026-09-27): quest rank r shows its first r + 1 quests,
     * each a permanent quest in its own slot (QuestSlots.FALLBACK_SLOT_BASE + index) - Vanilla's rolled price
     * and emerald reward, no scaling. Paused slots don't exist here; at the daily limit they grey out.
     */
    private static List<QuestOffer> fallbackOffers(Villager villager, VillagerState state, int rank) {
        boolean limitReached = QuestActions.isLimitReached(villager, state);
        FallbackCatalog.Catalog catalog = FallbackCatalog.of(villager);
        List<FallbackCatalog.Entry> quests = catalog.quests();
        // Price steps after the last quest unlocks (FallbackScaling): 120 % of Vanilla's amount, then less.
        int stage = FallbackScaling.stage(rank, catalog.lastQuestRank());
        List<QuestOffer> offers = new ArrayList<>();
        for (int i = 0; i < quests.size() && i <= rank; i++) {
            FallbackCatalog.Entry entry = quests.get(i);
            offers.add(new QuestOffer(QuestSlots.FALLBACK_SLOT_BASE + i, new ResolvedExchange(
                    entry.id(), ItemExchange.Tier.BASIC, FallbackScaling.questPrice(entry.costA(), 0), FallbackScaling.questPrice(entry.costA(), stage), entry.costB(),
                    new ItemAmount(entry.result().getItem(), entry.result().getCount()),
                    limitReached ? 0 : 1, 1, Optional.of(entry.result())
            )));
        }
        return offers;
    }

    /**
     * A category quest (input_variants) asks for one item of its category, chosen from the same seed as
     * the slot's pick - so it stays put until the slot rotates. Scrambled so it doesn't move in lockstep
     * with the pool index.
     */
    private static Item pickVariant(ItemExchange exchange, int seed, Set<Item> permanentItems) {
        List<Item> variants = exchange.questPool() == ItemExchange.QuestPool.PERMANENT
                ? (exchange.inputVariants().isEmpty() ? List.of(exchange.baseInput().item()) : exchange.inputVariants())
                : allowedVariants(exchange, permanentItems);
        return variants.get(Math.floorMod(HashCommon.murmurHash3(seed), variants.size()));
    }

    /**
     * Rule (2026-09-26): what a profession's permanent quest asks for never shows up in its easy or hard
     * pool. The items its permanent quests ask for (base input and any variants).
     */
    private static Set<Item> permanentItems(Registry<ItemExchange> registry, VillagerProfession profession) {
        Set<Item> items = new HashSet<>();
        for (ItemExchange exchange : registry) {
            if (exchange.profession() == profession && exchange.questPool() == ItemExchange.QuestPool.PERMANENT) {
                items.add(exchange.baseInput().item());
                items.addAll(exchange.inputVariants());
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
