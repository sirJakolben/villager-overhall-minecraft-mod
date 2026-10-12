package com.smartgolem.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.smartgolem.ai.GolemPathNavigation;
import com.smartgolem.ai.GolemPatrolStrollGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.GolemRandomStrollInVillageGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The only mixin of Smart Iron Golem, three narrow hooks into the IronGolem (nothing is cancelled, nothing of Vanilla's AI is removed):
 * - the one "stroll in the village" goal is built as the patrol stroll (a subclass: Vanilla's behaviour by day);
 * - one TemptGoal for iron ingots is added to the goals, at the priority animals use - Vanilla's own class, with the "tempt_range" attribute from
 *   GolemAttributes;
 * - at the end of the constructor the navigation is replaced by GolemPathNavigation - but only if it is still Vanilla's plain ground navigation, so
 *   another mod's own navigation stays.
 */
@Mixin(IronGolem.class)
public abstract class IronGolemMixin extends AbstractGolem {

    /** The priority animals give their TemptGoal: after attacking and fleeing, before strolling. */
    private static final int TEMPT_PRIORITY = 3;
    private static final double TEMPT_SPEED = 0.7;
    /** The golem stops this far from the player (blocks; Vanilla: 2.5 - the golem is big). */
    private static final double TEMPT_STOP_DISTANCE = 3.0;

    protected IronGolemMixin(EntityType<? extends AbstractGolem> type, Level level) {
        super(type, level);
    }

    @WrapOperation(method = "registerGoals", at = @At(value = "NEW", target = "net/minecraft/world/entity/ai/goal/GolemRandomStrollInVillageGoal"))
    private GolemRandomStrollInVillageGoal smartgolem$patrolStroll(PathfinderMob mob, double speedModifier,
                                                                  Operation<GolemRandomStrollInVillageGoal> original) {
        return new GolemPatrolStrollGoal(mob, speedModifier);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void smartgolem$addTempt(CallbackInfo ci) {
        goalSelector.addGoal(TEMPT_PRIORITY, new TemptGoal((IronGolem) (Object) this, TEMPT_SPEED, stack -> stack.is(Items.IRON_INGOT), false,
                TEMPT_STOP_DISTANCE));
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void smartgolem$navigation(EntityType<? extends IronGolem> type, Level level, CallbackInfo ci) {
        if (navigation.getClass() == GroundPathNavigation.class) {
            navigation = new GolemPathNavigation(this, level);
        }
    }
}
