package com.smartvillager.professions;

import com.smartvillager.progression.api.ExtensionHooks;
import com.smartvillager.progression.api.SectionDefinition;
import com.smartvillager.progression.api.StationDefinition;
import com.smartvillager.progression.section.CoreSections;
import com.smartvillager.professions.cartographer.CartographerBlocks;
import com.smartvillager.professions.farmer.FarmerBlocks;
import com.smartvillager.professions.fisherman.FishermanBlocks;
import com.smartvillager.professions.librarian.ExperienceBottles;
import com.smartvillager.professions.librarian.LibrarianBlocks;
import com.smartvillager.professions.mapping.MapPool;
import com.smartvillager.professions.mapping.MappingBlocks;
import com.smartvillager.professions.mason.MasonBlocks;
import com.smartvillager.professions.mason.VillagerStatueHappiness;
import com.smartvillager.professions.passive.BookUpgradeWork;
import com.smartvillager.professions.passive.CrushingWork;
import com.smartvillager.professions.passive.FishingWork;
import com.smartvillager.professions.passive.HarvestWork;
import com.smartvillager.professions.passive.MappingWork;
import com.smartvillager.professions.passive.PassiveLogic;
import com.smartvillager.professions.passive.RepairWork;
import com.smartvillager.professions.passive.SalvageWork;
import com.smartvillager.professions.quest.QuestEntry;
import com.smartvillager.professions.quest.QuestLogic;
import com.smartvillager.professions.runesmith.RunesmithBlocks;
import com.smartvillager.professions.goldsmith.GoldsmithBlocks;
import com.smartvillager.professions.trade.ExplorerMap;
import com.smartvillager.professions.trade.VillagerStatueOutput;
import com.smartvillager.professions.veteran.MobWeapons;
import com.smartvillager.professions.veteran.VeteranBlocks;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * What Professions plugs into the core (api/ExtensionHooks): its stations, its three sections and the
 * remaining hooks (mob weapons, XP-priced bottles, explorer maps). Together with the entries in
 * data/smartvillager_professions/smartvillager_progression/exchange/ this is what makes a profession "ours" - one with its own
 * entries or stations no longer runs on its Vanilla trades.
 *
 * - Quests: rotating easy/hard slots and a permanent quest (quest/QuestLogic), at the Vanilla job site like the basic trades (2026-10-11 - without it a villager had quests with no workplace), no meter shown.
 * - Masteries: the entries of the master station, which also is where the villager starts its work day.
 * - Passive: the badge at the top left; the work at the passive station (passive/PassiveLogic).
 * The basic trades are the core's own Trades section (at the Vanilla job site).
 */
public final class ProfessionsSections {

    public static final Identifier MASTER_STATION = id("master_station");
    public static final Identifier PASSIVE_STATION = id("passive_station");
    /** Morning order (decided 2026-09-24): master station first, then the Vanilla job site, then the passive station. */
    private static final int MASTER_STATION_MORNING_PRIORITY = 10;
    private static final int PASSIVE_STATION_MORNING_PRIORITY = -10;

    public static final SectionDefinition QUESTS = SectionDefinition.builder(id("quests"))
            .order(0)
            .station(StationDefinition.JOB_SITE)
            .hiddenMeter()
            .upgradeCosts(CoreSections.UPGRADE_COSTS, CoreSections.FULL_UNLOCK_RANK)
            .logic(new QuestLogic())
            .build();

    /** 7 ranks, ranks 1-4 unlock the four Masteries (2026-09-26). */
    public static final SectionDefinition MASTERIES = SectionDefinition.builder(id("masteries"))
            .order(2)
            .station(MASTER_STATION)
            .meter(id("master_productivity"))
            .upgradeCosts(List.of(2, 2, 2, 3, 3, 3, 5), 4)
            .build();

    public static final SectionDefinition PASSIVE = SectionDefinition.builder(id("passive"))
            .order(3)
            .badge()
            .station(PASSIVE_STATION)
            .meter(id("passive_productivity"))
            .upgradeCosts(List.of(3, 3, 3, 3, 3, 5), 3)
            .logic(new PassiveLogic(Map.of(
                    VillagerProfession.LIBRARIAN, new BookUpgradeWork(),
                    VillagerProfession.MASON, new CrushingWork(),
                    VillagerProfession.TOOLSMITH, new RepairWork(),
                    VillagerProfession.ARMORER, new SalvageWork(),
                    VillagerProfession.FARMER, new HarvestWork(),
                    VillagerProfession.FISHERMAN, new FishingWork(),
                    VillagerProfession.CARTOGRAPHER, new MappingWork(MapPool.Kind.ADVENTURE, new int[]{3, 3, 2, 2, 2, 1, 1}),
                    VillagerProfession.WEAPONSMITH, new MappingWork(MapPool.Kind.CHALLENGE, new int[]{6, 5, 4, 4, 3, 3, 2}))))
            .build();

