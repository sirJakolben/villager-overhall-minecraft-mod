package com.villageroverhaul.traderework.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.villageroverhaul.api.ExchangeExtensionType;
import com.villageroverhaul.data.ItemExchange;
import com.villageroverhaul.traderework.TradeReworkMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;

/**
 * What QuestLogic needs of a quest entry beyond the core's fields, under
 * "extensions": {"vo_trade_rework:quest": {...}} - an entry without it is an easy quest for exactly its base input.
 *
 * pool: "easy" (default), "hard" or "permanent" (QuestSlots). input_variants / input_enchantment_variants turn
 * one entry into a category like "any sign" or "a book with Soul Speed": the quest asks for one of them instead of
 * base_input's item or enchantment; the counts still come from base_input/max_input.
 */
public record QuestEntry(String pool, List<Item> inputVariants, List<ResourceKey<Enchantment>> inputEnchantmentVariants) {

    public static final QuestEntry DEFAULT = new QuestEntry(QuestSlots.EASY, List.of(), List.of());

    public static final Codec<QuestEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("pool", QuestSlots.EASY).forGetter(QuestEntry::pool),
            BuiltInRegistries.ITEM.byNameCodec().listOf().optionalFieldOf("input_variants", List.of()).forGetter(QuestEntry::inputVariants),
            ResourceKey.codec(Registries.ENCHANTMENT).listOf().optionalFieldOf("input_enchantment_variants", List.of()).forGetter(QuestEntry::inputEnchantmentVariants)
    ).apply(instance, QuestEntry::new));

    public static final ExchangeExtensionType<QuestEntry> TYPE =
            new ExchangeExtensionType<>(Identifier.fromNamespaceAndPath(TradeReworkMod.MODID, "quest"), CODEC);

    public static QuestEntry of(ItemExchange exchange) {
        return exchange.extension(TYPE).orElse(DEFAULT);
    }
}
