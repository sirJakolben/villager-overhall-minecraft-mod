package com.villageroverhaul.traderework;

import com.villageroverhaul.api.ExtensionHooks;
import com.villageroverhaul.api.SectionDefinition;
import com.villageroverhaul.api.StationDefinition;
import com.villageroverhaul.section.CoreSections;
import com.villageroverhaul.traderework.cartographer.CartographerBlocks;
import com.villageroverhaul.traderework.librarian.ExperienceBottles;
import com.villageroverhaul.traderework.librarian.LibrarianBlocks;
import com.villageroverhaul.traderework.mason.MasonBlocks;
import com.villageroverhaul.traderework.passive.BookUpgradeWork;
import com.villageroverhaul.traderework.passive.CrushingWork;
import com.villageroverhaul.traderework.passive.PassiveLogic;
import com.villageroverhaul.traderework.passive.RepairWork;
import com.villageroverhaul.traderework.passive.SmeltingWork;
import com.villageroverhaul.traderework.quest.QuestEntry;
import com.villageroverhaul.traderework.quest.QuestLogic;
import com.villageroverhaul.traderework.runesmith.RunesmithBlocks;
import com.villageroverhaul.traderework.salvager.SalvagerBlocks;
import com.villageroverhaul.traderework.trade.ExplorerMap;
import com.villageroverhaul.traderework.trade.VillagerStatueOutput;
import com.villageroverhaul.traderework.veteran.MobWeapons;
import com.villageroverhaul.traderework.veteran.VeteranBlocks;
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
 * What the Trade Rework plugs into the core (api/ExtensionHooks): its stations, its three sections and the
 * remaining hooks (mob weapons, XP-priced bottles, explorer maps). Together with the entries in
 * data/vo_trade_rework/villageroverhaul/exchange/ this is what makes a profession "ours" - one with its own
 * entries or stations no longer runs on its Vanilla trades.
 *
 * - Quests: rotating easy/hard slots and a permanent quest (quest/QuestLogic), no station, no meter shown.
 * - Masteries: the entries of the master station, which also is where the villager starts its work day.
 * - Passive: the badge at the top left; the work at the passive station (passive/PassiveLogic).
 * The basic trades are the core's own Trades section (at the Vanilla job site).
 */
public final class TradeReworkSections {

    public static final Identifier MASTER_STATION = id("master_station");
    public static final Identifier PASSIVE_STATION = id("passive_station");
    /** Morning order (decided 2026-09-24): master station first, then the Vanilla job site, then the passive station. */
    private static final int MASTER_STATION_MORNING_PRIORITY = 10;
    private static final int PASSIVE_STATION_MORNING_PRIORITY = -10;

    public static final SectionDefinition QUESTS = SectionDefinition.builder(id("quests"))
            .order(0)
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
                    VillagerProfession.ARMORER, new SmeltingWork())))
            .build();

    private TradeReworkSections() {
    }

    static void register() {
        masterStation(VillagerProfession.LIBRARIAN, PoiTypes.LIBRARIAN, LibrarianBlocks.WRITING_STATION);
        masterStation(VillagerProfession.MASON, PoiTypes.MASON, MasonBlocks.METAMORPH_STATION);
        masterStation(VillagerProfession.TOOLSMITH, PoiTypes.TOOLSMITH, RunesmithBlocks.UPGRADE_STATION);
        masterStation(VillagerProfession.WEAPONSMITH, PoiTypes.WEAPONSMITH, VeteranBlocks.CHALLENGE_STATION);
        masterStation(VillagerProfession.CARTOGRAPHER, PoiTypes.CARTOGRAPHER, CartographerBlocks.EXPLORE_STATION);
        masterStation(VillagerProfession.ARMORER, PoiTypes.ARMORER, SalvagerBlocks.REFINEMENT_STATION);

        passiveStation(VillagerProfession.LIBRARIAN, PoiTypes.LIBRARIAN, LibrarianBlocks.ENCHANTMENT_STATION);
        passiveStation(VillagerProfession.MASON, PoiTypes.MASON, MasonBlocks.CRUSHING_STATION);
        passiveStation(VillagerProfession.TOOLSMITH, PoiTypes.TOOLSMITH, RunesmithBlocks.REPAIR_STATION);
        passiveStation(VillagerProfession.ARMORER, PoiTypes.ARMORER, SalvagerBlocks.SMELTING_STATION);

        ExtensionHooks.registerSection(QUESTS);
        ExtensionHooks.registerSection(MASTERIES);
        ExtensionHooks.registerSection(PASSIVE);

        ExtensionHooks.registerExchangeExtension(QuestEntry.TYPE);
        ExtensionHooks.registerExchangeExtension(ExplorerMap.TYPE);
        ExtensionHooks.registerExchangeExtension(VillagerStatueOutput.TYPE);

        ExtensionHooks.setMobWeaponComponent(MobWeapons.MOB_WEAPON);
        ExtensionHooks.setPlayerXpCost(ExperienceBottles::playerXpCost);
    }

    private static void masterStation(ResourceKey<VillagerProfession> profession, ResourceKey<PoiType> jobSite, Supplier<? extends Block> block) {
        ExtensionHooks.registerStation(new StationDefinition(MASTER_STATION, profession, jobSite, block, MASTER_STATION_MORNING_PRIORITY));
    }

    private static void passiveStation(ResourceKey<VillagerProfession> profession, ResourceKey<PoiType> jobSite, Supplier<? extends Block> block) {
        ExtensionHooks.registerStation(new StationDefinition(PASSIVE_STATION, profession, jobSite, block, PASSIVE_STATION_MORNING_PRIORITY));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(TradeReworkMod.MODID, path);
    }
}
