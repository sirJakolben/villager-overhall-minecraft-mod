package com.villageroverhaul.traderework.cage;

import com.villageroverhaul.traderework.cage.ZombieVillagerCageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.level.pathfinder.Path;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The Iron Golem takes a zombie villager to a free Zombie Villager Cage, in two steps of one goal:
 * 1. APPROACH: goes to the nearest zombie villager within reach and grabs it - the zombie villager rides the golem (held in its arms, ClimbGolemClient
 *    and the passenger position in IronGolemMixin), without AI;
 * 2. CARRY: walks to the cage it reserved and puts the zombie villager in (ZombieVillagerCageBlockEntity.insert).
 *
 * Priority 0, with the flags of the Vanilla attack: while it runs, the golem cannot attack - its hands are full. It ends when the golem is hurt by
 * anything but a zombie villager (the "distraction"): the zombie villager is dropped and dazed for 3 to 4 seconds and the golem turns on its attacker -
 * Vanilla's attack takes over from there. Also ends, with the zombie villager put down quietly, when the cage is gone or taken, or the way takes too
 * long; the golems then leave that zombie villager to the Vanilla attack for a while (ZombieCaptives.giveUp). In APPROACH the golem also breaks off
 * when Vanilla has picked a target for it.
 *
 * The scan is throttled (every 20 ticks, the cage search only when a zombie villager is in reach).
 */
public class CarryZombieVillagerGoal extends Goal {

    private static final double APPROACH_SPEED = 1.1;
    private static final double CARRY_SPEED = 0.8;
    /** The golem grabs from this far (blocks, squared; its arms are long). */
    private static final double GRAB_REACH_SQR = 2.3 * 2.3;
    /** The golem is at the cage when it is this close (blocks, horizontally / vertically). */
    private static final double DELIVER_REACH = 1.9;
    private static final double DELIVER_HEIGHT = 1.6;
    private static final int APPROACH_LIMIT_TICKS = 200;
    private static final int CARRY_LIMIT_TICKS = 600;
    private static final int SCAN_INTERVAL = 20;
    /** After a distraction the golem fights; this long it takes no new zombie villager (ticks). */
    private static final int DISTRACTED_COOLDOWN = 100;
    private static final int AFTERWARDS_COOLDOWN = 40;
    /** The hit that distracts is this recent (ticks). */
    private static final int HURT_WINDOW = 20;

    private final IronGolem golem;
    private @Nullable ZombieVillager zombie;
    private @Nullable BlockPos cagePos;
    private @Nullable BlockPos standPos;
    private boolean carrying;
    private boolean distracted;
    private boolean delivered;
    private int timer;
    private int nextScanTick;
    private int cooldownUntil;

    public CarryZombieVillagerGoal(IronGolem golem) {
        this.golem = golem;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canUse() {
        if (!(golem.level() instanceof ServerLevel level) || golem.tickCount < nextScanTick || golem.tickCount < cooldownUntil) {
            return false;
        }
        nextScanTick = golem.tickCount + SCAN_INTERVAL + golem.getRandom().nextInt(10);
        if (hurtRecently()) {
            return false;
        }
        UUID id = golem.getUUID();
        ZombieVillager already = ZombieCaptives.carriedBy(golem);
        if (already != null) {
            // Carrying without the goal (reloaded, or the cage was taken on the way): on to a cage, or put it down.
            Optional<BlockPos> cage = ZombieCaptives.findFreeCage(level, golem.blockPosition(), id);
            if (cage.isPresent() && reserve(level, cage.get())) {
                begin(already, cage.get(), true);
                return true;
            }
            already.stopRiding();
            ZombieCaptives.giveUp(already, ZombieCaptives.GIVE_UP_TICKS);
            return false;
        }
        if (golem.getTarget() != null || golem.isPassenger()) {
            return false;
        }
        List<ZombieVillager> near = level.getEntitiesOfClass(ZombieVillager.class,
                golem.getBoundingBox().inflate(ZombieCaptives.SCAN_RANGE, 6.0, ZombieCaptives.SCAN_RANGE),
                z -> z.isAlive() && !z.isPassenger() && !ZombieCaptives.isUnreachable(z) && !ZombieCaptives.isClaimedByOther(z, id));
        if (near.isEmpty()) {
            return false;
        }
        Optional<BlockPos> cage = ZombieCaptives.findFreeCage(level, golem.blockPosition(), id);
        if (cage.isEmpty() || !reserve(level, cage.get())) {
            return false;
        }
        ZombieVillager nearest = near.stream().min(Comparator.comparingDouble(golem::distanceToSqr)).orElseThrow();
        begin(nearest, cage.get(), false);
        return true;
    }

    private void begin(ZombieVillager target, BlockPos cage, boolean alreadyCarrying) {
        zombie = target;
        cagePos = cage;
        standPos = null;
        carrying = alreadyCarrying;
        distracted = false;
        delivered = false;
        timer = 0;
        if (!alreadyCarrying) {
            ZombieCaptives.claim(target, golem.getUUID(), APPROACH_LIMIT_TICKS + 40);
        }
    }

    private boolean reserve(ServerLevel level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ZombieVillagerCageBlockEntity cage && cage.reserve(golem.getUUID(), level.getGameTime());
    }

    private @Nullable ZombieVillagerCageBlockEntity cage(ServerLevel level) {
        return cagePos != null && level.getBlockEntity(cagePos) instanceof ZombieVillagerCageBlockEntity cage ? cage : null;
    }

    private boolean hurtRecently() {
        LivingEntity attacker = golem.getLastHurtByMob();
        return attacker != null && !(attacker instanceof ZombieVillager) && golem.tickCount - golem.getLastHurtByMobTimestamp() <= HURT_WINDOW;
    }

    @Override
    public boolean canContinueToUse() {
        if (distracted || zombie == null || !zombie.isAlive() || !golem.isAlive() || !(golem.level() instanceof ServerLevel level)) {
            return false;
        }
        ZombieVillagerCageBlockEntity cage = cage(level);
        if (cage == null || !cage.isFreeFor(golem.getUUID(), level.getGameTime())) {
            return false;
        }
        if (carrying) {
            return !delivered && zombie.getVehicle() == golem && timer < CARRY_LIMIT_TICKS;
        }
        return golem.getTarget() == null && !zombie.isPassenger() && timer < APPROACH_LIMIT_TICKS;
    }

    @Override
    public void tick() {
        if (!(golem.level() instanceof ServerLevel level) || zombie == null) {
            return;
        }
        timer++;
        if (hurtRecently()) {
            distracted = true;
            return;
        }
        PathNavigation navigation = golem.getNavigation();
        if (!carrying) {
            golem.getLookControl().setLookAt(zombie, 30.0F, 30.0F);
            if (timer % 5 == 1 || navigation.isDone()) {
                navigation.moveTo(zombie, APPROACH_SPEED);
            }
            if (golem.distanceToSqr(zombie) <= GRAB_REACH_SQR) {
                grab();
            }
            return;
        }
        ZombieVillagerCageBlockEntity cage = cage(level);
        if (cage == null || cagePos == null) {
            return;
        }
        if (timer % 20 == 0) {
            cage.reserve(golem.getUUID(), level.getGameTime());
        }
        double dx = golem.getX() - (cagePos.getX() + 0.5);
        double dz = golem.getZ() - (cagePos.getZ() + 0.5);
        if (Math.sqrt(dx * dx + dz * dz) <= DELIVER_REACH && Math.abs(golem.getY() - cagePos.getY()) <= DELIVER_HEIGHT) {
            if (cage.insert(zombie)) {
                delivered = true;
                golem.playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.0F, 0.8F);
                navigation.stop();
            }
            return;
        }
        if (standPos == null || timer % 40 == 0 || navigation.isDone()) {
            walkToCage(level, navigation);
        }
    }

