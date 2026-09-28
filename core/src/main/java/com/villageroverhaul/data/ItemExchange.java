package com.villageroverhaul.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.villageroverhaul.api.ExchangeExtensionType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

import java.util.Optional;

/**
 * One entry of a section (a trade, a quest ...): input items for output items. section names the section it
 * belongs to (api/SectionDefinition) - the section's logic decides what the entry is: the standard logic lists
 * it as a restocking trade, the Trade Rework's quest logic draws it into a rotating slot. Input, output and max
 * stock scale from base to max with the section's rank (trade/ExchangeScaling); it unlocks at unlock_rank of its
 * section (default 0).
 *
 * second_input (optional) is Vanilla's second payment slot (costB), e.g. emeralds + book -> special book.
 * Its count is fixed and doesn't scale with rank, and the rank discount only ever touches base_input.
 *
 * extensions (optional): extra data for an extension's own logic, one value per registered type
 * (api/ExchangeExtensionType) - e.g. the Trade Rework's quest pools or explorer maps. Read with extension(TYPE).
 */
public record ItemExchange(
        VillagerProfession profession,
        Identifier section,
        int unlockRank,
        ItemAmount baseInput,
        ItemAmount maxInput,
        Optional<ItemAmount> secondInput,
        ItemAmount baseOutput,
        ItemAmount maxOutput,
        int baseMaxUses,
        int maxMaxUses,
        ExchangeExtensions extensions
) {

    public static final Codec<ItemExchange> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.VILLAGER_PROFESSION.byNameCodec().fieldOf("profession").forGetter(ItemExchange::profession),
            Identifier.CODEC.fieldOf("section").forGetter(ItemExchange::section),
            Codec.INT.optionalFieldOf("unlock_rank", 0).forGetter(ItemExchange::unlockRank),
            ItemAmount.CODEC.fieldOf("base_input").forGetter(ItemExchange::baseInput),
            ItemAmount.CODEC.fieldOf("max_input").forGetter(ItemExchange::maxInput),
            ItemAmount.CODEC.optionalFieldOf("second_input").forGetter(ItemExchange::secondInput),
            ItemAmount.CODEC.fieldOf("base_output").forGetter(ItemExchange::baseOutput),
            ItemAmount.CODEC.fieldOf("max_output").forGetter(ItemExchange::maxOutput),
            Codec.INT.fieldOf("base_max_uses").forGetter(ItemExchange::baseMaxUses),
            Codec.INT.fieldOf("max_max_uses").forGetter(ItemExchange::maxMaxUses),
            ExchangeExtensions.CODEC.optionalFieldOf("extensions", ExchangeExtensions.NONE).forGetter(ItemExchange::extensions)
    ).apply(instance, ItemExchange::new));

    public <T> Optional<T> extension(ExchangeExtensionType<T> type) {
        return extensions.get(type);
    }
}
