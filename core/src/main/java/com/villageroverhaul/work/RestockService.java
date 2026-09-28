package com.villageroverhaul.work;

import com.villageroverhaul.api.SectionDefinition;
import com.villageroverhaul.api.SectionOffer;
import com.villageroverhaul.section.StandardSection;
import com.villageroverhaul.section.VillagerSections;
import com.villageroverhaul.state.Productivity;
import com.villageroverhaul.state.VillagerState;
import com.villageroverhaul.state.VillagerStateAccess;
import com.villageroverhaul.station.StationFocus;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Work meters and restock (README.md, all numbers: Obsidian Tweak-Werte.md). Every section has its own meter
 * (Productivity); what a full meter does is its logic's business - the standard one restocks:
 *
 * - Every work check earns work points: HAPPINESS_FLOOR at 0 % happiness up to 100 at 100 %.
 * - They go into the meters of every workable section at the station the villager works at - sections sharing a
 *   station fill together. A full meter (METER_POINTS) adds REFILL_PERCENT of each trade's max stock to every
 *   unlocked trade of its section (capped at the max) and drops back to 0. A 100 % happy villager working a full
 *   day (EXPECTED_WORK_CHECKS_PER_DAY) at one station fills it from empty to full. Every refill gives at least
 *   MIN_REFILL_USES; fractions of a use are kept per trade and added to the next refill, so rounding loses nothing.
 * - If the section has nothing missing when its meter fills, the meter holds at full. It is released
 *   RELEASE_DELAY_TICKS after the next trade of that section; as a fallback for stock missing without a trade
 *   (e.g. a higher max after a rank-up), each morning also releases held meters.
 * - Which section the villager works on: StationFocus.choose.
 */
public final class RestockService {

    public static final int REFILL_PERCENT = 25;
    public static final int MIN_REFILL_USES = 1;
    /** Pause between a trade and the release of a held full meter of its section (2 s). */
    public static final int RELEASE_DELAY_TICKS = 40;
    public static final int EXPECTED_WORK_CHECKS_PER_DAY = 60;
    public static final int HAPPINESS_FLOOR = 50;
    private static final int POINTS_PER_HAPPY_CHECK = 100;
    public static final int METER_POINTS = EXPECTED_WORK_CHECKS_PER_DAY * POINTS_PER_HAPPY_CHECK * REFILL_PERCENT / 100;
    /** Points a 100 % happy villager earns in one full work day at one station. */
    public static final int FULL_DAY_POINTS = EXPECTED_WORK_CHECKS_PER_DAY * POINTS_PER_HAPPY_CHECK;
    /** Remainders are stored in 1/REMAINDER_SCALE of a use. */
    private static final int REMAINDER_SCALE = 100;

    private RestockService() {
    }

    /**
     * Stock of an entry that has never been restocked since it unlocked (no value in VillagerState.stock yet):
     * half its current max, rounded up so a max of 1 still starts usable. Applied lazily on read instead of
     * written at unlock time, because unlocking is derived from ranks and has no event of its own.
     */
    public static int initialStock(int maxUses) {
        return (maxUses + 1) / 2;
    }

    /** Points one work check earns: HAPPINESS_FLOOR at 0 % happiness, POINTS_PER_HAPPY_CHECK at 100 %. */
    public static int workPoints(int happinessPercent) {
        return HAPPINESS_FLOOR + (POINTS_PER_HAPPY_CHECK - HAPPINESS_FLOOR) * happinessPercent / 100;
    }

    public static int meterPoints(Villager villager, VillagerState state, SectionDefinition section) {
        return section.logic().meterPoints(villager, state, section);
    }

    public static boolean isMeterFull(Villager villager, VillagerState state, SectionDefinition section) {
        return state.productivity().meter(section.id()) >= meterPoints(villager, state, section);
    }

    /**
     * The villager is filling a list section's meter at the station it works at right now - only then does it
     * make its own work sound (mixin/VillagerWorkSoundMixin). A badge section (a passive) makes its station
     * sound instead, so a villager at its passive station stays silent.
     */
    public static boolean makesWorkSound(Villager villager, VillagerState state) {
        Optional<Identifier> station = StationFocus.focusSection(villager, state).flatMap(SectionDefinition::station);
        return station.isPresent() && VillagerSections.workable(villager, state).stream()
                .anyMatch(section -> section.display() == SectionDefinition.Display.LIST
                        && section.station().equals(station)
                        && !isMeterFull(villager, state, section));
    }

    /**
     * First scan of a new day: forgets stock of entries that no longer exist, refills sections whose meter was
     * held at full while stock went missing, and drops the focus - the day starts at the morning station.
     */
    public static VillagerState startDay(Villager villager, VillagerState state, long today) {
        VillagerState updated = forgetUnknownStock(villager, state);
        for (SectionDefinition section : VillagerSections.workable(villager, updated)) {
            if (isMeterFull(villager, updated, section) && section.logic().missingStock(villager, updated, section) > 0) {
                updated = section.logic().refill(villager, updated, section);
            }
        }
        return updated.withProductivity(updated.productivity().withFocus(Optional.empty())).withWorkDay(today);
    }

