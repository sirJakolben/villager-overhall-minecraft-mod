package com.smartvillager.professions.passive;

import com.smartvillager.progress.state.VillagerState;
import com.smartvillager.progress.state.VillagerStateAccess;
import com.smartvillager.professions.SmartVillagerProfessionsMod;
import com.smartvillager.professions.ProfessionsSections;
import com.smartvillager.progress.work.RestockService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Comparator;

/**
 * Debug: /vo passive fill fills the passive meter of the villager nearest to the player (within 8 blocks) at once, so the next work scan spends it on a step
 * without waiting for the work days. A carrying passive (Farmer, Fisherman) keeps its bar as its load - for those use /vo counters give.
 */
@EventBusSubscriber(modid = SmartVillagerProfessionsMod.MODID)
public final class PassiveFillCommand {

    private static final double REACH = 8.0;

    private PassiveFillCommand() {
    }

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("vo")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("passive")
                        .then(Commands.literal("fill").executes(ctx -> fill(ctx.getSource())))));
    }

    private static int fill(CommandSourceStack source) {
        Villager villager = source.getLevel().getEntitiesOfClass(Villager.class, AABB.ofSize(source.getPosition(), 2 * REACH, 2 * REACH, 2 * REACH), Villager::isAlive).stream()
                .min(Comparator.comparingDouble(v -> v.distanceToSqr(source.getPosition()))).orElse(null);
        if (villager == null) {
            source.sendFailure(Component.literal("No villager within " + (int) REACH + " blocks."));
            return 0;
        }
        VillagerStateAccess access = VillagerStateAccess.of(villager);
        VillagerState state = access.getState();
        int full = RestockService.meterPoints(villager, state, ProfessionsSections.PASSIVE);
        access.setState(state.withProductivity(state.productivity().withMeter(ProfessionsSections.PASSIVE.id(), full)));
        source.sendSuccess(() -> Component.literal("Passive meter filled (" + full + " points)."), false);
        return 1;
    }
}
