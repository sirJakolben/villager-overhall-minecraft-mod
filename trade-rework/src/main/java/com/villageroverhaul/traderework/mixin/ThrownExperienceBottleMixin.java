package com.villageroverhaul.traderework.mixin;

import com.villageroverhaul.traderework.librarian.ExperienceBottles;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Only the XP amount changes: the fixed ExperienceBottles.XP_POINTS instead of Vanilla's random
 * 3-11. Throwing, the impact, the splash particles and orb spawning (both the block-hit and the
 * entity-hit call) stay Vanilla. Applies to every Bottle o' Enchanting, wherever it came from.
 */
@Mixin(ThrownExperienceBottle.class)
public abstract class ThrownExperienceBottleMixin {

    @ModifyArg(
            method = "onHit",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;awardWithDirection(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;I)V"),
            index = 3
    )
    private int villageroverhaul$fixedBottleXp(int vanillaXp) {
        return ExperienceBottles.XP_POINTS;
    }
}
