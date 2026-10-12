package com.smartgolem.ai;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.Vec3;

/**
 * Vanilla's ground navigation for the Iron Golem, with two additions (see docs/smart-golem.md):
 * - the pathfinder knows climb edges (GolemNodeEvaluator) and charges GolemNodeEvaluator.CLIMB_COST for each - a way round of up to about eight blocks is preferred;
 * - when the golem has reached the foot of such a wall (the next path node is two blocks higher), it stops there instead of jumping and starts the climb
 *   (GolemClimbs); while it climbs, the navigation rests, afterwards the path goes on.
 * The patrol goal switches the patrol mode (GolemNodeEvaluator) on while it walks.
 */
public class GolemPathNavigation extends GroundPathNavigation {

    /** The golem is at the foot of the wall when its middle is this close to the middle of the node's block (blocks). */
    private static final double AT_FOOT = 1.0;
    /** Vanilla searches at most 16 nodes per block of follow range; the golem gets this many times that. */
    private static final float MAX_VISITED_FACTOR = 3.0F;

    public GolemPathNavigation(Mob mob, Level level) {
        super(mob, level);
        // A climb edge costs GolemNodeEvaluator.CLIMB_COST: before it is taken the search has looked at the ways round, which are many nodes in the open.
        setMaxVisitedNodesMultiplier(MAX_VISITED_FACTOR);
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        // Called from the constructor of PathNavigation: no field of this class may be set here.
        GolemNodeEvaluator evaluator = new GolemNodeEvaluator();
        this.nodeEvaluator = evaluator;
        return new PathFinder(evaluator, maxVisitedNodes);
    }

    public void setPatrolMode(boolean patrolMode) {
        if (nodeEvaluator instanceof GolemNodeEvaluator evaluator) {
            evaluator.setPatrolMode(patrolMode);
        }
    }

    @Override
    public void tick() {
        if (!(mob instanceof IronGolem golem)) {
            super.tick();
            return;
        }
        if (GolemClimbs.isClimbing(golem)) {
            return;
        }
        super.tick();
        approachClimb(golem);
    }

    /** The next node is two blocks higher than the one before: walk to the foot of the wall - never jump - and climb from there. */
    private void approachClimb(IronGolem golem) {
        Path path = getPath();
        if (path == null || path.isDone()) {
            return;
        }
        int next = path.getNextNodeIndex();
        if (next <= 0 || next >= path.getNodeCount()) {
            return;
        }
        Node foot = path.getNode(next - 1);
        Node top = path.getNode(next);
        if (top.y - foot.y < 2) {
            return;
        }
        // The golem's middle at the foot node (a node of a mob wider than a block is the corner of its footprint).
        Vec3 middle = ClimbGeometry.middleAt(foot, ClimbGeometry.footprint(golem));
        double dx = golem.getX() - middle.x;
        double dz = golem.getZ() - middle.z;
        if (dx * dx + dz * dz <= AT_FOOT * AT_FOOT && (golem.onGround() || golem.isInWater())) {
            if (!GolemClimbs.start(golem, foot, top)) {
                recomputePath();
            }
        } else {
            // Vanilla's move control would jump at a node two blocks up - walk to the foot of the wall instead.
            golem.getMoveControl().setWantedPosition(middle.x, middle.y, middle.z, speedModifier);
        }
    }
}
