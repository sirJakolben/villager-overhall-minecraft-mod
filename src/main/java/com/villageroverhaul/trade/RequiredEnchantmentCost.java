package com.villageroverhaul.trade;

import com.villageroverhaul.VillagerOverhaulMod;
import com.villageroverhaul.data.ItemAmount;
import com.villageroverhaul.veteran.MobWeapons;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;

/**
 * Trade/quest costs that need "this item WITH that enchantment, at any level" (e.g. the Librarian's hard
 * quest: an enchanted book with Soul Speed). Vanilla's ItemCost only knows exact component matches -
 * it would accept Soul Speed I alone and nothing else. So such a cost expects the enchantment at level I
 * (which is what the offer shows) plus the REQUIRES_ENCHANTMENT marker; for marked costs only,
 * ItemCostMixin swaps the exact comparison for matches below. Every other cost stays exact Vanilla.
 * The marker only lives on the cost's display stack, never on a real item.
 */
public final class RequiredEnchantmentCost {

    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, VillagerOverhaulMod.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> REQUIRES_ENCHANTMENT =
            DATA_COMPONENTS.registerComponentType("requires_enchantment",
                    builder -> builder.persistent(Unit.CODEC).networkSynchronized(Unit.STREAM_CODEC));

    private RequiredEnchantmentCost() {
    }

    /**
     * The Vanilla cost for an amount - with the enchantment requirement if it has one (and it exists), and
     * the mob-weapon mark if it asks for a mob's weapon. The mark is an exact component match, which is what
     * Vanilla's ItemCost does anyway; only the enchantment needs the marked "any level" comparison.
     */
    public static ItemCost of(ItemAmount amount, HolderLookup.Provider registries) {
        Optional<Holder.Reference<Enchantment>> enchantment = amount.enchantment()
                .flatMap(key -> registries.lookupOrThrow(Registries.ENCHANTMENT).get(key));
        if (enchantment.isEmpty() && amount.mob().isEmpty()) {
            return new ItemCost(amount.item(), amount.count());
        }
        DataComponentExactPredicate.Builder components = DataComponentExactPredicate.builder();
        // A mob weapon is always uncommon (MobWeaponEvents) - expecting that too shows the quest's item with its yellow name.
        amount.mob().ifPresent(mob -> components.expect(MobWeapons.MOB_WEAPON.get(), mob).expect(DataComponents.RARITY, Rarity.UNCOMMON));
        if (enchantment.isPresent()) {
            ItemEnchantments.Mutable levelOne = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            levelOne.set(enchantment.get(), 1);
            components.expect(EnchantmentHelper.getComponentType(new ItemStack(amount.item())), levelOne.toImmutable())
                    .expect(REQUIRES_ENCHANTMENT.get(), Unit.INSTANCE);
        }
        return new ItemCost(amount.item().builtInRegistryHolder(), amount.count(), components.build());
    }

    public static boolean isMarked(ItemCost cost) {
        return cost.itemStack().has(REQUIRES_ENCHANTMENT.get());
    }

    /** Every enchantment the cost shows must be on the item, at any level - and its mob-weapon mark, if it shows one. */
    public static boolean matches(ItemCost cost, DataComponentGetter actual) {
        DataComponentType<ItemEnchantments> type = EnchantmentHelper.getComponentType(cost.itemStack());
        ItemEnchantments required = cost.itemStack().getOrDefault(type, ItemEnchantments.EMPTY);
        ItemEnchantments present = actual.getOrDefault(type, ItemEnchantments.EMPTY);
        EntityType<?> requiredMob = cost.itemStack().get(MobWeapons.MOB_WEAPON.get());
        return required.keySet().stream().allMatch(enchantment -> present.getLevel(enchantment) > 0)
                && (requiredMob == null || requiredMob == actual.get(MobWeapons.MOB_WEAPON.get()));
    }
}
