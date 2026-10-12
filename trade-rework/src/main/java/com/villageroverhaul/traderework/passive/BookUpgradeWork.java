package com.villageroverhaul.traderework.passive;

import com.villageroverhaul.state.VillagerState;
import com.villageroverhaul.traderework.librarian.EnchantmentStationBlock;
import com.villageroverhaul.traderework.librarian.EnchantmentStationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.Optional;

/**
 * The Librarian's passive at work (Obsidian Librarian.md, "Passive Ability"). The passive meter fills
 * while the villager works at its enchantment station. Once it is full and the station holds a
 * book it may upgrade (isPending), the focus jumps to the passive station (StationFocus.choose), so
 * it walks there; standing right at it (UPGRADE_REACH) during work time, it upgrades one book by one
 * level: the meter drops to 0, the station pulses redstone and plays the Librarian work sound. With no
 * book to upgrade the meter just holds at full - like a trade meter with nothing missing.
 *
 * Runs on every work scan (100 ticks), also outside work time, so the station always knows its owner's
 * rank (markOwner - it decides which books a hopper may pull out). Cost: one block entity lookup at a
 * known position and at most 5 item checks - no block scan; unloaded stations are skipped.
 */
public final class BookUpgradeWork implements PassiveWork {

    /** How close the villager must stand to the station's center to upgrade - "right in front of it" (Tweak-Werte.md). */
    public static final double UPGRADE_REACH = 2.0;
    /** Book upgrades per full, happy work day, by passive rank 0..6 (Librarian.md "Passive Ability") - sizes the passive meter. */
    private static final int[] UPGRADES_PER_DAY_BY_RANK = {1, 1, 1, 1, 2, 3, 4};

    @Override
    public int stepsPerDay(int passiveRank) {
        return UPGRADES_PER_DAY_BY_RANK[Math.clamp(passiveRank, 0, UPGRADES_PER_DAY_BY_RANK.length - 1)];
    }

    private static Optional<EnchantmentStationBlockEntity> station(ServerLevel level, VillagerState state) {
        Optional<GlobalPos> station = PassiveLogic.station(state);
        if (station.isEmpty() || station.get().dimension() != level.dimension() || !level.isLoaded(station.get().pos())) {
            return Optional.empty();
        }
        return level.getBlockEntity(station.get().pos()) instanceof EnchantmentStationBlockEntity books ? Optional.of(books) : Optional.empty();
    }

    /** Full passive meter and a book in its station it may upgrade - time to walk there. */
    @Override
    public boolean isPending(ServerLevel level, Villager villager, VillagerState state) {
        return PassiveLogic.isMeterFull(villager, state)
                && station(level, state).map(books -> books.hasUpgradeableBook(PassiveLogic.rank(state))).orElse(false);
    }

    /** workTime: the villager is in its WORK activity - upgrades only happen then, never while it sleeps or idles. */
    @Override
    public VillagerState apply(ServerLevel level, Villager villager, VillagerState state, boolean workTime) {
        Optional<EnchantmentStationBlockEntity> station = station(level, state);
        if (station.isEmpty()) {
            return state;
        }
        EnchantmentStationBlockEntity books = station.get();
        BlockPos pos = books.getBlockPos();
        int rank = PassiveLogic.rank(state);
        books.markOwner(rank);
        if (!workTime || !PassiveLogic.worksAtStation(villager, state)
                || !PassiveLogic.isMeterFull(villager, state)
                || !pos.closerToCenterThan(villager.position(), UPGRADE_REACH) || !books.upgradeNextBook(rank)) {
            return state;
        }
        EnchantmentStationBlock.pulse(level, pos);
        level.playSound(null, pos, SoundEvents.VILLAGER_WORK_LIBRARIAN, SoundSource.NEUTRAL, 1.0F, 1.0F);
        return PassiveLogic.emptyMeter(state);
    }
}
