package com.smartvillager.professions.boat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The animals on the boat's decoration (Obsidian Fisherman.md, 2026-10-12): while a player sits in the boat, the player's tame parrot (not sitting, near) flies
 * to the crow's nest of a sail, and tame cats and dogs walk to the stern deck of a stern (one parrot, two cats/dogs). They are decoration passengers: they
 * take no seat (BoatSlots.canPerch). They leave when the player gets out, when the boat takes damage, or when it is destroyed; after being thrown out they stay
 * away a few seconds. Server only; one search twice a second while a seated player has a free perch, one steered animal at a time - nothing runs for a boat
 * without a sail or a stern.
 */
public final class DecorPerch {

    /** How far from the boat an animal is looked for, how often, and the patience: after this many ticks on the way it is placed on the perch. */
    private static final double SEARCH_RADIUS = 12.0;
    private static final int SEARCH_INTERVAL = 10;
    private static final int TRIP_TICKS = 100;
    /** A parrot counts as arrived this close to the nest; a cat or dog this close (horizontally) to the boat. */
    private static final double PARROT_ARRIVED = 1.2;
    private static final double PET_ARRIVED = 2.5;
    /** After being thrown out, no new boarding for this long. */
    private static final int LOCK_TICKS = 100;
    private static final int MAX_PARROTS = 1;
    private static final int MAX_PETS = 2;

    // Positions come from the Blockbench model (assets/Fisherman/boat.bbmodel); they are relative to the slot the decoration belongs to, in blocks.
    /** The crow's nest's top: 47 px above the hitbox bottom, 0.275 blocks behind the sail's slot. */
    private static final double NEST_HEIGHT = 47.0 / 16.0;
    private static final double NEST_BEHIND_SLOT = 0.275;
    /** The stern deck: its surface 15 px above the hitbox bottom, its middle 21.5 px behind the middle of the boat when the rear slot is at -0.6 (a Vanilla boat). */
    private static final double DECK_HEIGHT = 15.0 / 16.0;
    private static final double DECK_MIDDLE = -21.5 / 16.0;
    private static final double DECK_MODEL_SLOT = -0.6;
    /** The two animals sit this far to either side of the boat's centre line. */
    private static final double PET_SIDE = 0.3;

    private record Trip(int animalId, long since) {
    }

    private static final Map<AbstractBoat, Trip> TRIPS = new WeakHashMap<>();

    private DecorPerch() {
    }

    private static boolean isParrot(Entity animal) {
        return animal instanceof Parrot;
    }

    private static double rearSlotOffset(AbstractBoat boat) {
        return BoatSlots.offset(boat, BoatSlots.seats(boat) - 1);
    }

    /** Where an animal rides, relative to the boat: the nest for a parrot, a place on the stern deck for a cat or dog (the first sits on one side, the second on the other). */
    public static Vec3 attachment(AbstractBoat boat, BoatState state, Entity animal) {
        double yaw = Math.toRadians(boat.getYRot());
        double forward;
        double side = 0.0;
        double height;
        if (isParrot(animal)) {
            forward = BoatSlots.offset(boat, state.sailSlot()) - NEST_BEHIND_SLOT;
            height = NEST_HEIGHT;
        } else {
            forward = rearSlotOffset(boat) + (DECK_MIDDLE - DECK_MODEL_SLOT);
            height = DECK_HEIGHT;
            List<Entity> pets = boat.getPassengers().stream().filter(other -> !isParrot(other) && BoatSlots.canPerch(boat, other)).toList();
            side = pets.indexOf(animal) % 2 == 0 ? PET_SIDE : -PET_SIDE;
        }
        // forward is along (-sin yaw, cos yaw), side across it
        return new Vec3(-Math.sin(yaw) * forward + Math.cos(yaw) * side, height, Math.cos(yaw) * forward + Math.sin(yaw) * side);
    }

