package com.villageroverhaul.traderework.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.villageroverhaul.traderework.veteran.MobWeaponDefinition;
import com.villageroverhaul.traderework.veteran.MobWeapons;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A mob weapon with charge_reduction draws faster (Veteran.md, 2026-09-27: the piglin's crossbow, 20 %).
 * Hooked at the return of the one method every crossbow draw asks - after Quick Charge, so both stack - and
 * only for marked crossbows; every other crossbow keeps its Vanilla draw time. Runs on both sides, like
 * Vanilla's own charge, so the animation matches.
 */
@Mixin(CrossbowItem.class)
public abstract class CrossbowChargeMixin {

    @ModifyReturnValue(method = "getChargeDuration", at = @At("RETURN"))
    private static int villageroverhaul$fasterMobCrossbow(int duration, ItemStack crossbow, LivingEntity user) {
        float reduction = MobWeapons.mobOf(crossbow).flatMap(MobWeapons::definition)
                .map(MobWeaponDefinition::chargeReduction).orElse(0.0F);
        return reduction > 0.0F && reduction < 1.0F ? Math.max(1, Math.round(duration * (1.0F - reduction))) : duration;
    }
}
