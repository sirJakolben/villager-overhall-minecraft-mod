package com.villageroverhaul.traderework.mixin;

import com.villageroverhaul.traderework.cage.ZombieCaptives;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Two one-line hooks for the zombie villager a golem carries or has just dropped (ZombieCaptives), on the mob class itself because that is where Vanilla
 * asks:
 * - isNoAi: a held zombie villager has no AI - it cannot walk, attack or hit back. Not a saved flag, so there is nothing to clean up when the golem
 *   dies or the chunk unloads; the moment it is free, it thinks again;
 * - isSunBurnTick: a zombie villager in a golem's arms does not burn in the sun on the way to the cage (in the cage it is only data).
 */
@Mixin(Mob.class)
public abstract class CageMobMixin {

    @Inject(method = "isNoAi", at = @At("HEAD"), cancellable = true)
    private void vo$heldHasNoAi(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ZombieVillager zombie && ZombieCaptives.isHeld(zombie)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isSunBurnTick", at = @At("HEAD"), cancellable = true)
    private void vo$carriedDoesNotBurn(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ZombieVillager zombie && zombie.getVehicle() instanceof IronGolem) {
            cir.setReturnValue(false);
        }
    }
}
