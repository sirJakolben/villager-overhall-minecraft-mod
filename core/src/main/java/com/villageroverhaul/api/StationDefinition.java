package com.villageroverhaul.api;

import com.villageroverhaul.VillagerOverhaulMod;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

/**
 * A workplace block a profession has besides its Vanilla job site (register with ExtensionHooks.registerStation).
 * The block is added to the profession's Vanilla job-site type (jobSite) - to Vanilla it simply is one more lectern,
 * so a jobless villager takes the profession at it and any of them keeps the job (TradeReworkSections, core station/ProfessionStations).
 *
 * id names the kind of station, not the block: the Trade Rework registers "master_station" once per profession,
 * each with that profession's block, and its Masteries section points at that id. Several sections may point at
 * the same station. morningPriority: where the villager starts its work day - the owned station with the highest
 * priority; the Vanilla job site has JOB_SITE_MORNING_PRIORITY.
 */
public record StationDefinition(Identifier id, ResourceKey<VillagerProfession> profession, ResourceKey<PoiType> jobSite,
                                Supplier<? extends Block> block, int morningPriority) {

    /** The Vanilla job site of every profession (lectern, stonecutter ...) - a station without a definition. */
    public static final Identifier JOB_SITE = Identifier.fromNamespaceAndPath(VillagerOverhaulMod.MODID, "job_site");
    public static final int JOB_SITE_MORNING_PRIORITY = 0;
}
