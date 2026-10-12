package com.smartvillager.professions.passive;

import com.smartvillager.progress.state.VillagerState;
import com.smartvillager.progress.state.VillagerStateAccess;
import com.smartvillager.professions.fisherman.BiomeFish;
import com.smartvillager.professions.fisherman.VillagerBobber;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The Fisherman's passive (Obsidian Fisherman.md, reworked 2026-10-07). He really fishes: for every cast he first WALKS to the water - a spot on the
 * shore within WATER_REACH blocks of where he works (his focus is then that spot, SectionLogic.workPlace / isPending, walked by the core's
 * WorkAtStation, the Vanilla job-site behaviors pause meanwhile) - takes a fishing rod in his hand and casts a bobber (fisherman/VillagerBobber)
 * onto the water. Once it is out he is free again and walks wherever Vanilla sends him; when it bites, a fish is "caught" - no fish is spawned, it
 * is one unit on his load, up to the capacity of his passive rank (3 on rank 0, 9 on rank 6), and the passive bar shows the load. The next cast
 * means the next walk to the water. A full load, or one that has not grown for a minute, sends him to the fish basket (CarryingWork) instead.
 *
 * How many fish he catches is set by the passive rank, not by luck: dailyFish per work day (10 on rank 0 up to 30 on rank 6), and the wait for
 * the bite (biteWaitTicks) is tuned to it. Only on the top rank a catch counts two units with 50 % (BONUS_PERCENT) - extra fish that are not in the daily number.
 *
 * At the basket, every 3 seconds one unit is taken out and turns into exactly one fish. That fish
 * is the basket's biome fish with (rank + 1) percent - 1 % up to 7 % - and else cod (60) or salmon (40).
 *
 * Cost per work scan: a counter read; the bobber of this villager is looked up only while one is out; water and shore are looked for once when the last
 * look is WATER_RECHECK_TICKS old, never on every scan.
 */
public final class FishingWork extends CarryingWork {

    /** Water must be within this many blocks (sideways) of the Fisherman, and within WATER_HEIGHT above or below (Obsidian 02_Tweak-Werte). */
    public static final int WATER_REACH = 14;
    public static final int WATER_HEIGHT = 5;
    /** Standing places are looked for this far around the water (the cast is a throw of a few blocks), and the water may be at most MAX_CAST_DISTANCE from the hand. */
    private static final int SHORE_SEARCH = 6;
    private static final double MAX_CAST_DISTANCE = 9.0;
    /** How high above his feet the rod is held, for the line-of-sight check from a spot he is not standing on yet. */
    private static final double HAND_HEIGHT = 1.1;
    /** Nearest waters tried for one with a spot that sees it. */
    private static final int MAX_WATERS_TRIED = 12;
    /** Ticks before a failed search for water is repeated (one day is 24000). */
    private static final long WATER_RECHECK_TICKS = 1200;
    /** A work day (Vanilla schedule, ticks 1400-9600) and the fish a Fisherman catches in one at passive rank 0 and at the top rank (Obsidian, 2026-10-07). */
    public static final int WORK_DAY_TICKS = 8200;
    public static final int DAILY_FISH_RANK_0 = 10;
    public static final int DAILY_FISH_TOP_RANK = 30;
    private static final int TOP_RANK = 6;
    /** Per cast besides the wait for the bite: the pause, the throw, the dip, the walk to the shore (estimates) and the unit at the basket. */
    private static final int CAST_OVERHEAD_TICKS = 140;
    /** On the top rank a catch counts two units with this chance - on top of the daily number, which does not include it. */
    public static final int BONUS_PERCENT = 50;
    private static final int MIN_MEAN_WAIT_TICKS = 20;

    /** Pause after a catch or a lost cast before the next walk to the water (2 s). */
    private static final int CAST_PAUSE_TICKS = 40;
    /** A walk to the water that takes longer than this is given up (30 s), and tried again after RETRY_TICKS. */
    private static final int ERRAND_TIMEOUT_TICKS = 600;
    private static final int RETRY_TICKS = 1200;
    /** He casts from his fixed spot when he stands within this of its middle, and is nudged the last steps when closer than NUDGE_DISTANCE. */
    private static final double SPOT_RADIUS = 0.8;
    private static final double NUDGE_DISTANCE = 2.5;
    /** Of the fish that are not the biome fish: this many percent are cod, the rest salmon (Obsidian 02_Tweak-Werte). */
    private static final int COD_PERCENT = 60;

