package com.smartvillager.professions.passive;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.smartvillager.professions.SmartVillagerProfessionsMod;
import com.smartvillager.progress.state.VillagerStateAccess;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Comparator;
import java.util.List;

/**
 * Debug for the basket passives (2026-09-30), on the villager nearest to the player (within 8 blocks):
 * /vo counters shows the Farmer's or Fisherman's counters (points and caught fish per sort, catch rhythm, known water);
 * /vo counters give &lt;units&gt; puts that many units on his load (crops for the Farmer, catches for the Fisherman; beyond the capacity it still counts - the bar caps) - to test the
 * emptying at the basket without waiting for a harvest day.
 */
@EventBusSubscriber(modid = SmartVillagerProfessionsMod.MODID)
public final class CountersCommand {

    private static final double REACH = 8.0;

    private CountersCommand() {
    }

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("vo")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("counters")
                        .executes(ctx -> show(ctx.getSource()))
                        .then(Commands.literal("give")
                                .then(Commands.argument("units", IntegerArgumentType.integer(1, 20)).executes(CountersCommand::give)))));
    }

    private static Villager nearest(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        List<Villager> villagers = level.getEntitiesOfClass(Villager.class, net.minecraft.world.phys.AABB.ofSize(source.getPosition(), 2 * REACH, 2 * REACH, 2 * REACH), Villager::isAlive);
        return villagers.stream().min(Comparator.comparingDouble(v -> v.distanceToSqr(source.getPosition()))).orElse(null);
    }

    private static int show(CommandSourceStack source) {
        Villager villager = nearest(source);
        if (villager == null) {
            source.sendFailure(Component.literal("No villager within " + (int) REACH + " blocks."));
            return 0;
        }
        PassiveCounters counters = PassiveCounters.of(villager);
        source.sendSuccess(() -> Component.literal("Load of " + villager.getVillagerData().profession().getRegisteredName() + ": " + counters.carried() + " (" + counters.total()
                + "), returning " + counters.returning() + ", water " + counters.water().map(pos -> pos.toShortString()).orElse("none") + ", shore " + counters.shore().map(pos -> pos.toShortString()).orElse("none")
                + ", casting " + counters.casting() + ", next step in " + (counters.nextStepTick() - villager.level().getGameTime()) + " ticks, last look " + (villager.level().getGameTime() - counters.waterCheckedAt())  + ", owns basket " + PassiveLogic.station(VillagerStateAccess.of(villager).getState()).isPresent() + ", focus " + VillagerStateAccess.of(villager).getState().productivity().focus().map(Object::toString).orElse("none") + ", at basket " + PassiveLogic.worksAtStation(villager, VillagerStateAccess.of(villager).getState()) + ", work activity " + villager.getBrain().isActive(net.minecraft.world.entity.schedule.Activity.WORK)), false);
        return 1;
    }

    private static int give(CommandContext<CommandSourceStack> ctx) {
        Villager villager = nearest(ctx.getSource());
        if (villager == null) {
            ctx.getSource().sendFailure(Component.literal("No villager within " + (int) REACH + " blocks."));
            return 0;
        }
        int units = IntegerArgumentType.getInteger(ctx, "units");
        PassiveCounters counters = PassiveCounters.of(villager);
        long now = villager.level().getGameTime();
        if (villager.getVillagerData().profession().is(VillagerProfession.FARMER)) {
            for (int i = 0; i < units; i++) {
                counters = counters.withGained(Identifier.withDefaultNamespace(List.of("wheat", "carrots", "potatoes", "beetroots").get(i % 4)), now);
            }
        } else if (villager.getVillagerData().profession().is(VillagerProfession.FISHERMAN)) {
            for (int i = 0; i < units; i++) {
                counters = counters.withGained(PassiveCounters.CATCH, now);
            }
        } else {
            ctx.getSource().sendFailure(Component.literal("Not a Farmer or Fisherman."));
            return 0;
        }
        PassiveCounters.set(villager, counters);
        PassiveLogic.syncLoadMeter(villager);
        ctx.getSource().sendSuccess(() -> Component.literal("Gave " + units + " units."), false);
        return 1;
    }
}
