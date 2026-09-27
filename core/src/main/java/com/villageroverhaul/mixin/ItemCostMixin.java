package com.villageroverhaul.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.villageroverhaul.trade.RequiredEnchantmentCost;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.world.item.trading.ItemCost;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Only the component comparison inside ItemCost.test changes, and only for costs we marked with
 * RequiredEnchantmentCost.REQUIRES_ENCHANTMENT: "has this enchantment at any level" instead of "exactly
 * these components". The item check before it, and every unmarked cost, stay Vanilla. Runs wherever
 * Vanilla matches trade payments - result slot, taking the result, auto-filling the payment slots.
 */
@Mixin(ItemCost.class)
public abstract class ItemCostMixin {

    @WrapOperation(
            method = "test",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/component/DataComponentExactPredicate;test(Lnet/minecraft/core/component/DataComponentGetter;)Z")
    )
    private boolean villageroverhaul$anyEnchantmentLevel(DataComponentExactPredicate predicate, DataComponentGetter actual, Operation<Boolean> original) {
        ItemCost self = (ItemCost) (Object) this;
        return RequiredEnchantmentCost.isMarked(self) ? RequiredEnchantmentCost.matches(self, actual) : original.call(predicate, actual);
    }
}
