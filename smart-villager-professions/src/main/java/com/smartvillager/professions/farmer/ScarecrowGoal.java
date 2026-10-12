package com.smartvillager.professions.farmer;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Optional;

/**
 * A Zombie goes for a Scarecrow instead of a villager: with no target, or only a villager as its target, a Scarecrow within RANGE draws it - always,
 * however close the villager is. It walks there and stays at it; on Hard it breaks the Scarecrow down in BREAK_TICKS (the Vanilla door time), with the same
 * cracking animation. A player as its target always wins - the Scarecrow is only a better bait than villagers.
 *
 * Cost: one POI query every CHECK_INTERVAL ticks per Zombie that is not busy with a player; nothing per tick while it is not at a Scarecrow.
 */
public class ScarecrowGoal extends Goal {

    private static final int RANGE = 24;
    private static final int CHECK_INTERVAL = 20;
    private static final double SPEED = 1.0;
    /** Close enough to hit and break it: pressed against it, like at a door (touching, a zombie is 0.61 blocks from the center, at a corner 0.87). */
    private static final double HIT_SQR = 1.0 * 1.0;
    /** From here the last steps are taken by the move control, straight at the block - the path would end a block short. */
    private static final double CLOSE_SQR = 4.0 * 4.0;
    private static final int BREAK_TICKS = 240;
    private static final double REACH_SQR = 2.5 * 2.5;
    private static final int STUCK_TICKS = 20;

    private final Zombie zombie;
    private BlockPos scarecrow;
    private int breaking;
    private double lastDistanceSqr;
    private int noProgress;
    private long nextCheck;

    public ScarecrowGoal(Zombie zombie) {
        this.zombie = zombie;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private Optional<BlockPos> nearest() {
        if (!(zombie.level() instanceof ServerLevel level)) {
            return Optional.empty();
        }
        return level.getPoiManager()
                .getInRange(poi -> poi.is(ScarecrowBlock.SCARECROW_POI.getKey()), zombie.blockPosition(), RANGE, PoiManager.Occupancy.ANY)
                .map(record -> record.getPos())
                .min(Comparator.comparingDouble(pos -> pos.distSqr(zombie.blockPosition())));
    }

    private boolean busyWithPlayer() {
        LivingEntity target = zombie.getTarget();
        return target != null && !(target instanceof AbstractVillager);
    }

    @Override
    public boolean canUse() {
        long now = zombie.level().getGameTime();
        if (now < nextCheck || busyWithPlayer()) {
            return false;
        }
        nextCheck = now + CHECK_INTERVAL + zombie.getRandom().nextInt(10);
        // Any Scarecrow in range beats a villager, near or far.
        Optional<BlockPos> found = nearest();
        found.ifPresent(pos -> scarecrow = pos);
        return found.isPresent();
    }

    @Override
    public boolean canContinueToUse() {
        return scarecrow != null && !busyWithPlayer() && zombie.level().getBlockState(scarecrow).is(FarmerBlocks.SCARECROW.get());
    }

    @Override
    public void start() {
        breaking = 0;
        noProgress = 0;
        lastDistanceSqr = Double.MAX_VALUE;
        zombie.getNavigation().moveTo(scarecrow.getX() + 0.5, scarecrow.getY(), scarecrow.getZ() + 0.5, SPEED);
    }

    @Override
    public void stop() {
        if (scarecrow != null && zombie.level() instanceof ServerLevel level) {
            level.destroyBlockProgress(zombie.getId(), scarecrow, -1);
        }
        zombie.getNavigation().stop();
        scarecrow = null;
        breaking = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (!(zombie.level() instanceof ServerLevel level) || scarecrow == null) {
            return;
        }
        Vec3 center = Vec3.atCenterOf(scarecrow);
        zombie.getLookControl().setLookAt(center);
        double dx = center.x - zombie.getX();
        double dz = center.z - zombie.getZ();
        double horizontalSqr = dx * dx + dz * dz;
        // Held up on the way (another mob, a fence ...) close by: it hits from where it stands after a while - it never just stands there.
        noProgress = horizontalSqr < lastDistanceSqr - 0.01 ? 0 : noProgress + 1;
        lastDistanceSqr = horizontalSqr;
        boolean heldUp = noProgress > STUCK_TICKS && horizontalSqr <= REACH_SQR;
        if (horizontalSqr > HIT_SQR && !heldUp) {
            breaking = 0;
            if (horizontalSqr <= CLOSE_SQR) {
                zombie.getNavigation().stop();
                zombie.getMoveControl().setWantedPosition(center.x, scarecrow.getY(), center.z, SPEED);
            } else if (zombie.getNavigation().isDone()) {
                zombie.getNavigation().moveTo(center.x, scarecrow.getY(), center.z, SPEED);
            }
            return;
        }
        zombie.getNavigation().stop();
        // It keeps pushing against it, like a zombie at a door.
        zombie.getMoveControl().setWantedPosition(center.x, scarecrow.getY(), center.z, SPEED * 0.5);
        // It always hits at the Scarecrow; only on Hard that also breaks it.
        if (zombie.tickCount % 20 == 0) {
            zombie.swing(InteractionHand.MAIN_HAND);
        }
        if (level.getDifficulty() != Difficulty.HARD) {
            return;
        }
        if (++breaking % 10 == 0) {
            level.destroyBlockProgress(zombie.getId(), scarecrow, breaking * 10 / BREAK_TICKS);
        }
        if (breaking >= BREAK_TICKS) {
            level.destroyBlock(scarecrow, true, zombie);
            level.destroyBlockProgress(zombie.getId(), scarecrow, -1);
        }
    }
}
