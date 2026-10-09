package com.villageroverhaul.traderework.golem;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Iron Golem's night patrol (Obsidian Iron Golem, 2026-10-08): in the early morning, in the evening and at night he is always on the move through
 * the village, along its paths. He goes to the next point of the village patrol map (PatrolMap: one path block per chunk around the bell, the one visited longest ago
 * first) and walks there over as much path as he can: along the path blocks connected to where he is, and the rest the normal way.
 *
 * The goal sits below Vanilla's attack goals (priority 1-2) and above the strolling (4): a mob in sight makes him attack, and when it is dealt with
 * the same route goes on. The day (DAY_START to DAY_END) is left to Vanilla's villager-interaction behavior. Cost: a flood fill of at most
 * MAX_NODES blocks when a new destination is chosen, never per tick.
 */
public class PatrolGoal extends Goal {

    /** The part of the day that stays Vanilla (clock ticks of the day, 0 = sunrise): he strolls and visits villagers. */
    private static final long DAY_START = 1000L;
    private static final long DAY_END = 11000L;
    private static final double SPEED = 0.7;
    /** A random spot as the destination (a village without a patrol point) is about this far (blocks). */
    private static final int PATROL_DISTANCE = 24;
    /** The next patrol point is at least this far, and at most MAX_TRAVEL, from him (blocks). */
    private static final int MIN_DISTANCE = 16;
    private static final int MAX_TRAVEL = 64;
    /** A path is followed at most this often (ticks). */
    private static final int REPATH_TICKS = 10;
    /** How far from a path block he still counts as being on the path network (blocks). */
    private static final int START_REACH = 6;
    /** A flood fill never grows beyond this many blocks - the cost limit. */
    private static final int MAX_NODES = 800;
    /** A waypoint on the way is every this many path blocks. */
    private static final int WAYPOINT_STEP = 5;
    /** No progress for this long (ticks) skips the waypoint; a whole route taking this long is dropped. */
    private static final int STUCK_TICKS = 100;
    private static final int ROUTE_TIMEOUT_TICKS = 1200;
    private static final int RETRY_TICKS = 100;
    /** The flood fill over the paths stays this close to him (blocks). */
    private static final int FLOOD_RADIUS = 40;

    private final IronGolem golem;
    private final Deque<BlockPos> route = new ArrayDeque<>();
    private long nextTry;
    private long routeSince;
    private long lastProgress;
    private double lastDistance;
    private long nextRepath;
    private BlockPos destination;

