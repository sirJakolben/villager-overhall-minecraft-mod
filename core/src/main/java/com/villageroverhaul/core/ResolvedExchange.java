package com.villageroverhaul.core;

import com.villageroverhaul.data.ItemAmount;
import com.villageroverhaul.data.ItemExchange;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * An ItemExchange (Trade or Quest) with its input/output/max-uses already scaled to the villager's
 * current rank. basePrice is the unscaled Base input - when input is cheaper, the offer shows the
 * difference as a struck-through discount, see VillagerOffers.toOffer. secondInput is the unscaled
 * second payment (costB), if the exchange has one. resultStack (optional) is the exact result with all
 * its components - a fallback profession's rolled Vanilla offer (fallback/FallbackCatalog) - used instead
 * of output's plain item.
 */
public record ResolvedExchange(Identifier id, ItemExchange.Tier tier, ItemAmount basePrice, ItemAmount input, Optional<ItemAmount> secondInput,
                               ItemAmount output, int usesRemaining, int maxUses, Optional<ItemStack> resultStack) {

    public ResolvedExchange(Identifier id, ItemExchange.Tier tier, ItemAmount basePrice, ItemAmount input, Optional<ItemAmount> secondInput,
                            ItemAmount output, int usesRemaining, int maxUses) {
        this(id, tier, basePrice, input, secondInput, output, usesRemaining, maxUses, Optional.empty());
    }
}
