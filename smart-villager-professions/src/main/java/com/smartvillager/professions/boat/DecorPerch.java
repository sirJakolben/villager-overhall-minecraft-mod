package com.smartvillager.professions.boat;

import com.smartvillager.professions.mixin.ServerPlayerShoulderAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.TagValueInput;
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
 * to the crow's nest of a sail, and a tame cat or dog walks to the stern deck of a stern (one parrot, one cat or dog). They are decoration passengers: they
 * take no seat (BoatSlots.canPerch). They stay when the player gets out and leave only when the boat takes damage or is destroyed; after being thrown out they stay
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
    private static final int MAX_PETS = 1;

    // Positions come from the Blockbench model (assets/Fisherman/boat.bbmodel); they are relative to the slot the decoration belongs to, in blocks.
    /** Where a parrot sits: on the crow's nest, 5 px below its top edge (47 px above the hitbox bottom), 0.275 blocks behind the sail's slot. */
    private static final double NEST_HEIGHT = 42.0 / 16.0;
    private static final double NEST_BEHIND_SLOT = 0.275;
    /** The stern deck: its surface 15 px above the hitbox bottom, its middle 21.5 px behind the middle of the boat when the rear slot is at -0.6 (a Vanilla boat). */
    private static final double DECK_HEIGHT = 15.0 / 16.0;
    private static final double DECK_MIDDLE = -21.5 / 16.0;
    private static final double DECK_MODEL_SLOT = -0.6;

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

    /** Where an animal rides, relative to the boat: the nest for a parrot, the middle of the stern deck for a cat or dog. */
    public static Vec3 attachment(AbstractBoat boat, BoatState state, Entity animal) {
        double yaw = Math.toRadians(boat.getYRot());
        double forward;
        double height;
        if (isParrot(animal)) {
            forward = BoatSlots.offset(boat, state.sailSlot()) - NEST_BEHIND_SLOT;
            height = NEST_HEIGHT;
        } else {
            forward = rearSlotOffset(boat) + (DECK_MIDDLE - DECK_MODEL_SLOT);
            height = DECK_HEIGHT;
        }
        // forward is along (-sin yaw, cos yaw)
        return new Vec3(-Math.sin(yaw) * forward, height, Math.cos(yaw) * forward);
    }

    public static void tick(AbstractBoat boat, BoatState state, ServerLevel level) {
        List<Entity> perched = boat.getPassengers().stream().filter(passenger -> BoatSlots.canPerch(boat, passenger)).toList();
        boolean seated = boat.getPassengers().stream().anyMatch(passenger -> passenger instanceof Player);
        // cats and dogs on the deck sit (the sitting pose, not Vanilla's sit order)
        for (Entity pet : perched) {
            if (pet instanceof TamableAnimal tamable && !isParrot(pet) && !tamable.isInSittingPose()) {
                tamable.setInSittingPose(true);
            }
        }
        // new animals only come aboard while a player sits in the boat; the ones already aboard stay when the player gets out
        if (!seated || level.getGameTime() < state.petLockUntil()) {
            TRIPS.remove(boat);
            return;
        }
        if (state.sail() && level.getGameTime() % SEARCH_INTERVAL == 0) {
            takeParrotsOffShoulders(boat, state, level);
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

    /**
     * With a sail aboard a parrot no longer sits on its owner's shoulder: it is taken off and put straight on the crow's nest (or, if that is taken, set
     * free beside the player like Vanilla does when a parrot leaves the shoulder).
     */
    private static void takeParrotsOffShoulders(AbstractBoat boat, BoatState state, ServerLevel level) {
        for (Entity passenger : boat.getPassengers()) {
            if (!(passenger instanceof ServerPlayer player)) {
                continue;
            }
            ServerPlayerShoulderAccessor shoulders = (ServerPlayerShoulderAccessor) player;
            if (isParrotTag(player.getShoulderEntityLeft())) {
                releaseParrot(boat, state, level, player, player.getShoulderEntityLeft());
                shoulders.smartvillager_professions$setShoulderEntityLeft(new CompoundTag());
            }
            if (isParrotTag(player.getShoulderEntityRight())) {
                releaseParrot(boat, state, level, player, player.getShoulderEntityRight());
                shoulders.smartvillager_professions$setShoulderEntityRight(new CompoundTag());
            }
        }
    }

    private static boolean isParrotTag(CompoundTag tag) {
        return !tag.isEmpty() && tag.read("id", EntityType.CODEC).filter(type -> type == EntityType.PARROT).isPresent();
    }

    private static void releaseParrot(AbstractBoat boat, BoatState state, ServerLevel level, ServerPlayer player, CompoundTag tag) {
        EntityType.create(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag), level, EntitySpawnReason.LOAD).ifPresent(entity -> {
            if (entity instanceof TamableAnimal parrot) {
                parrot.setOwner(player);
                if (hasFreePerch(boat, parrot)) {
                    Vec3 nest = boat.position().add(attachment(boat, state, parrot));
                    parrot.setPos(nest.x, nest.y, nest.z);
                    level.addWithUUID(parrot);
                    parrot.startRiding(boat, true, true);
                } else {
                    parrot.setPos(player.getX(), player.getY() + 0.7, player.getZ());
                    level.addWithUUID(parrot);
                }
            }
        });
    }

    /** Whether the boat has a free perch for this kind of animal (one parrot, one cat or dog). */
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

    /** Throws every perched animal out (the boat took damage) and keeps them from coming back at once. */
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
        if (animal instanceof TamableAnimal tamable && !isParrot(animal)) {
            tamable.setInSittingPose(false);
        }
        animal.setDeltaMovement(animal.getDeltaMovement().add(0.0, 0.3, 0.0));
    }
}
