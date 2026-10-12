package com.smartvillager.professions.boat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.boat.AbstractChestBoat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntPredicate;

/**
 * The slots of a boat (Obsidian Fisherman.md, 2026-10-12): one per seat the boat itself offers (Vanilla 2, a chest boat 1, a modded boat maybe 3), numbered
 * front to back, each at a position along the boat. A part fills a slot and the slot is a seat less; the passengers sit in the slots that are left.
 * Positions are in blocks from the boat's middle, positive towards the bow. A mod whose boat seats people elsewhere registers its own Layout.
 */
public final class BoatSlots {

    /** The positions of a boat's seats, front first - one value per seat. */
    @FunctionalInterface
    public interface Layout {
        double[] offsets(AbstractBoat boat, int seats);
    }

    private static final Map<EntityType<?>, Layout> LAYOUTS = new ConcurrentHashMap<>();
    /** Vanilla's seats: the first passenger of two sits 0.2 forward, the second 0.6 back; a single one in the middle. */
    private static final double FRONT = 0.2;
    private static final double REAR = -0.6;

    private BoatSlots() {
    }

    public static void registerLayout(EntityType<?> boatType, Layout layout) {
        LAYOUTS.put(boatType, layout);
    }

    /** The boat's own seat count - the number of slots. */
    public static int seats(AbstractBoat boat) {
        return ((BoatSeats) boat).smartvillager_professions$rawSeats();
    }

    public static double[] offsets(AbstractBoat boat) {
        int seats = seats(boat);
        Layout layout = LAYOUTS.get(boat.getType());
        if (layout != null) {
            return layout.offsets(boat, seats);
        }
        double[] offsets = new double[seats];
        if (seats == 2) {
            offsets[0] = FRONT;
            offsets[1] = REAR;
        } else if (seats > 2) {
            double step = (0.4 - REAR) / (seats - 1);
            for (int i = 0; i < seats; i++) {
                offsets[i] = 0.4 - step * i;
            }
        }
        return offsets;
    }

    public static double offset(AbstractBoat boat, int slot) {
        double[] offsets = offsets(boat);
        return slot >= 0 && slot < offsets.length ? offsets[slot] : 0.0;
    }

    /** Whether this animal is the kind that perches on the boat's decoration: a parrot on a sail, a cat or dog (wolf) on a stern. */
    public static boolean canPerch(AbstractBoat boat, Entity animal) {
        BoatState state = BoatUpgrades.state(boat);
        return animal instanceof Parrot && state.sail() || (animal instanceof Cat || animal instanceof Wolf) && state.stern();
    }

    /** Perched animals are decoration, not passengers: they take no slot. */
    public static boolean isPerson(AbstractBoat boat, Entity passenger) {
        return !canPerch(boat, passenger);
    }

    public static List<Integer> freeSlots(AbstractBoat boat, BoatState state) {
        List<Integer> free = new ArrayList<>();
        for (int slot = 0; slot < seats(boat); slot++) {
            if (state.isFree(slot)) {
                free.add(slot);
            }
        }
        return free;
    }

    /** What the three capacity checks of AbstractBoat should compare the passenger count with: the free slots, plus the perched animals aboard (they are counted among the passengers). */
    public static int capacity(AbstractBoat boat, int vanillaMax) {
        int perched = (int) boat.getPassengers().stream().filter(passenger -> canPerch(boat, passenger)).count();
        return Math.max(0, vanillaMax - BoatUpgrades.state(boat).filledSlots()) + perched;
    }

    /** Where this passenger sits along the boat in blocks, or NaN while the boat has no part in a slot (then it is Vanilla's own seating). */
    public static double seatOffset(AbstractBoat boat, Entity passenger) {
        BoatState state = BoatUpgrades.state(boat);
        if (state.filledSlots() == 0) {
            return Double.NaN;
        }
        List<Entity> people = boat.getPassengers().stream().filter(other -> isPerson(boat, other)).toList();
        List<Integer> free = freeSlots(boat, state);
        if (free.isEmpty()) {
            return 0.0;
        }
        int index = Math.max(0, people.indexOf(passenger));
        return offset(boat, free.get(Math.min(index, free.size() - 1)));
    }

    /** The allowed slot closest to this position along the boat, or -1. */
    public static int nearest(AbstractBoat boat, double forward, IntPredicate allowed) {
        int best = -1;
        double bestDistance = Double.MAX_VALUE;
        for (int slot = 0; slot < seats(boat); slot++) {
            double distance = Math.abs(forward - offset(boat, slot));
            if (allowed.test(slot) && distance < bestDistance) {
                best = slot;
                bestDistance = distance;
            }
        }
        return best;
    }

    /** How far a horizontal offset (dx, dz) from the boat reaches along the boat towards the bow. */
    public static double forwardOf(AbstractBoat boat, double dx, double dz) {
        double yaw = Math.toRadians(boat.getYRot());
        return dx * -Math.sin(yaw) + dz * Math.cos(yaw);
    }

    /** Whether this kind of part may go into this slot now. A Vanilla chest boat has its own chest and takes only decoration. */
    public static boolean canInstall(AbstractBoat boat, BoatState state, BoatPartType type, int slot) {
        if (slot < 0 || slot >= seats(boat)) {
            return false;
        }
        if (type.kind() == BoatPartType.Kind.DECO) {
            return state.deco(type).isEmpty() && (type != BoatParts.STERN || slot == seats(boat) - 1);
        }
        if (boat instanceof AbstractChestBoat || !state.isFree(slot)) {
            return false;
        }
        return type != BoatParts.CANNON || slot == 0;
    }

    /** After a part took a slot, passengers that no longer have a seat get out. */
    public static void trimPassengers(AbstractBoat boat) {
        BoatState state = BoatUpgrades.state(boat);
        int free = Math.max(0, seats(boat) - state.filledSlots());
        List<Entity> people = boat.getPassengers().stream().filter(passenger -> isPerson(boat, passenger)).toList();
        for (int i = free; i < people.size(); i++) {
            people.get(i).stopRiding();
        }
    }
}
