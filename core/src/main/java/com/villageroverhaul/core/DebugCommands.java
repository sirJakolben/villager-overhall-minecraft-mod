package com.villageroverhaul.core;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.villageroverhaul.VillagerOverhaulMod;
import com.villageroverhaul.core.state.Happiness;
import com.villageroverhaul.core.state.StationSlot;
import com.villageroverhaul.core.state.VillagerState;
import com.villageroverhaul.data.ItemExchange;
import com.villageroverhaul.data.ModDataPackRegistries;
import com.villageroverhaul.freedom.HappinessCalculator;
import com.villageroverhaul.freedom.VillagerWorkScan;
import com.villageroverhaul.claim.StationFocus;
import com.villageroverhaul.progression.ProgressionService;
import com.villageroverhaul.progression.UpgradeGroup;
import com.villageroverhaul.quest.QuestActions;
import com.villageroverhaul.quest.QuestProviderImpl;
import com.villageroverhaul.quest.QuestSlots;
import com.villageroverhaul.trade.RestockService;
import com.villageroverhaul.trade.TradeActions;
import com.villageroverhaul.trade.TradeProviderImpl;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
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
import java.util.Map;
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
                        .then(Commands.literal("trades").executes(ctx -> executeState(ctx, DebugCommands::printTrades)))
                        .then(Commands.literal("quests").executes(ctx -> executeState(ctx, DebugCommands::printQuests)))
                        .then(Commands.literal("raw").executes(ctx -> executeState(ctx, DebugCommands::printRaw))))
                .then(Commands.literal("list")
                        .then(Commands.literal("trade").executes(ctx -> listRegistry(ctx.getSource(), ModDataPackRegistries.TRADE)))
                        .then(Commands.literal("quest").executes(ctx -> listRegistry(ctx.getSource(), ModDataPackRegistries.QUEST)))
                        .then(Commands.literal("passive").executes(ctx -> listRegistry(ctx.getSource(), ModDataPackRegistries.PASSIVE))))
                .then(Commands.literal("grant_xp")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(DebugCommands::executeGrantXp)))
                .then(Commands.literal("grant_points")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(DebugCommands::executeGrantPoints)))
                .then(Commands.literal("invest")
                        .then(Commands.literal("quest").executes(ctx -> executeInvest(ctx, UpgradeGroup.QUEST)))
                        .then(Commands.literal("basic_trade").executes(ctx -> executeInvest(ctx, UpgradeGroup.BASIC_TRADE)))
                        .then(Commands.literal("master_trade").executes(ctx -> executeInvest(ctx, UpgradeGroup.MASTER_TRADE)))
                        .then(Commands.literal("passive").executes(ctx -> executeInvest(ctx, UpgradeGroup.PASSIVE))))
                .then(Commands.literal("restock").executes(DebugCommands::executeRestock))
                .then(Commands.literal("trade")
                        .then(Commands.argument("id", IdentifierArgument.id())
                                .executes(DebugCommands::executeTrade)))
                .then(Commands.literal("quest_complete")
                        .then(Commands.argument("slot", IntegerArgumentType.integer(0))
                                .executes(DebugCommands::executeQuestComplete)))
                .then(Commands.literal("quest_reroll")
                        .then(Commands.argument("slot", IntegerArgumentType.integer(0))
                                .executes(DebugCommands::executeQuestReroll))));
    }

    private static <T> int listRegistry(CommandSourceStack source, ResourceKey<Registry<T>> registryKey) {
        Registry<T> registry = source.registryAccess().lookupOrThrow(registryKey);
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
        out.accept("Level " + state.level() + "/" + ProgressionService.MAX_LEVEL + ", XP " + state.xp() + "/" + ProgressionService.xpToNextLevel(state.level())
                + ", unspent points " + state.unspentUpgradePoints());
        out.accept("Happiness " + state.happiness().effectivePercent() + "% (long-term " + state.happiness().percent() + "%)"
                + ", working now: " + VillagerWorkScan.isWorking(villager));
        out.accept("Ranks: quest " + state.ranks().quest() + ", basic " + state.ranks().basicTrade()
                + ", master " + state.ranks().masterTrade() + ", passive " + state.ranks().passive());
        out.accept("Details: /vo state happiness | progression | productivity | trades | quests | raw");
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
        out.accept("Level " + state.level() + "/" + ProgressionService.MAX_LEVEL + " (title tier " + ProgressionService.merchantTier(state.level()) + ")");
        out.accept("XP " + state.xp() + "/" + ProgressionService.xpToNextLevel(state.level()) + " to next level, "
                + ProgressionService.xpBetweenLevels(state.level(), ProgressionService.MAX_LEVEL) + " to max level");
        out.accept("Unspent points: " + state.unspentUpgradePoints());
        for (UpgradeGroup group : UpgradeGroup.values()) {
            int rank = ProgressionService.rankOf(state.ranks(), group);
            int cost = group.upgradeCost(rank);
            out.accept("- " + group + ": rank " + rank + "/" + group.maxRank()
                    + (cost < 0 ? ", maxed" : ", next upgrade costs " + cost)
                    + (ProgressionService.isGroupOpen(state, group) ? "" : " (hidden: no station claimed, no rank)"));
        }
        out.accept("Stations: basic " + state.stations().basic().map(p -> p.pos().toShortString()).orElse("none")
                + ", master " + state.stations().master().map(p -> p.pos().toShortString()).orElse("none")
                + ", passive " + state.stations().passive().map(p -> p.pos().toShortString()).orElse("none")
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
        Map<ItemExchange.Tier, Integer> gaps = RestockService.gaps(villager, state);
        Optional<StationSlot> focus = state.dailyProductivity().focusSlot();
        for (StationSlot slot : StationSlot.values()) {
            Integer gap = slot.tier() == null ? null : gaps.get(slot.tier());
            out.accept("- " + slot.name().toLowerCase() + (focus.equals(Optional.of(slot)) ? " (focus)" : "")
                    + ": workplace " + state.stations().get(slot).map(p -> p.pos().toShortString()).orElse("none")
                    + ", meter " + state.dailyProductivity().of(slot) + " / " + RestockService.meterPoints(villager, slot, state)
                    + (gap == null ? (slot.tier() == null ? "" : ", no unlocked trades") : ", largest gap " + gap));
        }
        out.accept("Works at: " + StationFocus.workStation(villager, state).map(p -> p.pos().toShortString()).orElse("nowhere")
                + (state.dailyProductivity().focusSlot().isEmpty() ? " (morning, no focus chosen yet)" : ""));
        out.accept("Focus picked on day: " + state.lastProcessedDay() + " (today: " + today + ")");
    }

    private static void printTrades(Consumer<String> out, Villager villager, VillagerState state, long today) {
        List<ResolvedExchange> trades = new TradeProviderImpl().getAvailableTrades(villager);
        out.accept("== Unlocked trades: " + trades.size() + " ==");
        for (ResolvedExchange trade : trades) {
            out.accept("- " + trade.id() + " [" + trade.tier() + "]: " + trade.input().count() + "x " + BuiltInRegistries.ITEM.getKey(trade.input().item())
                    + trade.secondInput().map(second -> " + " + second.count() + "x " + BuiltInRegistries.ITEM.getKey(second.item())).orElse("")
                    + " -> " + trade.output().count() + "x " + BuiltInRegistries.ITEM.getKey(trade.output().item())
                    + ", stock " + trade.usesRemaining() + "/" + trade.maxUses()
                    + (state.tradeUsesRemaining().containsKey(trade.id()) ? "" : " (initial stock)"));
        }
    }

    private static void printQuests(Consumer<String> out, Villager villager, VillagerState state, long today) {
        List<QuestOffer> offers = new QuestProviderImpl().getCurrentOffers(villager);
        out.accept("== Active quests: " + offers.size() + " ==");
        int doneToday = state.questLog().day() == today ? state.questLog().completedToday() : 0;
        out.accept("Completed today: " + doneToday + "/" + QuestSlots.dailyLimit(villager, state.ranks().quest())
                + (QuestActions.isLimitReached(villager, state) ? " (limit reached, all quests wait until tomorrow)" : "")
                + ", slots pausing: " + state.questLog().hiddenSlots().size());
        for (QuestOffer offer : offers) {
            ResolvedExchange exchange = offer.exchange();
            long cooldown = QuestActions.rerollCooldownTicksRemaining(state, villager, offer.slot());
            out.accept("- slot " + offer.slot() + ": " + exchange.id() + ", " + exchange.input().count() + "x " + BuiltInRegistries.ITEM.getKey(exchange.input().item())
                    + " -> " + exchange.output().count() + "x " + BuiltInRegistries.ITEM.getKey(exchange.output().item())
                    + ", reroll " + (cooldown <= 0 ? "ready" : "in " + cooldown + " ticks"));
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

    private static int executeInvest(CommandContext<CommandSourceStack> ctx, UpgradeGroup group) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        Villager villager = requireLookedAtVillager(source);
        if (villager == null) {
            return 0;
        }

        VillagerState before = VillagerStateAccess.of(villager).getState();
        int cost = group.upgradeCost(ProgressionService.rankOf(before.ranks(), group));
        boolean invested = ProgressionService.investPoint(villager, group);
        if (!invested) {
            source.sendFailure(Component.literal(cost < 0
                    ? group + " is already at max rank."
                    : !ProgressionService.isGroupOpen(before, group)
                    ? group + " is hidden: the villager owns no station for it and has no rank in it."
                    : "Not enough points: next " + group + " upgrade costs " + cost + ", villager has " + before.unspentUpgradePoints() + "."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Upgraded " + group + " for " + cost + " point(s)."), false);
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

    private static int executeTrade(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        Villager villager = requireLookedAtVillager(source);
        if (villager == null) {
            return 0;
        }

        Identifier tradeId = IdentifierArgument.getId(ctx, "id");
        TradeActions.Result result = TradeActions.executeTrade(villager, player, tradeId);
        source.sendSuccess(() -> Component.literal("Trade result: " + result), false);
        return result == TradeActions.Result.SUCCESS ? 1 : 0;
    }

    private static int executeQuestComplete(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        Villager villager = requireLookedAtVillager(source);
        if (villager == null) {
            return 0;
        }

        int slot = IntegerArgumentType.getInteger(ctx, "slot");
        QuestActions.Result result = QuestActions.completeOffer(villager, player, slot);
        source.sendSuccess(() -> Component.literal("Quest result: " + result), false);
        return result == QuestActions.Result.SUCCESS ? 1 : 0;
    }

    private static int executeQuestReroll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        Villager villager = requireLookedAtVillager(source);
        if (villager == null) {
            return 0;
        }

        int slot = IntegerArgumentType.getInteger(ctx, "slot");
        QuestActions.Result result = QuestActions.reroll(villager, slot);
        source.sendSuccess(() -> Component.literal("Reroll result: " + result), false);
        return result == QuestActions.Result.SUCCESS ? 1 : 0;
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
