package com.villageroverhaul.core;

import com.villageroverhaul.core.state.VillagerState;
import net.minecraft.world.entity.npc.villager.Villager;

public interface VillagerStateAccess {

    VillagerState getState();

    /** Replaces the whole state and flags it for persistence and client sync. */
    void setState(VillagerState newState);

    static VillagerStateAccess of(Villager villager) {
        return new AttachmentVillagerStateAccess(villager);
    }
}
