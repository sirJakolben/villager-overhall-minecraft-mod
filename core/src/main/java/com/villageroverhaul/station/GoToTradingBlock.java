package com.villageroverhaul.station;

import com.google.common.collect.ImmutableMap;
import com.villageroverhaul.state.VillagerStateAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.Optional;

/**
 * While TradingBlockCall says so (powered block, or a right-click visit), the villager walks to
 * its Trading Block and stays next to it.
 * Added to Vanilla's core behaviors (VillagerGoalPackagesMixin), which run in every activity - it keeps
 * the walk target on the block, and Vanilla's strolls and walks to work or the meeting point only start
 * without a walk target, so they wait. No teleport: an unreachable block just isn't reached.
 */
public class GoToTradingBlock extends Behavior<Villager> {

    private static final float WALK_SPEED = 0.6F;
    /** Stands next to the block - the block itself is solid. */
    private static final int CLOSE_ENOUGH = 1;
    private static final int MAX_RUN_TICKS = 1200;

    public GoToTradingBlock() {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED), MAX_RUN_TICKS);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Villager body) {
        return TradingBlockCall.isCalled(body);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Villager body, long timestamp) {
        return TradingBlockCall.isCalled(body);
    }

    @Override
    protected void start(ServerLevel level, Villager body, long timestamp) {
        tick(level, body, timestamp);
    }

    /** Call over (power off, visit done): drop the walk target on the block, so its usual behaviors take over at once. */
    @Override
    protected void stop(ServerLevel level, Villager body, long timestamp) {
        body.getBrain().getMemory(MemoryModuleType.WALK_TARGET)
                .filter(target -> VillagerStateAccess.of(body).getState().stations().tradingBlock()
                        .map(block -> target.getTarget().currentBlockPosition().equals(block.pos())).orElse(false))
                .ifPresent(target -> body.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET));
    }

    @Override
    protected void tick(ServerLevel level, Villager body, long timestamp) {
        Optional<GlobalPos> block = TradingBlockCall.calledTo(body);
        if (block.isEmpty()) {
            return;
        }
        BlockPos pos = block.get().pos();
        Brain<Villager> brain = body.getBrain();
        boolean alreadyThere = brain.getMemory(MemoryModuleType.WALK_TARGET)
                .map(target -> target.getTarget().currentBlockPosition().equals(pos))
                .orElse(false);
        if (!alreadyThere) {
            brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(pos, WALK_SPEED, CLOSE_ENOUGH));
        }
    }
}
