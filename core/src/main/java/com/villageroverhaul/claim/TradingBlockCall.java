package com.villageroverhaul.claim;

import com.villageroverhaul.core.VillagerStateAccess;
import com.villageroverhaul.core.state.VillagerState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Block B: when a villager is called to its Trading Block (decided 2026-09-24).
 * - Powered block: it walks there and stays as long as the power lasts.
 * - Right-click on the block (TradingBlock): a short visit - it walks there, waits VISIT_WAIT_TICKS
 *   once it arrived, then goes back to whatever it was doing. A visit it can't finish walking within
 *   MAX_WALK_TICKS is dropped. Visits are not saved - a reload simply ends them.
 * Panic, raids and sleep still come first - a call only takes over from work, strolling and meeting.
 * Cheap enough to ask every tick: one state read, one map lookup, a loaded check and a redstone lookup
 * of the block's six neighbors.
 */
public final class TradingBlockCall {

    /** How long the villager waits at the block after a right-click (5 s). */
    private static final int VISIT_WAIT_TICKS = 100;
    /** A visit that doesn't arrive within this (60 s) is given up. */
    private static final int MAX_WALK_TICKS = 1200;
    /** Counts as "arrived" this close to the block's center. */
    private static final double ARRIVED_DISTANCE = 2.5;
    /** How far around a clicked block its villager is looked for. */
    private static final int OWNER_SEARCH_RADIUS = 64;
    private static final long STILL_WALKING = Long.MAX_VALUE;

    private record Visit(long startedAt, long leaveAt) {
    }

    /** Server-side, per villager UUID. */
    private static final Map<UUID, Visit> VISITS = new HashMap<>();

    private TradingBlockCall() {
    }

    /** A right-click on a Trading Block: its villager comes over for a short visit. */
    public static void callByClick(ServerLevel level, BlockPos pos) {
        GlobalPos target = GlobalPos.of(level.dimension(), pos);
        for (Villager villager : level.getEntitiesOfClass(Villager.class, new AABB(pos).inflate(OWNER_SEARCH_RADIUS), Villager::isAlive)) {
            if (VillagerStateAccess.of(villager).getState().stations().tradingBlock().equals(Optional.of(target))) {
                VISITS.put(villager.getUUID(), new Visit(level.getGameTime(), STILL_WALKING));
            }
        }
    }

    /** The Trading Block the villager is called to right now, if any. */
    public static Optional<GlobalPos> calledTo(Villager villager) {
        Level level = villager.level();
        Optional<GlobalPos> block = VillagerStateAccess.of(villager).getState().stations().tradingBlock()
                .filter(pos -> pos.dimension() == level.dimension())
                .filter(pos -> level.isLoaded(pos.pos()) && level.getBlockState(pos.pos()).is(ClaimBlocks.TRADING_BLOCK.get()));
        if (block.isEmpty()) {
            VISITS.remove(villager.getUUID());
            return Optional.empty();
        }
        if (villager.isSleeping() || villager.isBaby() || isBusyWithSomethingMoreImportant(villager.getBrain())) {
            return Optional.empty();
        }
        if (level.hasNeighborSignal(block.get().pos())) {
            return block;
        }
        return isVisiting(villager, block.get().pos(), level.getGameTime()) ? block : Optional.empty();
    }

    public static boolean isCalled(Villager villager) {
        return calledTo(villager).isPresent();
    }

    /** How far a Trading Block may be from the villager's nearest workplace - Vanilla's own job-site and bed reach. */
    public static final int MAX_DISTANCE_TO_WORKPLACE = 48;

    /**
     * Whether a Trading Block at pos is close enough to one of the villager's workplaces. Villagers can
     * only plan paths about 48 blocks far, so from a block further out it couldn't find its way back -
     * and Vanilla drops a job site it can't reach for a minute. Empty when it has no workplace at all.
     */
    public static Optional<Boolean> withinReach(Villager villager, BlockPos pos) {
        return withinReach(villager, VillagerStateAccess.of(villager).getState(), pos);
    }

    public static Optional<Boolean> withinReach(Villager villager, VillagerState state, BlockPos pos) {
        Level level = villager.level();
        var stations = state.stations();
        var workplaces = Stream.of(
                        villager.getBrain().getMemory(MemoryModuleType.JOB_SITE),
                        stations.basic(), stations.master(), stations.passive())
                .flatMap(Optional::stream)
                .filter(workplace -> workplace.dimension() == level.dimension())
                .toList();
        if (workplaces.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(workplaces.stream().anyMatch(workplace -> workplace.pos().closerThan(pos, MAX_DISTANCE_TO_WORKPLACE)));
    }

    /** Advances a right-click visit: walking there, then waiting, then over. */
    private static boolean isVisiting(Villager villager, BlockPos block, long now) {
        Visit visit = VISITS.get(villager.getUUID());
        if (visit == null) {
            return false;
        }
        if (visit.leaveAt() == STILL_WALKING) {
            if (block.getCenter().closerThan(villager.position(), ARRIVED_DISTANCE)) {
                VISITS.put(villager.getUUID(), new Visit(visit.startedAt(), now + VISIT_WAIT_TICKS));
            } else if (now - visit.startedAt() > MAX_WALK_TICKS) {
                VISITS.remove(villager.getUUID());
                return false;
            }
            return true;
        }
        if (now >= visit.leaveAt()) {
            VISITS.remove(villager.getUUID());
            return false;
        }
        return true;
    }

    private static boolean isBusyWithSomethingMoreImportant(Brain<Villager> brain) {
        return brain.isActive(Activity.PANIC) || brain.isActive(Activity.RAID) || brain.isActive(Activity.PRE_RAID)
                || brain.isActive(Activity.HIDE) || brain.isActive(Activity.REST);
    }
}
