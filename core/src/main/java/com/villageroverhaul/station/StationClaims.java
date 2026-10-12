package com.villageroverhaul.station;

import com.mojang.datafixers.util.Pair;
import com.villageroverhaul.VillagerOverhaulMod;
import com.villageroverhaul.interaction.TradingBlockCall;
import com.villageroverhaul.interaction.TradingBlocks;
import com.villageroverhaul.section.Sections;
import com.villageroverhaul.state.Stations;
import com.villageroverhaul.state.VillagerState;
import com.villageroverhaul.state.VillagerStateAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.pathfinder.Path;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * A villager owns one workplace per station of its profession (ProfessionStations). Vanilla only knows one of
 * them - its JOB_SITE - and claims that one itself (a jobless villager takes any free lectern or station); this
 * class takes care of the others. Runs from the periodic work scan (every 100 ticks per villager), in this order:
 *
 * 1. Mirror: Vanilla's job site goes into its station (by block).
 * 2. Validate: a workplace whose block is gone, or that no longer fits the profession, is dropped - and so is
 *    one another villager owns (StationOwners: the ticket alone doesn't know its holder).
 * 3. Backup: lost its job site (block broken) but owns another workplace - that one becomes the job site.
 * 4. Acquire: takes the closest free workplace for each station it lacks - skipping any a jobless villager
 *    nearby could take instead (UnemployedPriority).
 * 5. Keep the job: with two or more workplaces, Vanilla must not fire it when one breaks (see Stations).
 *
 * A profession without registered stations only mirrors its Vanilla job site. Reservation is Vanilla's
 * point-of-interest ticket (PoiManager.take / release), so a workplace never has two owners, survives saving,
 * and frees itself when the block is broken. Performance: indexed point-of-interest lookups, one path check
 * per found free workplace - never a block scan.
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
        Holder<VillagerProfession> profession = villager.getVillagerData().profession();
        Stations stations = state.stations();

        stations = mirrorJobSite(level, profession, stations, jobSite);
        stations = validate(level, profession, stations, jobSite);
        stations = dropForeign(level, villager, stations, jobSite);
        jobSite = brain.getMemory(MemoryModuleType.JOB_SITE).filter(pos -> pos.dimension() == level.dimension());
        if (jobSite.isEmpty()) {
            useBackupAsJobSite(level, villager, stations);
        }
        if (!villager.isBaby()) {
            stations = acquire(level, villager, profession, stations);
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
        boolean broken = level.isLoaded(block.get().pos()) && !level.getBlockState(block.get().pos()).is(TradingBlocks.TRADING_BLOCK.get());
        boolean tooFar = !TradingBlockCall.withinReach(villager, state, block.get().pos()).orElse(true);
        return broken || tooFar ? stations.withTradingBlock(Optional.empty()) : stations;
    }

    private static Stations mirrorJobSite(ServerLevel level, Holder<VillagerProfession> profession, Stations stations, Optional<GlobalPos> jobSite) {
        if (jobSite.isEmpty()) {
            return stations;
        }
        Identifier station = ProfessionStations.stationAt(level, profession, jobSite.get().pos());
        Optional<GlobalPos> previous = stations.get(station);
        if (previous.isPresent() && !previous.equals(jobSite) && previous.get().dimension() == level.dimension()) {
            release(level, previous.get().pos()); // Vanilla gave it a second one of this kind
        }
        return stations.with(station, jobSite);
    }

    private static Stations validate(ServerLevel level, Holder<VillagerProfession> profession, Stations stations, Optional<GlobalPos> jobSite) {
        List<Identifier> ownStations = ProfessionStations.ids(profession);
        for (Identifier station : List.copyOf(stations.positions().keySet())) {
            GlobalPos owned = stations.positions().get(station);
            if (owned.dimension() != level.dimension()) {
                continue; // a workplace in another dimension can't be checked from here - keep it
            }
            BlockPos pos = owned.pos();
            boolean fits = ownStations.contains(station)
                    && level.getPoiManager().exists(pos, jobSiteOf(profession))
                    && ProfessionStations.stationAt(level, profession, pos).equals(station);
            if (!fits) {
                stations = drop(level, stations, station, jobSite);
            }
        }
        return stations;
    }

    /**
     * A workplace another villager owns (StationOwners) is let go - without releasing the ticket, which is the
     * owner's. If it was the job site, the memory goes too; the villager then looks for another workplace.
     */
    private static Stations dropForeign(ServerLevel level, Villager villager, Stations stations, Optional<GlobalPos> jobSite) {
        for (Identifier station : List.copyOf(stations.positions().keySet())) {
            GlobalPos owned = stations.positions().get(station);
            if (owned.dimension() != level.dimension() || StationOwners.keep(level, villager, owned)) {
                continue;
            }
            stations = stations.with(station, Optional.empty());
            if (jobSite.equals(Optional.of(owned))) {
                villager.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
            }
        }
        return stations;
    }

    private static Predicate<Holder<PoiType>> jobSiteOf(Holder<VillagerProfession> profession) {
        return profession.value().heldJobSite();
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

    /** Frees a station - its ticket too, unless it is the job site, which Vanilla releases itself. */
    private static Stations drop(ServerLevel level, Stations stations, Identifier station, Optional<GlobalPos> jobSite) {
        Optional<GlobalPos> owned = stations.get(station);
        if (owned.isEmpty()) {
            return stations;
        }
        if (!owned.equals(jobSite) && owned.get().dimension() == level.dimension()) {
            release(level, owned.get().pos());
        }
        return stations.with(station, Optional.empty());
    }

    private static void useBackupAsJobSite(ServerLevel level, Villager villager, Stations stations) {
        Holder<VillagerProfession> profession = villager.getVillagerData().profession();
        for (GlobalPos owned : stations.positions().values()) {
            if (owned.dimension() == level.dimension()) {
                // Re-take the ticket in case Vanilla released it when it dropped the job site; a no-op if we still hold it.
                BlockPos pos = owned.pos();
                level.getPoiManager().take(jobSiteOf(profession), (t, p) -> p.equals(pos), pos, 1);
                villager.getBrain().setMemory(MemoryModuleType.JOB_SITE, owned);
                return;
            }
        }
    }

    /** Only for a profession with registered stations - with just the Vanilla job site, Vanilla's own search is enough. */
    private static Stations acquire(ServerLevel level, Villager villager, Holder<VillagerProfession> profession, Stations stations) {
        if (Sections.stationsOf(profession).isEmpty()) {
            return stations;
        }
        for (Identifier station : ProfessionStations.ids(profession)) {
            if (stations.get(station).isPresent()) {
                continue;
            }
            Optional<BlockPos> taken = takeClosest(level, villager, profession, station);
            if (taken.isPresent()) {
                GlobalPos pos = GlobalPos.of(level.dimension(), taken.get());
                StationOwners.claim(villager, pos); // its ticket was free - anyone else listing it has lost it
                stations = stations.with(station, Optional.of(pos));
                level.broadcastEntityEvent(villager, (byte) 14); // Vanilla's green "found a workplace" particles
            }
        }
        return stations;
    }

    private static Optional<BlockPos> takeClosest(ServerLevel level, Villager villager, Holder<VillagerProfession> profession, Identifier station) {
        PoiManager poiManager = level.getPoiManager();
        List<BlockPos> candidates = poiManager.findAllClosestFirstWithType(
                        jobSiteOf(profession), pos -> ProfessionStations.stationAt(level, profession, pos).equals(station),
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
            return poiManager.take(jobSiteOf(profession), (t, p) -> p.equals(pos), pos, 1);
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
            for (GlobalPos pos : VillagerStateAccess.of(villager).getState().stations().positions().values()) {
                if (pos.dimension() == level.dimension()) {
                    release(level, pos.pos());
                }
            }
        }
    }
}