    @Override
    protected SoundEvent workSound() {
        return SoundEvents.VILLAGER_WORK_FISHERMAN;
    }

    // --- At the water ---

    /** Ready for the next cast and the water is known: the walk to the shore spot is due (the focus goes to the passive, the place is the spot). */
    private static boolean wantsToCast(Villager villager, PassiveCounters counters, int rank) {
        return !counters.returning() && !counters.casting() && counters.total() < PassiveCounters.capacity(rank)
                && counters.water().isPresent() && counters.shore().isPresent()
                && villager.level().getGameTime() >= counters.nextStepTick() && isWork(villager);
    }

    private static boolean isWork(Villager villager) {
        Brain<Villager> brain = villager.getBrain();
        return brain.isActive(Activity.WORK);
    }

    @Override
    public boolean isPending(ServerLevel level, Villager villager, VillagerState state) {
        return super.isPending(level, villager, state) || wantsToCast(villager, PassiveCounters.of(villager), PassiveLogic.rank(state));
    }

    @Override
    public Optional<GlobalPos> workPlace(Villager villager, VillagerState state) {
        PassiveCounters counters = PassiveCounters.of(villager);
        if (!wantsToCast(villager, counters, PassiveLogic.rank(state))) {
            return Optional.empty();
        }
        return counters.shore().map(shore -> GlobalPos.of(villager.level().dimension(), shore));
    }

