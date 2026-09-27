package com.villageroverhaul.traderework.salvager;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.util.Map;

/**
 * One datapack file in data/villageroverhaul/villageroverhaul/salvage/ (Salvager passive, 2026-09-27): which
 * gear the smelting station takes and what it gives back. items maps each piece to its full yield of result
 * - the recipe amount (iron chestplate 8 ingots), or twice it in nuggets for chainmail. The station pays out
 * a share of that by passive rank and condition (SmeltingStationBlockEntity.payout).
 *
 * downgrade (optional, netherite): the piece doesn't melt, it comes back as this item - keeping enchantments,
 * name and trim, durability carried over relatively - and result is paid on top (netherite scraps).
 */
public record SalvageRule(Item result, Map<Item, Integer> items, Map<Item, Item> downgrade) {

    public static final Codec<SalvageRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("result").forGetter(SalvageRule::result),
            Codec.unboundedMap(BuiltInRegistries.ITEM.byNameCodec(), Codec.intRange(1, 64)).fieldOf("items").forGetter(SalvageRule::items),
            Codec.unboundedMap(BuiltInRegistries.ITEM.byNameCodec(), BuiltInRegistries.ITEM.byNameCodec())
                    .optionalFieldOf("downgrade", Map.of()).forGetter(SalvageRule::downgrade)
    ).apply(instance, SalvageRule::new));
}