    private void grab() {
        if (zombie == null) {
            return;
        }
        if (!zombie.startRiding(golem, true, true)) {
            ZombieCaptives.giveUp(zombie, ZombieCaptives.GIVE_UP_TICKS);
            timer = APPROACH_LIMIT_TICKS;
            return;
        }
        zombie.setTarget(null);
        zombie.getNavigation().stop();
        golem.getNavigation().stop();
        golem.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 0.7F);
        carrying = true;
        timer = 0;
        standPos = null;
    }

    /** Walks to the nearest of the cage's four neighbours the golem can reach; none reachable ends the carry. */
    private void walkToCage(ServerLevel level, PathNavigation navigation) {
        if (cagePos == null) {
            return;
        }
        Path best = null;
        BlockPos bestPos = null;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos pos = cagePos.relative(direction);
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                continue;
            }
            Path path = navigation.createPath(pos, 0);
            if (path != null && path.canReach() && (best == null || path.getNodeCount() < best.getNodeCount())) {
                best = path;
                bestPos = pos;
            }
        }
        if (best == null) {
            timer = CARRY_LIMIT_TICKS;
            return;
        }
        standPos = bestPos;
        navigation.moveTo(best, CARRY_SPEED);
    }

    @Override
    public void stop() {
        golem.getNavigation().stop();
        if (golem.level() instanceof ServerLevel level) {
            ZombieVillagerCageBlockEntity cage = cage(level);
            if (cage != null) {
                cage.unreserve(golem.getUUID());
            }
        }
        if (zombie != null) {
            ZombieCaptives.unclaim(zombie, golem.getUUID());
            if (!delivered && zombie.isAlive()) {
                boolean heldByMe = zombie.getVehicle() == golem;
                if (heldByMe) {
                    zombie.stopRiding();
                }
                if (distracted) {
                    // Dropped for the fight: knocked down for a few seconds (only if it was in the golem's arms).
                    if (heldByMe) {
                        ZombieCaptives.daze(zombie, ZombieCaptives.DAZE_MIN_TICKS + golem.getRandom().nextInt(ZombieCaptives.DAZE_EXTRA_TICKS + 1));
                    }
                } else if (heldByMe || timer >= APPROACH_LIMIT_TICKS) {
                    ZombieCaptives.giveUp(zombie, ZombieCaptives.GIVE_UP_TICKS);
                }
            }
        }
        if (distracted) {
            // On to the one who hit it - Vanilla's attack goals take over.
            LivingEntity attacker = golem.getLastHurtByMob();
            if (golem.getTarget() == null && attacker != null && attacker.isAlive() && golem.canAttack(attacker)) {
                golem.setTarget(attacker);
            }
            cooldownUntil = golem.tickCount + DISTRACTED_COOLDOWN;
        } else {
            cooldownUntil = golem.tickCount + AFTERWARDS_COOLDOWN;
        }
        zombie = null;
        cagePos = null;
        standPos = null;
        carrying = false;
        delivered = false;
        distracted = false;
    }
}
