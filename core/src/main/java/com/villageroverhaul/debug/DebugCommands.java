package com.villageroverhaul.debug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.villageroverhaul.VillagerOverhaulMod;
import com.villageroverhaul.api.SectionDefinition;
import com.villageroverhaul.api.SectionOffer;
import com.villageroverhaul.data.ModDataPackRegistries;
import com.villageroverhaul.happiness.HappinessCalculator;
import com.villageroverhaul.progression.ProgressionService;
import com.villageroverhaul.section.Sections;
import com.villageroverhaul.section.VillagerSections;
import com.villageroverhaul.state.Happiness;
import com.villageroverhaul.state.VillagerState;
import com.villageroverhaul.state.VillagerStateAccess;
import com.villageroverhaul.station.StationFocus;
import com.villageroverhaul.trade.ResolvedExchange;
import com.villageroverhaul.trade.TradeActions;
import com.villageroverhaul.work.DayClock;
import com.villageroverhaul.work.RestockService;
import com.villageroverhaul.work.VillagerWorkScan;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@EventBusSubscriber(modid = VillagerOverhaulMod.MODID)
public final class DebugCommands {

    private static final double REACH = 20.0;
    // Aiming help for /vo commands: hitbox enlargement in blocks, and the fallback cone around the view direction.
    private static final double AIM_TOLERANCE = 0.5;
    private static final double MAX_AIM_ANGLE_DEGREES = 6.0;

