package com.villageroverhaul.core.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * When each happiness element was last observed (as a day number, see DayClock) - see
 * README.md (reworked 2026-09-23). Bed and meeting point are one
 * day value each; villager contacts and companions (cats, golems, allays) are small lists of
 * different entities, each with its own last-seen day. percent is the resulting 0-100 score,
 * recomputed by the server (HappinessCalculator) and stored here so the client can show it without
 * a day clock.
 *
 * mood is a momentary override on top of that (VillagerWorkScan, 2026-09-23): 0 while the villager
 * is afraid (panic, raid, hiding), 100 while it sees a Hero of the Village, NO_MOOD otherwise. It
 * never touches the long-term elements or percent - once the moment passes, the villager is exactly
 * as happy as before. Work XP and the GUI use effectivePercent().
 *
 * Every field is optional in the codec, so older saves (earlier layouts of this record under other
 * keys) load as "never observed" instead of failing.
 */
public record Happiness(long lastBedDay, long lastMeetingPointDay, List<Encounter> contacts, List<Encounter> companions, int percent, int mood) {

    /** Day value for "never observed" - far enough in the past that every element counts as expired. */
    public static final long NEVER = -1000L;
    public static final int MAX_CONTACTS = 3;
    public static final int MAX_COMPANIONS = 2;

    /** mood value for "no momentary override" - see mood(). */
    public static final int NO_MOOD = -1;

    public static final Happiness EMPTY = new Happiness(NEVER, NEVER, List.of(), List.of(), 0, NO_MOOD);

    /** One recent encounter with a specific entity (another villager, or a companion), with the day it last happened. */
    public record Encounter(UUID entity, long day) {
        public static final Codec<Encounter> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("entity").forGetter(Encounter::entity),
                Codec.LONG.fieldOf("day").forGetter(Encounter::day)
        ).apply(instance, Encounter::new));

        public static final StreamCodec<ByteBuf, Encounter> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Encounter::entity,
                ByteBufCodecs.VAR_LONG, Encounter::day,
                Encounter::new
        );
    }

    public static final Codec<Happiness> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.optionalFieldOf("last_bed_day", NEVER).forGetter(Happiness::lastBedDay),
            Codec.LONG.optionalFieldOf("last_meeting_point_day", NEVER).forGetter(Happiness::lastMeetingPointDay),
            Encounter.CODEC.listOf().optionalFieldOf("contacts", List.of()).forGetter(Happiness::contacts),
            Encounter.CODEC.listOf().optionalFieldOf("companions", List.of()).forGetter(Happiness::companions),
            Codec.INT.optionalFieldOf("percent", 0).forGetter(Happiness::percent),
            Codec.INT.optionalFieldOf("mood", NO_MOOD).forGetter(Happiness::mood)
    ).apply(instance, Happiness::new));

    public static final StreamCodec<ByteBuf, Happiness> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, Happiness::lastBedDay,
            ByteBufCodecs.VAR_LONG, Happiness::lastMeetingPointDay,
            Encounter.STREAM_CODEC.apply(ByteBufCodecs.list()), Happiness::contacts,
            Encounter.STREAM_CODEC.apply(ByteBufCodecs.list()), Happiness::companions,
            ByteBufCodecs.VAR_INT, Happiness::percent,
            ByteBufCodecs.VAR_INT, Happiness::mood,
            Happiness::new
    );

    public Happiness withBed(long day) {
        return new Happiness(day, lastMeetingPointDay, contacts, companions, percent, mood);
    }

    public Happiness withMeetingPoint(long day) {
        return new Happiness(lastBedDay, day, contacts, companions, percent, mood);
    }

    public Happiness withContact(UUID villager, long day) {
        return new Happiness(lastBedDay, lastMeetingPointDay, recordEncounter(contacts, villager, day, MAX_CONTACTS), companions, percent, mood);
    }

    public Happiness withCompanion(UUID companion, long day) {
        return new Happiness(lastBedDay, lastMeetingPointDay, contacts, recordEncounter(companions, companion, day, MAX_COMPANIONS), percent, mood);
    }

    public Happiness withPercent(int newPercent) {
        return new Happiness(lastBedDay, lastMeetingPointDay, contacts, companions, newPercent, mood);
    }

    public Happiness withMood(int newMood) {
        return new Happiness(lastBedDay, lastMeetingPointDay, contacts, companions, percent, newMood);
    }

    /** What happiness counts as right now: the momentary mood if there is one, otherwise the long-term percent. */
    public int effectivePercent() {
        return mood == NO_MOOD ? percent : mood;
    }

    /** True once `entity` has already been recorded today - lets the scan skip redundant work. */
    public static boolean seenToday(List<Encounter> encounters, UUID entity, long day) {
        return encounters.stream().anyMatch(encounter -> encounter.entity().equals(entity) && encounter.day() == day);
    }

    /**
     * An already known entity just gets its day refreshed; a new one takes a free slot, or replaces
     * the encounter whose timer runs out first (the oldest) once all slots are taken.
     */
    private static List<Encounter> recordEncounter(List<Encounter> encounters, UUID entity, long day, int maxSlots) {
        List<Encounter> updated = new ArrayList<>(encounters);
        updated.removeIf(encounter -> encounter.entity().equals(entity));
        if (updated.size() >= maxSlots) {
            updated.sort(Comparator.comparingLong(Encounter::day));
            updated.remove(0);
        }
        updated.add(new Encounter(entity, day));
        return List.copyOf(updated);
    }
}
