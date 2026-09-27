package com.villageroverhaul.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.villageroverhaul.runesmith.BonusDurability;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The Runesmith's padding takes damage first (runesmith/BonusDurability). Hooked at the return of
 * ItemStack.processDurabilityChange - the one place every durability loss goes through (hurtAndBreak for
 * tools, weapons and armor, hurtWithoutBreaking), after Vanilla has applied Unbreaking. Only the amount that
 * reaches the Vanilla durability changes, and only for items that carry padding; repairs (negative amounts)
 * and every other item stay untouched.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackBonusDurabilityMixin {

    @ModifyReturnValue(method = "processDurabilityChange(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)I", at = @At("RETURN"))
    private int villageroverhaul$paddingFirst(int damage) {
        return BonusDurability.absorb((ItemStack) (Object) this, damage);
    }
}
