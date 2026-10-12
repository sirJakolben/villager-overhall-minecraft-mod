package com.smartvillager.professions.boat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The parrot in the crow's nest (Obsidian Fisherman.md, 2026-10-12): while a player sits in a boat with a mast, a tame parrot of that player that is not
 * sitting and is near flies to the crow's nest and perches there as a decoration passenger (it takes no seat, BoatSlots.isPerson). It leaves when the player
 * gets out, when the boat takes damage, or when the boat is destroyed; after being thrown out it stays away a few seconds. Server only, one search twice a
 * second while a seated player has no parrot, one steered parrot at a time - nothing runs for boats without a mast.
 */
public final class ParrotPerch {

    /** How far from the boat a parrot is looked for, how often, and the patience: after this many ticks of flying it is placed on the nest. */
    private static final double SEARCH_RADIUS = 12.0;
    private static final int SEARCH_INTERVAL = 10;
    private static final int FLIGHT_TICKS = 100;
    /** Closer than this to the nest counts as arrived. */
    private static final double ARRIVED = 1.2;
    /** After being thrown out, no new perching for this long. */
    private static final int LOCK_TICKS = 100;
    /** The crow's nest's top, in blocks above the boat's hitbox bottom (model: the lookout cube's top at 47 px) and behind the mast's slot. */
    private static final double NEST_HEIGHT = 47.0 / 16.0;
    private static final double NEST_BEHIND_SLOT = 0.275;

    private record Flight(int parrotId, long since) {
    }

    private static final Map<AbstractBoat, Flight> FLIGHTS = new WeakHashMap<>();

    private ParrotPerch() {
    }

    /** The nest, relative to the boat's middle along the boat (positive = bow), for the boat's current mast. */
    public static double nestForward(AbstractBoat boat, BoatState state) {
        return BoatSlots.offset(boat, state.mastSlot()) - NEST_BEHIND_SLOT;
    }

    public static Vec3 nestPosition(AbstractBoat boat, BoatState state) {
        double yaw = Math.toRadians(boat.getYRot());
        double forward = nestForward(boat, state);
        return boat.position().add(-Math.sin(yaw) * forward, NEST_HEIGHT, Math.cos(yaw) * forward);
    }

    /** The riding position of a parrot relative to the boat (used by the passenger attachment). */
    public static Vec3 attachment(AbstractBoat boat, BoatState state) {
        double yaw = Math.toRadians(boat.getYRot());
        double forward = nestForward(boat, state);
        return new Vec3(-Math.sin(yaw) * forward, NEST_HEIGHT, Math.cos(yaw) * forward);
    }

    public static void tick(AbstractBoat boat, BoatState state, ServerLevel level) {
        List<Entity> parrots = boat.getPassengers().stream().filter(passenger -> passenger instanceof Parrot).toList();
        boolean seated = boat.getPassengers().stream().anyMatch(passenger -> passenger instanceof Player);
        if (!state.mast() || !seated) {
            parrots.forEach(ParrotPerch::dismount);
            FLIGHTS.remove(boat);
            return;
        }
        if (!parrots.isEmpty() || level.getGameTime() < state.parrotLockUntil()) {
            FLIGHTS.remove(boat);
            return;
        }

        Flight flight = FLIGHTS.get(boat);
        Parrot parrot = flight == null ? null : level.getEntity(flight.parrotId()) instanceof Parrot found && stillEligible(boat, found) ? found : null;
        if (parrot == null) {
            FLIGHTS.remove(boat);
            if (level.getGameTime() % SEARCH_INTERVAL != 0) {
                return;
            }
            parrot = level.getEntitiesOfClass(Parrot.class, boat.getBoundingBox().inflate(SEARCH_RADIUS), found -> stillEligible(boat, found)).stream()
                    .min((a, b) -> Double.compare(a.distanceToSqr(boat), b.distanceToSqr(boat))).orElse(null);
            if (parrot == null) {
                return;
            }
            flight = new Flight(parrot.getId(), level.getGameTime());
            FLIGHTS.put(boat, flight);
        }

        Vec3 nest = nestPosition(boat, state);
        if (parrot.position().distanceTo(nest) < ARRIVED || level.getGameTime() - flight.since() > FLIGHT_TICKS) {
            parrot.setInSittingPose(false);
            parrot.startRiding(boat, true, true);
            FLIGHTS.remove(boat);
        } else {
            parrot.getNavigation().moveTo(nest.x, nest.y, nest.z, 1.2);
            parrot.getMoveControl().setWantedPosition(nest.x, nest.y, nest.z, 1.2);
        }
    }

    /** A parrot that may perch: tame, its owner sits in this boat, not told to sit, not riding anything. */
    private static boolean stillEligible(AbstractBoat boat, Parrot parrot) {
        LivingEntity owner = parrot.getOwner();
        return parrot.isAlive() && parrot.isTame() && !parrot.isOrderedToSit() && !parrot.isPassenger()
                && owner != null && boat.hasPassenger(owner);
    }

    /** Throws every parrot out of the nest (the boat took damage, the player left) and keeps them from coming back at once. */
    public static void eject(AbstractBoat boat) {
        if (!(boat.level() instanceof ServerLevel level)) {
            return;
        }
        List<Entity> parrots = boat.getPassengers().stream().filter(passenger -> passenger instanceof Parrot).toList();
        if (!parrots.isEmpty()) {
            parrots.forEach(ParrotPerch::dismount);
            BoatUpgrades.setState(boat, BoatUpgrades.state(boat).withParrotLock(level.getGameTime() + LOCK_TICKS));
        }
        FLIGHTS.remove(boat);
    }

    private static void dismount(Entity parrot) {
        parrot.stopRiding();
        parrot.setDeltaMovement(parrot.getDeltaMovement().add(0.0, 0.3, 0.0));
    }
}
