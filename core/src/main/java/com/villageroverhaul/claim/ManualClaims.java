package com.villageroverhaul.claim;

import com.villageroverhaul.core.ProfessionLock;
import com.villageroverhaul.core.VillagerStateAccess;
import com.villageroverhaul.core.state.StationSlot;
import com.villageroverhaul.core.state.Stations;
import com.villageroverhaul.core.state.VillagerState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.phys.AABB;

import java.util.Optional;

/**
 * Block B: assigning a villager to a clicked block by hand (EmeraldClaimTool). A manual claim always
 * wins - whoever owned the block before loses it but keeps level, points and profession (decided
 * 2026-09-24); it then looks for another free workplace on its own (StationClaims / Vanilla).
 *
 * - Trading Block: becomes the villager's call point (TradingBlockCall), any villager with a job.
 * - A workplace of the villager's own profession (lectern, writing or enchantment station for a
 *   Librarian): replaces the villager's workplace of that kind.
 * - A workplace of another profession: a jobless villager - or one that never traded or earned XP -
 *   takes that profession, like Vanilla's own job switching. A villager with a settled job refuses.
 */
public final class ManualClaims {

    /** How far around a block the previous owner is searched - well past Vanilla's 48-block job-site reach. */
    private static final int OWNER_SEARCH_RADIUS = 64;

    private ManualClaims() {
    }

    /** Performs the claim and returns the message for the player - none for a Trading Block, which only shows particles. */
    public static Optional<Component> assign(ServerLevel level, Villager villager, BlockPos pos) {
        Component name = villager.getDisplayName();
        if (villager.isBaby() || villager.getVillagerData().profession().is(VillagerProfession.NITWIT)) {
            return Optional.of(Component.translatable("message.villageroverhaul.claim.cannot_work", name));
        }
        if (level.getBlockState(pos).is(ClaimBlocks.TRADING_BLOCK.get())) {
            Optional<Boolean> withinReach = TradingBlockCall.withinReach(villager, pos);
            if (withinReach.isEmpty()) {
                return Optional.of(Component.translatable("message.villageroverhaul.claim.no_workplace", name));
            }
            if (!withinReach.get()) {
                return Optional.of(Component.translatable("message.villageroverhaul.claim.too_far", name, TradingBlockCall.MAX_DISTANCE_TO_WORKPLACE));
            }
            assignTradingBlock(level, villager, pos);
            return Optional.empty();
        }
        Optional<Holder<PoiType>> type = level.getPoiManager().getType(pos);
        Optional<Holder<VillagerProfession>> jobOfBlock = type.flatMap(poi -> BuiltInRegistries.VILLAGER_PROFESSION.listElements()
                .filter(profession -> profession.value().heldJobSite().test(poi))
                .findFirst()
                .map(profession -> (Holder<VillagerProfession>) profession));
        if (jobOfBlock.isEmpty()) {
            return Optional.of(Component.translatable("message.villageroverhaul.claim.not_workplace"));
        }
        if (ownsWorkplace(villager, level, pos)) {
            return Optional.of(Component.translatable("message.villageroverhaul.claim.already", name));
        }

        Holder<VillagerProfession> profession = villager.getVillagerData().profession();
        if (!profession.equals(jobOfBlock.get())) {
            boolean settled = !profession.is(VillagerProfession.NONE) && villager.getVillagerXp() > 0;
            if (settled) {
                return Optional.of(Component.translatable("message.villageroverhaul.claim.settled", name));
            }
            releaseAllWorkplaces(level, villager);
            villager.setVillagerData(villager.getVillagerData().withProfession(jobOfBlock.get()));
            villager.refreshBrain(level);
        }

        // A villager on its way to a workplace Vanilla picked would take that one on arrival instead.
        villager.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE)
                .filter(potential -> potential.dimension() == level.dimension())
                .ifPresent(potential -> StationClaims.release(level, potential.pos()));
        villager.getBrain().eraseMemory(MemoryModuleType.POTENTIAL_JOB_SITE);

