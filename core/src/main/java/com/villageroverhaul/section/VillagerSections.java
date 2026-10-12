package com.villageroverhaul.section;

import com.villageroverhaul.api.SectionDefinition;
import com.villageroverhaul.state.VillagerState;
import com.villageroverhaul.vanilla.VanillaCatalog;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

import java.util.List;
import java.util.Optional;

/**
 * Which sections one villager has, and in what state:
 * - a section belongs to the villager when it has entries for its profession (data pack or Vanilla catalog) or
 *   a station registered for its profession - Farmer & co. therefore only ever have Quests and Trades;
 * - it is open (shown, upgradeable, offering) when it needs no station, when the villager owns its station, or
 *   when its rank is above 0 - a lost station never takes bought ranks away;
 * - it is workable (its meter can fill, the villager can focus on it) when it is open and its station is owned.
 */
public final class VillagerSections {

    private VillagerSections() {
    }

    /** Server only - on the client the Vanilla catalog is unknown; the screen gets the result sent (SectionView). */
    public static List<SectionDefinition> of(Villager villager) {
        Holder<VillagerProfession> profession = villager.getVillagerData().profession();
        return Sections.all().stream()
                .filter(section -> hasEntries(villager, section) || hasOwnStation(profession, section))
                .toList();
    }

    public static boolean isOpen(VillagerState state, SectionDefinition section) {
        return section.station().isEmpty() || stationOf(state, section).isPresent() || state.rank(section.id()) > 0;
    }

    public static List<SectionDefinition> open(Villager villager, VillagerState state) {
        return of(villager).stream().filter(section -> isOpen(state, section)).toList();
    }

    public static List<SectionDefinition> workable(Villager villager, VillagerState state) {
        return of(villager).stream().filter(section -> stationOf(state, section).isPresent()).toList();
    }

    /** Where the villager works on this section - empty without a station or while it owns none. */
    public static Optional<GlobalPos> stationOf(VillagerState state, SectionDefinition section) {
        return section.station().flatMap(station -> state.stations().get(station));
    }

    /** See SectionLogic.refresh. */
    public static void refresh(Villager villager) {
        for (SectionDefinition section : of(villager)) {
            section.logic().refresh(villager, section);
        }
    }

    /** See SectionLogic.onWorkScan. */
    public static VillagerState onWorkScan(ServerLevel level, Villager villager, VillagerState state, boolean workTime) {
        VillagerState updated = state;
        for (SectionDefinition section : of(villager)) {
            updated = section.logic().onWorkScan(level, villager, updated, section, workTime);
        }
        return updated;
    }

    private static boolean hasEntries(Villager villager, SectionDefinition section) {
        return VanillaCatalog.usedBy(villager)
                ? !VanillaCatalog.of(villager).entries(section.id()).isEmpty()
                : !SectionEntries.of(villager.level().registryAccess(), villager.getVillagerData().profession().value(), section.id()).isEmpty();
    }

    private static boolean hasOwnStation(Holder<VillagerProfession> profession, SectionDefinition section) {
        return section.station().isPresent()
                && Sections.stationsOf(profession).stream().anyMatch(station -> station.id().equals(section.station().get()));
    }
}
