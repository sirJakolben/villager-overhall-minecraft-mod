package com.smartvillager.progress.mixin;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;
import com.smartvillager.progress.station.UnemployedPriority;
import com.smartvillager.progress.station.WorkAtStation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
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
 * Narrow additions to Vanilla's villager behavior packages - everything else in them stays.
 *
 * - Core package, the job-site search (AcquirePoi): a villager that already has a profession skips a
 *   free job site while a jobless villager nearby could take it. Only the "is this site OK" test is
 * - Work package: adds WorkAtStation. The pause of the job-site-bound behaviors while the villager works at another
 *   station is Smart Villager's (SmartVillagerApi stay-put, registered in SmartVillagerProgressMod). With no station
 *   focus, the work activity is exactly Vanilla's.
 * - Core package: adds GoToTradingBlock (the Trading Block call).
 */
@Mixin(VillagerGoalPackages.class)
public abstract class VillagerGoalPackagesMixin {

    /** Just before Vanilla's walk-to-job-site, so a station's walk target wins. */
    private static final int WORK_AT_STATION_PRIORITY = 1;

    @WrapOperation(
            method = "getCorePackage",
            require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/behavior/AcquirePoi;create(Ljava/util/function/Predicate;Lnet/minecraft/world/entity/ai/memory/MemoryModuleType;Lnet/minecraft/world/entity/ai/memory/MemoryModuleType;ZLjava/util/Optional;Ljava/util/function/BiPredicate;)Lnet/minecraft/world/entity/ai/behavior/BehaviorControl;")
    )
    private static BehaviorControl<PathfinderMob> smartvillager_progress$joblessFirst(
            Predicate<Holder<PoiType>> poiType, MemoryModuleType<GlobalPos> toValidate, MemoryModuleType<GlobalPos> toAcquire,
            boolean onlyIfAdult, Optional<Byte> event, BiPredicate<ServerLevel, BlockPos> validPoi,
            Operation<BehaviorControl<PathfinderMob>> original, @Local(argsOnly = true) Holder<VillagerProfession> profession) {
        BiPredicate<ServerLevel, BlockPos> test = profession.is(VillagerProfession.NONE)
                ? validPoi
                : (level, pos) -> validPoi.test(level, pos) && !UnemployedPriority.isUnemployedNear(level, pos);
        return original.call(poiType, toValidate, toAcquire, onlyIfAdult, event, test);
    }

    /** The station work joins the work package; the pause of the job-site-bound behaviors while away is Smart Villager's (it listens to StationFocus). */
    @ModifyReturnValue(method = "getWorkPackage", at = @At("RETURN"), require = 1)
    private static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> smartvillager_progress$addStationWork(
            ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> original) {
        return ImmutableList.<Pair<Integer, ? extends BehaviorControl<? super Villager>>>builder()
                .addAll(original)
                .add(Pair.of(WORK_AT_STATION_PRIORITY, new WorkAtStation()))
                .build();
    }
}
