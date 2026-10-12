package com.smartgolem.ai;

import com.smartgolem.patrol.PathBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Vanilla's walking node evaluator (which blocks the golem can walk on, and how it steps up one block), plus two things for the Iron Golem:
 * - a CLIMB neighbour: straight ahead of the foot node there is a wall of two full blocks (across the whole width of the golem), with room for the golem on
 *   top: the cell on top of the wall is a neighbour two blocks higher (ClimbGeometry says which cell). The pathfinder plans through such walls - also
 *   out of holes, also under water; GolemPathNavigation lets the golem stop in front of the wall and climb it (GolemClimbs). The pathfinder charges extra for
 *   such an edge (CLIMB_COST), so a short way round wins.
 * - in the patrol mode (set by the patrol goal) a small extra cost for every node that is not on a path block, so the way follows the paths.
 */
public class GolemNodeEvaluator extends WalkNodeEvaluator {

    /**
     * What a climb costs the pathfinder on top of the walk (blocks of walking): a way round of up to about this much is preferred. It is the cost of the top
     * node, not a longer edge - the pathfinder counts the length of an edge as walked distance, and that is limited to the follow range.
     */
    public static final float CLIMB_COST = 8.0F;
    /** Extra cost of a node off the paths in the patrol mode - about a block of detour. */
    static final float OFF_PATH_COST = 1.0F;

    private boolean patrolMode;
    /** Nodes that already got their off-path cost in this search (a node is a neighbour of several others). */
    private final Set<Node> charged = Collections.newSetFromMap(new IdentityHashMap<>());

    public void setPatrolMode(boolean patrolMode) {
        this.patrolMode = patrolMode;
    }

    @Override
    public void prepare(PathNavigationRegion level, Mob entity) {
        super.prepare(level, entity);
        charged.clear();
    }

    @Override
    public void done() {
        charged.clear();
        super.done();
    }

    @Override
    public int getNeighbors(Node[] neighbors, Node pos) {
        int count = super.getNeighbors(neighbors, pos);
        // A golem with something on its arms (a carried zombie villager) does not climb: no climb edges are planned for it.
        if (!mob.isVehicle()) {
            count = addClimbs(neighbors, pos, count);
        }
        if (patrolMode) {
            for (int i = 0; i < count; i++) {
                chargeOffPath(neighbors[i]);
            }
        }
        return count;
    }

    private void chargeOffPath(Node node) {
        if (node.costMalus >= 0.0F && charged.add(node) && !onPath(node)) {
            node.costMalus += OFF_PATH_COST;
        }
    }

    private boolean onPath(Node node) {
        BlockPos below = new BlockPos(node.x, node.y - 1, node.z);
        return PathBlocks.LIKE.contains(currentContext.level().getBlockState(below).getBlock());
    }

    /** Straight ahead of the footprint a wall of exactly two full blocks, the cells on top of it - if the golem fits there (one row of them is enough). */
    private int addClimbs(Node[] neighbors, Node pos, int count) {
        CollisionGetter level = currentContext.level();
        int footprint = ClimbGeometry.footprint(mob);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            int ahead = ClimbGeometry.aheadCell(pos, side, footprint);
            if (!wallAhead(level, pos, side, ahead, footprint)) {
                continue;
            }
            // Room for the whole golem on top, where it stands when the climb ends.
            Vec3 end = ClimbGeometry.endMiddle(pos, side, footprint, pos.y + 2);
            double halfWidth = mob.getBbWidth() / 2.0;
            AABB topBox = new AABB(end.x - halfWidth, end.y + 0.001, end.z - halfWidth, end.x + halfWidth, end.y + mob.getBbHeight() - 0.002, end.z + halfWidth);
            if (!level.noCollision(mob, topBox)) {
                continue;
            }
            // Only the cells the golem stands on count - the row of cells across the wall's top, not the footprint of 2 x 2 Vanilla would test for the node
            // (that would want a second row behind them, which the golem does not need: it ends close behind the wall's edge, ClimbGeometry.END_PAST_FACE).
            float malus = 0.0F;
            boolean blocked = false;
            for (int across = 0; across < footprint; across++) {
                BlockPos cell = ClimbGeometry.wallCell(pos, side, ahead, across, pos.y + 2);
                float cellMalus = mob.getPathfindingMalus(WalkNodeEvaluator.getPathTypeStatic(currentContext, new BlockPos.MutableBlockPos(cell.getX(), cell.getY(), cell.getZ())));
                if (cellMalus < 0.0F) {
                    blocked = true;
                    break;
                }
                malus = Math.max(malus, cellMalus);
            }
            if (blocked) {
                continue;
            }
            BlockPos top = ClimbGeometry.topCell(pos, side, ahead);
            PathType type = PathType.WALKABLE;
            Node node = getNode(top.getX(), top.getY(), top.getZ());
            node.type = type;
            node.costMalus = Math.max(node.costMalus, malus + CLIMB_COST);
            // Vanilla's own test for every neighbour: not a node the search has already closed (the pathfinder takes closed nodes in again as they are, and
            // two nodes that took each other as their way back make the path endless).
            if (isNeighborValid(node, pos)) {
                neighbors[count++] = node;
            }
        }
        return count;
    }

    /** All cells of the column straight ahead, across the footprint's width, are full blocks at the foot's height and the one above. */
    static boolean wallAhead(CollisionGetter level, Node foot, Direction side, int ahead, int footprint) {
        for (int across = 0; across < footprint; across++) {
            if (!fullBlock(level, ClimbGeometry.wallCell(foot, side, ahead, across, foot.y))
                    || !fullBlock(level, ClimbGeometry.wallCell(foot, side, ahead, across, foot.y + 1))) {
                return false;
            }
        }
        return true;
    }

    private static boolean fullBlock(CollisionGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return Block.isShapeFullBlock(state.getCollisionShape(level, pos));
    }
}
