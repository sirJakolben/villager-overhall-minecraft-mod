package com.smartvillager.progression.trade;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.smartvillager.progression.SmartVillagerProgressionMod;
import com.smartvillager.progression.data.ItemAmount;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * A payment that accepts any item of an item tag (e.g. any boat) instead of one exact item, optionally only items that lack
 * certain data components (a boat without a cannon yet). Vanilla's ItemCost knows one item and exact components, so - like
 * RequiredEnchantmentCost - such a cost carries a marker on its display stack and, for marked costs only, ItemCostMixin
 * swaps the comparison for matches below. The display stack is the amount's item (the offer shows it), named after the tag.
 * The marker only lives on the cost's display stack, never on a real item.
 */
public final class TagCost {

    /** What the marked cost accepts: items of tag, none of which carries a component named in without. */
    public record Marker(TagKey<Item> tag, List<Identifier> without) {
        public static final Codec<Marker> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                TagKey.codec(Registries.ITEM).fieldOf("tag").forGetter(Marker::tag),
                Identifier.CODEC.listOf().fieldOf("without").forGetter(Marker::without)
        ).apply(instance, Marker::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Marker> STREAM_CODEC = StreamCodec.composite(
                TagKey.streamCodec(Registries.ITEM), Marker::tag,
                Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), Marker::without,
                Marker::new);
    }

    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, SmartVillagerProgressionMod.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Marker>> ANY_OF_TAG =
            DATA_COMPONENTS.registerComponentType("any_of_tag",
                    builder -> builder.persistent(Marker.CODEC).networkSynchronized(Marker.STREAM_CODEC));

    private TagCost() {
    }

    /** The Vanilla cost for an amount that has a tag; the caller checks that it does. */
    public static ItemCost of(ItemAmount amount) {
        TagKey<Item> tag = amount.tag().orElseThrow();
        DataComponentExactPredicate components = DataComponentExactPredicate.builder()
                .expect(ANY_OF_TAG.get(), new Marker(tag, amount.without()))
                .expect(DataComponents.ITEM_NAME, Component.translatable("tag.item." + tag.location().getNamespace() + "." + tag.location().getPath().replace('/', '.')))
                .build();
        return new ItemCost(amount.item().builtInRegistryHolder(), amount.count(), components);
    }

    public static boolean isMarked(ItemCost cost) {
        return cost.itemStack().has(ANY_OF_TAG.get());
    }

    /** The whole item test of a marked cost (its components are the marker, not something the real item carries). */
    public static boolean matches(ItemCost cost, ItemStack stack) {
        Marker marker = cost.itemStack().get(ANY_OF_TAG.get());
        return marker != null && stack.is(marker.tag()) && marker.without().stream().noneMatch(id -> ItemAmount.carries(stack, id));
    }
}
