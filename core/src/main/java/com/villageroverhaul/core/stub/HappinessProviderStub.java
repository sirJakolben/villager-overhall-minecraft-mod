package com.villageroverhaul.core.stub;

import com.villageroverhaul.core.HappinessProvider;
import com.villageroverhaul.core.state.Happiness;
import net.minecraft.world.entity.npc.villager.Villager;

public class HappinessProviderStub implements HappinessProvider {

    @Override
    public Happiness getHappiness(Villager villager) {
        return Happiness.EMPTY;
    }
}
