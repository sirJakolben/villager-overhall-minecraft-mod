package com.villageroverhaul.claim;

import com.mojang.datafixers.util.Pair;
import com.villageroverhaul.VillagerOverhaulMod;
import com.villageroverhaul.core.VillagerStateAccess;
import com.villageroverhaul.core.state.StationSlot;
import com.villageroverhaul.core.state.Stations;
import com.villageroverhaul.core.state.VillagerState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.pathfinder.Path;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.List;
import java.util.Optional;

/**
 * Block W: a villager owns up to three equal workplaces (StationSlot). Vanilla only knows one of them -
 * its JOB_SITE - and claims that one itself (a jobless villager takes any free lectern or station, see
 * ProfessionStations); this class takes care of the other two. Runs from the periodic work scan
 * (every 100 ticks per villager), in this order:
 *
 * 1. Mirror: Vanilla's job site goes into its slot (by block).
 * 2. Validate: a workplace whose block is gone, or that no longer fits the profession, is dropped.
 * 3. Backup: lost its job site (block broken) but owns another workplace - that one becomes the job site.
 * 4. Acquire: takes the closest free workplace for each empty slot - skipping any a jobless villager
 *    nearby could take instead (UnemployedPriority).
 * 5. Keep the job: with two or more workplaces, Vanilla must not fire it when one breaks (see Stations).
 *
 * Reservation is Vanilla's point-of-interest ticket (PoiManager.take / release), so a workplace never
 * has two owners, survives saving, and frees itself when the block is broken. Performance: indexed
 * point-of-interest lookups, one path check per found free workplace - never a block scan.
 */
@EventBusSubscriber(modid = VillagerOverhaulMod.MODID)
public final class StationClaims {

    private static final int MAX_CANDIDATES = 5;
    /** Same "does it make sense to walk there" check Vanilla does before claiming a job site. */
    private static final int PATH_REACH = 1;

    private StationClaims() {
    }

    public static VillagerState update(ServerLevel level, Villager villager, VillagerState state) {
        Brain<Villager> brain = villager.getBrain();
        Optional<GlobalPos> jobSite = brain.getMemory(MemoryModuleType.JOB_SITE).filter(pos -> pos.dimension() == level.dimension());
        Optional<ProfessionStations.Entry> entry = ProfessionStations.of(villager.getVillagerData().profession());
        Stations stations = state.stations();

        if (entry.isEmpty()) {
            // No stations of our own (a fallback profession, FallbackCatalog): its Vanilla job site is its basic
            // station, so its trades open; master and passive stay empty.
            for (StationSlot slot : StationSlot.values()) {
                stations = slot == StationSlot.BASIC && jobSite.isPresent() ? stations.with(slot, jobSite) : drop(level, stations, slot, jobSite);
            }
        } else {
            stations = mirrorJobSite(level, entry.get(), stations, jobSite);
            stations = validate(level, entry.get(), stations, jobSite);
            if (jobSite.isEmpty()) {
                useBackupAsJobSite(level, villager, entry.get(), stations);
            }
            if (!villager.isBaby()) {
                stations = acquire(level, villager, entry.get(), stations);
            }
        }
        stations = keepJob(villager, state, stations);
        VillagerState updated = stations.equals(state.stations()) ? state : state.withStations(stations);
        Stations checked = forgetUnusableTradingBlock(level, villager, updated);
        return checked.equals(updated.stations()) ? updated : updated.withStations(checked);
    }

    /**
     * A Trading Block that was broken (or replaced), or that ended up more than 48 blocks from all of the
     * villager's workplaces (e.g. after it moved to a new workplace), no longer calls its villager. With no
     * workplace at all for the moment, the block is kept.
     */
    private static Stations forgetUnusableTradingBlock(ServerLevel level, Villager villager, VillagerState state) {
        Stations stations = state.stations();
        Optional<GlobalPos> block = stations.tradingBlock().filter(pos -> pos.dimension() == level.dimension());
        if (block.isEmpty()) {
            return stations;
        }
        boolean broken = level.isLoaded(block.get().pos()) && !level.getBlockState(block.get().pos()).is(ClaimBlocks.TRADING_BLOCK.get());
        boolean tooFar = !TradingBlockCall.withinReach(villager, state, block.get().pos()).orElse(true);
        return broken || tooFar ? stations.withTradingBlock(Optional.empty()) : stations;
    }

    private static Stations mirrorJobSite(ServerLevel level, ProfessionStations.Entry entry, Stations stations, Optional<GlobalPos> jobSite) {
        if (jobSite.isEmpty()) {
            return stations;
        }
        StationSlot slot = ProfessionStations.slotAt(level, entry, jobSite.get().pos());
        Optional<GlobalPos> previous = stations.get(slot);
        if (previous.isPresent() && !previous.equals(jobSite) && previous.get().dimension() == level.dimension()) {
            release(level, previous.get().pos()); // Vanilla gave it a second one of this kind
        }
        return stations.with(slot, jobSite);
    }

