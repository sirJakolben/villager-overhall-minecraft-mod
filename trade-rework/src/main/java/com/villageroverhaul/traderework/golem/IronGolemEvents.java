package com.villageroverhaul.traderework.golem;

import com.villageroverhaul.traderework.TradeReworkMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Gives every Iron Golem joining the world the night patrol (PatrolGoal) - a goal added next to Vanilla's, no mixin. */
@EventBusSubscriber(modid = TradeReworkMod.MODID)
public final class IronGolemEvents {

    /** Between Vanilla's attack goals (1-2) and its strolling (4). */
    private static final int PRIORITY = 3;

    private IronGolemEvents() {
    }

    @SubscribeEvent
    static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel && event.getEntity() instanceof IronGolem golem
                && golem.goalSelector.getAvailableGoals().stream().noneMatch(wrapped -> wrapped.getGoal() instanceof PatrolGoal)) {
            golem.goalSelector.addGoal(PRIORITY, new PatrolGoal(golem));
        }
    }
}
