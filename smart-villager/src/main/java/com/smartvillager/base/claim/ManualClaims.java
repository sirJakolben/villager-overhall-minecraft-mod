package com.smartvillager.base.claim;

import com.smartvillager.base.api.ClaimHandler;
import com.smartvillager.base.api.SmartVillagerApi;
import com.smartvillager.base.trading.TradingBlockCall;
import com.smartvillager.base.trading.TradingBlockLinks;
import com.smartvillager.base.trading.TradingBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;

/**
 * Assigning a villager to a clicked block by hand (EmeraldClaimTool). A manual claim always
 * wins - whoever owned the block before loses it but keeps level, points and profession (decided
 * 2026-09-24); it then looks for another free workplace on its own (Vanilla, or a mod's ClaimHandler).
 *
 * - Trading Block: becomes the villager's call point (TradingBlockCall), any villager with a job.
 * - A workplace of the villager's own profession: replaces its job site (a ClaimHandler may track more than one).
 * - A workplace of another profession: a jobless villager - or one that never traded or earned XP -
 *   takes that profession, like Vanilla's own job switching. A villager with a settled job refuses.
 *
 * Works on Vanilla alone: its job site memory and point-of-interest ticket. Mods that give villagers more workplaces (Progression's stations)
 * add their bookkeeping through api/ClaimHandler.
 */
public final class ManualClaims {

    /** How far around a block the previous owner is searched - well past Vanilla's 48-block job-site reach. */
    private static final int OWNER_SEARCH_RADIUS = 64;
    /** Stars over a claimed block - as many as Vanilla shows around the villager - and their spread in blocks. */
    private static final int CLAIM_PARTICLES = 5;
    private static final double CLAIM_PARTICLE_SPREAD = 0.3;

    private ManualClaims() {
    }

    /** Performs the claim and returns the message for the player - none for a Trading Block, which only shows particles. */
    public static Optional<Component> assign(ServerLevel level, Villager villager, BlockPos pos) {
        Component name = villager.getDisplayName();
        if (villager.isBaby() || villager.getVillagerData().profession().is(VillagerProfession.NITWIT)) {
            return Optional.of(Component.translatable("message.smartvillager.claim.cannot_work", name));
        }
        if (level.getBlockState(pos).is(TradingBlocks.TRADING_BLOCK.get())) {
            Optional<Boolean> withinReach = TradingBlockCall.withinReach(villager, pos);
            if (withinReach.isEmpty()) {
                return Optional.of(Component.translatable("message.smartvillager.claim.no_workplace", name));
            }
            if (!withinReach.get()) {
                return Optional.of(Component.translatable("message.smartvillager.claim.too_far", name, TradingBlockCall.MAX_DISTANCE_TO_WORKPLACE));
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
            return Optional.of(Component.translatable("message.smartvillager.claim.not_workplace"));
        }
        if (ownsWorkplace(villager, level, pos)) {
            return Optional.of(Component.translatable("message.smartvillager.claim.already", name));
        }

        Holder<VillagerProfession> profession = villager.getVillagerData().profession();
        if (!profession.equals(jobOfBlock.get())) {
            boolean settled = !profession.is(VillagerProfession.NONE) && villager.getVillagerXp() > 0;
            if (settled) {
                return Optional.of(Component.translatable("message.smartvillager.claim.settled", name));
            }
            releaseAllWorkplaces(level, villager);
            villager.setVillagerData(villager.getVillagerData().withProfession(jobOfBlock.get()));
            villager.refreshBrain(level);
        }

        // A villager on its way to a workplace Vanilla picked would take that one on arrival instead.
        villager.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE)
                .filter(potential -> potential.dimension() == level.dimension())
                .ifPresent(potential -> release(level, potential.pos()));
        villager.getBrain().eraseMemory(MemoryModuleType.POTENTIAL_JOB_SITE);

        displaceOwner(level, pos, villager);
        takeWorkplace(level, villager, pos);
        showClaimParticles(level, villager, pos);
        return Optional.of(Component.translatable("message.smartvillager.claim.assigned", name));
    }

