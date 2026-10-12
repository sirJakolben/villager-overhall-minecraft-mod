package com.smartvillager.professions.passive;

import com.smartvillager.progress.state.VillagerState;
import com.smartvillager.professions.basket.BasketStationBlock;
import com.smartvillager.professions.basket.BasketStationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * The passive of the Farmer and the Fisherman (Obsidian Farmer.md / Fisherman.md, reworked 2026-10-07): the villager works for real - the Farmer
 * harvests the field, the Fisherman casts his rod - and every harvested crop or caught fish is one unit on his load (PassiveCounters). The passive
 * bar shows the load, out of the capacity of his passive rank (PassiveCounters.capacity: 3 up to 9). A full load sends him to the basket
 * (SectionLogic.isPending), and he also goes when nothing came in for IDLE_TICKS; once on his way he stays until he has put it all down.
 *
 * At the basket (his focus, close to the block, work time) he takes out one unit every drainInterval (a full load always takes DRAIN_TOTAL_TICKS) and "rolls" it: process decides what the
 * unit turns into (golden crops, fish ...), which goes straight into the basket (BasketStationBlockEntity.deposit); the bar drops by one
 * unit each time. A basket without room keeps him from going there.
 *
 * Cost per work scan (every 50 ticks): a block entity lookup at a known position and a counter read - no block scan.
 */
public abstract class CarryingWork implements PassiveWork {

    /** How close the villager must stand to the basket's center - same as the other passives (Tweak-Werte.md). */
    public static final double WORK_REACH = 2.0;
    /** Ticks to empty a full load at the basket (9 s) - the same for every rank, so a small load is taken out slowly, a big one quickly. */
    public static final int DRAIN_TOTAL_TICKS = 540;
    /** A schedule older than this is a leftover (cast pause ...), not the pace of the current emptying. */
    private static final int STALE_TICKS = 60;

    /** Ticks between two units taken out: the time for a full load divided by the capacity of the rank. */
    public static int drainInterval(int passiveRank) {
        return DRAIN_TOTAL_TICKS / PassiveCounters.capacity(passiveRank);
    }
    /** Ticks without a new unit after which a villager carrying something brings it to the basket (1 min). */
    public static final int IDLE_TICKS = 1200;

    @Override
    public int stepsPerDay(int passiveRank) {
        return 1;
    }

    @Override
    public int capacity(int passiveRank) {
        return PassiveCounters.capacity(passiveRank);
    }

    protected abstract SoundEvent workSound();

    /** What one unit of the sort turns into at the basket (rolled now, on the server). */
    protected abstract List<ItemStack> process(ServerLevel level, BlockPos basket, Identifier sort, int passiveRank, RandomSource random);

    /** Every work scan, before the basket: the villager's own work besides the harvest vanilla does (the Fisherman fishes). */
    protected VillagerState work(ServerLevel level, Villager villager, VillagerState state, boolean workTime, PassiveCounters counters) {
        return state;
    }

    protected static Optional<BasketStationBlockEntity> basket(ServerLevel level, VillagerState state) {
        Optional<GlobalPos> station = PassiveLogic.station(state);
        if (station.isEmpty() || station.get().dimension() != level.dimension() || !level.isLoaded(station.get().pos())) {
            return Optional.empty();
        }
        return level.getBlockEntity(station.get().pos()) instanceof BasketStationBlockEntity basket ? Optional.of(basket) : Optional.empty();
    }

    /** On his way to the basket (or already there) with something to put down, and room for it. */
    @Override
    public boolean isPending(ServerLevel level, Villager villager, VillagerState state) {
        PassiveCounters counters = PassiveCounters.of(villager);
        return counters.returning() && !counters.isEmpty() && basket(level, state).map(BasketStationBlockEntity::hasSpace).orElse(false);
    }

    @Override
    public VillagerState apply(ServerLevel level, Villager villager, VillagerState state, boolean workTime) {
        if (PassiveLogic.station(state).isEmpty()) {
            return state;
        }
        PassiveCounters counters = PassiveCounters.of(villager);
        int rank = PassiveLogic.rank(state);
        long now = level.getGameTime();
        boolean full = counters.total() >= capacity(rank);
        boolean idle = now - counters.lastGainTick() >= IDLE_TICKS;
        // Whatever is still carried when the work day is over stays marked: first thing in the morning it goes to the basket, before he starts anew.
        boolean morning = !workTime;
        if (!counters.returning() && !counters.isEmpty() && (full || idle || morning)) {
            counters = counters.withReturning(true);
            PassiveCounters.set(villager, counters);
        }
        state = work(level, villager, state, workTime, counters);
        counters = PassiveCounters.of(villager);
        if (!workTime || !counters.returning() || !PassiveLogic.worksAtStation(villager, state) || now < counters.nextStepTick()) {
            return state;
        }
        Optional<BasketStationBlockEntity> basket = basket(level, state);
        Optional<Identifier> sort = counters.next();
        if (basket.isEmpty() || sort.isEmpty() || !basket.get().hasSpace() || !basket.get().getBlockPos().closerToCenterThan(villager.position(), WORK_REACH)) {
            return state;
        }
        BlockPos pos = basket.get().getBlockPos();
        // The scan runs every 50 ticks, so every unit that has come due since the last one is taken now - the schedule keeps its pace.
        int interval = drainInterval(rank);
        long due = counters.nextStepTick() < now - STALE_TICKS ? now : counters.nextStepTick();
        while (due <= now && sort.isPresent() && basket.get().hasSpace()) {
            basket.get().deposit(process(level, pos, sort.get(), rank, villager.getRandom()));
            counters = counters.withTaken(sort.get());
            due += interval;
            sort = counters.next();
            BasketStationBlock.pulse(level, pos);
            level.playSound(null, pos, workSound(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
        PassiveCounters.set(villager, counters.withNextStep(due));
        return state;
    }
}
