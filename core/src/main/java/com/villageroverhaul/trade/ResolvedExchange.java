package com.villageroverhaul.trade;

import com.villageroverhaul.data.ItemAmount;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * An entry with its input/output/max stock already scaled to the villager's rank in its section. id is the
 * entry id (its stock key). basePrice is the unscaled Base input - when input is cheaper, the offer shows the
 * difference as a struck-through discount, see VillagerOffers.toOffer. secondInput is the unscaled second
 * payment (costB), if any. resultStack (optional) is the exact result with all its components - an entry rolled
 * from Vanilla trades (vanilla/VanillaCatalog) - used instead of output's plain item.
 */
public record ResolvedExchange(Identifier id, ItemAmount basePrice, ItemAmount input, Optional<ItemAmount> secondInput,
                               ItemAmount output, int usesRemaining, int maxUses, Optional<ItemStack> resultStack) {

    public ResolvedExchange(Identifier id, ItemAmount basePrice, ItemAmount input, Optional<ItemAmount> secondInput,
                            ItemAmount output, int usesRemaining, int maxUses) {
        this(id, basePrice, input, secondInput, output, usesRemaining, maxUses, Optional.empty());
    }
}
