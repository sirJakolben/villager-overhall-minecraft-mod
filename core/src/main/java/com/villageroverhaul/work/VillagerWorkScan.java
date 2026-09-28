package com.villageroverhaul.work;

import com.villageroverhaul.VillagerOverhaulMod;
import com.villageroverhaul.happiness.HappinessCalculator;
import com.villageroverhaul.progression.ProgressionService;
import com.villageroverhaul.section.VillagerSections;
import com.villageroverhaul.state.Happiness;
import com.villageroverhaul.state.VillagerState;
import com.villageroverhaul.state.VillagerStateAccess;
import com.villageroverhaul.station.StationClaims;
import com.villageroverhaul.station.StationFocus;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The periodic villager scan: every SCAN_PERIOD ticks per villager, staggered by entity id so villagers don't
 * all scan in the same tick. Server side only, pure polling of Vanilla brain memories and nearby entities - no
 * mixin.
 *
 * Happiness: records which elements were observed today (bed = has a home, meeting point = near its
 * bell, companions = up to two different entities from the happiness_companions tag nearby). Each
 * element is only looked up until it was satisfied today, so the companion entity search stops once
 * two companions were seen that day. Villager contact comes from the gossip hook instead, see
 * HappinessTracker. Mood: fear and Hero of the Village temporarily override happiness, see currentMood.
 *
 * Stations: each scan first lets StationClaims validate and claim the villager's stations.
 *
 * Work: a villager in its WORK activity near the station it works at (StationFocus) earns WORK_XP per scan,
 * scaled by (effective) happiness from x1 (0 %) to x3 (100 %) - at 65 % that is 23 XP per scan, ~1400 per
 * in-game day (~60 scans). Each such scan also fills the meters of the sections at that station, which
 * restocks them when full; the first scan of a new day starts the day - see RestockService. Then every section
 * does its own work (SectionLogic.onWorkScan - a passive's step), also outside work time.
 *
 * Half-way between two scans a light scan books the other half of the work points (bookWork), so the
 * meters move every 50 ticks. The state is written at most once per scan, and only if something changed.
 */
@EventBusSubscriber(modid = VillagerOverhaulMod.MODID)
public final class VillagerWorkScan {

    public static final int SCAN_PERIOD = 100;
    /** Ticks after a full scan the light work booking runs - half-way, so the meters move every 50 ticks. */
    private static final int WORK_BOOKING_OFFSET = SCAN_PERIOD / 2;
    private static final int WORK_XP = 10;
    /**
     * Vanilla's own work area: during WORK a villager strolls up to 10 blocks from its job site before it walks
     * back (VillagerGoalPackages.getWorkPackage, StrollToPoi) - all of that counts as working (2026-09-28, was 3).
     */
    private static final double WORK_RADIUS = 10.0;
    private static final double MEETING_POINT_RADIUS = 8.0;
    private static final double COMPANION_RADIUS = 8.0;
    /** Which entities count as companions (cats, golems, allays by default) - a data pack tag, editable without code. */
    private static final TagKey<EntityType<?>> HAPPINESS_COMPANIONS =
            TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(VillagerOverhaulMod.MODID, "happiness_companions"));

    private VillagerWorkScan() {
    }

    @SubscribeEvent
    static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Villager villager)
                || !(villager.level() instanceof ServerLevel level)
                || villager.isBaby()) {
            return;
        }
        int phase = (villager.tickCount + villager.getId()) % SCAN_PERIOD;
        if (phase == WORK_BOOKING_OFFSET) {
            bookWork(level, villager);
            return;
        }
        if (phase != 0) {
            return;
        }

        VillagerSections.refresh(villager);
        RestockService.releaseDue(villager);
        VillagerStateAccess access = VillagerStateAccess.of(villager);
        VillagerState state = access.getState();
        long today = DayClock.today(level);

        Happiness happiness = observeHappiness(level, villager, state.happiness(), today);
        happiness = happiness.withPercent(HappinessCalculator.percent(happiness, today)).withMood(currentMood(villager));

        VillagerState updated = state.withHappiness(happiness);
        if (state.workDay() != today) {
            updated = RestockService.startDay(villager, updated, today);
        }
        updated = StationClaims.update(level, villager, updated);
        if (isWorking(villager, StationFocus.awayStation(villager, updated))) {
            int points = RestockService.workPoints(happiness.effectivePercent());
            updated = ProgressionService.withXp(updated, workXp(happiness.effectivePercent()));
            updated = RestockService.withWork(level, villager, updated, points - points / 2);
        }
        updated = VillagerSections.onWorkScan(level, villager, updated, villager.getBrain().isActive(Activity.WORK));
        if (!updated.equals(state)) {
            access.setState(updated);
        }
    }

    /**
     * The light half-way scan (2026-09-26: meters move twice as often): books the other half of a scan's
     * work points and runs the sections' own work - nothing else, no station search or happiness lookups. A
     * full scan plus this one add up to exactly one scan's points, so a day fills the meters as before.
     * Skipped until the day's first full scan has run (it starts the day).
     */
    private static void bookWork(ServerLevel level, Villager villager) {
        VillagerStateAccess access = VillagerStateAccess.of(villager);
        VillagerState state = access.getState();
        if (state.workDay() != DayClock.today(level)) {
            return;
        }
        VillagerState updated = state;
        if (isWorking(villager, StationFocus.awayStation(villager, state))) {
            int points = RestockService.workPoints(state.happiness().effectivePercent());
            updated = RestockService.withWork(level, villager, updated, points / 2);
        }
        updated = VillagerSections.onWorkScan(level, villager, updated, villager.getBrain().isActive(Activity.WORK));
        if (!updated.equals(state)) {
            access.setState(updated);
        }
    }

    /** x1 at 0 % happiness up to x3 at 100 %; exact integers, since happiness moves in 5 % steps. */
    public static int workXp(int happinessPercent) {
        return WORK_XP * (100 + 2 * happinessPercent) / 100;
    }

    private static Happiness observeHappiness(ServerLevel level, Villager villager, Happiness happiness, long today) {
        Brain<Villager> brain = villager.getBrain();
        if (happiness.lastBedDay() != today && brain.getMemory(MemoryModuleType.HOME).isPresent()) {
            happiness = happiness.withBed(today);
        }
        if (happiness.lastMeetingPointDay() != today && isNear(villager, brain.getMemory(MemoryModuleType.MEETING_POINT), MEETING_POINT_RADIUS)) {
            happiness = happiness.withMeetingPoint(today);
        }
        if (happiness.companions().stream().filter(companion -> companion.day() == today).count() < Happiness.MAX_COMPANIONS) {
            List<Entity> nearby = level.getEntities(villager, villager.getBoundingBox().inflate(COMPANION_RADIUS),
                    entity -> entity.isAlive() && entity.is(HAPPINESS_COMPANIONS));
            nearby.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(villager)));
            for (int i = 0; i < nearby.size() && i < Happiness.MAX_COMPANIONS; i++) {
                if (!Happiness.seenToday(happiness.companions(), nearby.get(i).getUUID(), today)) {
                    happiness = happiness.withCompanion(nearby.get(i).getUUID(), today);
                }
            }
        }
        return happiness;
    }

    /**
     * Momentary mood, read from Vanilla's own signals: afraid (0 %) while the brain is in PANIC (hurt,
     * or a hostile like a zombie nearby), RAID, PRE_RAID or HIDE; delighted (100 %) while it sees a
     * player with Hero of the Village - the same check Vanilla uses for hero gifts (GiveGiftToHero).
     * Fear wins if both apply. Only overrides the current value, never the long-term happiness.
     */
    public static int currentMood(Villager villager) {
        Brain<Villager> brain = villager.getBrain();
        if (brain.isActive(Activity.PANIC) || brain.isActive(Activity.RAID) || brain.isActive(Activity.PRE_RAID) || brain.isActive(Activity.HIDE)) {
            return 0;
        }
        boolean seesHero = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER)
                .filter(player -> player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE))
                .isPresent();
        return seesHero ? 100 : Happiness.NO_MOOD;
    }

    /**
     * Same "is at work" signal every profession shares: the WORK activity of the schedule, near the
     * station it currently works at - its job site, or the away station StationFocus names.
     */
    public static boolean isWorking(Villager villager) {
        return isWorking(villager, StationFocus.awayStation(villager));
    }

    private static boolean isWorking(Villager villager, Optional<GlobalPos> awayStation) {
        Brain<Villager> brain = villager.getBrain();
        Optional<GlobalPos> workplace = awayStation.isPresent() ? awayStation : brain.getMemory(MemoryModuleType.JOB_SITE);
        return brain.isActive(Activity.WORK) && isNear(villager, workplace, WORK_RADIUS);
    }

    private static boolean isNear(Villager villager, Optional<GlobalPos> target, double radius) {
        return target.isPresent()
                && target.get().dimension() == villager.level().dimension()
                && target.get().pos().closerToCenterThan(villager.position(), radius);
    }
}
