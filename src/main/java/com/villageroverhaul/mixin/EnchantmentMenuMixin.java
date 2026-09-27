package com.villageroverhaul.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.villageroverhaul.librarian.SpecialBookItem;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Vanilla drops one random enchantment from a plain book's roll when it has several; the special books
 * get the same cut (decided 2026-09-26: more targeted than a book, not more generous). Only the single
 * "is this a book?" check in getEnchantmentList also answers yes for a SpecialBookItem. The cut stays
 * inside that method, so the table's hover preview and the applied result are computed from the same,
 * already cut list - exactly as for a plain book.
 */
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {

    @WrapOperation(
            method = "getEnchantmentList",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Ljava/lang/Object;)Z")
    )
    private boolean villageroverhaul$cutLikeABook(ItemStack stack, Object item, Operation<Boolean> original) {
        return original.call(stack, item) || stack.getItem() instanceof SpecialBookItem;
    }
}