    public static void tick(AbstractBoat boat, BoatState state, ServerLevel level) {
        List<Entity> perched = boat.getPassengers().stream().filter(passenger -> BoatSlots.canPerch(boat, passenger)).toList();
        boolean seated = boat.getPassengers().stream().anyMatch(passenger -> passenger instanceof Player);
        if (!seated) {
            perched.forEach(DecorPerch::dismount);
            TRIPS.remove(boat);
            return;
        }
        if (level.getGameTime() < state.petLockUntil()) {
            TRIPS.remove(boat);
            return;
        }
        boolean wantParrot = state.sail() && perched.stream().filter(DecorPerch::isParrot).count() < MAX_PARROTS;
        boolean wantPet = state.stern() && perched.stream().filter(animal -> !isParrot(animal)).count() < MAX_PETS;
        if (!wantParrot && !wantPet) {
            TRIPS.remove(boat);
            return;
        }

        Trip trip = TRIPS.get(boat);
        TamableAnimal animal = trip == null ? null : level.getEntity(trip.animalId()) instanceof TamableAnimal found && eligible(boat, found, wantParrot, wantPet) ? found : null;
        if (animal == null) {
            TRIPS.remove(boat);
            if (level.getGameTime() % SEARCH_INTERVAL != 0) {
                return;
            }
            animal = level.getEntitiesOfClass(TamableAnimal.class, boat.getBoundingBox().inflate(SEARCH_RADIUS), found -> eligible(boat, found, wantParrot, wantPet)).stream()
                    .min((a, b) -> Double.compare(a.distanceToSqr(boat), b.distanceToSqr(boat))).orElse(null);
            if (animal == null) {
                return;
            }
            trip = new Trip(animal.getId(), level.getGameTime());
            TRIPS.put(boat, trip);
        }

        Vec3 target = boat.position().add(attachment(boat, state, animal));
        boolean arrived = isParrot(animal)
                ? animal.position().distanceTo(target) < PARROT_ARRIVED
                : animal.position().subtract(boat.position()).horizontalDistance() < PET_ARRIVED;
        if (arrived || level.getGameTime() - trip.since() > TRIP_TICKS) {
            animal.setInSittingPose(false);
            animal.startRiding(boat, true, true);
            TRIPS.remove(boat);
        } else if (isParrot(animal)) {
            animal.getNavigation().moveTo(target.x, target.y, target.z, 1.2);
            animal.getMoveControl().setWantedPosition(target.x, target.y, target.z, 1.2);
        } else {
            animal.getNavigation().moveTo(boat.getX(), boat.getY(), boat.getZ(), 1.2);
        }
    }

    /** Whether the boat has a free perch for this kind of animal (one parrot, two cats or dogs). */
    public static boolean hasFreePerch(AbstractBoat boat, Entity animal) {
        long same = boat.getPassengers().stream().filter(other -> isParrot(other) == isParrot(animal) && BoatSlots.canPerch(boat, other)).count();
        return same < (isParrot(animal) ? MAX_PARROTS : MAX_PETS);
    }

    /** An animal that may board: tame, its owner sits in this boat, not told to sit, not riding anything, and of the kind a free perch wants. */
    private static boolean eligible(AbstractBoat boat, TamableAnimal animal, boolean wantParrot, boolean wantPet) {
        LivingEntity owner = animal.getOwner();
        boolean kindWanted = isParrot(animal) ? wantParrot : wantPet && BoatSlots.canPerch(boat, animal);
        return kindWanted && animal.isAlive() && animal.isTame() && !animal.isOrderedToSit() && !animal.isPassenger()
                && owner != null && boat.hasPassenger(owner);
    }

    /** Throws every perched animal out (the boat took damage, the player left) and keeps them from coming back at once. */
    public static void eject(AbstractBoat boat) {
        if (!(boat.level() instanceof ServerLevel level)) {
            return;
        }
        List<Entity> perched = boat.getPassengers().stream().filter(passenger -> BoatSlots.canPerch(boat, passenger)).toList();
        if (!perched.isEmpty()) {
            perched.forEach(DecorPerch::dismount);
            BoatUpgrades.setState(boat, BoatUpgrades.state(boat).withPetLock(level.getGameTime() + LOCK_TICKS));
        }
        TRIPS.remove(boat);
    }

    private static void dismount(Entity animal) {
        animal.stopRiding();
        animal.setDeltaMovement(animal.getDeltaMovement().add(0.0, 0.3, 0.0));
    }
}
