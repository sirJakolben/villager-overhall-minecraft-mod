package com.villageroverhaul.station;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.Set;

/**
 * Wraps one of Vanilla's job-site-bound work behaviors (walk back to the job site, work / stroll around
 * it) so it pauses while the villager works at another station, and runs unchanged otherwise. Without
 * this, Vanilla would pull the villager back to its job site the moment it arrives at a station.
 */
public final class PausedAtStation implements BehaviorControl<Villager> {

    private final BehaviorControl<? super Villager> delegate;

    public PausedAtStation(BehaviorControl<? super Villager> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Behavior.Status getStatus() {
        return delegate.getStatus();
    }

    @Override
    public Set<MemoryModuleType<?>> getRequiredMemories() {
        return delegate.getRequiredMemories();
    }

    @Override
    public boolean tryStart(ServerLevel level, Villager body, long timestamp) {
        return StationFocus.awayStation(body).isEmpty() && delegate.tryStart(level, body, timestamp);
    }

    @Override
    public void tickOrStop(ServerLevel level, Villager body, long timestamp) {
        if (StationFocus.awayStation(body).isPresent()) {
            delegate.doStop(level, body, timestamp);
        } else {
            delegate.tickOrStop(level, body, timestamp);
        }
    }

    @Override
    public void doStop(ServerLevel level, Villager body, long timestamp) {
        delegate.doStop(level, body, timestamp);
    }

    @Override
    public String debugString() {
        return "PausedAtStation(" + delegate.debugString() + ")";
    }
}
