package com.villageroverhaul.mixin;

import com.villageroverhaul.runesmith.UpgradeTemplates;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Upgrading with the Runesmith's upgrade template keeps the RELATIVE durability (2026-09-26): an iron
 * sword at 20 % comes out as a diamond sword at 20 %, not with the iron sword's absolute damage (which
 * would make it almost new). Vanilla copies the damage value as it is (TransmuteRecipe.createWithOriginalComponents);
 * this only rescales the finished result, and only for our template - Vanilla's netherite upgrade stays as
 * it is. See runesmith/UpgradeTemplates.rescaleDurability.
 */
@Mixin(SmithingTransformRecipe.class)
public abstract class SmithingUpgradeDurabilityMixin {

    @Inject(method = "assemble", at = @At("RETURN"))
    private void villageroverhaul$relativeDurability(SmithingRecipeInput input, CallbackInfoReturnable<ItemStack> cir) {
        if (UpgradeTemplates.isOurs(input.template())) {
            UpgradeTemplates.rescaleDurability(input.base(), cir.getReturnValue());
        }
    }
}
