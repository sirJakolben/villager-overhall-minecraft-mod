package com.villageroverhaul.core;

import net.minecraft.world.entity.npc.villager.Villager;

import java.util.List;

public interface QuestProvider {

    /**
     * The villager's currently active quest offers, one per unlocked slot (1-3, see QuestSlots),
     * each derived from villager UUID, slot index, and that slot's rotation counter.
     */
    List<QuestOffer> getCurrentOffers(Villager villager);
}
