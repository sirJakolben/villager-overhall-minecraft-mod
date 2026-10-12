package com.smartvillager.professions.fisherman;

import com.smartvillager.professions.passive.FishingWork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

/**
 * The bobber a fishing Villager casts (Obsidian Fisherman.md, reworked 2026-10-07) - Vanilla's fishing bobber for a player, made for a villager: the
 * Vanilla one needs a player as its owner. It flies in an arc to the water, floats, and after a wait around the mean the villager brings (half to one and a half times, MIN_WAIT_TICKS at least) it dips for BITE_TICKS, like a bite; then the villager "reels in" and the catch goes straight onto his load
 * (FishingWork.onCatch) - no fish is spawned. Drawn by client/render/VillagerBobberRenderer (Vanilla's texture, the line to the villager's hand). Not saved: a reload
 * drops it, the villager casts again.
 */
public class VillagerBobber extends Projectile {

    private static final EntityDataAccessor<Boolean> DATA_BITING = SynchedEntityData.defineId(VillagerBobber.class, EntityDataSerializers.BOOLEAN);
    /** The wait for a bite after landing is half to one and a half times the villager's mean wait (FishingWork.biteWaitTicks), at least this long. */
    private static final int MIN_WAIT_TICKS = 20;
    /** How long the bobber dips before the villager reels in. */
    private static final int BITE_TICKS = 20;
    private static final double GRAVITY = 0.03;
    /** The bobber is dropped if it has not found water after this long, or its villager is gone or this far away. */
    private static final int MAX_FLIGHT_TICKS = 60;
    private static final int MAX_LIFE_TICKS = 1200;
    private static final double MAX_DISTANCE_SQR = 32.0 * 32.0;
    /** The villager keeps looking at the bobber - and standing still - for this long after the cast and after it landed (1.5 s and 1 s): he never glances and turns away or wanders off. */
    private static final int LOOK_AFTER_CAST_TICKS = 30;
    private static final int LOOK_AFTER_LANDING_TICKS = 20;

    private final InterpolationHandler interpolation = new InterpolationHandler(this);
    private boolean landed;
    private int age;
    private int timeUntilBite;
    private int meanWaitTicks;
    private int biteTicks;
    private int landedAge;
    /** The block the villager stood in when he cast - his fishing spot, once the bobber has landed (FishingWork.onLanded). */
    private BlockPos castFrom = BlockPos.ZERO;

    public VillagerBobber(EntityType<? extends VillagerBobber> type, Level level) {
        super(type, level);
    }

