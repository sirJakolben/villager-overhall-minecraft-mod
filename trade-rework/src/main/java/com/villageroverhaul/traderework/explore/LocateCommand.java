package com.villageroverhaul.traderework.explore;

import com.villageroverhaul.data.ExplorerMap;
import com.mojang.brigadier.context.CommandContext;
import com.villageroverhaul.traderework.TradeReworkMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Debug check for the seed searches (2026-09-27): /vo locate geode | copper_vein | iron_vein | dungeon predicts the
 * nearest spot from where the command runs - exactly what the map trades do - then loads that chunk and
 * counts the blocks that should be there (amethyst / budding amethyst, copper or iron ore and raw blocks)
 * within CHECK_RADIUS. A count of 0 means the prediction missed.
 */
@EventBusSubscriber(modid = TradeReworkMod.MODID)
public final class LocateCommand {

    private static final int RADIUS_CHUNKS = 32;
    private static final int CHECK_RADIUS = 8;

    private LocateCommand() {
    }

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("vo")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("locate")
                        .then(Commands.literal("geode").executes(ctx -> locate(ctx, "geode")))
                        .then(Commands.literal("copper_vein").executes(ctx -> locate(ctx, "copper_vein")))
                        .then(Commands.literal("iron_vein").executes(ctx -> locate(ctx, "iron_vein")))
                        .then(Commands.literal("dungeon").executes(ctx -> locate(ctx, "dungeon")))));
    }

    private static ResourceKey<PlacedFeature> placed(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, Identifier.withDefaultNamespace(name));
    }

    private static int locate(CommandContext<CommandSourceStack> ctx, String what) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos origin = BlockPos.containing(ctx.getSource().getPosition());
        Optional<BlockPos> found;
        Set<Block> expected;
        switch (what) {
            case "geode" -> {
                found = FeatureLocator.findNearest(level, List.of(placed("amethyst_geode")), origin, RADIUS_CHUNKS, Optional.empty());
                expected = Set.of(Blocks.AMETHYST_BLOCK, Blocks.BUDDING_AMETHYST, Blocks.CALCITE);
            }
            case "dungeon" -> {
                found = FeatureLocator.findNearest(level, List.of(placed("monster_room"), placed("monster_room_deep")), origin, RADIUS_CHUNKS,
                        Optional.of(Blocks.SPAWNER));
                expected = Set.of(Blocks.SPAWNER, Blocks.MOSSY_COBBLESTONE);
            }
            case "copper_vein" -> {
                found = OreVeinLocator.findNearest(level, ExplorerMap.VeinType.COPPER, origin, RADIUS_CHUNKS);
                expected = Set.of(Blocks.COPPER_ORE, Blocks.RAW_COPPER_BLOCK);
            }
            default -> {
                found = OreVeinLocator.findNearest(level, ExplorerMap.VeinType.IRON, origin, RADIUS_CHUNKS);
                expected = Set.of(Blocks.DEEPSLATE_IRON_ORE, Blocks.RAW_IRON_BLOCK);
            }
        }
        if (found.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("[VO] locate " + what + ": nothing within " + RADIUS_CHUNKS + " chunks"));
            return 0;
        }
        BlockPos pos = found.get();
        int count = 0;
        for (BlockPos check : BlockPos.betweenClosed(pos.offset(-CHECK_RADIUS, -CHECK_RADIUS, -CHECK_RADIUS), pos.offset(CHECK_RADIUS, CHECK_RADIUS, CHECK_RADIUS))) {
            if (expected.contains(level.getBlockState(check).getBlock())) {
                count++;
            }
        }
        int matching = count;
        ctx.getSource().sendSuccess(() -> Component.literal("[VO] locate " + what + ": " + pos.toShortString()
                + ", distance " + (int) Math.sqrt(pos.distSqr(origin)) + ", matching blocks within " + CHECK_RADIUS + ": " + matching), false);
        TradeReworkMod.LOGGER.info("[VO] locate {} from {}: {} -> {} matching blocks", what, origin.toShortString(), pos.toShortString(), matching);
        return matching;
    }
}
