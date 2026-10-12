package com.villageroverhaul.traderework.passive;

import com.villageroverhaul.state.VillagerState;
import com.villageroverhaul.traderework.runesmith.RepairStationBlock;
import com.villageroverhaul.traderework.runesmith.RepairStationBlockEntity;
import com.villageroverhaul.work.VillagerWorkScan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.Optional;

/**
 * The Runesmith's passive at work (Obsidian Runesmith.md, "Passive Ability - Repair Station"), the
 * same rhythm as the Mason's crushing (CrushingWork): once the passive meter is full and the repair station
 * holds a damaged item, the focus jumps to the station; standing right at it during work time, the
 * Runesmith does one repair step - REPAIR_POINTS_PER_STEP durability on the first damaged item, free and
 * without XP - the meter drops to 0, the station pulses redstone and plays the toolsmith work sound. A
 * higher passive rank means more steps per day: the meter is sized so a full, happy work day fills it
 * STEPS_PER_DAY_BY_RANK times (PassiveLogic.meterPoints). Durability points,
 * not percent: cheap gear is repaired fast, diamond gear takes days (Runesmith.md).
 *
 * While it fills the passive meter at the station, the station plays the grindstone sound (markWorked).
 *
 * Cost per work scan: one block entity lookup at a known position and a look at 3 slots - no block scan.
 */
public final class RepairWork implements PassiveWork {

    /** How close the villager must stand to the station's center to repair - same as the book upgrade (Tweak-Werte.md). */
    public static final double WORK_REACH = 2.0;
    /** Repair steps per full, happy work day, by passive rank 0..6 (Tweak-Werte.md) - sizes the passive meter. */
    private static final int[] STEPS_PER_DAY_BY_RANK = {8, 12, 16, 20, 24, 28, 32};
    /** Durability points one repair step restores (Tweak-Werte.md) - 8 steps on rank 0 fix an iron sword (250) in about a day. */
    private static final int REPAIR_POINTS_PER_STEP = 25;
    /** Extra ticks the grindstone sound keeps going past the next expected work scan, so it doesn't stutter. */
    private static final int SOUND_GRACE_TICKS = 20;

    @Override
    public int stepsPerDay(int passiveRank) {
        return STEPS_PER_DAY_BY_RANK[Math.clamp(passiveRank, 0, STEPS_PER_DAY_BY_RANK.length - 1)];
    }

    private static Optional<RepairStationBlockEntity> station(ServerLevel level, VillagerState state) {
        Optional<GlobalPos> station = PassiveLogic.station(state);
        if (station.isEmpty() || station.get().dimension() != level.dimension() || !level.isLoaded(station.get().pos())) {
            return Optional.empty();
        }
        return level.getBlockEntity(station.get().pos()) instanceof RepairStationBlockEntity repairer ? Optional.of(repairer) : Optional.empty();
    }

    /** Full passive meter and a damaged item in its station - time to walk there. */
    @Override
    public boolean isPending(ServerLevel level, Villager villager, VillagerState state) {
        return PassiveLogic.isMeterFull(villager, state)
                && station(level, state).map(repairer -> repairer.hasWork()).orElse(false);
    }

    /** workTime: the villager is in its WORK activity - it repairs only then. */
    @Override
    public VillagerState apply(ServerLevel level, Villager villager, VillagerState state, boolean workTime) {
        Optional<RepairStationBlockEntity> station = station(level, state);
        // Every scan, like the Librarian's station: the station learns its owner's rank (padding on the top rank).
        station.ifPresent(repairer -> repairer.markOwner(PassiveLogic.rank(state)));
        if (!workTime || !PassiveLogic.worksAtStation(villager, state)) {
            return state;
        }
        if (station.isEmpty() || !station.get().getBlockPos().closerToCenterThan(villager.position(), WORK_REACH)) {
            return state;
        }
        BlockPos pos = station.get().getBlockPos();
        boolean meterFull = PassiveLogic.isMeterFull(villager, state);
        if (!meterFull) {
            // Filling the passive meter right here: the station plays the grindstone sound until the next scan.
            station.get().markWorked(level.getGameTime() + VillagerWorkScan.SCAN_PERIOD + SOUND_GRACE_TICKS);
            return state;
        }
        if (!station.get().repairStep(REPAIR_POINTS_PER_STEP)) {
            return state;
        }
        RepairStationBlock.pulse(level, pos);
        level.playSound(null, pos, SoundEvents.VILLAGER_WORK_TOOLSMITH, SoundSource.NEUTRAL, 1.0F, 1.0F);
        return PassiveLogic.emptyMeter(state);
    }
}