    /**
     * Frees a point-of-interest ticket. Vanilla's PoiManager.release throws when the point of interest is already gone
     * (block broken), so every release goes through here.
     */
    public static void release(ServerLevel level, BlockPos pos) {
        if (level.getPoiManager().getType(pos).isPresent()) {
            level.getPoiManager().release(pos);
        }
    }

    /**
     * Vanilla's green "found a workplace" stars (entity event 14) around the villager - and the same stars over the
     * claimed block (2026-09-28), so both ends of the assignment light up.
     */
    private static void showClaimParticles(ServerLevel level, Villager villager, BlockPos pos) {
        level.broadcastEntityEvent(villager, (byte) 14);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                CLAIM_PARTICLES, CLAIM_PARTICLE_SPREAD, CLAIM_PARTICLE_SPREAD, CLAIM_PARTICLE_SPREAD, 0.0);
    }

    private static void assignTradingBlock(ServerLevel level, Villager villager, BlockPos pos) {
        GlobalPos target = GlobalPos.of(level.dimension(), pos);
        for (Villager other : villagersNear(level, pos)) {
            if (other != villager && TradingBlockLinks.get(other).equals(Optional.of(target))) {
                TradingBlockLinks.set(other, Optional.empty());
            }
        }
        TradingBlockLinks.set(villager, Optional.of(target));
        showClaimParticles(level, villager, pos); // no message
    }

    private static boolean ownsWorkplace(Villager villager, ServerLevel level, BlockPos pos) {
        GlobalPos target = GlobalPos.of(level.dimension(), pos);
        return villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).equals(Optional.of(target))
                || SmartVillagerApi.claimHandlers().stream().anyMatch(handler -> handler.owns(villager, level, pos));
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
            for (ClaimHandler handler : SmartVillagerApi.claimHandlers()) {
                owned |= handler.displace(other, level, pos);
            }
            if (owned && !released) {
                release(level, pos);
                released = true;
            }
        }
    }

    /**
     * The clicked block becomes the villager's workplace: a ClaimHandler that tracks several workplaces takes it as its own; otherwise the
     * block replaces the job site.
     */
    private static void takeWorkplace(ServerLevel level, Villager villager, BlockPos pos) {
        for (ClaimHandler handler : SmartVillagerApi.claimHandlers()) {
            if (handler.take(villager, level, pos)) {
                return;
            }
        }
        GlobalPos target = GlobalPos.of(level.dimension(), pos);
        villager.getBrain().getMemory(MemoryModuleType.JOB_SITE)
                .filter(old -> old.dimension() == level.dimension() && !old.equals(target))
                .ifPresent(old -> release(level, old.pos()));
        // Fails when a previous owner out of reach (not displaced) still holds the ticket - the claim wins anyway.
        level.getPoiManager().take(poi -> true, (poi, p) -> p.equals(pos), pos, 1);
        villager.getBrain().setMemory(MemoryModuleType.JOB_SITE, target);
    }

    /** Frees every workplace of a villager that is about to change its profession. */
    private static void releaseAllWorkplaces(ServerLevel level, Villager villager) {
        villager.getBrain().getMemory(MemoryModuleType.JOB_SITE)
                .filter(pos -> pos.dimension() == level.dimension())
                .ifPresent(pos -> release(level, pos.pos()));
        villager.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
        for (ClaimHandler handler : SmartVillagerApi.claimHandlers()) {
            handler.releaseAll(villager, level);
        }
    }

    private static List<Villager> villagersNear(ServerLevel level, BlockPos pos) {
        return level.getEntitiesOfClass(Villager.class, new AABB(pos).inflate(OWNER_SEARCH_RADIUS), Villager::isAlive);
    }
}
