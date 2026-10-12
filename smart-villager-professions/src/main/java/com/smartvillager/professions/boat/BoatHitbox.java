package com.smartvillager.professions.boat;

import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.phys.AABB;

/**
 * A stern makes the boat's hitbox 50 % longer, towards the stern only (Obsidian Fisherman.md, 2026-10-12). An entity's box cannot turn with the boat, so this is the
 * box around the lengthened rectangle as the boat is turned right now - it has to be refreshed whenever the boat turns (mixin/AbstractBoatMixin does, each tick),
 * and a boat that would swing its stern into a wall by turning does not turn there.
 */
public final class BoatHitbox {

    /** The extra length at the rear, in boat widths (the boat is as long as it is wide, so +50 %). */
    private static final double EXTRA_REAR = 0.5;

    private BoatHitbox() {
    }

    /** The boat's Vanilla box (square, centred on the boat) turned into the box of the lengthened rectangle; unchanged without a stern. */
    public static AABB of(AbstractBoat boat, AABB vanilla) {
        if (boat.tickCount == 0 || !BoatUpgrades.state(boat).stern()) {
            return vanilla;
        }
        double width = vanilla.getXsize();
        double half = width / 2.0;
        double rear = half + width * EXTRA_REAR;
        double yaw = Math.toRadians(boat.getYRot());
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double sideX = Math.cos(yaw);
        double sideZ = Math.sin(yaw);
        double centreX = (vanilla.minX + vanilla.maxX) / 2.0;
        double centreZ = (vanilla.minZ + vanilla.maxZ) / 2.0;
        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;
        for (double along : new double[]{half, -rear}) {
            for (double across : new double[]{half, -half}) {
                double x = centreX + forwardX * along + sideX * across;
                double z = centreZ + forwardZ * along + sideZ * across;
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minZ = Math.min(minZ, z);
                maxZ = Math.max(maxZ, z);
            }
        }
        return new AABB(minX, vanilla.minY, minZ, maxX, vanilla.maxY, maxZ);
    }
}
