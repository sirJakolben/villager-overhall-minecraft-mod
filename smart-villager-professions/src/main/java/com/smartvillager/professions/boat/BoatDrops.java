package com.smartvillager.professions.boat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.ItemStack;

/** A destroyed boat gives back everything built into it (Obsidian Fisherman.md, 2026-10-12): each part as its item, the mast with its banner, the chests' contents. */
public final class BoatDrops {

    private BoatDrops() {
    }

    public static void dropAll(AbstractBoat boat, ServerLevel level) {
        BoatState state = BoatUpgrades.state(boat);
        for (var id : state.slots()) {
            BoatParts.get(id).ifPresent(type -> boat.spawnAtLocation(level, new ItemStack(type.item().get())));
        }
        state.deco().ifPresent(deco -> {
            if (!deco.stack().isEmpty()) {
                boat.spawnAtLocation(level, deco.stack().copy());
            }
        });
        for (ItemStack stack : BoatInventory.stacks(boat)) {
            boat.spawnAtLocation(level, stack.copy());
        }
    }
}
