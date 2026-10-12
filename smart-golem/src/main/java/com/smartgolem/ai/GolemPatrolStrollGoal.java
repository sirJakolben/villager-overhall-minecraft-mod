package com.smartgolem.ai;

import com.smartgolem.patrol.PatrolMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.GolemRandomStrollInVillageGoal;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Vanilla's "stroll in the village" of the Iron Golem (GolemRandomStrollInVillageGoal) with the night patrol on top: in the early morning, in the
 * evening and at night the golem never rests - it walks to the next point of the patrol map (one path block per chunk around the bell, the one visited
 * longest ago first; PatrolMap) and picks the next right after. By day, and where the village has no patrol point, everything is Vanilla's: where it
 * strolls, how often. The way follows the paths: the navigation's patrol mode charges a little for every step off the path blocks.
 *
 * Attacking and defending (priorities 1 and 2) go before this goal, as before; when the fight is over the golem simply picks the next point.
 */
public class GolemPatrolStrollGoal extends GolemRandomStrollInVillageGoal {

    /** The part of the day that stays Vanilla (ticks of the day, 0 = sunrise). */
    private static final long DAY_START = 1000L;
    private static final long DAY_END = 11000L;
    /** The next point is at least this far and at most this far (blocks). */
    private static final int MIN_DISTANCE = 16;
    private static final int MAX_DISTANCE = 64;

    private final int dayInterval;
    private @Nullable BlockPos patrolPoint;

    public GolemPatrolStrollGoal(PathfinderMob mob, double speedModifier) {
        super(mob, speedModifier);
        this.dayInterval = this.interval;
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
        // By night the goal fires every tick it is idle (nextInt(1) is always 0); by day Vanilla's interval.
        setInterval(mob.level() instanceof ServerLevel level && isPatrolTime(level) ? 1 : dayInterval);
        return super.canUse();
    }

    @Override
    protected @Nullable Vec3 getPosition() {
        patrolPoint = null;
        if (mob.level() instanceof ServerLevel level && isPatrolTime(level)) {
            BlockPos here = mob.blockPosition();
            BlockPos center = PatrolMap.centerOf(level, here);
            PatrolMap map = PatrolMap.of(level);
            map.scanSome(level, center);
            patrolPoint = map.next(level, here, center, MIN_DISTANCE, MAX_DISTANCE).orElse(null);
            if (patrolPoint != null) {
                return Vec3.atBottomCenterOf(patrolPoint.above());
            }
        }
        return super.getPosition();
    }

    @Override
    public void start() {
        if (mob.getNavigation() instanceof GolemPathNavigation navigation) {
            navigation.setPatrolMode(patrolPoint != null);
        }
        super.start();
        // No way at all to the point (walled in, a roof ...): the map leaves it alone for a while.
        if (patrolPoint != null && mob.getNavigation().getPath() == null && mob.level() instanceof ServerLevel level) {
            PatrolMap.of(level).unreachable(level, patrolPoint);
        }
    }

    @Override
    public void stop() {
        if (mob.getNavigation() instanceof GolemPathNavigation navigation) {
            navigation.setPatrolMode(false);
        }
        patrolPoint = null;
        super.stop();
    }
}
