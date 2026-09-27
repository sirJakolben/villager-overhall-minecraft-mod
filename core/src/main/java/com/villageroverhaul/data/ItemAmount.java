package com.villageroverhaul.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.villageroverhaul.api.ExtensionHooks;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.Optional;

/**
 * An item and a count. enchantment (optional) additionally requires the item to carry that
 * enchantment at any level - e.g. an enchanted book with Soul Speed, see RequiredEnchantmentCost.
 * mob (optional) requires it to be that mob's weapon - e.g. a zombie's iron sword for the Veteran's quests. The
 * mark itself comes from an extension (api/ExtensionHooks.mobWeaponComponent, Trade Rework's veteran/MobWeapons);
 * without one, no item matches.
 */
public record ItemAmount(Item item, int count, Optional<ResourceKey<Enchantment>> enchantment, Optional<EntityType<?>> mob) {

    public static final Codec<ItemAmount> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(ItemAmount::item),
            Codec.INT.fieldOf("count").forGetter(ItemAmount::count),
            ResourceKey.codec(Registries.ENCHANTMENT).optionalFieldOf("enchantment").forGetter(ItemAmount::enchantment),
            BuiltInRegistries.ENTITY_TYPE.byNameCodec().optionalFieldOf("mob").forGetter(ItemAmount::mob)
    ).apply(instance, ItemAmount::new));

    public ItemAmount(Item item, int count) {
        this(item, count, Optional.empty(), Optional.empty());
    }

    public ItemAmount(Item item, int count, Optional<ResourceKey<Enchantment>> enchantment) {
        this(item, count, enchantment, Optional.empty());
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
        return stack.is(item)
                && enchantment.map(key -> EnchantmentHelper.getEnchantmentsForCrafting(stack).keySet().stream().anyMatch(holder -> holder.is(key))).orElse(true)
                && mob.map(type -> isWeaponOf(stack, type)).orElse(true);
    }

    private static boolean isWeaponOf(ItemStack stack, EntityType<?> type) {
        DataComponentType<EntityType<?>> mark = ExtensionHooks.mobWeaponComponent();
        return mark != null && stack.get(mark) == type;
    }
}
