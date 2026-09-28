package com.villageroverhaul.station;

import com.google.common.collect.ImmutableMap;
import com.villageroverhaul.interaction.TradingBlockCall;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.Optional;

/**
 * Vanilla's WorkAtPoi for our stations: while StationFocus names a station, the villager walks there
 * and works at it - looks at it and plays its profession's work sound now and then. Added to Vanilla's
 * work activity by VillagerGoalPackagesMixin; the job-site-bound behaviors pause meanwhile (PausedAtStation).
 * Work XP, productivity and the first-use unlock are booked by the periodic work scan, not here.
 */
public class WorkAtStation extends Behavior<Villager> {

    /** Vanilla's WorkAtPoi distance. */
    private static final double ARRIVED_DISTANCE = 1.73;
    private static final float WALK_SPEED = 0.5F;
    private static final int MAX_RUN_TICKS = 1200;
    /** Vanilla's WorkAtPoi check cooldown - work sounds come about as often as at the job site. */
    private static final int WORK_SOUND_INTERVAL = 300;

    private long nextWorkSound;

    public WorkAtStation() {
        super(ImmutableMap.of(
                MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_PRESENT,
                MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED,
                MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED
        ), MAX_RUN_TICKS);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Villager body) {
        return StationFocus.awayStation(body).isPresent() && !TradingBlockCall.isCalled(body);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Villager body, long timestamp) {
        return StationFocus.awayStation(body).isPresent() && !TradingBlockCall.isCalled(body);
    }

    @Override
    protected void start(ServerLevel level, Villager body, long timestamp) {
        tick(level, body, timestamp);
    }

    @Override
    protected void tick(ServerLevel level, Villager body, long timestamp) {
        Optional<GlobalPos> station = StationFocus.awayStation(body);
        if (station.isEmpty()) {
            return;
        }
        BlockPos pos = station.get().pos();
        Brain<Villager> brain = body.getBrain();
        if (!pos.closerToCenterThan(body.position(), ARRIVED_DISTANCE)) {
            // Only set a new walk target when it changes - resetting it every tick would re-path every tick.
            boolean alreadyWalking = brain.getMemory(MemoryModuleType.WALK_TARGET)
                    .map(target -> target.getTarget().currentBlockPosition().equals(pos))
                    .orElse(false);
            if (!alreadyWalking) {
                brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(pos, WALK_SPEED, 1));
            }
            return;
        }
        brain.setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(pos));
        if (timestamp >= nextWorkSound) {
            body.playWorkSound();
            nextWorkSound = timestamp + WORK_SOUND_INTERVAL + level.getRandom().nextInt(WORK_SOUND_INTERVAL);
        }
    }
}
