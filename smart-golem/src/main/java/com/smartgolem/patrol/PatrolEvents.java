package com.smartgolem.patrol;

import com.smartgolem.SmartGolemMod;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Keeps the patrol map (PatrolMap) up to date with the paths players lay, and forgets it when the server stops. */
@EventBusSubscriber(modid = SmartGolemMod.MODID)
public final class PatrolEvents {

    private PatrolEvents() {
    }

    /** A path-like block placed. */
    @SubscribeEvent
    static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            PatrolMap.of(level).added(level, event.getPos(), event.getPlacedBlock().getBlock());
        }
    }

    /** A dirt path made with a shovel. */
    @SubscribeEvent
    static void onToolModification(BlockEvent.BlockToolModificationEvent event) {
        if (!event.isSimulated() && event.getLevel() instanceof ServerLevel level && event.getFinalState() != null) {
            PatrolMap.of(level).added(level, event.getPos(), event.getFinalState().getBlock());
        }
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        PatrolMap.clearAll();
    }
}