    private ProfessionsSections() {
    }

    static void register() {
        masterStation(VillagerProfession.LIBRARIAN, PoiTypes.LIBRARIAN, LibrarianBlocks.WRITING_STATION);
        masterStation(VillagerProfession.MASON, PoiTypes.MASON, MasonBlocks.METAMORPH_STATION);
        masterStation(VillagerProfession.TOOLSMITH, PoiTypes.TOOLSMITH, RunesmithBlocks.UPGRADE_STATION);
        masterStation(VillagerProfession.WEAPONSMITH, PoiTypes.WEAPONSMITH, VeteranBlocks.TRAPPINGS_STATION);
        masterStation(VillagerProfession.CARTOGRAPHER, PoiTypes.CARTOGRAPHER, CartographerBlocks.MAP_STATION);
        masterStation(VillagerProfession.ARMORER, PoiTypes.ARMORER, GoldsmithBlocks.GILDING_STATION);
        masterStation(VillagerProfession.FARMER, PoiTypes.FARMER, FarmerBlocks.GLAZING_STATION);
        masterStation(VillagerProfession.FISHERMAN, PoiTypes.FISHERMAN, FishermanBlocks.SHIPYARD_STATION);

        passiveStation(VillagerProfession.LIBRARIAN, PoiTypes.LIBRARIAN, LibrarianBlocks.ENCHANTMENT_STATION);
        passiveStation(VillagerProfession.MASON, PoiTypes.MASON, MasonBlocks.CRUSHING_STATION);
        passiveStation(VillagerProfession.TOOLSMITH, PoiTypes.TOOLSMITH, RunesmithBlocks.REPAIR_STATION);
        passiveStation(VillagerProfession.ARMORER, PoiTypes.ARMORER, GoldsmithBlocks.SALVAGE_STATION);
        passiveStation(VillagerProfession.FARMER, PoiTypes.FARMER, FarmerBlocks.HARVEST_BASKET_STATION);
        passiveStation(VillagerProfession.FISHERMAN, PoiTypes.FISHERMAN, FishermanBlocks.FISH_BASKET_STATION);
        passiveStation(VillagerProfession.CARTOGRAPHER, PoiTypes.CARTOGRAPHER, MappingBlocks.ADVENTURE_STATION);
        passiveStation(VillagerProfession.WEAPONSMITH, PoiTypes.WEAPONSMITH, MappingBlocks.CHALLENGE_STATION);

        ExtensionHooks.registerSection(QUESTS);
        ExtensionHooks.registerSection(MASTERIES);
        ExtensionHooks.registerSection(PASSIVE);

        ExtensionHooks.registerExchangeExtension(QuestEntry.TYPE);
        ExtensionHooks.registerExchangeExtension(ExplorerMap.TYPE);
        ExtensionHooks.registerExchangeExtension(VillagerStatueOutput.TYPE);

        ExtensionHooks.registerHappinessElement(VillagerStatueHappiness.ELEMENT);

        ExtensionHooks.setMobWeaponComponent(MobWeapons.MOB_WEAPON);
        ExtensionHooks.registerStayPut(FishingWork::isHeld);
        ExtensionHooks.setPlayerXpCost(ExperienceBottles::playerXpCost);
    }

    private static void masterStation(ResourceKey<VillagerProfession> profession, ResourceKey<PoiType> jobSite, Supplier<? extends Block> block) {
        ExtensionHooks.registerStation(new StationDefinition(MASTER_STATION, profession, jobSite, block, MASTER_STATION_MORNING_PRIORITY));
    }

    private static void passiveStation(ResourceKey<VillagerProfession> profession, ResourceKey<PoiType> jobSite, Supplier<? extends Block> block) {
        ExtensionHooks.registerStation(new StationDefinition(PASSIVE_STATION, profession, jobSite, block, PASSIVE_STATION_MORNING_PRIORITY));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(SmartVillagerProfessionsMod.MODID, path);
    }
}
