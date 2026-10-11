package com.smartvillager.base.mixin;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.datafixers.util.Pair;
import com.smartvillager.base.trading.GoToTradingBlock;
import com.smartvillager.base.trading.PausedWhileAway;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.VillagerGoalPackages;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Narrow additions to Vanilla's villager behavior packages for the Trading Block call - everything else in them stays.
 *
 * - Core package: adds GoToTradingBlock.
 * - Work package: the job-site-bound behaviors (walk back to the job site, the work/stroll choice) pause while the villager is called to its
 *   Trading Block or a mod asks it to stay put (api/SmartVillagerApi). With no call and no reason, the work activity is exactly Vanilla's.
 * - Meet package: the bell-bound behaviors pause for the same reasons.
 */
@Mixin(VillagerGoalPackages.class)
public abstract class VillagerGoalPackagesMixin {

    /** Vanilla's walk-to-job-site behavior is the work package's only priority-2 entry. */
    private static final int JOB_SITE_WALK_PRIORITY = 2;
    /** The meet package's bell-bound entries: the stroll/socialize choice and the walk to the bell. */
    private static final int MEETING_WALK_PRIORITY = 2;
    /** Same as Vanilla's panic trigger and door handling - before the walk-target sink (1) acts on it. */
    private static final int TRADING_BLOCK_CALL_PRIORITY = 0;

    /** The Trading Block call joins Vanilla's always-active core behaviors, ahead of everything that walks. */
    @ModifyReturnValue(method = "getCorePackage", at = @At("RETURN"), require = 1)
    private static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> smartvillager$addTradingBlockCall(
            ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> original) {
        return ImmutableList.<Pair<Integer, ? extends BehaviorControl<? super Villager>>>builder()
                .addAll(original)
                .add(Pair.of(TRADING_BLOCK_CALL_PRIORITY, new GoToTradingBlock()))
                .build();
    }

    @ModifyReturnValue(method = "getWorkPackage", at = @At("RETURN"), require = 1)
    private static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> smartvillager$pauseWorkWhenAway(
            ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> original) {
        ImmutableList.Builder<Pair<Integer, ? extends BehaviorControl<? super Villager>>> builder = ImmutableList.builder();
        for (Pair<Integer, ? extends BehaviorControl<? super Villager>> entry : original) {
            boolean jobSiteBound = entry.getSecond() instanceof RunOne<?> || entry.getFirst() == JOB_SITE_WALK_PRIORITY;
            builder.add(jobSiteBound ? Pair.of(entry.getFirst(), PausedWhileAway.duringWork(entry.getSecond())) : entry);
        }
        return builder.build();
    }

    /**
     * Meet package: the stroll/socialize choice at the bell and the walk to it (both priority 2) pause while the
     * villager is called to its Trading Block - strolling and socializing set their walk target even over ours.
     */
    @ModifyReturnValue(method = "getMeetPackage", at = @At("RETURN"), require = 1)
    private static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> smartvillager$pauseMeetingWhenAway(
            ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> original) {
        ImmutableList.Builder<Pair<Integer, ? extends BehaviorControl<? super Villager>>> builder = ImmutableList.builder();
        for (Pair<Integer, ? extends BehaviorControl<? super Villager>> entry : original) {
            boolean bellBound = entry.getFirst() == MEETING_WALK_PRIORITY;
            builder.add(bellBound ? Pair.of(entry.getFirst(), PausedWhileAway.anyTime(entry.getSecond())) : entry);
        }
        return builder.build();
    }
}
