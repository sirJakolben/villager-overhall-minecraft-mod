package com.smartvillager.professions.mixin;

import com.smartvillager.professions.boat.BoatUpgrades;
import com.smartvillager.professions.boat.Cannon;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The cannon charges at half the horse's speed (Obsidian Fisherman.md, 2026-10-12). Vanilla's jump charge counts ticks and turns them into the bar
 * fill (the same formula for every vehicle, so there is no hook per vehicle); right after it has done so, a held charge on a cannon boat is
 * recomputed from half the ticks. Only a cannon boat is touched.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerCannonChargeMixin {

    @Shadow
    private int jumpRidingTicks;

    @Shadow
    private float jumpRidingScale;

    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;aiStep()V"))
    private void smartvillager_professions$slowCannonCharge(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (this.jumpRidingTicks > 0 && player.getControlledVehicle() instanceof AbstractBoat boat && BoatUpgrades.state(boat).cannon()) {
            float ticks = this.jumpRidingTicks * Cannon.CHARGE_SPEED;
            this.jumpRidingScale = ticks < 10.0F ? ticks * 0.1F : 0.8F + 2.0F / (ticks - 9.0F) * 0.1F;
        }
    }
}