    @Override
    protected VillagerState work(ServerLevel level, Villager villager, VillagerState state, boolean workTime, PassiveCounters counters) {
        int rank = PassiveLogic.rank(state);
        long now = level.getGameTime();
        boolean fishing = workTime && !counters.returning() && counters.total() < capacity(rank);
        if (!fishing) {
            bobberOf(level, villager).ifPresent(VillagerBobber::discard);
            stowRod(villager);
            if (counters.casting()) {
                PassiveCounters.set(villager, counters.withCasting(false).withNextStep(now + CAST_PAUSE_TICKS));
            }
            return state;
        }
        if (counters.casting()) {
            // The bobber is out - the rod stays in his hand until it is reeled in. If it is gone without a word (unloaded, killed), the cast is over.
            if (bobberOf(level, villager).isEmpty()) {
                stowRod(villager);
                PassiveCounters.set(villager, counters.withCasting(false).withNextStep(now + CAST_PAUSE_TICKS));
            }
            return state;
        }
        if (now < counters.nextStepTick()) {
            return state;
        }
        if (counters.water().filter(pos -> isWater(level, pos)).isEmpty() && now - counters.waterCheckedAt() >= WATER_RECHECK_TICKS) {
            Optional<Spot> spot = findSpot(level, villager);
            counters = counters.withWater(spot.map(Spot::water), spot.map(Spot::shore), now);
            PassiveCounters.set(villager, counters);
        }
        if (counters.water().isEmpty() || counters.shore().isEmpty()) {
            return state;
        }
        Vec3 target = surfaceOf(level, counters.water().get());
        BlockPos spot = counters.shore().get();
        boolean sees = sees(level, villager, target);
        if (counters.fixedSpot()) {
            // His fishing spot: every cast is made from this block.
            if (spot.closerToCenterThan(villager.position(), SPOT_RADIUS)) {
                if (sees) {
                    return cast(level, villager, state, counters, target);
                }
                // The view from his spot is gone (something was built there): look for a new one.
                PassiveCounters.set(villager, counters.withErrandSince(PassiveCounters.NEVER).withWater(Optional.empty(), Optional.empty(), PassiveCounters.NEVER));
                return state;
            }
            if (spot.closerToCenterThan(villager.position(), NUDGE_DISTANCE)) {
                // The core walks him to within 1.7 blocks; the last steps onto the block are his own.
                villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(spot, 0.4F, 0));
            }
        } else if (sees && !missedFrom(villager, villager.blockPosition())) {
            // Not fixed yet: a clear line to the water is enough - he turns to it and casts, never over his shoulder, never into a wall.
            return cast(level, villager, state, counters, target);
        }
        if (counters.errandSince() == PassiveCounters.NEVER) {
            PassiveCounters.set(villager, counters.withErrandSince(now));
        } else if (now - counters.errandSince() > ERRAND_TIMEOUT_TICKS) {
            // Could not get to the place in time - leave it for a while (and look for another spot then).
            PassiveCounters.set(villager, counters.withErrandSince(PassiveCounters.NEVER).withNextStep(now + RETRY_TICKS)
                    .withWater(Optional.empty(), Optional.empty(), PassiveCounters.NEVER));
        }
        return state;
    }

    /**
     * Turns to the water, takes the rod and casts. The focus is cleared right away: the cast was the errand, and without this the stale focus (the
     * passive, whose place is now the basket) would send him there while the bobber is still out.
     */
    private static VillagerState cast(ServerLevel level, Villager villager, VillagerState state, PassiveCounters counters, Vec3 target) {
        faceTo(villager, target);
        holdRod(villager);
        VillagerBobber.cast(level, villager, counters.water().get(), biteWaitTicks(PassiveLogic.rank(state)));
        PassiveCounters.set(villager, counters.withCasting(true).withErrandSince(PassiveCounters.NEVER));
        return state.withProductivity(state.productivity().withFocus(Optional.empty()));
    }

    /** Villagers (by UUID) that stand still until this game tick - the bobber renews it every tick while the cast is in the air and just landed. Not saved. */
    private static final java.util.Map<java.util.UUID, Long> HOLD_UNTIL = new java.util.concurrent.ConcurrentHashMap<>();

    /** The bobber asks the villager to stand still for the next ticks (the stay-put reason registered with the core: Vanilla's strolling pauses). */
    public static void hold(Villager villager, long untilTick) {
        HOLD_UNTIL.put(villager.getUUID(), untilTick);
    }

    /** The core's stay-put question (ExtensionHooks.registerStayPut): is this villager mid-cast? */
    public static boolean isHeld(Villager villager) {
        Long until = HOLD_UNTIL.get(villager.getUUID());
        if (until == null) {
            return false;
        }
        if (until <= villager.level().getGameTime()) {
            HOLD_UNTIL.remove(villager.getUUID());
            return false;
        }
        return true;
    }

    /** The first cast that lands in the water fixes the block he stood in as his fishing spot - he uses the same one every time after. */
    public static void onLanded(Villager villager, BlockPos castFrom) {
        MISSED_FROM.remove(villager.getUUID());
        LAST_MISS.remove(villager.getUUID());
        PassiveCounters counters = PassiveCounters.of(villager);
        if (!counters.fixedSpot()) {
            PassiveCounters.set(villager, counters.withFixedSpot(castFrom));
        }
    }

    /** Fish the Fisherman catches in a full work day at this passive rank, bonus fish not counted: 10 on rank 0 up to 30 on rank 6, evenly in between. */
    public static double dailyFish(int passiveRank) {
        return DAILY_FISH_RANK_0 + (double) (DAILY_FISH_TOP_RANK - DAILY_FISH_RANK_0) * Math.clamp(passiveRank, 0, TOP_RANK) / TOP_RANK;
    }

    /**
     * The mean wait for a bite that makes a catch about every WORK_DAY_TICKS / dailyFish ticks, counting what a cycle costs besides waiting (the
     * pause, throw, dip, walk and the unit taken out at the basket). A rank that cannot get there with the overhead waits MIN_MEAN_WAIT_TICKS.
     */
    static int biteWaitTicks(int passiveRank) {
        int cycle = (int) (WORK_DAY_TICKS / dailyFish(passiveRank));
        return Math.max(MIN_MEAN_WAIT_TICKS, cycle - CAST_OVERHEAD_TICKS - drainInterval(passiveRank));
    }

    /** The bobber reeled in with a fish: one unit more on the load, or two with the top rank's bonus chance; capped by the capacity. */
    public static void onCatch(Villager villager) {
        stowRod(villager);
        PassiveCounters counters = PassiveCounters.of(villager);
        long now = villager.level().getGameTime();
        int rank = PassiveLogic.rank(VillagerStateAccess.of(villager).getState());
        int units = rank >= TOP_RANK && villager.getRandom().nextInt(100) < BONUS_PERCENT ? 2 : 1;
        for (int i = 0; i < units && counters.total() < PassiveCounters.capacity(rank); i++) {
            counters = counters.withGained(PassiveCounters.CATCH, now);
        }
        PassiveCounters.set(villager, counters.withCasting(false).withNextStep(now + CAST_PAUSE_TICKS));
        PassiveLogic.syncLoadMeter(villager);
    }

    /** Blocks (by villager UUID) a cast from which missed the water, and the last such block: the next spot is another one, the nearest to the miss. Not saved. */
    private static final java.util.Map<java.util.UUID, java.util.Set<BlockPos>> MISSED_FROM = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<java.util.UUID, BlockPos> LAST_MISS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final int MAX_MISSED_SPOTS = 12;

    private static boolean missedFrom(Villager villager, BlockPos pos) {
        java.util.Set<BlockPos> missed = MISSED_FROM.get(villager.getUUID());
        return missed != null && missed.contains(pos);
    }

    /** The bobber never reached the water: this block is not tried again until a cast lands, and the new spot is looked for next to it. */
    public static void onMissed(Villager villager, BlockPos castFrom) {
        java.util.Set<BlockPos> missed = MISSED_FROM.computeIfAbsent(villager.getUUID(), id -> java.util.concurrent.ConcurrentHashMap.newKeySet());
        if (missed.size() >= MAX_MISSED_SPOTS) {
            missed.clear();
        }
        missed.add(castFrom.immutable());
        LAST_MISS.put(villager.getUUID(), castFrom.immutable());
        onLost(villager);
    }

    /** The cast found no water to land in, or the bobber was lost: a short pause, then the next walk to the water. */
    public static void onLost(Villager villager) {
        stowRod(villager);
        PassiveCounters counters = PassiveCounters.of(villager);
        // A cast that was lost: the spot was not good after all - look for a new one.
        PassiveCounters.set(villager, counters.withCasting(false).withNextStep(villager.level().getGameTime() + CAST_PAUSE_TICKS)
                .withWater(Optional.empty(), Optional.empty(), PassiveCounters.NEVER));
    }

    private static Optional<VillagerBobber> bobberOf(ServerLevel level, Villager villager) {
        if (!PassiveCounters.of(villager).casting()) {
            return Optional.empty();
        }
        return level.getEntitiesOfClass(VillagerBobber.class, villager.getBoundingBox().inflate(2 * WATER_REACH + 8), bobber -> bobber.getOwner() == villager)
                .stream().findFirst();
    }

    private static void holdRod(Villager villager) {
        if (!villager.getMainHandItem().is(Items.FISHING_ROD)) {
            villager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.FISHING_ROD));
            villager.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        }
    }

    private static void stowRod(Villager villager) {
        if (villager.getMainHandItem().is(Items.FISHING_ROD)) {
            villager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
    }

    /** What the Fisherman needs for a cast: the water block to land in and the spot to stand on that sees it. */
    private record Spot(BlockPos water, BlockPos shore) {
    }

    /**
     * The nearest water with an open surface (a lily pad is fine) within reach - of his job site if he has one, so the result does not depend on where
     * he happens to stand - together with the nearest spot to stand on that has a clear line to it (seesFrom). Several waters are tried until one
     * has such a spot, so a pool walled in by the basket or a house is skipped.
     */
    private static Optional<Spot> findSpot(ServerLevel level, Villager villager) {
        BlockPos center = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).map(GlobalPos::pos).orElse(villager.blockPosition());
        List<BlockPos> waters = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-WATER_REACH, -WATER_HEIGHT, -WATER_REACH), center.offset(WATER_REACH, WATER_HEIGHT, WATER_REACH))) {
            if (isWater(level, pos) && isOpenAbove(level, pos)) {
                waters.add(pos.immutable());
            }
        }
        waters.sort(Comparator.comparingDouble(pos -> pos.distSqr(center)));
        BlockPos lastMiss = LAST_MISS.get(villager.getUUID());
        BlockPos from = lastMiss != null ? lastMiss : villager.blockPosition();
        for (BlockPos water : waters.stream().limit(MAX_WATERS_TRIED).toList()) {
            Vec3 target = surfaceOf(level, water);
            List<BlockPos> spots = new ArrayList<>();
            for (BlockPos pos : BlockPos.betweenClosed(water.offset(-SHORE_SEARCH, -2, -SHORE_SEARCH), water.offset(SHORE_SEARCH, 2, SHORE_SEARCH))) {
                if (isStandable(level, pos) && !missedFrom(villager, pos)) {
                    spots.add(pos.immutable());
                }
            }
            spots.sort(Comparator.comparingDouble(pos -> pos.distSqr(from)));
            for (BlockPos spot : spots) {
                if (seesFrom(level, villager, Vec3.atBottomCenterOf(spot).add(0.0, HAND_HEIGHT, 0.0), target)) {
                    return Optional.of(new Spot(water, spot));
                }
            }
        }
        return Optional.empty();
    }

    /** The middle of the water surface of this block - what the bobber lands on. */
    private static Vec3 surfaceOf(ServerLevel level, BlockPos water) {
        BlockPos surface = water;
        while (level.getFluidState(surface.above()).is(FluidTags.WATER)) {
            surface = surface.above();
        }
        return new Vec3(surface.getX() + 0.5, surface.getY() + level.getFluidState(surface).getHeight(level, surface), surface.getZ() + 0.5);
    }

    /** Nothing solid between the hand and the water, and the water within throwing distance. */
    private static boolean seesFrom(ServerLevel level, Villager villager, Vec3 hand, Vec3 target) {
        if (hand.distanceToSqr(target) > MAX_CAST_DISTANCE * MAX_CAST_DISTANCE) {
            return false;
        }
        HitResult hit = level.clip(new ClipContext(hand, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, villager));
        return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(target) < 0.7 * 0.7;
    }

    /** From where he would hold the rod if he turned to the water now: a clear line to it. */
    private static boolean sees(ServerLevel level, Villager villager, Vec3 target) {
        return seesFrom(level, villager, VillagerBobber.handPosition(villager, yawTo(villager, target)), target);
    }

    private static float yawTo(Villager villager, Vec3 target) {
        return (float) (Mth.atan2(-(target.x - villager.getX()), target.z - villager.getZ()) * (180.0 / Math.PI));
    }

    /** Turns the whole villager to face the target - body and head - so the cast leaves his hand towards the water. */
    private static void faceTo(Villager villager, Vec3 target) {
        float yaw = yawTo(villager, target);
        villager.setYRot(yaw);
        villager.yBodyRot = yaw;
        villager.yBodyRotO = yaw;
        villager.setYHeadRot(yaw);
        villager.yHeadRotO = yaw;
    }

    private static boolean isStandable(ServerLevel level, BlockPos pos) {
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState below = level.getBlockState(pos.below());
        return feet.getCollisionShape(level, pos).isEmpty() && level.getFluidState(pos).isEmpty()
                && head.getCollisionShape(level, pos.above()).isEmpty() && level.getFluidState(pos.above()).isEmpty()
                && below.isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP) && level.getFluidState(pos.below()).isEmpty();
    }

    private static boolean isOpenAbove(ServerLevel level, BlockPos water) {
        BlockState above = level.getBlockState(water.above());
        return above.isAir() || above.is(Blocks.LILY_PAD) || above.getCollisionShape(level, water.above()).isEmpty() && level.getFluidState(water.above()).isEmpty();
    }

    private static boolean isWater(ServerLevel level, BlockPos pos) {
        return level.isLoaded(pos) && level.getFluidState(pos).is(FluidTags.WATER);
    }

    // --- At the basket ---

    /** Biome fish chance in percent: 1 on rank 0 up to 7 on rank 6. */
    static int biomeFishPercent(int passiveRank) {
        return Math.max(0, passiveRank) + 1;
    }

    @Override
    protected List<ItemStack> process(ServerLevel level, BlockPos basket, Identifier sort, int passiveRank, RandomSource random) {
        Optional<BiomeFish> biomeFish = BiomeFish.at(level, basket);
        int cod = 0;
        int salmon = 0;
        int biome = 0;
        // One unit is exactly one fish.
        if (biomeFish.isPresent() && random.nextInt(100) < biomeFishPercent(passiveRank)) {
            biome++;
        } else if (random.nextInt(100) < COD_PERCENT) {
            cod++;
        } else {
            salmon++;
        }
        List<ItemStack> result = new ArrayList<>();
        if (cod > 0) {
            result.add(new ItemStack(Items.COD, cod));
        }
        if (salmon > 0) {
            result.add(new ItemStack(Items.SALMON, salmon));
        }
        if (biome > 0) {
            result.add(new ItemStack(biomeFish.get().item(), biome));
        }
        return result;
    }
}
