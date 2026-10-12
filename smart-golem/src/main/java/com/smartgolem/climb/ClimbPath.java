package com.smartgolem.climb;

import net.minecraft.world.phys.Vec3;

/**
 * Where the Iron Golem is during a climb, from the time alone - the server puts the golem there every tick, and the client works out the same place
 * for every frame, so what is drawn does not wait for the position packets (which come every few ticks and are smoothed).
 *
 * - at first the golem glides from where it stood to the start of the animation (a block in front of the wall; GLIDE_TICKS);
 * - it then stays put while the animation has the hands on the ledge (until ClimbPose.MOVE_START_SECONDS);
 * - from then on it moves in a straight line to the top of the wall, to arrive when the animation ends (ClimbPose.MOVE_END_SECONDS).
 * The model's parts have this movement taken out (ClimbPose), so together they are the animation of Blockbench.
 */
public final class ClimbPath {

    /** The glide to the start of the animation (ticks). */
    public static final double GLIDE_TICKS = 8.0;

    private ClimbPath() {
    }

    private static double smooth(double t) {
        double c = Math.min(Math.max(t, 0.0), 1.0);
        return c * c * (3.0 - 2.0 * c);
    }

    /** How much of the way up the golem has gone, 0 to 1 (a straight line between the two times). */
    public static double travel(double seconds) {
        return Math.min(Math.max((seconds - ClimbPose.MOVE_START_SECONDS) / (ClimbPose.MOVE_END_SECONDS - ClimbPose.MOVE_START_SECONDS), 0.0), 1.0);
    }

    /** The golem's position this many ticks into the climb. */
    public static Vec3 position(GolemClimb climb, double ticks) {
        Vec3 base = climb.from().lerp(climb.start(), smooth(ticks / GLIDE_TICKS));
        return base.add(climb.target().subtract(climb.start()).scale(travel(ticks / 20.0)));
    }
}
