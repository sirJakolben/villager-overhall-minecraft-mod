package com.villageroverhaul.traderework.librarian;

import com.villageroverhaul.core.VillagerStateAccess;
import com.villageroverhaul.progression.ProgressionService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;

/**
 * An XP orb for villagers, dropped by the Villager Experience Bottle. Physics mirror Vanilla's
 * ExperienceOrb (spawn scatter, gravity, bouncing, water/lava, 5-minute lifetime) and it renders with
 * the Vanilla orb texture in a turquoise tint (VillagerExperienceOrbRenderer).
 *
 * Deliberately not a subclass of ExperienceOrb: Vanilla orbs merge every second with any nearby
 * ExperienceOrb of equal value, so a subclass would get swallowed by normal orbs and its villager XP
 * would turn into player XP. Only villagers with a real profession (not jobless, not nitwit, not
 * babies) attract and absorb it - players can't pick it up.
 */
public class VillagerExperienceOrb extends Entity {

    private static final EntityDataAccessor<Integer> DATA_VALUE = SynchedEntityData.defineId(VillagerExperienceOrb.class, EntityDataSerializers.INT);
    private static final int LIFETIME = 6000;
    private static final int TARGET_SCAN_PERIOD = 20;
    private static final double MAX_FOLLOW_DIST = 8.0;
    private static final double ABSORB_DIST_SQR = 1.0;

    private final InterpolationHandler interpolation = new InterpolationHandler(this);
    private int age;
    private @Nullable Villager targetVillager;

    public VillagerExperienceOrb(EntityType<? extends VillagerExperienceOrb> type, Level level) {
        super(type, level);
    }

    /** Same spawn scatter as ExperienceOrb's own spawning constructor. */
    public VillagerExperienceOrb(Level level, Vec3 pos, Vec3 roughly, int value) {
        this(LibrarianEntities.VILLAGER_EXPERIENCE_ORB.get(), level);
        this.setPos(pos);
        this.setYRot(this.random.nextFloat() * 360.0F);
        Vec3 randomMovement = new Vec3(
                (this.random.nextDouble() * 0.2 - 0.1) * 2.0, this.random.nextDouble() * 0.2 * 2.0, (this.random.nextDouble() * 0.2 - 0.1) * 2.0
        );
        if (roughly.lengthSqr() > 0.0 && roughly.dot(randomMovement) < 0.0) {
            randomMovement = randomMovement.scale(-1.0);
        }
        this.setPos(pos.add(roughly.normalize().scale(this.getBoundingBox().getSize() * 0.5)));
        this.setDeltaMovement(randomMovement);
        this.setValue(value);
    }

