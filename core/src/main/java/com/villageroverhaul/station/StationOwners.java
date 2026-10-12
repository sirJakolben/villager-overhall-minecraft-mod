package com.villageroverhaul.station;

import com.villageroverhaul.VillagerOverhaulMod;
import com.villageroverhaul.state.VillagerStateAccess;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Who owns which workplace - the guard that a workplace never ends up with two owners (bug 2026-09-28: two
 * Masons worked at one crushing station). The point-of-interest ticket alone can't guarantee that: it only
 * counts, it doesn't know its holder, and a villager keeps a workplace in its state even when the ticket was
 * lost - a manual claim that couldn't find the previous owner, a station broken and set again between two
 * scans, a backup job site whose ticket someone else took meanwhile.
 *
 * Each work scan checks every workplace the villager holds against this index (StationClaims.update): the first
 * one recorded keeps it as long as it is loaded, alive and still lists it; any other villager listing it lets
 * go of it - without releasing the ticket, which belongs to the one who keeps it - and looks for another. A
 * manual claim (claim) always takes over. Server-side memory only, rebuilt by the scans after a restart
 * (whoever scans first keeps it); a stale entry of a dead or displaced villager fails that check and is replaced.
 */
@EventBusSubscriber(modid = VillagerOverhaulMod.MODID)
public final class StationOwners {

    private static final Map<GlobalPos, UUID> OWNERS = new HashMap<>();

    private StationOwners() {
    }

    /**
     * True if the villager may keep this workplace - it is recorded as its owner (now, if nobody valid was).
     * False if another loaded villager already owns it.
     */
    public static boolean keep(ServerLevel level, Villager villager, GlobalPos pos) {
        UUID recorded = OWNERS.get(pos);
        if (recorded != null && !recorded.equals(villager.getUUID())
                && level.getEntity(recorded) instanceof Villager other && other.isAlive() && lists(other, pos)) {
            return false;
        }
        OWNERS.put(pos, villager.getUUID());
        return true;
    }

    /** A manual claim wins: the villager is the owner from now on, whoever was recorded before. */
    public static void claim(Villager villager, GlobalPos pos) {
        OWNERS.put(pos, villager.getUUID());
    }

    private static boolean lists(Villager villager, GlobalPos pos) {
        return VillagerStateAccess.of(villager).getState().stations().positions().containsValue(pos);
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        OWNERS.clear();
    }
}
