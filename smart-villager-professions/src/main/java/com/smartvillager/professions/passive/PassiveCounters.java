package com.smartvillager.professions.passive;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.smartvillager.professions.SmartVillagerProfessionsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.Villager;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * What a Farmer or Fisherman carries to his basket (Obsidian Farmer.md / Fisherman.md, reworked 2026-10-07), in its own attachment like the quests -
 * the core's VillagerState knows nothing of it. Server only: nobody reads it on the client (the passive bar shows it); it survives a zombie conversion.
 *
 * carried: per sort (a crop's block id; for the Fisherman one sort, CATCH), how many units he carries. Every harvested crop or caught fish is one
 * unit; the total is capped by the passive rank (capacity: 3 on rank 0 up to 9 on rank 6). At the basket each unit is taken out and rolled.
 * lastGainTick: when the last unit came in - a villager that gains nothing for a while brings what he has (CarryingWork.IDLE_TICKS).
 * returning: he is on his way to the basket (full, or idle) and stays until he has put everything down.
 * nextStepTick: the game tick of his next step - at the basket the next unit is taken out, at the water the next cast is made.
 * casting / errandSince: the Fisherman's bobber is out; since when he is walking to the water to cast.
 * water / shore / fixedSpot / waterCheckedAt: the water block found near the Fisherman, the spot he casts from (a candidate until the first cast lands in the water, then fixed - the block he stood in - and the same every time), and when it was looked for - so there is no block scan on every work scan.
 */
public record PassiveCounters(Map<Identifier, Integer> carried, long lastGainTick, boolean returning, long nextStepTick,
                              boolean casting, long errandSince, Optional<BlockPos> water, Optional<BlockPos> shore, boolean fixedSpot, long waterCheckedAt) {

    /** The one sort of the Fisherman's catch - a fish counts the same whatever it will turn into at the basket. */
    public static final Identifier CATCH = Identifier.fromNamespaceAndPath(SmartVillagerProfessionsMod.MODID, "catch");
    /** Carry capacity by passive rank 0..6: 3 units on rank 0, 9 on rank 6 (Obsidian, 2026-10-07). */
    private static final int[] CAPACITY_BY_RANK = {3, 4, 5, 6, 7, 8, 9};
    public static final long NEVER = -1_000_000L;

    public static final PassiveCounters EMPTY = new PassiveCounters(Map.of(), NEVER, false, 0L, false, NEVER, Optional.empty(), Optional.empty(), false, NEVER);

    public static int capacity(int passiveRank) {
        return CAPACITY_BY_RANK[Math.clamp(passiveRank, 0, CAPACITY_BY_RANK.length - 1)];
    }

    public int total() {
        return carried.values().stream().mapToInt(Integer::intValue).sum();
    }

    public boolean isEmpty() {
        return carried.isEmpty();
    }

    /** One more unit of the sort; the call checks the capacity before. */
    public PassiveCounters withGained(Identifier sort, long gameTime) {
        Map<Identifier, Integer> map = new HashMap<>(carried);
        map.merge(sort, 1, Integer::sum);
        return new PassiveCounters(Map.copyOf(map), gameTime, returning, nextStepTick, casting, errandSince, water, shore, fixedSpot, waterCheckedAt);
    }

    /** The sort to take out next: the one he carries most of (ties: by id), so the result does not depend on map order. */
    public Optional<Identifier> next() {
        return carried.entrySet().stream()
                .max(Map.Entry.<Identifier, Integer>comparingByValue().thenComparing(entry -> entry.getKey().toString()))
                .map(Map.Entry::getKey);
    }

    /** One unit of the sort taken out; with nothing left he is no longer returning. */
    public PassiveCounters withTaken(Identifier sort) {
        Map<Identifier, Integer> map = new HashMap<>(carried);
        map.computeIfPresent(sort, (key, count) -> count > 1 ? count - 1 : null);
        return new PassiveCounters(Map.copyOf(map), lastGainTick, !map.isEmpty() && returning, nextStepTick, casting, errandSince, water, shore, fixedSpot, waterCheckedAt);
    }

    public PassiveCounters withReturning(boolean value) {
        return new PassiveCounters(carried, lastGainTick, value, nextStepTick, casting, errandSince, water, shore, fixedSpot, waterCheckedAt);
    }

    public PassiveCounters withNextStep(long tick) {
        return new PassiveCounters(carried, lastGainTick, returning, tick, casting, errandSince, water, shore, fixedSpot, waterCheckedAt);
    }

    public PassiveCounters withWater(Optional<BlockPos> newWater, Optional<BlockPos> newShore, long checkedAt) {
        return new PassiveCounters(carried, lastGainTick, returning, nextStepTick, casting, errandSince, newWater, newShore, false, checkedAt);
    }

    /** The block the Fisherman fishes from every time, from now on (where he stood when a cast first landed in the water). */
    public PassiveCounters withFixedSpot(BlockPos spot) {
        return new PassiveCounters(carried, lastGainTick, returning, nextStepTick, casting, errandSince, water, Optional.of(spot), true, waterCheckedAt);
    }

    /** The bobber is out (or not). */
    public PassiveCounters withCasting(boolean value) {
        return new PassiveCounters(carried, lastGainTick, returning, nextStepTick, value, errandSince, water, shore, fixedSpot, waterCheckedAt);
    }

    /** When the walk to the water began (NEVER-like value: not on the way). */
    public PassiveCounters withErrandSince(long tick) {
        return new PassiveCounters(carried, lastGainTick, returning, nextStepTick, casting, tick, water, shore, fixedSpot, waterCheckedAt);
    }

    public static final MapCodec<PassiveCounters> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.unboundedMap(Identifier.CODEC, Codec.INT).optionalFieldOf("carried", Map.of()).forGetter(PassiveCounters::carried),
            Codec.LONG.optionalFieldOf("last_gain", NEVER).forGetter(PassiveCounters::lastGainTick),
            Codec.BOOL.optionalFieldOf("returning", false).forGetter(PassiveCounters::returning),
            Codec.LONG.optionalFieldOf("next_step", 0L).forGetter(PassiveCounters::nextStepTick),
            Codec.BOOL.optionalFieldOf("casting", false).forGetter(PassiveCounters::casting),
            Codec.LONG.optionalFieldOf("errand_since", NEVER).forGetter(PassiveCounters::errandSince),
            BlockPos.CODEC.optionalFieldOf("water").forGetter(PassiveCounters::water),
            BlockPos.CODEC.optionalFieldOf("shore").forGetter(PassiveCounters::shore),
            Codec.BOOL.optionalFieldOf("fixed_spot", false).forGetter(PassiveCounters::fixedSpot),
            Codec.LONG.optionalFieldOf("water_checked_at", NEVER).forGetter(PassiveCounters::waterCheckedAt)
    ).apply(instance, PassiveCounters::new));

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, SmartVillagerProfessionsMod.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PassiveCounters>> ATTACHMENT = ATTACHMENT_TYPES.register(
            "passive_counters",
            () -> AttachmentType.builder(() -> EMPTY)
                    .serialize(MAP_CODEC)
                    // Also carried through a zombie conversion and back (NeoForge copies it then), like the quest state.
                    .copyOnDeath()
                    .build()
    );

    public static PassiveCounters of(Villager villager) {
        return villager.getData(ATTACHMENT);
    }

    public static void set(Villager villager, PassiveCounters counters) {
        if (!counters.equals(of(villager))) {
            villager.setData(ATTACHMENT, counters);
        }
    }
}