    /** Splits a total into orbs using Vanilla's own orb sizes (ExperienceOrb.getExperienceValue), like awardWithDirection. */
    public static void award(ServerLevel level, Vec3 pos, Vec3 roughDirection, int amount) {
        while (amount > 0) {
            int value = ExperienceOrb.getExperienceValue(amount);
            amount -= value;
            level.addFreshEntity(new VillagerExperienceOrb(level, pos, roughDirection, value));
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        entityData.define(DATA_VALUE, 0);
    }

    public int getValue() {
        return this.entityData.get(DATA_VALUE);
    }

    private void setValue(int value) {
        this.entityData.set(DATA_VALUE, value);
    }

    /** Vanilla's size steps (ExperienceOrb.getIcon), so the orb looks as big as a normal one of equal value. */
    public int getIcon() {
        int value = getValue();
        int[] thresholds = {3, 7, 17, 37, 73, 149, 307, 617, 1237, 2477};
        int icon = 0;
        for (int threshold : thresholds) {
            if (value >= threshold) {
                icon++;
            }
        }
        return icon;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.03;
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    public void tick() {
        this.interpolation.interpolate();
        if (this.firstTick && this.level().isClientSide()) {
            this.firstTick = false;
            return;
        }
        super.tick();
        boolean colliding = !this.level().noCollision(this.getBoundingBox());
        if (this.isEyeInFluid(FluidTags.WATER)) {
            Vec3 movement = this.getDeltaMovement();
            this.setDeltaMovement(movement.x * 0.99F, Math.min(movement.y + 5.0E-4F, 0.06F), movement.z * 0.99F);
        } else if (!colliding) {
            this.applyGravity();
        }
        if (this.level().getFluidState(this.blockPosition()).is(FluidTags.LAVA)) {
            this.setDeltaMovement(
                    (this.random.nextFloat() - this.random.nextFloat()) * 0.2F, 0.2F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F
            );
        }

        if (!this.level().isClientSide()) {
            followNearbyVillager();
        }

        double fallSpeed = this.getDeltaMovement().y;
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.applyEffectsFromBlocks();
        float friction = 0.98F;
        if (this.onGround()) {
            BlockPos pos = this.getBlockPosBelowThatAffectsMyMovement();
            friction = this.level().getBlockState(pos).getFriction(this.level(), pos, this) * 0.98F;
        }
        this.setDeltaMovement(this.getDeltaMovement().scale(friction));
        if (this.verticalCollisionBelow && fallSpeed < -this.getGravity()) {
            this.setDeltaMovement(new Vec3(this.getDeltaMovement().x, -fallSpeed * 0.4, this.getDeltaMovement().z));
        }

        if (!this.level().isClientSide()) {
            tryAbsorbInto(targetVillager);
        }

        if (++this.age >= LIFETIME) {
            this.discard();
        }
    }

    /** Vanilla's followNearbyPlayer, aimed at villagers; the nearby-entity scan runs once a second, not every tick. */
    private void followNearbyVillager() {
        if (targetVillager != null && (!canAbsorb(targetVillager) || targetVillager.distanceToSqr(this) > MAX_FOLLOW_DIST * MAX_FOLLOW_DIST)) {
            targetVillager = null;
        }
        if (targetVillager == null && this.tickCount % TARGET_SCAN_PERIOD == 1) {
            targetVillager = this.level().getEntitiesOfClass(Villager.class, this.getBoundingBox().inflate(MAX_FOLLOW_DIST), VillagerExperienceOrb::canAbsorb)
                    .stream()
                    .min(Comparator.comparingDouble(v -> v.distanceToSqr(this)))
                    .orElse(null);
        }
        if (targetVillager != null) {
            Vec3 delta = new Vec3(
                    targetVillager.getX() - this.getX(),
                    targetVillager.getY() + targetVillager.getEyeHeight() / 2.0 - this.getY(),
                    targetVillager.getZ() - this.getZ()
            );
            double power = 1.0 - Math.sqrt(delta.lengthSqr()) / MAX_FOLLOW_DIST;
            this.setDeltaMovement(this.getDeltaMovement().add(delta.normalize().scale(power * power * 0.1)));
        }
    }

    /** Full villagers (max level, or point cap with a full XP bar) are skipped, so the orb flies on to one that can use it. */
    private static boolean canAbsorb(Villager villager) {
        return villager.isAlive()
                && !villager.isBaby()
                && !villager.getVillagerData().profession().is(VillagerProfession.NONE)
                && !villager.getVillagerData().profession().is(VillagerProfession.NITWIT)
                && ProgressionService.canGainXp(VillagerStateAccess.of(villager).getState(), ProgressionService.maxLevel(villager));
    }

    private void tryAbsorbInto(@Nullable Villager villager) {
        if (villager == null || this.isRemoved()) {
            return;
        }
        Vec3 center = villager.position().add(0.0, villager.getBbHeight() / 2.0, 0.0);
        if (center.distanceToSqr(this.position()) > ABSORB_DIST_SQR + villager.getBbHeight()) {
            return;
        }
        ProgressionService.grantXp(villager, getValue());
        this.level().playSound(null, villager.getX(), villager.getY(), villager.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.AMBIENT, 0.1F, 0.5F * ((this.random.nextFloat() - this.random.nextFloat()) * 0.7F + 1.8F));
        this.discard();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.AMBIENT;
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return this.interpolation;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putShort("Age", (short) this.age);
        output.putInt("Value", this.getValue());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.age = input.getShortOr("Age", (short) 0);
        this.setValue(input.getIntOr("Value", 0));
    }
}
