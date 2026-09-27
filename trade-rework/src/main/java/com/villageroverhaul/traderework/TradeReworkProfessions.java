package com.villageroverhaul.traderework;

import com.villageroverhaul.api.ExtensionHooks;
import com.villageroverhaul.claim.ProfessionStations;
import com.villageroverhaul.core.state.VillagerState;
import com.villageroverhaul.traderework.cartographer.CartographerBlocks;
import com.villageroverhaul.traderework.librarian.ExperienceBottles;
import com.villageroverhaul.traderework.librarian.LibrarianBlocks;
import com.villageroverhaul.traderework.mason.MasonBlocks;
import com.villageroverhaul.traderework.passive.BookUpgradeWork;
import com.villageroverhaul.traderework.passive.CrushingWork;
import com.villageroverhaul.traderework.passive.RepairWork;
import com.villageroverhaul.traderework.passive.SmeltingWork;
import com.villageroverhaul.traderework.runesmith.RunesmithBlocks;
import com.villageroverhaul.traderework.salvager.SalvagerBlocks;
import com.villageroverhaul.traderework.trade.ExplorerMaps;
import com.villageroverhaul.traderework.veteran.MobWeapons;
import com.villageroverhaul.traderework.veteran.VeteranBlocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

import java.util.function.IntUnaryOperator;

/**
 * Everything that makes a Vanilla profession "ours" on the core's side (api/ExtensionHooks): its master and
 * passive stations, its passive work, and the special cases (Veteran's second hard quest slot, mob weapons,
 * XP-priced bottles, explorer maps). Together with the trade/quest files in data/vo_trade_rework/villageroverhaul/
 * this lifts a profession off the core's non-implementation rule.
 */
public final class TradeReworkProfessions {

    @FunctionalInterface
    private interface PendingCheck {
        boolean test(ServerLevel level, Villager villager, VillagerState state);
    }

    @FunctionalInterface
    private interface PassiveStep {
        VillagerState apply(ServerLevel level, Villager villager, VillagerState state, boolean workTime);
    }

    private record Passive(PendingCheck pending, PassiveStep step, IntUnaryOperator steps) implements ExtensionHooks.PassiveHandler {
        @Override
        public boolean isPending(ServerLevel level, Villager villager, VillagerState state) {
            return pending.test(level, villager, state);
        }

        @Override
        public VillagerState apply(ServerLevel level, Villager villager, VillagerState state, boolean workTime) {
            return step.apply(level, villager, state, workTime);
        }

        @Override
        public int stepsPerDay(int passiveRank) {
            return steps.applyAsInt(passiveRank);
        }
    }

    private TradeReworkProfessions() {
    }

    static void register() {
        ExtensionHooks.registerStations(new ProfessionStations.Entry(VillagerProfession.LIBRARIAN, PoiTypes.LIBRARIAN, LibrarianBlocks.WRITING_STATION, LibrarianBlocks.ENCHANTMENT_STATION));
        ExtensionHooks.registerStations(new ProfessionStations.Entry(VillagerProfession.MASON, PoiTypes.MASON, MasonBlocks.METAMORPH_STATION, MasonBlocks.CRUSHING_STATION));
        ExtensionHooks.registerStations(new ProfessionStations.Entry(VillagerProfession.TOOLSMITH, PoiTypes.TOOLSMITH, RunesmithBlocks.UPGRADE_STATION, RunesmithBlocks.REPAIR_STATION));
        ExtensionHooks.registerStations(new ProfessionStations.Entry(VillagerProfession.WEAPONSMITH, PoiTypes.WEAPONSMITH, VeteranBlocks.CHALLENGE_STATION, ProfessionStations.Entry.NO_STATION));
        ExtensionHooks.registerStations(new ProfessionStations.Entry(VillagerProfession.CARTOGRAPHER, PoiTypes.CARTOGRAPHER, CartographerBlocks.EXPLORE_STATION, ProfessionStations.Entry.NO_STATION));
        ExtensionHooks.registerStations(new ProfessionStations.Entry(VillagerProfession.ARMORER, PoiTypes.ARMORER, SalvagerBlocks.REFINEMENT_STATION, SalvagerBlocks.SMELTING_STATION));

        ExtensionHooks.registerPassive(VillagerProfession.LIBRARIAN, new Passive(BookUpgradeWork::isPending, BookUpgradeWork::apply, BookUpgradeWork::upgradesPerDay));
        ExtensionHooks.registerPassive(VillagerProfession.MASON, new Passive(CrushingWork::isPending, CrushingWork::apply, CrushingWork::batchesPerDay));
        ExtensionHooks.registerPassive(VillagerProfession.TOOLSMITH, new Passive(RepairWork::isPending, RepairWork::apply, RepairWork::stepsPerDay));
        ExtensionHooks.registerPassive(VillagerProfession.ARMORER, new Passive(SmeltingWork::isPending, SmeltingWork::apply, SmeltingWork::stepsPerDay));

        ExtensionHooks.registerSecondHardSlot(VillagerProfession.WEAPONSMITH);
        ExtensionHooks.setMobWeaponComponent(MobWeapons.MOB_WEAPON);
        ExtensionHooks.setPlayerXpCost(ExperienceBottles::playerXpCost);
        ExtensionHooks.setExplorerMaps(ExplorerMaps::outputFor);
    }
}