    private DebugCommands() {
    }

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("vo")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("state")
                        .executes(ctx -> executeState(ctx, DebugCommands::printOverview))
                        .then(Commands.literal("happiness").executes(ctx -> executeState(ctx, DebugCommands::printHappiness)))
                        .then(Commands.literal("progression").executes(ctx -> executeState(ctx, DebugCommands::printProgression)))
                        .then(Commands.literal("productivity").executes(ctx -> executeState(ctx, DebugCommands::printProductivity)))
                        .then(Commands.literal("sections").executes(ctx -> executeState(ctx, DebugCommands::printSections)))
                        .then(Commands.literal("raw").executes(ctx -> executeState(ctx, DebugCommands::printRaw))))
                .then(Commands.literal("list").executes(ctx -> listEntries(ctx.getSource())))
                .then(Commands.literal("grant_xp")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(DebugCommands::executeGrantXp)))
                .then(Commands.literal("grant_points")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(DebugCommands::executeGrantPoints)))
                .then(Commands.literal("invest")
                        .then(Commands.argument("section", IdentifierArgument.id())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                        Sections.all().stream().map(SectionDefinition::id), builder))
                                .executes(DebugCommands::executeInvest)))
                .then(Commands.literal("restock").executes(DebugCommands::executeRestock))
                .then(Commands.literal("trade")
                        .then(Commands.argument("id", IdentifierArgument.id())
                                .executes(DebugCommands::executeTrade)))
                .then(Commands.literal("action")
                        .then(Commands.argument("section", IdentifierArgument.id())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                        Sections.all().stream().map(SectionDefinition::id), builder))
                                .then(Commands.argument("slot", IntegerArgumentType.integer(0))
                                        .then(Commands.argument("action", IntegerArgumentType.integer(0))
                                                .executes(DebugCommands::executeAction))))));
    }

    private static int listEntries(CommandSourceStack source) {
        Registry<?> registry = source.registryAccess().lookupOrThrow(ModDataPackRegistries.EXCHANGE);
        for (var entry : registry.entrySet()) {
            source.sendSuccess(() -> Component.literal(entry.getKey().identifier() + " -> " + entry.getValue()), false);
        }
        int count = registry.size();
        source.sendSuccess(() -> Component.literal(count + " entries."), false);
        return count;
    }

    /** One /vo state section: prints lines about the looked-at villager. */
    @FunctionalInterface
    private interface StatePrinter {
        void print(Consumer<String> out, Villager villager, VillagerState state, long today);
    }

    private static int executeState(CommandContext<CommandSourceStack> ctx, StatePrinter printer) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        Villager villager = requireLookedAtVillager(source);
        if (villager == null) {
            return 0;
        }
        VillagerState state = VillagerStateAccess.of(villager).getState();
        long today = DayClock.today(source.getLevel());
        printer.print(line -> source.sendSuccess(() -> Component.literal(line), false), villager, state, today);
        return 1;
    }

    private static void printOverview(Consumer<String> out, Villager villager, VillagerState state, long today) {
        out.accept("== " + villager.getName().getString() + " (" + villager.getVillagerData().profession().getRegisteredName() + ") ==");
        out.accept("Level " + state.level() + "/" + ProgressionService.maxLevel(villager) + ", XP " + state.xp() + "/" + ProgressionService.xpToNextLevel(state.level())
                + ", unspent points " + state.unspentUpgradePoints());
        out.accept("Happiness " + state.happiness().effectivePercent() + "% (long-term " + state.happiness().percent() + "%)"
                + ", working now: " + VillagerWorkScan.isWorking(villager));
        out.accept("Ranks: " + state.ranks());
        out.accept("Details: /vo state happiness | progression | productivity | sections | raw");
    }

    private static void printHappiness(Consumer<String> out, Villager villager, VillagerState state, long today) {
        Happiness happiness = state.happiness();
        String mood = switch (happiness.mood()) {
            case 0 -> "afraid (0%)";
            case 100 -> "sees Hero of the Village (100%)";
            default -> "none";
        };
        out.accept("== Happiness: " + happiness.effectivePercent() + "% now, " + happiness.percent() + "% long-term, today = day " + today + " ==");
        out.accept("Mood override: " + mood + " (live check: " + VillagerWorkScan.currentMood(villager) + ")");
        for (HappinessCalculator.Part part : HappinessCalculator.breakdown(happiness, today)) {
            String seen = part.lastDay() <= Happiness.NEVER ? "never" : (today - part.lastDay()) + " day(s) ago";
            out.accept("- " + part.label() + ": " + part.value() + "/" + part.weight() + "%, last seen " + seen + ", timer " + part.timerDays() + " days");
        }
    }

    private static void printProgression(Consumer<String> out, Villager villager, VillagerState state, long today) {
        out.accept("== Progression ==");
        out.accept("Level " + state.level() + "/" + ProgressionService.maxLevel(villager) + " (title tier " + ProgressionService.merchantTier(state.level()) + ")");
        out.accept("XP " + state.xp() + "/" + ProgressionService.xpToNextLevel(state.level()) + " to next level");
        out.accept("Unspent points: " + state.unspentUpgradePoints());
        for (SectionDefinition section : VillagerSections.of(villager)) {
            int cost = ProgressionService.upgradeCost(villager, state, section);
            out.accept("- " + section.id() + ": rank " + state.rank(section.id())
                    + (cost < 0 ? ", maxed" : ", next rank costs " + cost)
                    + (VillagerSections.isOpen(state, section) ? "" : " (closed: station not owned, no rank)"));
        }
        out.accept("Stations: " + state.stations().positions().entrySet().stream()
                .map(station -> station.getKey() + " " + station.getValue().pos().toShortString()).toList()
                + ", profession held by station: " + state.stations().heldByStation()
                + ", working at: " + StationFocus.awayStation(villager, state).map(p -> p.pos().toShortString()).orElse("job site"));
        out.accept("Profession locked (Vanilla trade XP > 0): " + (villager.getVillagerXp() > 0));
    }

    private static void printProductivity(Consumer<String> out, Villager villager, VillagerState state, long today) {
        int happiness = state.happiness().effectivePercent();
        out.accept("== Productivity ==");
        out.accept("Working now: " + VillagerWorkScan.isWorking(villager) + ", work XP per check at " + happiness + "% happiness: "
                + VillagerWorkScan.workXp(happiness));
        out.accept("Work points per check at " + happiness + "% happiness: " + RestockService.workPoints(happiness));
        Optional<Identifier> focus = state.productivity().focus();
        for (SectionDefinition section : VillagerSections.workable(villager, state)) {
            out.accept("- " + section.id() + (focus.equals(Optional.of(section.id())) ? " (focus)" : "")
                    + ": station " + section.station().orElseThrow()
                    + ", meter " + state.productivity().meter(section.id()) + " / " + RestockService.meterPoints(villager, state, section)
                    + ", missing stock " + section.logic().missingStock(villager, state, section));
        }
        out.accept("Works at: " + StationFocus.workStation(villager, state).map(p -> p.pos().toShortString()).orElse("nowhere")
                + (focus.isEmpty() ? " (morning, no focus chosen yet)" : ""));
        out.accept("Work day started: " + state.workDay() + " (today: " + today + ")");
    }

    private static void printSections(Consumer<String> out, Villager villager, VillagerState state, long today) {
        for (SectionDefinition section : VillagerSections.open(villager, state)) {
            List<SectionOffer> offers = section.logic().offers(villager, state, section);
            out.accept("== " + section.id() + ": rank " + state.rank(section.id()) + ", " + offers.size() + " offer(s) ==");
            section.logic().describe(villager, state, section).forEach(out);
            for (SectionOffer offer : offers) {
                ResolvedExchange exchange = offer.exchange();
                out.accept("- " + exchange.id() + (offer.slot() == SectionOffer.NO_SLOT ? "" : " (slot " + offer.slot() + ")") + ": "
                        + exchange.input().count() + "x " + BuiltInRegistries.ITEM.getKey(exchange.input().item())
                        + exchange.secondInput().map(second -> " + " + second.count() + "x " + BuiltInRegistries.ITEM.getKey(second.item())).orElse("")
                        + " -> " + exchange.output().count() + "x " + BuiltInRegistries.ITEM.getKey(exchange.output().item())
                        + ", stock " + exchange.usesRemaining() + "/" + exchange.maxUses());
            }
        }
    }

    private static void printRaw(Consumer<String> out, Villager villager, VillagerState state, long today) {
        out.accept(state.toString());
    }

    /** Adds unspent points directly, past the 5-point cap that XP respects - for testing upgrades quickly. */
    private static int executeGrantPoints(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        Villager villager = requireLookedAtVillager(source);
        if (villager == null) {
            return 0;
        }

        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        ProgressionService.grantPoints(VillagerStateAccess.of(villager), amount);
        int unspent = VillagerStateAccess.of(villager).getState().unspentUpgradePoints();
        source.sendSuccess(() -> Component.literal("Granted " + amount + " point(s). Now " + unspent + " unspent point(s)."), false);
        return 1;
    }

    private static int executeGrantXp(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        Villager villager = requireLookedAtVillager(source);
        if (villager == null) {
            return 0;
        }

        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        ProgressionService.grantXp(villager, amount);
        VillagerState state = VillagerStateAccess.of(villager).getState();
        source.sendSuccess(() -> Component.literal("Granted " + amount + " XP. Now level "
                + state.level() + ", " + state.unspentUpgradePoints() + " unspent point(s)."), false);
        return 1;
    }

    private static int executeInvest(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        Villager villager = requireLookedAtVillager(source);
        if (villager == null) {
            return 0;
        }

        Identifier section = IdentifierArgument.getId(ctx, "section");
        ProgressionService.InvestResult result = ProgressionService.investPoint(villager, section);
        if (result != ProgressionService.InvestResult.UPGRADED) {
            source.sendFailure(Component.literal("Not upgraded (" + section + "): " + result));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Upgraded " + section + " to rank " + VillagerStateAccess.of(villager).getState().rank(section) + "."), false);
        return 1;
    }

    private static int executeRestock(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        Villager villager = requireLookedAtVillager(source);
        if (villager == null) {
            return 0;
        }

        RestockService.restock(villager);
        source.sendSuccess(() -> Component.literal("Restocked."), false);
        return 1;
    }

    /** Executes an offer by its entry id - a trade or a quest, whatever section it is in. */
    private static int executeTrade(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        Villager villager = requireLookedAtVillager(source);
        if (villager == null) {
            return 0;
        }

        Identifier entryId = IdentifierArgument.getId(ctx, "id");
        TradeActions.Result result = TradeActions.execute(villager, player, entryId);
        source.sendSuccess(() -> Component.literal("Trade result: " + result), false);
        return result == TradeActions.Result.SUCCESS ? 1 : 0;
    }

    /** A row action as its button would send it (SectionLogic.onAction) - e.g. the Trade Rework's quest reroll. */
    private static int executeAction(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        Villager villager = requireLookedAtVillager(source);
        if (villager == null) {
            return 0;
        }

        Identifier sectionId = IdentifierArgument.getId(ctx, "section");
        Optional<SectionDefinition> section = Sections.get(sectionId).filter(VillagerSections.of(villager)::contains);
        if (section.isEmpty()) {
            source.sendFailure(Component.literal("The villager has no section " + sectionId + "."));
            return 0;
        }
        section.get().logic().onAction(villager, player, section.get(),
                IntegerArgumentType.getInteger(ctx, "slot"), IntegerArgumentType.getInteger(ctx, "action"));
        source.sendSuccess(() -> Component.literal("Action sent to " + sectionId + "."), false);
        return 1;
    }

    private static Villager requireLookedAtVillager(CommandSourceStack source) throws CommandSyntaxException {
        Villager villager = findLookedAtVillager(source.getPlayerOrException());
        if (villager == null) {
            source.sendFailure(Component.literal("No villager in view."));
        }
        return villager;
    }

    /**
     * The villager the player is aiming at, forgiving enough for wandering villagers:
     * 1. the view ray, stopped only by solid blocks (collision shapes - grass, flowers etc. don't block),
     *    against villager hitboxes enlarged by AIM_TOLERANCE;
     * 2. if that misses, the visible villager closest to the view direction within MAX_AIM_ANGLE_DEGREES.
     */
    private static Villager findLookedAtVillager(ServerPlayer player) {
        Vec3 from = player.getEyePosition();
        Vec3 view = player.getViewVector(1.0F);
        Vec3 to = from.add(view.scale(REACH));
        HitResult blockHit = player.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double maxDistance = blockHit.getLocation().distanceTo(from);

        List<Villager> candidates = player.level().getEntitiesOfClass(Villager.class,
                player.getBoundingBox().expandTowards(view.scale(REACH)).inflate(2.0), Villager::isAlive);

        Villager best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Villager villager : candidates) {
            Optional<Vec3> clip = villager.getBoundingBox().inflate(AIM_TOLERANCE).clip(from, to);
            if (clip.isPresent()) {
                double distance = clip.get().distanceTo(from);
                if (distance <= maxDistance + AIM_TOLERANCE && distance < bestDistance) {
                    best = villager;
                    bestDistance = distance;
                }
            }
        }
        if (best != null) {
            return best;
        }

        double bestAngle = MAX_AIM_ANGLE_DEGREES;
        for (Villager villager : candidates) {
            Vec3 toVillager = villager.getBoundingBox().getCenter().subtract(from);
            if (toVillager.length() > REACH || !player.hasLineOfSight(villager)) {
                continue;
            }
            double angle = Math.toDegrees(Math.acos(Math.clamp(view.dot(toVillager.normalize()), -1.0, 1.0)));
            if (angle < bestAngle) {
                best = villager;
                bestAngle = angle;
            }
        }
        return best;
    }
}
