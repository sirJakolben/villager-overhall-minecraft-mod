package com.villageroverhaul.traderework.librarian;

import com.villageroverhaul.traderework.TradeReworkMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.List;
import java.util.function.Consumer;

/**
 * Weapon / ranged / tool / armor book (Obsidian Librarian.md, "Master Trades"): enchanted at the enchanting table
 * like a plain book, but the table only rolls enchantments from the book's enchantment tag
 * (data/villageroverhaul/tags/enchantment/*_book.json - editable without code). The result is a Vanilla
 * enchanted book (decided 2026-09-26), so anvil, grindstone and the enchantment station treat it as usual.
 *
 * Both hooks are NeoForge item extensions the enchanting table calls itself - no mixin: isPrimaryItemFor
 * filters what the table offers, applyEnchantments turns the item into the enchanted book (EnchantmentMenuMixin
 * adds Vanilla's book cut: one of several rolled enchantments is dropped). Only table
 * enchantments (Vanilla's in_enchanting_table tag) can come up, the tag just narrows them down.
 */
public class SpecialBookItem extends Item {

    public static final TagKey<Enchantment> WEAPON_BOOK_ENCHANTMENTS = tag("weapon_book");
    public static final TagKey<Enchantment> RANGED_BOOK_ENCHANTMENTS = tag("ranged_book");
    public static final TagKey<Enchantment> TOOL_BOOK_ENCHANTMENTS = tag("tool_book");
    public static final TagKey<Enchantment> ARMOR_BOOK_ENCHANTMENTS = tag("armor_book");

    private final TagKey<Enchantment> enchantments;

    public SpecialBookItem(Properties properties, TagKey<Enchantment> enchantments) {
        super(properties);
        this.enchantments = enchantments;
    }

    private static TagKey<Enchantment> tag(String name) {
        return TagKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(TradeReworkMod.MODID, name));
    }

    @Override
    public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
        return enchantment.is(enchantments);
    }

    /** One gray line naming the category, e.g. "Weapon Enchant Book" (decided 2026-09-26). */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public ItemStack applyEnchantments(ItemStack stack, List<EnchantmentInstance> newEnchantments) {
        ItemStack book = stack.transmuteCopy(Items.ENCHANTED_BOOK);
        for (EnchantmentInstance instance : newEnchantments) {
            book.enchant(instance.enchantment(), instance.level());
        }
        return book;
    }
}
