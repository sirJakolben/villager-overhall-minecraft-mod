package com.villageroverhaul.traderework.passive;

import com.villageroverhaul.passive.PassiveWork;
import com.villageroverhaul.core.state.StationSlot;
import com.villageroverhaul.core.state.VillagerState;
import com.villageroverhaul.freedom.VillagerWorkScan;
import com.villageroverhaul.traderework.salvager.SmeltingStationBlock;
import com.villageroverhaul.traderework.salvager.SmeltingStationBlockEntity;
import com.villageroverhaul.trade.RestockService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.Optional;

/**
 * The Salvager's passive at work (Obsidian Salvager.md, "Passive Ability - Smelting Station", 2026-09-27),
 * the same rhythm as the Runesmith's repairing (RepairWork): once the passive meter is full and the smelting
 * station holds metal gear, the focus jumps to the station; standing right at it during work time, the
 * Salvager smelts one piece - the meter drops to 0, the station pulses redstone and plays the armorer work
 * sound. A higher passive rank means more pieces per day (STEPS_PER_DAY_BY_RANK) and a bigger share of the
 * recipe back (SmeltingStationBlockEntity.SHARE_PERCENT_BY_RANK).
 *
 * While it fills the passive meter at the station, the station crackles like a blast furnace (markWorked).
 */
public final class SmeltingWork {

    /** How close the villager must stand to the station's center - same as the other passives (Tweak-Werte.md). */
    public static final double WORK_REACH = 2.0;
    /** Pieces smelted per full, happy work day, by passive rank 0..6 (Tweak-Werte.md) - sizes the passive meter. */
    private static final int[] STEPS_PER_DAY_BY_RANK = {8, 12, 16, 20, 24, 28, 32};
    /** Extra ticks the furnace sound keeps going past the next expected work scan, so it doesn't stutter. */
    private static final int SOUND_GRACE_TICKS = 20;

    private SmeltingWork() {
    }

    public static int stepsPerDay(int passiveRank) {
        return STEPS_PER_DAY_BY_RANK[Math.clamp(passiveRank, 0, STEPS_PER_DAY_BY_RANK.length - 1)];
    }

    private static Optional<SmeltingStationBlockEntity> station(ServerLevel level, VillagerState state) {
        Optional<GlobalPos> station = state.stations().passive();
        if (station.isEmpty() || station.get().dimension() != level.dimension() || !level.isLoaded(station.get().pos())) {
            return Optional.empty();
        }
        return level.getBlockEntity(station.get().pos()) instanceof SmeltingStationBlockEntity smelter ? Optional.of(smelter) : Optional.empty();
    }

    /** Full passive meter and gear in its station - time to walk there. */
    public static boolean isPending(ServerLevel level, Villager villager, VillagerState state) {
        return state.dailyProductivity().passive() >= RestockService.meterPoints(villager, StationSlot.PASSIVE, state)
                && station(level, state).map(SmeltingStationBlockEntity::hasWork).orElse(false);
    }

    /** workTime: the villager is in its WORK activity - it smelts only then. */
    public static VillagerState apply(ServerLevel level, Villager villager, VillagerState state, boolean workTime) {
        if (!workTime || !PassiveWork.worksAtPassiveStation(villager, state)) {
            return state;
        }
        Optional<SmeltingStationBlockEntity> station = station(level, state);
        if (station.isEmpty() || !station.get().getBlockPos().closerToCenterThan(villager.position(), WORK_REACH)) {
            return state;
        }
        BlockPos pos = station.get().getBlockPos();
        boolean meterFull = state.dailyProductivity().passive() >= RestockService.meterPoints(villager, StationSlot.PASSIVE, state);
        if (!meterFull) {
            // Filling the passive meter right here: the station crackles until the next scan.
            station.get().markWorked(level.getGameTime() + VillagerWorkScan.SCAN_PERIOD + SOUND_GRACE_TICKS);
            return state;
        }
        if (!station.get().smeltStep(state.ranks().passive())) {
            return state;
        }
        SmeltingStationBlock.pulse(level, pos);
        level.playSound(null, pos, SoundEvents.VILLAGER_WORK_ARMORER, SoundSource.NEUTRAL, 1.0F, 1.0F);
        return new VillagerState(
                state.level(), state.xp(), state.unspentUpgradePoints(), state.ranks(),
                state.questSlotRotations(), state.tradeUsesRemaining(), state.dailyProductivity().with(StationSlot.PASSIVE, 0),
                state.happiness(), state.lastProcessedDay(), state.questSlotRerollAvailableAtTick(), state.questLog(), state.stations()
        );
    }
}
