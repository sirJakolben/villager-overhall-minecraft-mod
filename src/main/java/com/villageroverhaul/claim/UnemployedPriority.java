package com.villageroverhaul.claim;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.phys.AABB;

/**
 * Jobless villagers get free workplaces first (decided 2026-09-24): a villager that already has a
 * profession skips a free lectern or station while a jobless villager is around who is not already
 * heading for another workplace. Performance: one entity lookup per candidate, and only villagers
 * that are actually looking for a workplace ask - Vanilla at most every 1-2 s, StationClaims every 5 s.
 */
public final class UnemployedPriority {

    /** Same reach as Vanilla's own workplace search (AcquirePoi). */
    public static final int RADIUS = 48;

    private UnemployedPriority() {
    }

    public static boolean isUnemployedNear(ServerLevel level, BlockPos pos) {
        return !level.getEntitiesOfClass(Villager.class, new AABB(pos).inflate(RADIUS), UnemployedPriority::isLookingForWork).isEmpty();
    }

    public static boolean isLookingForWork(Villager villager) {
        return villager.isAlive()
                && !villager.isBaby()
                && villager.getVillagerData().profession().is(VillagerProfession.NONE)
                && villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).isEmpty()
                && villager.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).isEmpty();
    }
}
