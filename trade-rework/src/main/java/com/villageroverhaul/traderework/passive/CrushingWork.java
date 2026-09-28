package com.villageroverhaul.traderework.passive;

import com.villageroverhaul.state.VillagerState;
import com.villageroverhaul.traderework.mason.CrushingStationBlock;
import com.villageroverhaul.traderework.mason.CrushingStationBlockEntity;
import com.villageroverhaul.work.VillagerWorkScan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.Optional;

/**
 * The Mason's passive at work (Obsidian Mason.md, "Passive Ability - Brechstation"), the same rhythm as
 * the Librarian's book upgrade (BookUpgradeWork): once the passive meter is full and the crushing station
 * has something to crush, the focus jumps to the station; standing right at it during work time, the
 * Mason crushes one batch (2026-09-26: up to 4 results of one kind, see CrushingStationBlockEntity.crushBatch),
 * the meter drops to 0, the station pulses redstone and plays the Mason work sound. A higher passive rank
 * means more batches per day: the meter is sized so a full, happy work day fills it
 * BATCHES_PER_DAY_BY_RANK times (PassiveLogic.meterPoints).
 *
 * While it fills the passive meter at the station, the station plays the stone hit sound (markWorked).
 *
 * Cost per work scan: one block entity lookup at a known position and a look at 3 slots - no block scan.
 */
public final class CrushingWork implements PassiveWork {

    /** How close the villager must stand to the station's center to crush - same as the book upgrade (Tweak-Werte.md). */
    public static final double WORK_REACH = 2.0;
    /** Batches per full, happy work day, by passive rank 0..6 (Tweak-Werte.md) - sizes the passive meter. */
    private static final int[] BATCHES_PER_DAY_BY_RANK = {8, 12, 16, 20, 24, 28, 32};
    /** Extra ticks the stone sound keeps going past the next expected work scan, so it doesn't stutter. */
    private static final int SOUND_GRACE_TICKS = 20;

    @Override
    public int stepsPerDay(int passiveRank) {
        return BATCHES_PER_DAY_BY_RANK[Math.clamp(passiveRank, 0, BATCHES_PER_DAY_BY_RANK.length - 1)];
    }

    private static Optional<CrushingStationBlockEntity> station(ServerLevel level, VillagerState state) {
        Optional<GlobalPos> station = PassiveLogic.station(state);
        if (station.isEmpty() || station.get().dimension() != level.dimension() || !level.isLoaded(station.get().pos())) {
            return Optional.empty();
        }
        return level.getBlockEntity(station.get().pos()) instanceof CrushingStationBlockEntity crusher ? Optional.of(crusher) : Optional.empty();
    }

    /** Full passive meter and something to crush in its station - time to walk there. */
    @Override
    public boolean isPending(ServerLevel level, Villager villager, VillagerState state) {
        return PassiveLogic.isMeterFull(villager, state)
                && station(level, state).map(crusher -> crusher.hasWork(level)).orElse(false);
    }

    /** workTime: the villager is in its WORK activity - it crushes only then. */
    @Override
    public VillagerState apply(ServerLevel level, Villager villager, VillagerState state, boolean workTime) {
        if (!workTime || !PassiveLogic.worksAtStation(villager, state)) {
            return state;
        }
        Optional<CrushingStationBlockEntity> station = station(level, state);
        if (station.isEmpty() || !station.get().getBlockPos().closerToCenterThan(villager.position(), WORK_REACH)) {
            return state;
        }
        BlockPos pos = station.get().getBlockPos();
        boolean meterFull = PassiveLogic.isMeterFull(villager, state);
        if (!meterFull) {
            // Filling the passive meter right here: the station plays the tuff mining sound until the next scan (2026-09-26).
            station.get().markWorked(level.getGameTime() + VillagerWorkScan.SCAN_PERIOD + SOUND_GRACE_TICKS);
            return state;
        }
        if (station.get().crushBatch(level) == 0) {
            return state;
        }
        CrushingStationBlock.pulse(level, pos);
        level.playSound(null, pos, SoundEvents.VILLAGER_WORK_MASON, SoundSource.NEUTRAL, 1.0F, 1.0F);
        return PassiveLogic.emptyMeter(state);
    }
}
