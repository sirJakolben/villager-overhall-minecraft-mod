package com.villageroverhaul.core;

import com.villageroverhaul.core.state.Happiness;
import net.minecraft.world.entity.npc.villager.Villager;

public interface HappinessProvider {

    Happiness getHappiness(Villager villager);
}
