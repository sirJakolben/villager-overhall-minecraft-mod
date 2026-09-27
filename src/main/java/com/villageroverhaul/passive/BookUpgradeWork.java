package com.villageroverhaul.passive;

import com.villageroverhaul.core.state.StationSlot;
import com.villageroverhaul.core.state.VillagerState;
import com.villageroverhaul.librarian.EnchantmentStationBlock;
import com.villageroverhaul.librarian.EnchantmentStationBlockEntity;
import com.villageroverhaul.trade.RestockService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.Optional;

/**
 * The Librarian's passive at work (Obsidian Librarian.md, "Passive Ability"). The passive meter fills as
 * before, while the villager works near its enchantment station. Once it is full and the station holds a
 * book it may upgrade (isPending), the focus jumps to the passive station (RestockService.chooseFocus), so
 * it walks there; standing right at it (UPGRADE_REACH) during work time, it upgrades one book by one
 * level: the meter drops to 0, the station pulses redstone and plays the Librarian work sound. With no
 * book to upgrade the meter just holds at full - like a trade meter with nothing missing.
 *
 * Runs on every work scan (100 ticks), also outside work time, so the station always knows its owner's
 * rank (markOwner - it decides which books a hopper may pull out). Cost: one block entity lookup at a
 * known position and at most 5 item checks - no block scan; unloaded stations are skipped.
 */
public final class BookUpgradeWork {

    /** How close the villager must stand to the station's center to upgrade - "right in front of it" (Tweak-Werte.md). */
    public static final double UPGRADE_REACH = 2.0;
    /** Book upgrades per full, happy work day, by passive rank 0..6 (Librarian.md "Passive Ability") - sizes the passive meter. */
    private static final int[] UPGRADES_PER_DAY_BY_RANK = {1, 1, 1, 1, 2, 3, 4};

    private BookUpgradeWork() {
    }

    public static int upgradesPerDay(int passiveRank) {
        return UPGRADES_PER_DAY_BY_RANK[Math.clamp(passiveRank, 0, UPGRADES_PER_DAY_BY_RANK.length - 1)];
    }

    private static Optional<EnchantmentStationBlockEntity> station(ServerLevel level, VillagerState state) {
        Optional<GlobalPos> station = state.stations().passive();
        if (station.isEmpty() || station.get().dimension() != level.dimension() || !level.isLoaded(station.get().pos())) {
            return Optional.empty();
        }
        return level.getBlockEntity(station.get().pos()) instanceof EnchantmentStationBlockEntity books ? Optional.of(books) : Optional.empty();
    }

    /** Full passive meter and a book in its station it may upgrade - time to walk there. */
    public static boolean isPending(ServerLevel level, Villager villager, VillagerState state) {
        return state.dailyProductivity().passive() >= RestockService.meterPoints(villager, StationSlot.PASSIVE, state)
                && station(level, state).map(books -> books.hasUpgradeableBook(state.ranks().passive())).orElse(false);
    }

    /** workTime: the villager is in its WORK activity - upgrades only happen then, never while it sleeps or idles. */
    public static VillagerState apply(ServerLevel level, Villager villager, VillagerState state, boolean workTime) {
        Optional<EnchantmentStationBlockEntity> station = station(level, state);
        if (station.isEmpty()) {
            return state;
        }
        EnchantmentStationBlockEntity books = station.get();
        BlockPos pos = books.getBlockPos();
        int rank = state.ranks().passive();
        books.markOwner(rank);
        if (!workTime || !PassiveWork.worksAtPassiveStation(villager, state)
                || state.dailyProductivity().passive() < RestockService.meterPoints(villager, StationSlot.PASSIVE, state)
                || !pos.closerToCenterThan(villager.position(), UPGRADE_REACH) || !books.upgradeNextBook(rank)) {
            return state;
        }
        EnchantmentStationBlock.pulse(level, pos);
        level.playSound(null, pos, SoundEvents.VILLAGER_WORK_LIBRARIAN, SoundSource.NEUTRAL, 1.0F, 1.0F);
        return new VillagerState(
                state.level(), state.xp(), state.unspentUpgradePoints(), state.ranks(),
                state.questSlotRotations(), state.tradeUsesRemaining(), state.dailyProductivity().with(StationSlot.PASSIVE, 0),
                state.happiness(), state.lastProcessedDay(), state.questSlotRerollAvailableAtTick(), state.questLog(), state.stations()
        );
    }
}
