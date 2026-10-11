package com.smartvillager.progression.station;

import com.smartvillager.base.api.ClaimHandler;
import com.smartvillager.base.claim.ManualClaims;
import com.smartvillager.progression.progression.ProfessionLock;
import com.smartvillager.progression.state.Stations;
import com.smartvillager.progression.state.VillagerState;
import com.smartvillager.progression.state.VillagerStateAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Smart Villager's emerald claim (ManualClaims) knows Vanilla's one job site; a villager here owns one workplace per station
 * (state/Stations, StationOwners). This is the bookkeeping the claim hands over to us, registered in SmartVillagerApi:
 * who loses a workplace keeps its profession (ProfessionLock) and lets go of the station; who takes one gets it as the station's
 * workplace, and as job site if that station was the job site (or there was none).
 */
public final class StationClaimHandler implements ClaimHandler {

    @Override
    public boolean owns(Villager villager, ServerLevel level, BlockPos pos) {
        return VillagerStateAccess.of(villager).getState().stations().positions().containsValue(GlobalPos.of(level.dimension(), pos));
    }

    @Override
    public boolean displace(Villager villager, ServerLevel level, BlockPos pos) {
        GlobalPos target = GlobalPos.of(level.dimension(), pos);
        Stations stations = VillagerStateAccess.of(villager).getState().stations();
        boolean owned = false;
        for (Identifier station : List.copyOf(stations.positions().keySet())) {
            if (stations.positions().get(station).equals(target)) {
                stations = stations.with(station, Optional.empty());
                owned = true;
            }
        }
        if (owned) {
            ProfessionLock.lock(villager); // keeps its profession even without any workplace left
            setStations(villager, stations);
        }
        return owned;
    }

    @Override
    public boolean take(Villager villager, ServerLevel level, BlockPos pos) {
        GlobalPos target = GlobalPos.of(level.dimension(), pos);
        Holder<VillagerProfession> profession = villager.getVillagerData().profession();
        Optional<GlobalPos> jobSite = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
        Stations stations = VillagerStateAccess.of(villager).getState().stations();

        Identifier station = ProfessionStations.stationAt(level, profession, pos);
        boolean jobSiteSameStation = jobSite.isEmpty()
                || (jobSite.get().dimension() == level.dimension() && ProfessionStations.stationAt(level, profession, jobSite.get().pos()).equals(station));
        Optional<GlobalPos> replaced = jobSiteSameStation ? jobSite : stations.get(station);
        replaced.filter(old -> old.dimension() == level.dimension()).ifPresent(old -> ManualClaims.release(level, old.pos()));

        // Fails when a previous owner out of reach (not displaced) still holds the ticket - the claim wins anyway:
        // that owner lets go at its next scan without releasing it, so the one ticket stays with one owner.
        level.getPoiManager().take(poi -> true, (poi, p) -> p.equals(pos), pos, 1);
        StationOwners.claim(villager, target);
        if (jobSiteSameStation) {
            villager.getBrain().setMemory(MemoryModuleType.JOB_SITE, target);
        }
        setStations(villager, stations.with(station, Optional.of(target)));
        return true;
    }

    @Override
    public void releaseAll(Villager villager, ServerLevel level) {
        Stations stations = VillagerStateAccess.of(villager).getState().stations();
        for (GlobalPos pos : stations.positions().values()) {
            if (pos.dimension() == level.dimension()) {
                ManualClaims.release(level, pos.pos());
            }
        }
        setStations(villager, new Stations(Map.of(), stations.heldByStation()));
    }

    @Override
    public Stream<GlobalPos> workplaces(Villager villager) {
        return VillagerStateAccess.of(villager).getState().stations().positions().values().stream();
    }

    private static void setStations(Villager villager, Stations stations) {
        VillagerStateAccess access = VillagerStateAccess.of(villager);
        VillagerState state = access.getState();
        if (!state.stations().equals(stations)) {
            access.setState(state.withStations(stations));
        }
    }
}
