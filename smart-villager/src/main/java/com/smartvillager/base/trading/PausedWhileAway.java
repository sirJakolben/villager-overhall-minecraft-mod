package com.smartvillager.base.trading;

import com.smartvillager.base.api.SmartVillagerApi;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.Set;
import java.util.function.Predicate;

/**
 * Wraps one of Vanilla's place-bound behaviors (walk back to the job site, work / stroll around it, stroll and
 * socialize at the bell) so it pauses while the villager is needed somewhere else, and runs unchanged otherwise.
 * Several of them set the walk target even when one is already set (StrollAroundPoi, StrollToPoi,
 * SocializeAtBell) - without this, Vanilla would pull the villager back the moment it arrives, or keep
 * redirecting it on the way (bug 2026-09-28: villagers never made it to their Trading Block).
 *
 * - duringWork: it is called to its Trading Block, or a mod asks it to stay put - also only-while-working reasons (SmartVillagerApi).
 * - anyTime: the same without the only-while-working reasons.
 */
public final class PausedWhileAway implements BehaviorControl<Villager> {

    private final BehaviorControl<? super Villager> delegate;
    private final Predicate<Villager> away;

    private PausedWhileAway(BehaviorControl<? super Villager> delegate, Predicate<Villager> away) {
        this.delegate = delegate;
        this.away = away;
    }

    public static PausedWhileAway duringWork(BehaviorControl<? super Villager> delegate) {
        return new PausedWhileAway(delegate, body -> TradingBlockCall.isCalled(body) || SmartVillagerApi.isStayingPutWhileWorking(body));
    }

    public static PausedWhileAway anyTime(BehaviorControl<? super Villager> delegate) {
        return new PausedWhileAway(delegate, body -> TradingBlockCall.isCalled(body) || SmartVillagerApi.isStayingPut(body));
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
        return !away.test(body) && delegate.tryStart(level, body, timestamp);
    }

    @Override
    public void tickOrStop(ServerLevel level, Villager body, long timestamp) {
        if (away.test(body)) {
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
        return "PausedWhileAway(" + delegate.debugString() + ")";
    }
}
