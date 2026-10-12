package com.smartgolem.ai;

import com.smartgolem.SmartGolemMod;
import com.smartgolem.climb.GolemClimb;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * The Iron Golem's climb up a wall two blocks high (the animation "2_block_climb" of the Blockbench project, 4 seconds), the part of the plan after the
 * navigation: GolemPathNavigation brings the golem to the foot of the wall and calls start.
 * - start: the golem is put on top of the wall at once (where the animation ends), turned to the wall, and the climb
 *   is set on it (GolemClimb, synced to the clients, which play the animation drawn at the start, a block from the wall: ClimbGolemClient);
 * - during the climb the golem cannot move or turn - whatever its goals do, it is held where it is: on top of the wall (the target);
 * - at the end the climb is simply cleared - the golem is already where the animation ends, in the standing pose - and its path is worked out
 *   anew from there. Nothing hops or jumps: walking and jumping stay Vanilla's.
 */
@EventBusSubscriber(modid = SmartGolemMod.MODID)
public final class GolemClimbs {

    /** The animation is 4 seconds. */
    public static final int CLIMB_TICKS = 80;
    private GolemClimbs() {
    }

    public static boolean isClimbing(IronGolem golem) {
        return golem.getData(GolemClimb.ATTACHMENT).active();
    }

    /** Starts the climb from the foot node to the top node (two blocks higher, on the other side of the wall); false if the wall or the room on top is no longer there. */
    public static boolean start(IronGolem golem, Node foot, Node top) {
        if (!(golem.level() instanceof ServerLevel level)) {
            return false;
        }
        int footprint = ClimbGeometry.footprint(golem);
        Direction direction = ClimbGeometry.sideOf(foot, top);
        if (!GolemNodeEvaluator.wallAhead(level, foot, direction, ClimbGeometry.aheadCell(foot, direction, footprint), footprint)) {
            return false;
        }
        Vec3 start = ClimbGeometry.startMiddle(foot, direction, footprint, golem.getY());
        Vec3 target = ClimbGeometry.endMiddle(foot, direction, footprint, top.y);
        if (!level.noCollision(golem, golem.getDimensions(golem.getPose()).makeBoundingBox(target).deflate(1.0E-3))) {
            return false;
        }
        float yaw = direction.toYRot();
        Vec3 from = golem.position();
        // The golem itself is at the target from the first tick, the model is drawn at the start (client): nothing moves when the animation ends.
        golem.snapTo(target.x, target.y, target.z, yaw, 0.0F);
        face(golem, yaw);
        golem.setDeltaMovement(Vec3.ZERO);
        // No jump towards the node two blocks up in this tick.
        golem.getMoveControl().setWantedPosition(target.x, target.y, target.z, 0.0);
        golem.noPhysics = true;
        golem.setData(GolemClimb.ATTACHMENT, new GolemClimb(level.getGameTime(), from, start, target));
        return true;
    }

    @SubscribeEvent
    static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof IronGolem golem) || !(golem.level() instanceof ServerLevel level) || !golem.isAlive()) {
            return;
        }
        GolemClimb climb = golem.getData(GolemClimb.ATTACHMENT);
        if (!climb.active()) {
            return;
        }
        long elapsed = level.getGameTime() - climb.startTick();
        if (elapsed >= CLIMB_TICKS || elapsed < 0) {
            finish(golem, level, climb);
            return;
        }
        // Held where it is - on top; the model is drawn at the start meanwhile (client) - looking at the wall.
        golem.noPhysics = true;
        golem.setPos(climb.target().x, climb.target().y, climb.target().z);
        face(golem, yawOf(climb));
        golem.setDeltaMovement(Vec3.ZERO);
        golem.fallDistance = 0.0;
    }

    private static void finish(IronGolem golem, ServerLevel level, GolemClimb climb) {
        golem.noPhysics = false;
        face(golem, yawOf(climb));
        golem.setDeltaMovement(Vec3.ZERO);
        golem.fallDistance = 0.0;
        golem.setData(GolemClimb.ATTACHMENT, GolemClimb.NONE);
        golem.getNavigation().recomputePath();
    }

    /** The way the climb faces: from the start to the target. */
    private static float yawOf(GolemClimb climb) {
        Vec3 way = climb.target().subtract(climb.start());
        return (float) (Mth.atan2(-way.x, way.z) * (180.0 / Math.PI));
    }

    private static void face(IronGolem golem, float yaw) {
        golem.setYRot(yaw);
        golem.yRotO = yaw;
        golem.setXRot(0.0F);
        golem.xRotO = 0.0F;
        golem.setYBodyRot(yaw);
        golem.yBodyRotO = yaw;
        golem.setYHeadRot(yaw);
        golem.yHeadRotO = yaw;
    }

}
