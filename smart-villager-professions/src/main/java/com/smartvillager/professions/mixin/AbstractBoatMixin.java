package com.smartvillager.professions.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.smartvillager.professions.boat.BoatHitbox;
import com.smartvillager.professions.boat.BoatSeats;
import com.smartvillager.professions.boat.BoatSlots;
import com.smartvillager.professions.boat.BoatState;
import com.smartvillager.professions.boat.BoatUpgrades;
import com.smartvillager.professions.boat.Cannon;
import com.smartvillager.professions.boat.DecorPerch;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A boat with parts built in (Obsidian Fisherman.md, 2026-10-12) differs from a plain one in a handful of narrow spots, all through the boat's synced
 * BoatState, so client prediction and server agree - and a boat without parts takes none of these branches:
 * - Seats: the three capacity checks (taking a passenger, the boat picking up mobs, animals turning) compare with the FREE slots instead of Vanilla's seat
 *   count, and a passenger sits at the position of a free slot (a filled slot is a seat less, wherever it is).
 * - Animals may perch on the decorations: a parrot in the crow's nest of a sail, a cat or dog on a stern deck. They are decoration passengers and take no seat
 *   (BoatSlots.canPerch, DecorPerch).
 * - A stern lengthens the hitbox by 50 % towards the stern (BoatHitbox): the box is refreshed here whenever the boat turns or gets its stern.
 * - With a cannon: 40 % less top speed (a stern: 40 % less turn rate), and Space charges the cannon - the boat becomes a PlayerRideableJumping, so Vanilla's own horse jump bar, charge
 *   and release packet do the rest.
 */
@Mixin(AbstractBoat.class)
public abstract class AbstractBoatMixin implements PlayerRideableJumping, BoatSeats {

    @Shadow
    private float deltaRotation;

    @Shadow
    protected abstract int getMaxPassengers();

    /** The stern and the yaw the box was last made for - the box is refreshed when either changes. */
    @Unique
    private boolean smartvillager_professions$boxStern;

    @Unique
    private float smartvillager_professions$boxYaw;

    @Shadow
    protected abstract double rideHeight(EntityDimensions dimensions);

    private AbstractBoat smartvillager_professions$self() {
        return (AbstractBoat) (Object) this;
    }

    private BoatState smartvillager_professions$state() {
        return BoatUpgrades.state(smartvillager_professions$self());
    }

    @Override
    public int smartvillager_professions$rawSeats() {
        return this.getMaxPassengers();
    }

    @WrapOperation(
            method = {"canAddPassenger", "tick", "positionRider"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/boat/AbstractBoat;getMaxPassengers()I")
    )
    private int smartvillager_professions$freeSlots(AbstractBoat boat, Operation<Integer> original) {
        return BoatSlots.capacity(boat, original.call(boat));
    }

    /** An animal that has a perch on this boat is let on whatever the free seats - as long as its perch is free. */
    @ModifyReturnValue(method = "canAddPassenger", at = @At("RETURN"))
    private boolean smartvillager_professions$animalMayPerch(boolean allowed, Entity passenger) {
        AbstractBoat boat = smartvillager_professions$self();
        if (BoatSlots.canPerch(boat, passenger)) {
            return DecorPerch.hasFreePerch(boat, passenger) && !boat.isEyeInFluid(FluidTags.WATER);
        }
        return allowed;
    }

    /** Seats at the position of a free slot, perched animals on their decoration; a boat without parts keeps Vanilla's seating. */
    @Inject(method = "getPassengerAttachmentPoint", at = @At("HEAD"), cancellable = true)
    private void smartvillager_professions$seat(Entity passenger, EntityDimensions dimensions, float scale, CallbackInfoReturnable<Vec3> cir) {
        AbstractBoat boat = smartvillager_professions$self();
        BoatState state = smartvillager_professions$state();
        if (BoatSlots.canPerch(boat, passenger)) {
            cir.setReturnValue(DecorPerch.attachment(boat, state, passenger));
            return;
        }
        double offset = BoatSlots.seatOffset(boat, passenger);
        if (!Double.isNaN(offset)) {
            cir.setReturnValue(new Vec3(0.0, this.rideHeight(dimensions), offset).yRot(-boat.getYRot() * (float) (Math.PI / 180.0)));
        }
    }

    @WrapOperation(method = "controlBoat", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 smartvillager_professions$slowerPush(Vec3 movement, double x, double y, double z, Operation<Vec3> original) {
        if (smartvillager_professions$state().cannon()) {
            return original.call(movement, x * Cannon.SPEED_FACTOR, y, z * Cannon.SPEED_FACTOR);
        }
        return original.call(movement, x, y, z);
    }

    /** Both A/D steps change deltaRotation by one; with a stern (-40 %) only by its share - the top speed is not touched. */
    @WrapOperation(method = "controlBoat", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/vehicle/boat/AbstractBoat;deltaRotation:F", opcode = Opcodes.PUTFIELD))
    private void smartvillager_professions$slowerTurn(AbstractBoat boat, float value, Operation<Void> original) {
        BoatState state = smartvillager_professions$state();
        if (state.stern()) {
            float factor = BoatUpgrades.STERN_TURN_FACTOR;
            float old = this.deltaRotation;
            original.call(boat, old + (value - old) * factor);
        } else {
            original.call(boat, value);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void smartvillager_professions$tickParts(CallbackInfo ci) {
        AbstractBoat boat = smartvillager_professions$self();
        BoatState state = smartvillager_professions$state();
        if (state.cannon()) {
            Cannon.tickSoot(boat, state);
        }
        if (state.stern() != smartvillager_professions$boxStern || state.stern() && boat.getYRot() != smartvillager_professions$boxYaw) {
            AABB box = BoatHitbox.of(boat, boat.getDimensions(boat.getPose()).makeBoundingBox(boat.position()));
            if (smartvillager_professions$boxStern && state.stern() && boat.level().getBlockCollisions(boat, box).iterator().hasNext()) {
                // the long stern would swing into a wall: the boat does not turn there (a plain boat's square box cannot clip like this)
                boat.setYRot(smartvillager_professions$boxYaw);
                this.deltaRotation = 0.0F;
                box = BoatHitbox.of(boat, boat.getDimensions(boat.getPose()).makeBoundingBox(boat.position()));
            }
            smartvillager_professions$boxStern = state.stern();
            smartvillager_professions$boxYaw = boat.getYRot();
            boat.setBoundingBox(box);
        }
        if ((state.sail() || state.stern()) && boat.level() instanceof ServerLevel level) {
            DecorPerch.tick(boat, state, level);
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