    /** Casts a bobber from the villager's hand to the water block, in a flight of about a second. */
    public static VillagerBobber cast(ServerLevel level, Villager villager, BlockPos water, int meanWaitTicks) {
        VillagerBobber bobber = new VillagerBobber(FishermanEntities.VILLAGER_BOBBER.get(), level);
        bobber.setOwner(villager);
        bobber.meanWaitTicks = meanWaitTicks;
        bobber.castFrom = villager.blockPosition();
        Vec3 hand = handPosition(villager);
        BlockPos surface = water;
        while (level.getFluidState(surface.above()).is(FluidTags.WATER)) {
            surface = surface.above();
        }
        FluidState fluid = level.getFluidState(surface);
        Vec3 target = new Vec3(surface.getX() + 0.5, surface.getY() + fluid.getHeight(level, surface), surface.getZ() + 0.5);
        Vec3 delta = target.subtract(hand);
        double flight = Math.max(8.0, Math.min(40.0, Math.sqrt(delta.x * delta.x + delta.z * delta.z) / 0.7));
        // Position after t ticks: hand + t * v - GRAVITY * t * (t - 1) / 2, so the speed that lands on the target after `flight` ticks is
        bobber.setPos(hand);
        bobber.setDeltaMovement(delta.x / flight, (delta.y + GRAVITY * flight * (flight - 1.0) / 2.0) / flight, delta.z / flight);
        level.addFreshEntity(bobber);
        level.playSound(null, hand.x, hand.y, hand.z, SoundEvents.FISHING_BOBBER_THROW, SoundSource.NEUTRAL, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        return bobber;
    }

    /** Where the villager holds the rod: in front of the body, at the height of his hands. Also where the line of the renderer starts. */
    public static Vec3 handPosition(Villager villager) {
        return handPosition(villager, villager.yBodyRot);
    }

    /** The same for a body that faces the given yaw (degrees) - to check the way to the water before turning. */
    public static Vec3 handPosition(Villager villager, float bodyYaw) {
        double yaw = Math.toRadians(bodyYaw);
        return villager.position().add(-Math.sin(yaw) * 0.5, villager.getBbHeight() * 0.6, Math.cos(yaw) * 0.5);
    }

    public boolean isBiting() {
        return entityData.get(DATA_BITING);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        entityData.define(DATA_BITING, false);
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return interpolation;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0;
    }

    @Override
    public void tick() {
        super.tick();
        interpolation.interpolate();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (!(getOwner() instanceof Villager villager) || !villager.isAlive() || villager.level() != level
                || villager.distanceToSqr(this) > MAX_DISTANCE_SQR || ++age > MAX_LIFE_TICKS) {
            discard();
            return;
        }
        if (age <= LOOK_AFTER_CAST_TICKS || landed && age - landedAge <= LOOK_AFTER_LANDING_TICKS) {
            lookAtMe(villager);
            holdStill(level, villager);
        }
        if (!landed) {
            fly(level, villager);
        } else {
            bob(level, villager);
        }
    }

    /**
     * While he casts and the bobber lands he stands still, the way a villager does for the Trading Block: the core pauses Vanilla's place-bound behaviors
     * (strolling, walking back to the job site) while FishingWork.isHeld, which is renewed every tick here. A walk already under way is stopped once, at the cast.
     */
    private void holdStill(ServerLevel level, Villager villager) {
        FishingWork.hold(villager, level.getGameTime() + 2);
        if (age == 1) {
            villager.getNavigation().stop();
            villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        }
    }

    /** Turns the villager - body and head, every tick, so nothing else turns him away - to the bobber. */
    private void lookAtMe(Villager villager) {
        float yaw = (float) (Mth.atan2(-(getX() - villager.getX()), getZ() - villager.getZ()) * (180.0 / Math.PI));
        villager.setYRot(yaw);
        villager.yBodyRot = yaw;
        villager.yBodyRotO = yaw;
        villager.setYHeadRot(yaw);
        villager.yHeadRotO = yaw;
    }

    private void fly(ServerLevel level, Villager villager) {
        setDeltaMovement(getDeltaMovement().add(0.0, -GRAVITY, 0.0));
        move(MoverType.SELF, getDeltaMovement());
        BlockPos pos = blockPosition();
        if (level.getFluidState(pos).is(FluidTags.WATER)) {
            landed = true;
            landedAge = age;
            FishingWork.onLanded(villager, castFrom);
            setDeltaMovement(Vec3.ZERO);
            timeUntilBite = Math.max(MIN_WAIT_TICKS, meanWaitTicks / 2 + random.nextInt(meanWaitTicks + 1));
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.25F, 1.0F + (random.nextFloat() - random.nextFloat()) * 0.4F);
        } else if (onGround() || horizontalCollision || age > MAX_FLIGHT_TICKS) {
            FishingWork.onLost(villager);
            discard();
        }
    }

    private void bob(ServerLevel level, Villager villager) {
        BlockPos pos = blockPosition();
        FluidState fluid = level.getFluidState(pos);
        if (!fluid.is(FluidTags.WATER)) {
            FishingWork.onLost(villager);
            discard();
            return;
        }
        double surface = pos.getY() + fluid.getHeight(level, pos);
        setPos(getX(), surface - (isBiting() ? 0.2 : 0.05), getZ());
        if (!isBiting()) {
            if (--timeUntilBite <= 0) {
                entityData.set(DATA_BITING, true);
                biteTicks = BITE_TICKS;
                level.playSound(null, getX(), getY(), getZ(), SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.25F, 1.0F + (random.nextFloat() - random.nextFloat()) * 0.4F);
                level.sendParticles(ParticleTypes.BUBBLE, getX(), surface, getZ(), 6, 0.2, 0.0, 0.2, 0.02);
                level.sendParticles(ParticleTypes.FISHING, getX(), surface, getZ(), 6, 0.2, 0.0, 0.2, 0.02);
            }
        } else if (--biteTicks <= 0) {
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.NEUTRAL, 1.0F, 0.4F / (random.nextFloat() * 0.4F + 0.8F));
            FishingWork.onCatch(villager);
            discard();
        }
    }
}
