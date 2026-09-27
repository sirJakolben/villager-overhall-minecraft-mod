package com.villageroverhaul.core.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Today's quest turn-ins (decided 2026-09-23): completedToday counts completions on day `day`; once at the
 * daily limit, completed slots get no new quest until the next day. hiddenSlots holds, per quest slot key,
 * the game tick at which a just-completed slot shows its next quest again - the short pause between
 * turning one in and the next one popping up. Offers are built from this state only (never from the
 * clock), so the offer list stays a pure view of the state - see QuestActions for who clears it.
 */
public record QuestLog(long day, int completedToday, Map<Identifier, Long> hiddenSlots) {

    public static final QuestLog EMPTY = new QuestLog(0L, 0, Map.of());

    public static final Codec<QuestLog> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("day").forGetter(QuestLog::day),
            Codec.INT.fieldOf("completed_today").forGetter(QuestLog::completedToday),
            Codec.unboundedMap(Identifier.CODEC, Codec.LONG).optionalFieldOf("hidden_slots", Map.of()).forGetter(QuestLog::hiddenSlots)
    ).apply(instance, QuestLog::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, QuestLog> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, QuestLog::day,
            ByteBufCodecs.VAR_INT, QuestLog::completedToday,
            ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, ByteBufCodecs.VAR_LONG), QuestLog::hiddenSlots,
            QuestLog::new
    );
}
