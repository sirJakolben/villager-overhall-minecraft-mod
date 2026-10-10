package com.villageroverhaul.traderework.cage;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.phys.Vec3;

/**
 * Where a carried zombie villager lies: across the golem's two arms held out in front, head to the golem's right. The server puts the passenger
 * there (IronGolemMixin), the client turns it onto its back in that direction (client/CarryClient). The numbers are in blocks.
 */
public final class CarryPose {

    /** The middle of the zombie villager, in front of the golem's body. */
    public static final double FORWARD = 1.1;
    public static final double HEIGHT = 1.55;
    /**
     * A lying mob is drawn like a sleeping player: shifted back from its position by the eye height - 0.1 along the way its head points, so its middle
     * ends up this far behind the position: the position sits this far on the head's side of the middle.
     */
    public static final double HEAD_SHIFT = 0.55;

    private CarryPose() {
    }

    /** The side the carried head points to: the golem's right. */
    public static Direction headDirection(float bodyYaw) {
        return Direction.fromYRot(bodyYaw).getClockWise();
    }

    /** The position of the carried mob (its feet, which is the middle of its lying body shifted as described at HEAD_SHIFT). */
    public static Vec3 ridePosition(IronGolem golem) {
        float yaw = golem.yBodyRot;
        double radians = yaw * Mth.DEG_TO_RAD;
        Direction head = headDirection(yaw);
        return new Vec3(
                golem.getX() - Mth.sin(radians) * FORWARD + head.getStepX() * HEAD_SHIFT,
                golem.getY() + HEIGHT,
                golem.getZ() + Mth.cos(radians) * FORWARD + head.getStepZ() * HEAD_SHIFT);
    }
}
