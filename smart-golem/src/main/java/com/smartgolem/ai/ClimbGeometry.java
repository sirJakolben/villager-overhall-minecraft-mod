package com.smartgolem.ai;

import com.smartgolem.climb.ClimbPose;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.phys.Vec3;

/**
 * Where a climb happens, in the terms of Vanilla's pathfinding for a mob wider than a block (the golem is 1.4 wide). A path node of such a mob is the
 * corner cell of a footprint of FOOTPRINT x FOOTPRINT cells (2 x 2 for the golem), its middle is the node plus FOOTPRINT / 2 blocks (Path.getEntityPosAtNode) -
 * so a node is never next to a wall on the positive sides, and next to it on the negative sides only at a distance.
 *
 * A climb edge goes from the foot node to the top node: the wall is the column of cells straight ahead of the footprint (all cells across its width,
 * two full blocks high), the top node is the cell of that column the foot node's own row points at - its floor is the wall.
 * The animation begins with the golem's middle START_DISTANCE in front of the wall's face and ends END_PAST_FACE behind it, on top (GolemClimbs): it moves by
 * ClimbPose.ENTITY_FORWARD_PIXELS and two blocks up in between.
 */
final class ClimbGeometry {

    /** The golem's middle is this far in front of the wall's face when the animation begins (a block: the animation starts this far back, its arms reach the wall). */
    static final double START_DISTANCE = 1.0;
    /** ... and this far behind it, on top, when it ends: the animation ends 18.09 px from where it begins in the direction of the wall, in the standing pose. */
    static final double END_PAST_FACE = ClimbPose.ENTITY_FORWARD_PIXELS / 16.0 - START_DISTANCE;

    private ClimbGeometry() {
    }

    /** Cells along each side of a node's footprint, as Vanilla's node evaluator counts them. */
    static int footprint(Mob mob) {
        return Mth.floor(mob.getBbWidth() + 1.0F);
    }

    /** The middle of a mob standing at the node. */
    static Vec3 middleAt(Node node, int footprint) {
        return new Vec3(node.x + footprint / 2.0, node.y, node.z + footprint / 2.0);
    }

    /** The cell coordinate along the direction's axis of the wall column straight ahead of the node's footprint. */
    static int aheadCell(Node foot, Direction side, int footprint) {
        int coordinate = side.getAxis() == Direction.Axis.X ? foot.x : foot.z;
        int step = side.getAxisDirection().getStep();
        return step > 0 ? coordinate + footprint : coordinate - 1;
    }

    /** The coordinate of the wall's face (the side towards the golem) along the direction's axis. */
    static double face(int aheadCell, Direction side) {
        return side.getAxisDirection().getStep() > 0 ? aheadCell : aheadCell + 1.0;
    }

    /** The cells of the wall column across the footprint's width, at the given height. */
    static BlockPos wallCell(Node foot, Direction side, int aheadCell, int across, int y) {
        return side.getAxis() == Direction.Axis.X ? new BlockPos(aheadCell, y, foot.z + across) : new BlockPos(foot.x + across, y, aheadCell);
    }

    /** The top node: the wall column's cell in the foot node's own row, two blocks higher. */
    static BlockPos topCell(Node foot, Direction side, int aheadCell) {
        return side.getAxis() == Direction.Axis.X ? new BlockPos(aheadCell, foot.y + 2, foot.z) : new BlockPos(foot.x, foot.y + 2, aheadCell);
    }

    /** Where the golem stands when the climb begins: in front of the wall, on the foot node's middle line. */
    static Vec3 startMiddle(Node foot, Direction side, int footprint, double y) {
        int ahead = aheadCell(foot, side, footprint);
        double along = face(ahead, side) - side.getAxisDirection().getStep() * START_DISTANCE;
        Vec3 middle = middleAt(foot, footprint);
        return side.getAxis() == Direction.Axis.X ? new Vec3(along, y, middle.z) : new Vec3(middle.x, y, along);
    }

    /** Where the golem stands when the climb ends: on top of the wall, close behind its face. */
    static Vec3 endMiddle(Node foot, Direction side, int footprint, double y) {
        int ahead = aheadCell(foot, side, footprint);
        double along = face(ahead, side) + side.getAxisDirection().getStep() * END_PAST_FACE;
        Vec3 middle = middleAt(foot, footprint);
        return side.getAxis() == Direction.Axis.X ? new Vec3(along, y, middle.z) : new Vec3(middle.x, y, along);
    }

    /** The side of the foot node the top node lies on. */
    static Direction sideOf(Node foot, Node top) {
        if (top.x > foot.x) {
            return Direction.EAST;
        } else if (top.x < foot.x) {
            return Direction.WEST;
        }
        return top.z > foot.z ? Direction.SOUTH : Direction.NORTH;
    }
}
