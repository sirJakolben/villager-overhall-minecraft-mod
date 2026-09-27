package com.villageroverhaul.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.villageroverhaul.core.state.VillagerState;
import com.villageroverhaul.progression.ProgressionService;
import com.villageroverhaul.progression.UpgradeGroup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;
import java.util.Optional;

/**
 * One input item for one output item - the shared shape behind both a Trade (Block C, restockable,
 * uses-per-day capped by baseMaxUses/maxMaxUses) and a Quest (Block D, one at a time, rotates on
 * completion). Input/output/max-uses scale from base to max with the owning group's rank, see
 * ExchangeScaling.
 *
 * Unlocking (reworked 2026-09-24) depends only on the rank of the exchange's own group (unlock_rank,
 * default 0) - the villager level only earns points - and on its group being open (ProgressionService.isGroupOpen:
 * station owned or rank invested). quest_pool only matters for quests: which slot draws it.
 *
 * input_variants (quests only, optional) turns one quest into a category like "any sign": each concrete
 * quest asks for one of these items instead of base_input's item, picked along with the slot's quest -
 * see QuestProviderImpl. The counts still come from base_input/max_input.
 *
 * input_enchantment_variants (quests only, optional) works the same way for enchantments: each concrete
 * quest asks for the input item carrying one of them, at any level (e.g. an enchanted book with Soul Speed).
 *
 * second_input (optional) is Vanilla's second payment slot (costB), e.g. emeralds + book -> special book.
 * Its count is fixed and doesn't scale with rank, and the rank discount only ever touches base_input.
 *
 * explorer_map (trades only, optional) makes the output a Vanilla explorer map to the nearest structure of
 * a tag instead of base_output's item - see ExplorerMap; the map is built by an extension (api/ExtensionHooks).
 */
public record ItemExchange(
        VillagerProfession profession,
        Tier tier,
        int unlockRank,
        QuestPool questPool,
        List<Item> inputVariants,
        List<ResourceKey<Enchantment>> inputEnchantmentVariants,
        ItemAmount baseInput,
        ItemAmount maxInput,
        Optional<ItemAmount> secondInput,
        ItemAmount baseOutput,
        ItemAmount maxOutput,
        int baseMaxUses,
        int maxMaxUses,
        Optional<ExplorerMap> explorerMap
) {

    public enum Tier implements StringRepresentable {
        BASIC("basic"),
        MASTER("master");

        public static final Codec<Tier> CODEC = StringRepresentable.fromEnum(Tier::values);

        private final String serializedName;

        Tier(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    /** Easy quests fill the normal slots, hard ones the hard slot; the permanent quest never rotates. See QuestSlots. */
    public enum QuestPool implements StringRepresentable {
        EASY("easy"),
        HARD("hard"),
        PERMANENT("permanent");

        public static final Codec<QuestPool> CODEC = StringRepresentable.fromEnum(QuestPool::values);

        private final String serializedName;

        QuestPool(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    public static final Codec<ItemExchange> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.VILLAGER_PROFESSION.byNameCodec().fieldOf("profession").forGetter(ItemExchange::profession),
            Tier.CODEC.fieldOf("tier").forGetter(ItemExchange::tier),
            Codec.INT.optionalFieldOf("unlock_rank", 0).forGetter(ItemExchange::unlockRank),
            QuestPool.CODEC.optionalFieldOf("quest_pool", QuestPool.EASY).forGetter(ItemExchange::questPool),
            BuiltInRegistries.ITEM.byNameCodec().listOf().optionalFieldOf("input_variants", List.of()).forGetter(ItemExchange::inputVariants),
            ResourceKey.codec(Registries.ENCHANTMENT).listOf().optionalFieldOf("input_enchantment_variants", List.of()).forGetter(ItemExchange::inputEnchantmentVariants),
            ItemAmount.CODEC.fieldOf("base_input").forGetter(ItemExchange::baseInput),
            ItemAmount.CODEC.fieldOf("max_input").forGetter(ItemExchange::maxInput),
            ItemAmount.CODEC.optionalFieldOf("second_input").forGetter(ItemExchange::secondInput),
            ItemAmount.CODEC.fieldOf("base_output").forGetter(ItemExchange::baseOutput),
            ItemAmount.CODEC.fieldOf("max_output").forGetter(ItemExchange::maxOutput),
            Codec.INT.fieldOf("base_max_uses").forGetter(ItemExchange::baseMaxUses),
            Codec.INT.fieldOf("max_max_uses").forGetter(ItemExchange::maxMaxUses),
            ExplorerMap.CODEC.optionalFieldOf("explorer_map").forGetter(ItemExchange::explorerMap)
    ).apply(instance, ItemExchange::new));

    /** The group this trade belongs to - basic or master, by tier. */
    public UpgradeGroup tradeGroup() {
        return tier == Tier.MASTER ? UpgradeGroup.MASTER_TRADE : UpgradeGroup.BASIC_TRADE;
    }

    /** Rank of the trade group this trade scales with. */
    public int tradeGroupRank(VillagerState state) {
        return tier == Tier.MASTER ? state.ranks().masterTrade() : state.ranks().basicTrade();
    }

    /** Trade unlock: its group is open (station owned or rank invested) and its unlock rank reached. */
    public boolean isTradeUnlocked(VillagerState state) {
        return unlockRank <= tradeGroupRank(state) && ProgressionService.isGroupOpen(state, tradeGroup());
    }
}
