package com.villageroverhaul.work;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.timeline.Timelines;

/**
 * The current in-game day number, read the same way Vanilla's own villager restock check does
 * (Villager.shouldRestock). Unlike raw game time, it advances when the night is skipped by sleeping,
 * so a skipped night counts as a passed day for all day-based timers.
 */
public final class DayClock {

    private DayClock() {
    }

    public static long today(ServerLevel level) {
        return level.registryAccess()
                .get(Timelines.OVERWORLD_DAY)
                .map(timeline -> (long) timeline.value().getPeriodCount(level.clockManager()))
                .orElse(0L);
    }
}
