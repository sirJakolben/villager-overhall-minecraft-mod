package com.villageroverhaul.station;

import com.villageroverhaul.VillagerOverhaulMod;
import com.villageroverhaul.api.StationDefinition;
import com.villageroverhaul.section.Sections;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.poi.ExtendPoiTypesEvent;

import java.util.List;
import java.util.stream.Stream;

/**
 * A profession's workplaces: its Vanilla job site plus the stations registered for it (api/StationDefinition).
 * The registered blocks are added to the profession's Vanilla job-site type through NeoForge's
 * ExtendPoiTypesEvent - so to Vanilla they simply are lecterns: a jobless villager takes the profession at any of
 * them, any of them keeps the job and gives a work time, and Vanilla validates them like a lectern. No mixin
 * needed. Which station a job site is, is told apart by its block (stationAt).
 */
@EventBusSubscriber(modid = VillagerOverhaulMod.MODID)
public final class ProfessionStations {

    private ProfessionStations() {
    }

    @SubscribeEvent
    static void onExtendPoiTypes(ExtendPoiTypesEvent event) {
        for (StationDefinition station : Sections.stations()) {
            event.addBlockToPoi(station.jobSite(), station.block().get());
        }
    }

    /** The profession's station ids, the Vanilla job site included. */
    public static List<Identifier> ids(Holder<VillagerProfession> profession) {
        return Stream.concat(
                Stream.of(StationDefinition.JOB_SITE),
                Sections.stationsOf(profession).stream().map(StationDefinition::id)).toList();
    }

    /** Which of the profession's stations the job site at pos is: a registered station by its block, else the Vanilla job site. */
    public static Identifier stationAt(ServerLevel level, Holder<VillagerProfession> profession, BlockPos pos) {
        Block block = level.getBlockState(pos).getBlock();
        for (StationDefinition station : Sections.stationsOf(profession)) {
            if (station.block().get() == block) {
                return station.id();
            }
        }
        return StationDefinition.JOB_SITE;
    }

    /** Where the work day starts: the owned station with the highest morning priority. */
    public static int morningPriority(Holder<VillagerProfession> profession, Identifier station) {
        return Sections.stationsOf(profession).stream()
                .filter(definition -> definition.id().equals(station))
                .mapToInt(StationDefinition::morningPriority)
                .findFirst()
                .orElse(StationDefinition.JOB_SITE_MORNING_PRIORITY);
    }
}