    public PatrolGoal(IronGolem golem) {
        this.golem = golem;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    private static boolean isPatrolTime(ServerLevel level) {
        if (level.dimensionType().hasFixedTime()) {
            return false;
        }
        long time = level.getOverworldClockTime() % 24000L;
        return time < DAY_START || time >= DAY_END;
    }

    @Override
    public boolean canUse() {
        if (!(golem.level() instanceof ServerLevel level) || !isPatrolTime(level)) {
            route.clear();
            return false;
        }
        long now = level.getGameTime();
        if (!route.isEmpty() && now - routeSince > ROUTE_TIMEOUT_TICKS) {
            reportUnreachable(level);
            route.clear();
        }
        if (route.isEmpty()) {
            if (now < nextTry) {
                return false;
            }
            plan(level);
            if (route.isEmpty()) {
                nextTry = now + RETRY_TICKS;
                return false;
            }
            routeSince = now;
        }
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return !route.isEmpty() && golem.level() instanceof ServerLevel level && isPatrolTime(level) && level.getGameTime() - routeSince <= ROUTE_TIMEOUT_TICKS;
    }

    @Override
    public void start() {
        lastProgress = golem.level().getGameTime();
        lastDistance = Double.MAX_VALUE;
        moveToNext();
    }

    @Override
    public void stop() {
        golem.getNavigation().stop();
    }

    @Override
    public void tick() {
        BlockPos next = route.peekFirst();
        if (next == null) {
            return;
        }
        long now = golem.level().getGameTime();
        double distance = golem.position().distanceToSqr(Vec3.atBottomCenterOf(next));
        if (distance < 2.25 || golem.getNavigation().isDone() && distance < 9.0) {
            route.pollFirst();
            moveToNext();
            lastProgress = now;
            lastDistance = Double.MAX_VALUE;
        } else if (distance < lastDistance - 0.25) {
            lastDistance = distance;
            lastProgress = now;
        } else if (now - lastProgress > STUCK_TICKS) {
            // Stuck on the way: this waypoint is skipped.
            if (route.size() == 1 && golem.level() instanceof ServerLevel level) {
                reportUnreachable(level);
            }
            route.pollFirst();
            moveToNext();
            lastProgress = now;
            lastDistance = Double.MAX_VALUE;
        } else if (golem.getNavigation().isDone() && now >= nextRepath) {
            moveToNext();
        }
    }

    /** The destination was not reached: the patrol map leaves that point alone for a while (a roof, an island ...). */
    private void reportUnreachable(ServerLevel level) {
        if (destination != null && golem.blockPosition().distSqr(destination) > 16.0) {
            PatrolMap.of(level).unreachable(level, destination);
        }
        destination = null;
    }

    private void moveToNext() {
        BlockPos next = route.peekFirst();
        if (next != null) {
            nextRepath = golem.level().getGameTime() + REPATH_TICKS;
            golem.getNavigation().moveTo(next.getX() + 0.5, next.getY(), next.getZ() + 0.5, SPEED);
        }
    }

    // ---- choosing the route ----

    /**
     * The next point of the patrol map (the village's point visited longest ago) is the destination, connected to the paths he is on or not. On
     * the way he follows as much path as possible: along the path blocks he can reach from where he is, to the one of them that is closest to the
     * destination, and from there the normal way.
     */
    private void plan(ServerLevel level) {
        route.clear();
        destination = null;
        BlockPos here = golem.blockPosition();
        BlockPos center = PatrolMap.centerOf(level, here);
        PatrolMap map = PatrolMap.of(level);
        map.scanSome(level, center);
        BlockPos target = map.next(level, here, center, MIN_DISTANCE, MAX_TRAVEL).orElse(null);
        if (target == null) {
            Vec3 spot = DefaultRandomPos.getPos(golem, PATROL_DISTANCE, 5);
            if (spot != null && level.isVillage(BlockPos.containing(spot))) {
                route.add(BlockPos.containing(spot));
            }
            return;
        }
        destination = target;
        BlockPos start = nearestPath(level, here);
        if (start != null) {
            followPaths(level, start, target);
        }
        route.add(target.above());
    }

    private BlockPos nearestPath(ServerLevel level, BlockPos here) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(here.offset(-START_REACH, -3, -START_REACH), here.offset(START_REACH, 2, START_REACH))) {
            double distance = pos.distSqr(here);
            if (distance < bestDistance && PathBlocks.isPath(level, pos, PathBlocks.LIKE)) {
                bestDistance = distance;
                best = pos.immutable();
            }
        }
        return best;
    }

    /**
     * Floods the path blocks (all kinds) reachable from the start and adds waypoints along the chain to the block closest to the destination -
     * if that is closer to it than he is now.
     */
    private void followPaths(ServerLevel level, BlockPos start, BlockPos target) {
        Map<BlockPos, BlockPos> parents = new HashMap<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        BlockPos here = golem.blockPosition();
        BlockPos closest = start;
        parents.put(start, start);
        queue.add(start);
        while (!queue.isEmpty() && parents.size() < MAX_NODES) {
            BlockPos pos = queue.poll();
            if (pos.distSqr(target) < closest.distSqr(target)) {
                closest = pos;
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        BlockPos next = pos.offset(dx, dy, dz);
                        if ((dx != 0 || dz != 0) && !parents.containsKey(next) && next.distSqr(here) <= FLOOD_RADIUS * FLOOD_RADIUS
                                && PathBlocks.isPath(level, next, PathBlocks.LIKE)) {
                            parents.put(next, pos);
                            queue.add(next);
                        }
                    }
                }
            }
        }
        if (closest.distSqr(target) >= here.distSqr(target)) {
            return;
        }
        List<BlockPos> chain = new ArrayList<>();
        for (BlockPos pos = closest; !pos.equals(start); pos = parents.get(pos)) {
            chain.add(pos);
        }
        chain.add(start);
        // chain runs from the end back to the start: waypoints from the start on, the end of the chain always last.
        for (int i = chain.size() - 1; i >= 0; i -= WAYPOINT_STEP) {
            route.add(chain.get(i).above());
        }
        if (!route.peekLast().equals(closest.above())) {
            route.add(closest.above());
        }
    }
}
