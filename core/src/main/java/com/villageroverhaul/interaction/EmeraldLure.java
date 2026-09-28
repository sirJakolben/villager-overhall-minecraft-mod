package com.villageroverhaul.interaction;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

/**
 * Villagers follow a nearby player holding an emerald, like cows follow wheat (decided
 * 2026-09-24). While lured they forget everything except sleep - work, meetings, even panic and raids:
 * VillagerLureMixin skips the villager's whole brain for that tick and this class steers it instead
 * (Vanilla's TemptGoal: walk towards the player each tick, stop a little before, look at them).
 * When the player puts the emerald away, the brain simply runs again from where it was.
 *
 * Performance: one pass over the level's players per villager and tick - there are only ever a few.
 */
public final class EmeraldLure {

    /** Vanilla's tempt range for animals. */
    private static final double RANGE = 10.0;
    /** Vanilla's TemptGoal stops this close. */
    private static final double STOP_DISTANCE = 2.5;
    /** A little faster than a villager's normal walk (0.5). */
    private static final double SPEED = 0.6;

    private EmeraldLure() {
    }

    /** Steers a lured villager for this tick; false (brain runs as usual) when nobody lures it. */
    public static boolean steer(ServerLevel level, Villager villager) {
        if (villager.isSleeping()) {
            return false;
        }
        Player lurer = null;
        double closest = RANGE * RANGE;
        for (ServerPlayer player : level.players()) {
            double distance = player.distanceToSqr(villager);
            if (distance <= closest && !player.isSpectator() && player.isAlive() && holdsEmerald(player)) {
                closest = distance;
                lurer = player;
            }
        }
        if (lurer == null) {
            return false;
        }
        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        villager.getLookControl().setLookAt(lurer, villager.getMaxHeadYRot() + 20, villager.getMaxHeadXRot());
        if (closest < STOP_DISTANCE * STOP_DISTANCE) {
            villager.getNavigation().stop();
        } else {
            villager.getNavigation().moveTo(lurer, SPEED);
        }
        return true;
    }

    private static boolean holdsEmerald(Player player) {
        return player.getMainHandItem().is(Items.EMERALD) || player.getOffhandItem().is(Items.EMERALD);
    }
}