    private static Stations validate(ServerLevel level, ProfessionStations.Entry entry, Stations stations, Optional<GlobalPos> jobSite) {
        for (StationSlot slot : StationSlot.values()) {
            Optional<GlobalPos> owned = stations.get(slot);
            if (owned.isEmpty() || owned.get().dimension() != level.dimension()) {
                continue; // a workplace in another dimension can't be checked from here - keep it
            }
            BlockPos pos = owned.get().pos();
            boolean fits = level.getPoiManager().exists(pos, type -> type.is(entry.jobSite()))
                    && ProfessionStations.slotAt(level, entry, pos) == slot;
            if (!fits) {
                stations = drop(level, stations, slot, jobSite);
            }
        }
        return stations;
    }

    /**
     * Frees a reservation. Vanilla's PoiManager.release throws when the point of interest is already gone
     * (block broken), so every release in this mod goes through here.
     */
    public static void release(ServerLevel level, BlockPos pos) {
        if (level.getPoiManager().getType(pos).isPresent()) {
            level.getPoiManager().release(pos);
        }
    }

    /** Frees a slot - its ticket too, unless it is the job site, which Vanilla releases itself. */
    private static Stations drop(ServerLevel level, Stations stations, StationSlot slot, Optional<GlobalPos> jobSite) {
        Optional<GlobalPos> owned = stations.get(slot);
        if (owned.isEmpty()) {
            return stations;
        }
        if (!owned.equals(jobSite) && owned.get().dimension() == level.dimension()) {
            release(level, owned.get().pos());
        }
        return stations.with(slot, Optional.empty());
    }

    private static void useBackupAsJobSite(ServerLevel level, Villager villager, ProfessionStations.Entry entry, Stations stations) {
        for (StationSlot slot : StationSlot.values()) {
            Optional<GlobalPos> owned = stations.get(slot).filter(pos -> pos.dimension() == level.dimension());
            if (owned.isPresent()) {
                // Re-take the ticket in case Vanilla released it when it dropped the job site; a no-op if we still hold it.
                BlockPos pos = owned.get().pos();
                level.getPoiManager().take(type -> type.is(entry.jobSite()), (t, p) -> p.equals(pos), pos, 1);
                villager.getBrain().setMemory(MemoryModuleType.JOB_SITE, owned.get());
                return;
            }
        }
    }

    private static Stations acquire(ServerLevel level, Villager villager, ProfessionStations.Entry entry, Stations stations) {
        for (StationSlot slot : StationSlot.values()) {
            if (stations.get(slot).isPresent()) {
                continue;
            }
            Optional<BlockPos> taken = takeClosest(level, villager, entry, slot);
            if (taken.isPresent()) {
                stations = stations.with(slot, Optional.of(GlobalPos.of(level.dimension(), taken.get())));
                level.broadcastEntityEvent(villager, (byte) 14); // Vanilla's green "found a workplace" particles
            }
        }
        return stations;
    }

    private static Optional<BlockPos> takeClosest(ServerLevel level, Villager villager, ProfessionStations.Entry entry, StationSlot slot) {
        PoiManager poiManager = level.getPoiManager();
        List<BlockPos> candidates = poiManager.findAllClosestFirstWithType(
                        type -> type.is(entry.jobSite()), pos -> ProfessionStations.slotAt(level, entry, pos) == slot,
                        villager.blockPosition(), UnemployedPriority.RADIUS, PoiManager.Occupancy.HAS_SPACE)
                .limit(MAX_CANDIDATES)
                .map(Pair::getSecond)
                .toList();
        for (BlockPos pos : candidates) {
            if (UnemployedPriority.isUnemployedNear(level, pos)) {
                continue;
            }
            Path path = villager.getNavigation().createPath(pos, PATH_REACH);
            if (path == null || !path.canReach()) {
                continue;
            }
            return poiManager.take(type -> type.is(entry.jobSite()), (t, p) -> p.equals(pos), pos, 1);
        }
        return Optional.empty();
    }

    /**
     * Vanilla fires a villager without experience the moment its job site breaks - even if it still owns
     * another workplace. While it owns two or more, its Vanilla experience is held at 1; that is undone
     * once it is down to one again and never traded or earned our XP.
     */
    private static Stations keepJob(Villager villager, VillagerState state, Stations stations) {
        if (stations.count() >= 2 && villager.getVillagerXp() == 0) {
            villager.setVillagerXp(1);
            return stations.withHeldByStation(true);
        }
        if (stations.heldByStation() && stations.count() <= 1 && state.level() == 0 && state.xp() == 0) {
            villager.setVillagerXp(0);
            return stations.withHeldByStation(false);
        }
        return stations;
    }

    @SubscribeEvent
    static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Villager villager && villager.level() instanceof ServerLevel level) {
            Stations stations = VillagerStateAccess.of(villager).getState().stations();
            for (StationSlot slot : StationSlot.values()) {
                stations.get(slot).filter(pos -> pos.dimension() == level.dimension()).ifPresent(pos -> release(level, pos.pos()));
            }
        }
    }
}
