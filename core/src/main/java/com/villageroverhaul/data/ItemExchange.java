package com.villageroverhaul.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;
import java.util.Optional;

/**
 * One entry of a section (a trade, a quest ...): input items for output items. section names the section it
 * belongs to (api/SectionDefinition) - the section's logic decides what the entry is: the standard logic lists
 * it as a restocking trade, the Trade Rework's quest logic draws it into a rotating slot. Input, output and max
 * stock scale from base to max with the section's rank (trade/ExchangeScaling); it unlocks at unlock_rank of its
 * section (default 0).
 *
 * pool (optional) is read by the section's logic only - the Trade Rework's quests use "easy", "hard" and
 * "permanent". input_variants / input_enchantment_variants (optional) turn one entry into a category like
 * "any sign" or "a book with Soul Speed": a logic that supports it (the quests) asks for one of them instead of
 * base_input's item or enchantment; the counts still come from base_input/max_input.
 *
 * second_input (optional) is Vanilla's second payment slot (costB), e.g. emeralds + book -> special book.
 * Its count is fixed and doesn't scale with rank, and the rank discount only ever touches base_input.
 *
 * explorer_map (optional) makes the output a Vanilla explorer map to the nearest structure of a tag instead of
 * base_output's item - see ExplorerMap; the map is built by an extension (api/ExtensionHooks).
 */
public record ItemExchange(
        VillagerProfession profession,
        Identifier section,
        int unlockRank,
        Optional<String> pool,
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

    public static final Codec<ItemExchange> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.VILLAGER_PROFESSION.byNameCodec().fieldOf("profession").forGetter(ItemExchange::profession),
            Identifier.CODEC.fieldOf("section").forGetter(ItemExchange::section),
            Codec.INT.optionalFieldOf("unlock_rank", 0).forGetter(ItemExchange::unlockRank),
            Codec.STRING.optionalFieldOf("pool").forGetter(ItemExchange::pool),
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
}