    /**
     * Stock of entries that no longer exist (removed from the data pack, the profession changed) - dropped each
     * morning so old ids don't stay in the save forever. Only entries of the villager's own sections are known,
     * so this runs where the offers are built from, never in the codec.
     */
    private static VillagerState forgetUnknownStock(Villager villager, VillagerState state) {
        Set<Identifier> known = StandardSection.entryIds(villager);
        if (known.containsAll(state.stock().keySet()) && known.containsAll(state.productivity().refillRemainders().keySet())) {
            return state;
        }
        Map<Identifier, Integer> stock = new HashMap<>(state.stock());
        stock.keySet().retainAll(known);
        Map<Identifier, Integer> remainders = new HashMap<>(state.productivity().refillRemainders());
        remainders.keySet().retainAll(known);
        return state.withStock(stock).withProductivity(state.productivity().withRefillRemainders(remainders));
    }

    /**
     * After a trade of this section: if its meter is held at full, schedule the release RELEASE_DELAY_TICKS from
     * now - it then refills right away instead of waiting for the morning.
     */
    public static Productivity scheduleRelease(Villager villager, VillagerState state, SectionDefinition section) {
        Productivity meters = state.productivity();
        if (!isMeterFull(villager, state, section)) {
            return meters;
        }
        Map<Identifier, Long> releaseAt = new HashMap<>(meters.releaseAtTick());
        releaseAt.put(section.id(), villager.level().getGameTime() + RELEASE_DELAY_TICKS);
        return meters.withReleaseAtTick(releaseAt);
    }

    /**
     * Releases held meters whose delay after a trade is over: refills the section if it is missing stock.
     * Called every tick while someone trades (VillagerMenu.broadcastChanges) and by the work scan.
     */
    public static void releaseDue(Villager villager) {
        VillagerStateAccess access = VillagerStateAccess.of(villager);
        VillagerState state = access.getState();
        Map<Identifier, Long> releaseAt = state.productivity().releaseAtTick();
        long now = villager.level().getGameTime();
        if (releaseAt.isEmpty() || releaseAt.values().stream().noneMatch(tick -> tick <= now)) {
            return;
        }
        Map<Identifier, Long> remaining = new HashMap<>(releaseAt);
        remaining.values().removeIf(tick -> tick <= now);
        VillagerState updated = state;
        for (SectionDefinition section : VillagerSections.workable(villager, state)) {
            Long tick = releaseAt.get(section.id());
            if (tick != null && tick <= now && isMeterFull(villager, updated, section)
                    && section.logic().missingStock(villager, updated, section) > 0) {
                updated = section.logic().refill(villager, updated, section);
            }
        }
        access.setState(updated.withProductivity(updated.productivity().withReleaseAtTick(remaining)));
    }

    /**
     * One work check: re-picks the focus if needed (StationFocus.choose), fills the meters of every workable
     * section at the focused section's station, and refills those that are full and missing stock.
     */
    public static VillagerState withWork(ServerLevel level, Villager villager, VillagerState state, int points) {
        List<SectionDefinition> workable = VillagerSections.workable(villager, state);
        Optional<SectionDefinition> focus = StationFocus.choose(level, villager, state, workable);
        VillagerState updated = state.withProductivity(state.productivity().withFocus(focus.map(SectionDefinition::id)));
        if (focus.isEmpty()) {
            return updated;
        }
        for (SectionDefinition section : workable) {
            if (!section.station().equals(focus.get().station())) {
                continue;
            }
            int filled = Math.min(meterPoints(villager, updated, section), updated.productivity().meter(section.id()) + points);
            updated = updated.withProductivity(updated.productivity().withMeter(section.id(), filled));
            if (isMeterFull(villager, updated, section) && section.logic().missingStock(villager, updated, section) > 0) {
                updated = section.logic().refill(villager, updated, section);
            }
        }
        return updated;
    }

    /** Adds REFILL_PERCENT of each offer's max to its stock, keeping the fractions, and empties the section's meter. */
    public static VillagerState refill(VillagerState state, SectionDefinition section, List<SectionOffer> offers) {
        Map<Identifier, Integer> stock = new HashMap<>(state.stock());
        Map<Identifier, Integer> remainders = new HashMap<>(state.productivity().refillRemainders());
        for (SectionOffer offer : offers) {
            Identifier id = offer.exchange().id();
            int maxUses = offer.exchange().maxUses();
            // At least MIN_REFILL_USES per refill; a use handed out early is paid for from later fractions.
            int scaled = remainders.getOrDefault(id, 0) + maxUses * REFILL_PERCENT * REMAINDER_SCALE / 100;
            int added = Math.max(MIN_REFILL_USES, scaled / REMAINDER_SCALE);
            stock.put(id, Math.min(maxUses, offer.exchange().usesRemaining() + added));
            remainders.put(id, Math.max(0, scaled - added * REMAINDER_SCALE));
        }
        Productivity meters = state.productivity().withMeter(section.id(), 0).withRefillRemainders(remainders);
        return state.withStock(stock).withProductivity(meters);
    }

    /** Debug: fills every open section's offers to their max right away (/vo restock). */
    public static void restock(Villager villager) {
        VillagerStateAccess access = VillagerStateAccess.of(villager);
        VillagerState state = access.getState();
        Map<Identifier, Integer> stock = new HashMap<>(state.stock());
        for (SectionDefinition section : VillagerSections.open(villager, state)) {
            for (SectionOffer offer : section.logic().offers(villager, state, section)) {
                stock.put(offer.exchange().id(), offer.exchange().maxUses());
            }
        }
        access.setState(state.withStock(stock));
    }
}
