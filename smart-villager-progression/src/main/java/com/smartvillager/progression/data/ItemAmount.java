package com.smartvillager.progression.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.smartvillager.progression.api.ExtensionHooks;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.List;
import java.util.Optional;

/**
 * An item and a count. enchantment (optional) additionally requires the item to carry that
 * enchantment at any level - e.g. an enchanted book with Soul Speed, see RequiredEnchantmentCost.
 * mob (optional) requires it to be that mob's weapon - e.g. a zombie's iron sword for the Veteran's quests. The
 * mark itself comes from an extension (api/ExtensionHooks.mobWeaponComponent, Professions' veteran/MobWeapons);
 * without one, no item matches.
 * tag (optional) accepts any item of that item tag instead of just item (item then only is what the offer shows,
 * e.g. the first boat for "any boat"), and without (optional) lists data component types the item must NOT carry
 * (a boat that has no cannon yet) - see trade/TagCost. Meant for payments; as an output the item is used as is.
 */
public record ItemAmount(Item item, int count, Optional<ResourceKey<Enchantment>> enchantment, Optional<EntityType<?>> mob,
                         Optional<TagKey<Item>> tag, List<Identifier> without) {

    public static final Codec<ItemAmount> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(ItemAmount::item),
            Codec.INT.fieldOf("count").forGetter(ItemAmount::count),
            ResourceKey.codec(Registries.ENCHANTMENT).optionalFieldOf("enchantment").forGetter(ItemAmount::enchantment),
            BuiltInRegistries.ENTITY_TYPE.byNameCodec().optionalFieldOf("mob").forGetter(ItemAmount::mob),
            TagKey.codec(Registries.ITEM).optionalFieldOf("tag").forGetter(ItemAmount::tag),
            Identifier.CODEC.listOf().optionalFieldOf("without", List.of()).forGetter(ItemAmount::without)
    ).apply(instance, ItemAmount::new));

    public ItemAmount(Item item, int count) {
        this(item, count, Optional.empty(), Optional.empty());
    }

    public ItemAmount(Item item, int count, Optional<ResourceKey<Enchantment>> enchantment) {
        this(item, count, enchantment, Optional.empty());
    }

    public ItemAmount(Item item, int count, Optional<ResourceKey<Enchantment>> enchantment, Optional<EntityType<?>> mob) {
        this(item, count, enchantment, mob, Optional.empty(), List.of());
    }

    /** The same amount with another count - keeps every requirement. */
    public ItemAmount withCount(int newCount) {
        return new ItemAmount(item, newCount, enchantment, mob, tag, without);
    }

    /**
     * The stack this amount stands for as an output (2026-09-27, Runesmith books): with enchantment, the item
     * carries it at level I - on an enchanted book as a stored enchantment, like a Vanilla librarian book.
     */
    public ItemStack toStack(HolderLookup.Provider registries) {
        ItemStack stack = new ItemStack(item, count);
        enchantment.flatMap(key -> registries.lookupOrThrow(Registries.ENCHANTMENT).get(key)).ifPresent(holder -> {
            ItemEnchantments.Mutable levelOne = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            levelOne.set(holder, 1);
            stack.set(EnchantmentHelper.getComponentType(stack), levelOne.toImmutable());
        });
        return stack;
    }

    public boolean matches(ItemStack stack) {
        return tag.map(stack::is).orElseGet(() -> stack.is(item))
                && without.stream().noneMatch(id -> carries(stack, id))
                && enchantment.map(key -> EnchantmentHelper.getEnchantmentsForCrafting(stack).keySet().stream().anyMatch(holder -> holder.is(key))).orElse(true)
                && mob.map(type -> isWeaponOf(stack, type)).orElse(true);
    }

    /** Whether the stack carries the data component type of that id (an unknown id never matches). */
    public static boolean carries(ItemStack stack, Identifier componentId) {
        DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(componentId);
        return type != null && stack.has(type);
    }

    private static boolean isWeaponOf(ItemStack stack, EntityType<?> type) {
        DataComponentType<EntityType<?>> mark = ExtensionHooks.mobWeaponComponent();
        return mark != null && stack.get(mark) == type;
    }
}
