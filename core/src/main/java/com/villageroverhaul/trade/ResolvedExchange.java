package com.villageroverhaul.trade;

import com.villageroverhaul.data.ItemAmount;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * An entry with its input/output/max stock already scaled to the villager's rank in its section. id is the
 * entry id (its stock key). basePrice is the unscaled Base input - when input is cheaper, the offer shows the
 * difference as a struck-through discount, see VillagerOffers.toOffer. baseOutputCount is the same for the
 * result: the unscaled Base yield - when output gives more, the screen shows the Base count struck through
 * next to it (network/VillagerOffersPayload carries it per row). secondInput is the unscaled second payment
 * (costB), if any. resultStack (optional) is the exact result with all its components - an entry rolled from
 * Vanilla trades (vanilla/VanillaCatalog) - used instead of output's plain item.
 */
public record ResolvedExchange(Identifier id, ItemAmount basePrice, ItemAmount input, Optional<ItemAmount> secondInput,
                               int baseOutputCount, ItemAmount output, int usesRemaining, int maxUses, Optional<ItemStack> resultStack) {

    public ResolvedExchange(Identifier id, ItemAmount basePrice, ItemAmount input, Optional<ItemAmount> secondInput,
                            int baseOutputCount, ItemAmount output, int usesRemaining, int maxUses) {
        this(id, basePrice, input, secondInput, baseOutputCount, output, usesRemaining, maxUses, Optional.empty());
    }
}
