package com.smartvillager.progress.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.smartvillager.progress.trade.RequiredEnchantmentCost;
import com.smartvillager.progress.trade.TagCost;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Only the two comparisons inside ItemCost.test change, and only for costs we marked. With RequiredEnchantmentCost.REQUIRES_ENCHANTMENT
 * the component comparison becomes "has this enchantment at any level" instead of "exactly these components". With TagCost.ANY_OF_TAG
 * the item comparison becomes "any item of the tag" (and the component comparison is part of it, see TagCost.matches). Every unmarked
 * cost stays Vanilla. Runs wherever Vanilla matches trade payments - result slot, taking the result, auto-filling the payment slots.
 */
@Mixin(ItemCost.class)
public abstract class ItemCostMixin {

    @WrapOperation(
            method = "test",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/core/Holder;)Z")
    )
    private boolean smartvillager_progress$anyItemOfTag(ItemStack stack, Holder<Item> item, Operation<Boolean> original) {
        ItemCost self = (ItemCost) (Object) this;
        return TagCost.isMarked(self) ? TagCost.matches(self, stack) : original.call(stack, item);
    }

    @WrapOperation(
            method = "test",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/component/DataComponentExactPredicate;test(Lnet/minecraft/core/component/DataComponentGetter;)Z")
    )
    private boolean smartvillager_progress$anyEnchantmentLevel(DataComponentExactPredicate predicate, DataComponentGetter actual, Operation<Boolean> original) {
        ItemCost self = (ItemCost) (Object) this;
        if (TagCost.isMarked(self)) {
            return true;
        }
        return RequiredEnchantmentCost.isMarked(self) ? RequiredEnchantmentCost.matches(self, actual) : original.call(predicate, actual);
    }
}
