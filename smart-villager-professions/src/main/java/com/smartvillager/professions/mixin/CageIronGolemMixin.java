package com.smartvillager.professions.mixin;

import com.smartvillager.professions.cage.CageAware;
import com.smartvillager.professions.cage.CarryPose;
import com.smartvillager.professions.cage.CarryZombieVillagerGoal;
import com.smartvillager.professions.cage.ZombieCaptives;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The Zombie Villager Cage's hooks into the Iron Golem (nothing of Vanilla's AI is removed):
 * - CarryZombieVillagerGoal in front of the attack (priority 0, same flags - with full arms the golem cannot attack);
 * - canAttack spares zombie villagers while there is an empty cage around or one is in the golem's arms - the target selection stays Vanilla's, it
 *   just never gets to pick them;
 * - the carried zombie villager lies across the arms (passenger position), and does not "steer" the golem: Vanilla treats a mob passenger as the
 *   controller and would switch the golem's MOVE and LOOK goals off.
 */
@Mixin(IronGolem.class)
public abstract class CageIronGolemMixin extends AbstractGolem implements CageAware {

    private static final int CAGE_PRIORITY = 0;
    /** The search for an empty cage is repeated at this interval (ticks). */
    private static final int CAGE_CHECK_INTERVAL = 40;

    @Unique
    private long vo$cageCheckedAt = Long.MIN_VALUE / 2;
    @Unique
    private boolean vo$emptyCageNear;

    protected CageIronGolemMixin(EntityType<? extends AbstractGolem> type, Level level) {
        super(type, level);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void vo$addCageGoal(CallbackInfo ci) {
        goalSelector.addGoal(CAGE_PRIORITY, new CarryZombieVillagerGoal((IronGolem) (Object) this));
    }

    @Inject(method = "canAttack", at = @At("HEAD"), cancellable = true)
    private void vo$spareZombieVillagers(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (target instanceof ZombieVillager zombie && !ZombieCaptives.isUnreachable(zombie)
                && (ZombieCaptives.carriedBy((IronGolem) (Object) this) != null || vo$emptyCageNear())) {
            cir.setReturnValue(false);
        }
    }

    @Override
    @Unique
    public boolean vo$emptyCageNear() {
        if (!(level() instanceof ServerLevel server)) {
            return false;
        }
        long now = server.getGameTime();
        if (now - vo$cageCheckedAt >= CAGE_CHECK_INTERVAL) {
            vo$cageCheckedAt = now;
            vo$emptyCageNear = ZombieCaptives.hasEmptyCage(server, blockPosition());
        }
        return vo$emptyCageNear;
    }

    /** A carried zombie villager lies across the arms held out in front (CarryPose). */
    @Override
    public Vec3 getPassengerRidingPosition(Entity passenger) {
        if (passenger instanceof ZombieVillager) {
            return CarryPose.ridePosition((IronGolem) (Object) this);
        }
        return super.getPassengerRidingPosition(passenger);
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof ZombieVillager ? null : super.getControllingPassenger();
    }
}
