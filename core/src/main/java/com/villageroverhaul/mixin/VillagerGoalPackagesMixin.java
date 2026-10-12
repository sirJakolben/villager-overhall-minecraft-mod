package com.villageroverhaul.mixin;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;
import com.villageroverhaul.interaction.GoToTradingBlock;
import com.villageroverhaul.station.PausedWhileAway;
import com.villageroverhaul.station.UnemployedPriority;
import com.villageroverhaul.station.WorkAtStation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.VillagerGoalPackages;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Blocks W and B: narrow additions to Vanilla's villager behavior packages - everything else in them stays.
 *
 * - Core package, the job-site search (AcquirePoi): a villager that already has a profession skips a
 *   free job site while a jobless villager nearby could take it. Only the "is this site OK" test is
 *   extended; jobless villagers search exactly as in Vanilla.
 * - Work package: adds WorkAtStation, and pauses the job-site-bound behaviors (walk back to the job site,
 *   the work/stroll choice) while the villager works at another station or is called to its Trading Block.
 *   With no station focus and no call, the work activity is exactly Vanilla's.
 * - Meet package: the bell-bound behaviors pause while it is called to its Trading Block.
 * - Core package: adds GoToTradingBlock (the Trading Block call).
 */
@Mixin(VillagerGoalPackages.class)
public abstract class VillagerGoalPackagesMixin {

    /** Vanilla's walk-to-job-site behavior is the work package's only priority-2 entry. */
    private static final int JOB_SITE_WALK_PRIORITY = 2;
    /** The meet package's bell-bound entries: the stroll/socialize choice and the walk to the bell. */
    private static final int MEETING_WALK_PRIORITY = 2;
    /** Just before Vanilla's walk-to-job-site, so a station's walk target wins. */
    private static final int WORK_AT_STATION_PRIORITY = 1;
    /** Same as Vanilla's panic trigger and door handling - before the walk-target sink (1) acts on it. */
    private static final int TRADING_BLOCK_CALL_PRIORITY = 0;

    @WrapOperation(
            method = "getCorePackage",
            require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/behavior/AcquirePoi;create(Ljava/util/function/Predicate;Lnet/minecraft/world/entity/ai/memory/MemoryModuleType;Lnet/minecraft/world/entity/ai/memory/MemoryModuleType;ZLjava/util/Optional;Ljava/util/function/BiPredicate;)Lnet/minecraft/world/entity/ai/behavior/BehaviorControl;")
    )
    private static BehaviorControl<PathfinderMob> villageroverhaul$joblessFirst(
            Predicate<Holder<PoiType>> poiType, MemoryModuleType<GlobalPos> toValidate, MemoryModuleType<GlobalPos> toAcquire,
            boolean onlyIfAdult, Optional<Byte> event, BiPredicate<ServerLevel, BlockPos> validPoi,
            Operation<BehaviorControl<PathfinderMob>> original, @Local(argsOnly = true) Holder<VillagerProfession> profession) {
        BiPredicate<ServerLevel, BlockPos> test = profession.is(VillagerProfession.NONE)
                ? validPoi
                : (level, pos) -> validPoi.test(level, pos) && !UnemployedPriority.isUnemployedNear(level, pos);
        return original.call(poiType, toValidate, toAcquire, onlyIfAdult, event, test);
    }

    /** The Trading Block call joins Vanilla's always-active core behaviors, ahead of everything that walks. */
    @ModifyReturnValue(method = "getCorePackage", at = @At("RETURN"), require = 1)
    private static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> villageroverhaul$addTradingBlockCall(
            ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> original) {
        return ImmutableList.<Pair<Integer, ? extends BehaviorControl<? super Villager>>>builder()
                .addAll(original)
                .add(Pair.of(TRADING_BLOCK_CALL_PRIORITY, new GoToTradingBlock()))
                .build();
    }

    @ModifyReturnValue(method = "getWorkPackage", at = @At("RETURN"), require = 1)
    private static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> villageroverhaul$addStationWork(
            ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> original) {
        ImmutableList.Builder<Pair<Integer, ? extends BehaviorControl<? super Villager>>> builder = ImmutableList.builder();
        for (Pair<Integer, ? extends BehaviorControl<? super Villager>> entry : original) {
            boolean lecternBound = entry.getSecond() instanceof RunOne<?> || entry.getFirst() == JOB_SITE_WALK_PRIORITY;
            builder.add(lecternBound ? Pair.of(entry.getFirst(), PausedWhileAway.atStation(entry.getSecond())) : entry);
        }
        builder.add(Pair.of(WORK_AT_STATION_PRIORITY, new WorkAtStation()));
        return builder.build();
    }

    /**
     * Meet package: the stroll/socialize choice at the bell and the walk to it (both priority 2) pause while the
     * villager is called to its Trading Block - strolling and socializing set their walk target even over ours.
     */
    @ModifyReturnValue(method = "getMeetPackage", at = @At("RETURN"), require = 1)
    private static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> villageroverhaul$pauseMeetingWhenCalled(
            ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> original) {
        ImmutableList.Builder<Pair<Integer, ? extends BehaviorControl<? super Villager>>> builder = ImmutableList.builder();
        for (Pair<Integer, ? extends BehaviorControl<? super Villager>> entry : original) {
            boolean bellBound = entry.getFirst() == MEETING_WALK_PRIORITY;
            builder.add(bellBound ? Pair.of(entry.getFirst(), PausedWhileAway.called(entry.getSecond())) : entry);
        }
        return builder.build();
    }
}