        displaceOwner(level, pos, villager);
        takeWorkplace(level, villager, pos);
        level.broadcastEntityEvent(villager, (byte) 14); // Vanilla's green "found a workplace" particles
        return Optional.of(Component.translatable("message.villageroverhaul.claim.assigned", name));
    }

    private static void assignTradingBlock(ServerLevel level, Villager villager, BlockPos pos) {
        GlobalPos target = GlobalPos.of(level.dimension(), pos);
        for (Villager other : villagersNear(level, pos)) {
            if (other != villager && VillagerStateAccess.of(other).getState().stations().tradingBlock().equals(Optional.of(target))) {
                setStations(other, VillagerStateAccess.of(other).getState().stations().withTradingBlock(Optional.empty()));
            }
        }
        setStations(villager, VillagerStateAccess.of(villager).getState().stations().withTradingBlock(Optional.of(target)));
        level.broadcastEntityEvent(villager, (byte) 14); // Vanilla's green star particles, no message
    }

    private static boolean ownsWorkplace(Villager villager, ServerLevel level, BlockPos pos) {
        GlobalPos target = GlobalPos.of(level.dimension(), pos);
        Stations stations = VillagerStateAccess.of(villager).getState().stations();
        return villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).equals(Optional.of(target))
                || java.util.Arrays.stream(StationSlot.values()).anyMatch(slot -> stations.get(slot).equals(Optional.of(target)));
    }

    /** The previous owner loses the block: its reservation is freed, its memory of it dropped, its job kept. */
    private static void displaceOwner(ServerLevel level, BlockPos pos, Villager newOwner) {
        GlobalPos target = GlobalPos.of(level.dimension(), pos);
        boolean released = false;
        for (Villager other : villagersNear(level, pos)) {
            if (other == newOwner) {
                continue;
            }
            boolean owned = false;
            if (other.getBrain().getMemory(MemoryModuleType.JOB_SITE).equals(Optional.of(target))) {
                other.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
                owned = true;
            }
            Stations stations = VillagerStateAccess.of(other).getState().stations();
            for (StationSlot slot : StationSlot.values()) {
                if (stations.get(slot).equals(Optional.of(target))) {
                    stations = stations.with(slot, Optional.empty());
                    owned = true;
                }
            }
            if (owned) {
                ProfessionLock.lock(other); // keeps its profession even without any workplace left
                setStations(other, stations);
                if (!released) {
                    StationClaims.release(level, pos);
                    released = true;
                }
            }
        }
    }

    /**
     * Replaces the villager's workplace of the same kind (for a profession with extra stations: the same
     * slot, else its Vanilla job site) and reserves the clicked one. It becomes the job site when that
     * kind was the job site, or when there was none.
     */
    private static void takeWorkplace(ServerLevel level, Villager villager, BlockPos pos) {
        GlobalPos target = GlobalPos.of(level.dimension(), pos);
        Optional<GlobalPos> jobSite = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
        Optional<ProfessionStations.Entry> entry = ProfessionStations.of(villager.getVillagerData().profession());
        Stations stations = VillagerStateAccess.of(villager).getState().stations();

        StationSlot slot = entry.map(e -> ProfessionStations.slotAt(level, e, pos)).orElse(StationSlot.BASIC);
        boolean jobSiteSameKind = jobSite.isEmpty() || entry.isEmpty()
                || (jobSite.get().dimension() == level.dimension() && ProfessionStations.slotAt(level, entry.get(), jobSite.get().pos()) == slot);
        Optional<GlobalPos> replaced = jobSiteSameKind ? jobSite : stations.get(slot);
        replaced.filter(old -> old.dimension() == level.dimension()).ifPresent(old -> StationClaims.release(level, old.pos()));

        level.getPoiManager().take(poi -> true, (poi, p) -> p.equals(pos), pos, 1);
        if (jobSiteSameKind) {
            villager.getBrain().setMemory(MemoryModuleType.JOB_SITE, target);
        }
        if (entry.isPresent()) {
            setStations(villager, stations.with(slot, Optional.of(target)));
        }
    }

    /** Frees every workplace of a villager that is about to change its profession. */
    private static void releaseAllWorkplaces(ServerLevel level, Villager villager) {
        villager.getBrain().getMemory(MemoryModuleType.JOB_SITE)
                .filter(pos -> pos.dimension() == level.dimension())
                .ifPresent(pos -> StationClaims.release(level, pos.pos()));
        villager.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
        Stations stations = VillagerStateAccess.of(villager).getState().stations();
        for (StationSlot slot : StationSlot.values()) {
            stations.get(slot).filter(pos -> pos.dimension() == level.dimension()).ifPresent(pos -> StationClaims.release(level, pos.pos()));
            stations = stations.with(slot, Optional.empty());
        }
        setStations(villager, stations);
    }

    private static java.util.List<Villager> villagersNear(ServerLevel level, BlockPos pos) {
        return level.getEntitiesOfClass(Villager.class, new AABB(pos).inflate(OWNER_SEARCH_RADIUS), Villager::isAlive);
    }

    private static void setStations(Villager villager, Stations stations) {
        VillagerStateAccess access = VillagerStateAccess.of(villager);
        VillagerState state = access.getState();
        if (!state.stations().equals(stations)) {
            access.setState(state.withStations(stations));
        }
    }
}
