package com.villageroverhaul.traderework.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.villageroverhaul.trade.VillagerOffers;
import com.villageroverhaul.traderework.TradeReworkMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.npc.villager.Villager;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.HashMap;
import java.util.Map;

/**
 * A villager's quest slots (QuestLogic), in their own attachment - the core's VillagerState knows nothing of
 * quests. Synced, because the reroll button shows its cooldown.
 *
 * rotations: per slot, how often it moved on (completion or reroll) - the pick is derived from it, never stored.
 * rerollReadyAt: per slot, the game tick its reroll button is ready again. day / completedToday: today's
 * turn-ins (decided 2026-09-23); once at the daily limit, completed slots get no new quest until the next day.
 * pausedUntil: per slot, the game tick a just-completed slot shows its next quest - the short pause between
 * turning one in and the next one popping up (PAUSED_UNTIL_TOMORROW: waiting for the next day). Offers are built
 * from this state only (never from the clock), so they stay a pure view of it - see QuestLogic.refresh.
 */
public record QuestState(Map<Integer, Integer> rotations, Map<Integer, Long> rerollReadyAt, long day, int completedToday,
                         Map<Integer, Long> pausedUntil) {

    public static final QuestState EMPTY = new QuestState(Map.of(), Map.of(), 0L, 0, Map.of());
    public static final long PAUSED_UNTIL_TOMORROW = Long.MAX_VALUE;

    public int rotation(int slot) {
        return rotations.getOrDefault(slot, 0);
    }

    public boolean isPaused(int slot) {
        return pausedUntil.containsKey(slot);
    }

    public QuestState withRotated(int slot) {
        Map<Integer, Integer> newRotations = new HashMap<>(rotations);
        newRotations.merge(slot, 1, Integer::sum);
        return new QuestState(Map.copyOf(newRotations), rerollReadyAt, day, completedToday, pausedUntil);
    }

    public QuestState withRerollReadyAt(int slot, long tick) {
        Map<Integer, Long> newReadyAt = new HashMap<>(rerollReadyAt);
        newReadyAt.put(slot, tick);
        return new QuestState(rotations, Map.copyOf(newReadyAt), day, completedToday, pausedUntil);
    }

    public QuestState withLog(long newDay, int newCompletedToday, Map<Integer, Long> newPausedUntil) {
        return new QuestState(rotations, rerollReadyAt, newDay, newCompletedToday, Map.copyOf(newPausedUntil));
    }

    /** NBT compound keys are strings - the slot index is written as one. */
    private static final Codec<Integer> SLOT_KEY = Codec.STRING.xmap(Integer::parseInt, String::valueOf);

    public static final MapCodec<QuestState> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.unboundedMap(SLOT_KEY, Codec.INT).fieldOf("rotations").forGetter(QuestState::rotations),
            Codec.unboundedMap(SLOT_KEY, Codec.LONG).fieldOf("reroll_ready_at").forGetter(QuestState::rerollReadyAt),
            Codec.LONG.fieldOf("day").forGetter(QuestState::day),
            Codec.INT.fieldOf("completed_today").forGetter(QuestState::completedToday),
            Codec.unboundedMap(SLOT_KEY, Codec.LONG).fieldOf("paused_until").forGetter(QuestState::pausedUntil)
    ).apply(instance, QuestState::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, QuestState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.VAR_INT, ByteBufCodecs.VAR_INT), QuestState::rotations,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.VAR_INT, ByteBufCodecs.VAR_LONG), QuestState::rerollReadyAt,
            ByteBufCodecs.VAR_LONG, QuestState::day,
            ByteBufCodecs.VAR_INT, QuestState::completedToday,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.VAR_INT, ByteBufCodecs.VAR_LONG), QuestState::pausedUntil,
            QuestState::new
    );

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, TradeReworkMod.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<QuestState>> ATTACHMENT = ATTACHMENT_TYPES.register(
            "quest_state",
            () -> AttachmentType.builder(() -> EMPTY)
                    .serialize(MAP_CODEC)
                    .sync(STREAM_CODEC)
                    .build()
    );

    public static QuestState of(Villager villager) {
        return villager.getData(ATTACHMENT);
    }

    /** Stores the new state and rebuilds the villager's offers - they are a view of it. */
    public static void set(Villager villager, QuestState state) {
        if (!state.equals(of(villager))) {
            villager.setData(ATTACHMENT, state);
            VillagerOffers.refresh(villager);
        }
    }
}
