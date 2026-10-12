package com.smartvillager.professions.farmer;

import com.smartvillager.professions.SmartVillagerProfessionsMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Gives every Zombie (also Husk, Drowned, Zombie Villager; not the Zombified Piglin) the Scarecrow goal - added next to the Vanilla ones, no mixin. */
@EventBusSubscriber(modid = SmartVillagerProfessionsMod.MODID)
public final class ScarecrowEvents {

    /** Above the Vanilla attack goal (2), so it can win against a villager target. */
    private static final int PRIORITY = 1;

    private ScarecrowEvents() {
    }

    @SubscribeEvent
    static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel && event.getEntity() instanceof Zombie zombie && !(zombie instanceof ZombifiedPiglin)
                && zombie.goalSelector.getAvailableGoals().stream().noneMatch(wrapped -> wrapped.getGoal() instanceof ScarecrowGoal)) {
            zombie.goalSelector.addGoal(PRIORITY, new ScarecrowGoal(zombie));
        }
    }
}
