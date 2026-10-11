package com.smartvillager.professions.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.smartvillager.professions.boat.BoatState;
import com.smartvillager.professions.boat.BoatUpgrades;
import com.smartvillager.professions.boat.Cannon;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A boat with a cannon (Obsidian Fisherman.md, 2026-10-12) differs from a normal one in a handful of narrow spots, all through the boat's
 * synced BoatState, so client prediction and server agree: one seat less, and that seat at the back; 30 % less speed and turn rate; and
 * Space charges the cannon - the boat becomes a PlayerRideableJumping, so Vanilla's own horse jump bar, charge and release packet do the rest.
 * Boats without a cannon take none of these branches.
 */
@Mixin(AbstractBoat.class)
public abstract class AbstractBoatMixin implements PlayerRideableJumping {

    @Shadow
    private float deltaRotation;

    private AbstractBoat smartvillager_professions$self() {
        return (AbstractBoat) (Object) this;
    }

    private BoatState smartvillager_professions$state() {
        return BoatUpgrades.state(smartvillager_professions$self());
    }

    @ModifyReturnValue(method = "getMaxPassengers", at = @At("RETURN"))
    private int smartvillager_professions$oneSeatLess(int seats) {
        return smartvillager_professions$state().cannon() ? Math.max(1, seats - 1) : seats;
    }

    /** The single passenger of a cannon boat sits at the back, where Vanilla seats the second one. */
    @WrapOperation(method = "getPassengerAttachmentPoint", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/boat/AbstractBoat;getSinglePassengerXOffset()F"))
    private float smartvillager_professions$rearSeat(AbstractBoat boat, Operation<Float> original) {
        return smartvillager_professions$state().cannon() ? Cannon.REAR_SEAT_OFFSET : original.call(boat);
    }

    @WrapOperation(method = "controlBoat", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 smartvillager_professions$slowerPush(Vec3 movement, double x, double y, double z, Operation<Vec3> original) {
        if (smartvillager_professions$state().cannon()) {
            return original.call(movement, x * Cannon.SPEED_FACTOR, y, z * Cannon.SPEED_FACTOR);
        }
        return original.call(movement, x, y, z);
    }

    /** Both A/D steps change deltaRotation by one; for a cannon boat only by its share. */
    @WrapOperation(method = "controlBoat", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/vehicle/boat/AbstractBoat;deltaRotation:F", opcode = Opcodes.PUTFIELD))
    private void smartvillager_professions$slowerTurn(AbstractBoat boat, float value, Operation<Void> original) {
        if (smartvillager_professions$state().cannon()) {
            float old = this.deltaRotation;
            original.call(boat, old + (value - old) * Cannon.TURN_FACTOR);
        } else {
            original.call(boat, value);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void smartvillager_professions$soot(CallbackInfo ci) {
        BoatState state = smartvillager_professions$state();
        if (state.cannon()) {
            Cannon.tickSoot(smartvillager_professions$self(), state);
        }
    }

    // PlayerRideableJumping: only a cannon boat can be charged; the shot is fired when the release arrives on the server.

    @Override
    public boolean canJump() {
        return smartvillager_professions$state().cannon();
    }

    @Override
    public int getJumpCooldown() {
        return smartvillager_professions$state().cooldown(smartvillager_professions$self().level().getGameTime());
    }

    @Override
    public void onPlayerJump(int jumpAmount) {
    }

    @Override
    public void handleStartJump(int jumpScale) {
        Cannon.fire(smartvillager_professions$self(), jumpScale);
    }

    @Override
    public void handleStopJump() {
    }
}
