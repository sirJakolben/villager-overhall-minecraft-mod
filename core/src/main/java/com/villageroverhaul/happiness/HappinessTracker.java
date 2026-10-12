package com.villageroverhaul.happiness;

import com.villageroverhaul.state.Happiness;
import com.villageroverhaul.state.VillagerState;
import com.villageroverhaul.state.VillagerStateAccess;
import com.villageroverhaul.work.DayClock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;

/**
 * Villager-to-villager contact for happiness, fed by VillagerGossipMixin: whenever two villagers
 * actually exchange gossip (Vanilla's own "met and talked" moment, after its once-a-minute cooldown),
 * each records the other as a contact for today.
 */
public final class HappinessTracker {

    private HappinessTracker() {
    }

    public static void recordContact(ServerLevel level, Villager villager, Villager other) {
        VillagerStateAccess access = VillagerStateAccess.of(villager);
        VillagerState state = access.getState();
        long today = DayClock.today(level);
        Happiness happiness = state.happiness().withContact(other.getUUID(), today);
        happiness = happiness.withPercent(HappinessCalculator.percent(happiness, today));
        if (!happiness.equals(state.happiness())) {
            access.setState(state.withHappiness(happiness));
        }
    }
}
