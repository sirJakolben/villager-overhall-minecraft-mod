package com.villageroverhaul.traderework.passive;

import com.villageroverhaul.state.VillagerState;
import com.villageroverhaul.traderework.mapping.MapPool;
import com.villageroverhaul.traderework.mapping.MapStationBlock;
import com.villageroverhaul.traderework.mapping.MapStationBlockEntity;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.Optional;

/**
 * The Cartographer's and the Veteran's passive at work (2026-10-10): they draw maps. Once the passive meter is full and the station
 * holds a blank map (adventure map for the Cartographer, challenge map for the Veteran) with room for the result, the focus jumps to the
 * station; standing right at it during work time, the villager draws one map - the blank one becomes a filled map to a structure of the
 * pool (MapStationBlockEntity.draw), the meter drops to 0, the station pulses redstone and plays the cartographer work sound. If no
 * structure is in reach the meter stays full and the station waits before the next try (MapStationBlockEntity.RETRY_TICKS).
 *
 * Maps are slow on purpose (daysPerStep: whole work days per map, by passive rank 0..6) - there is no "finish the last one first" rule.
 * Cost per work scan: one block entity lookup at a known position and a look at 6 slots; a structure search only when a map is drawn.
 */
public final class MappingWork implements PassiveWork {

    /** How close the villager must stand to the station's center - same as the other passives. */
    public static final double WORK_REACH = 2.0;

    private final MapPool.Kind kind;
    private final int[] daysPerMapByRank;

    public MappingWork(MapPool.Kind kind, int[] daysPerMapByRank) {
        this.kind = kind;
        this.daysPerMapByRank = daysPerMapByRank;
    }

    @Override
    public int stepsPerDay(int passiveRank) {
        return 1;
    }

    /** Whole work days one map takes (Tweak-Werte.md). */
    @Override
    public int daysPerStep(int passiveRank) {
        return daysPerMapByRank[Math.clamp(passiveRank, 0, daysPerMapByRank.length - 1)];
    }

    private Optional<MapStationBlockEntity> station(ServerLevel level, VillagerState state) {
        Optional<GlobalPos> station = PassiveLogic.station(state);
        if (station.isEmpty() || station.get().dimension() != level.dimension() || !level.isLoaded(station.get().pos())) {
            return Optional.empty();
        }
        return level.getBlockEntity(station.get().pos()) instanceof MapStationBlockEntity maps && maps.kind() == kind
                ? Optional.of(maps) : Optional.empty();
    }

    /** Full passive meter and a blank map in its station - time to walk there. */
    @Override
    public boolean isPending(ServerLevel level, Villager villager, VillagerState state) {
        return PassiveLogic.isMeterFull(villager, state)
                && station(level, state).map(maps -> maps.hasWork(level)).orElse(false);
    }

    @Override
    public VillagerState apply(ServerLevel level, Villager villager, VillagerState state, boolean workTime) {
        if (!workTime || !PassiveLogic.worksAtStation(villager, state) || !PassiveLogic.isMeterFull(villager, state)) {
            return state;
        }
        Optional<MapStationBlockEntity> station = station(level, state);
        if (station.isEmpty() || !station.get().getBlockPos().closerToCenterThan(villager.position(), WORK_REACH)
                || !station.get().hasWork(level) || !station.get().draw(level)) {
            return state;
        }
        MapStationBlock.pulse(level, station.get().getBlockPos());
        level.playSound(null, station.get().getBlockPos(), SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.NEUTRAL, 1.0F, 1.0F);
        return PassiveLogic.emptyMeter(state);
    }
}
