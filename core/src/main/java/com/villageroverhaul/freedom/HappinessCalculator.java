package com.villageroverhaul.freedom;

import com.villageroverhaul.core.state.Happiness;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Happiness in percent (README.md, reworked 2026-09-23). Each
 * element has a weight and a timer in days: observed today it counts fully, then it falls linearly
 * and reaches 0 % exactly TIMER days after the last observation. Each element is rounded UP to 5 %
 * steps, and the total is capped at 100 %. Because everything is measured in whole days, the score
 * only ever changes once per day (or when something new is observed).
 *
 * Slot elements count different entities, newest first, each with its own timer:
 * - villager contact: 20 % for the most recent villager, +10 % for each of two further ones (max 40 %)
 * - companions (cats, golems, allays - the happiness_companions entity tag): 10 % each for two (max 20 %)
 *
 * Weights add up to 110 % (bed 30, meeting point 20, contacts 40, companions 20), so a villager can
 * still reach 100 % with one element partly missing. All numbers: Obsidian Tweak-Werte.md.
 */
public final class HappinessCalculator {

    private static final int BED_WEIGHT = 30;
    private static final int MEETING_POINT_WEIGHT = 20;
    private static final int[] CONTACT_WEIGHTS = {20, 10, 10};
    private static final int[] COMPANION_WEIGHTS = {10, 10};

    private static final int BED_TIMER_DAYS = 2;
    private static final int MEETING_POINT_TIMER_DAYS = 2;
    private static final int CONTACT_TIMER_DAYS = 3;
    private static final int COMPANION_TIMER_DAYS = 3;

    private static final int ROUNDING_STEP = 5;

    /** One element's share of the score - also what /vo state happiness prints. */
    public record Part(String label, long lastDay, int weight, int timerDays, int value) {
    }

    private HappinessCalculator() {
    }

    public static int percent(Happiness happiness, long today) {
        return Math.min(100, breakdown(happiness, today).stream().mapToInt(Part::value).sum());
    }

    /** Every element with its current contribution, in a fixed order; contacts and companions newest first. */
    public static List<Part> breakdown(Happiness happiness, long today) {
        List<Part> parts = new ArrayList<>();
        parts.add(part("bed", BED_WEIGHT, happiness.lastBedDay(), BED_TIMER_DAYS, today));
        parts.add(part("meeting point", MEETING_POINT_WEIGHT, happiness.lastMeetingPointDay(), MEETING_POINT_TIMER_DAYS, today));
        slots(parts, "villager contact", CONTACT_WEIGHTS, happiness.contacts(), CONTACT_TIMER_DAYS, today);
        slots(parts, "companion", COMPANION_WEIGHTS, happiness.companions(), COMPANION_TIMER_DAYS, today);
        return parts;
    }

    private static void slots(List<Part> parts, String label, int[] weights, List<Happiness.Encounter> encounters, int timerDays, long today) {
        List<Happiness.Encounter> newestFirst = encounters.stream()
                .sorted(Comparator.comparingLong(Happiness.Encounter::day).reversed())
                .toList();
        for (int i = 0; i < weights.length; i++) {
            long lastDay = i < newestFirst.size() ? newestFirst.get(i).day() : Happiness.NEVER;
            String slotLabel = label + " " + (i + 1) + (i < newestFirst.size() ? " (" + newestFirst.get(i).entity().toString().substring(0, 8) + ")" : "");
            parts.add(part(slotLabel, weights[i], lastDay, timerDays, today));
        }
    }

    private static Part part(String label, int weight, long lastDay, int timerDays, long today) {
        long daysSince = Math.max(0, today - lastDay);
        int value = 0;
        if (daysSince < timerDays) {
            // weight * (timer - daysSince) / timer, always rounded UP to the next ROUNDING_STEP -
            // in integers, so an exact 15 can't turn into 15.0000001 and jump to 20.
            int remaining = weight * (timerDays - (int) daysSince);
            value = Math.ceilDiv(remaining, timerDays * ROUNDING_STEP) * ROUNDING_STEP;
        }
        return new Part(label, lastDay, weight, timerDays, value);
    }
}
