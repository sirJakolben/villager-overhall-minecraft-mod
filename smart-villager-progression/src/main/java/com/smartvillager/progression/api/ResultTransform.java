package com.smartvillager.progression.api;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * Decides the stack in the trade window's result slot from what the player put in, instead of the offer's fixed result -
 * e.g. the Professions' boat upgrades: the player's own boat (whatever wood) comes back with a cannon. Set with
 * ExtensionHooks.setResultTransform; asked on both sides every time the result slot is refreshed (menu/VillagerMenu),
 * so it must be pure and cheap. Return result itself (same object) for every offer that isn't yours.
 */
@FunctionalInterface
public interface ResultTransform {

    /** @param result the offer's own result; paymentA/paymentB are the two payment slots as they are now (either may be empty). */
    ItemStack apply(MerchantOffer offer, ItemStack paymentA, ItemStack paymentB, ItemStack result);
}
