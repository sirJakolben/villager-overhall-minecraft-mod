package com.villageroverhaul.traderework;

import com.mojang.logging.LogUtils;
import com.villageroverhaul.traderework.cartographer.CartographerBlocks;
import com.villageroverhaul.traderework.cartographer.CartographerItems;
import com.villageroverhaul.traderework.basket.BasketBlockEntities;
import com.villageroverhaul.traderework.client.ui.TradeReworkMenus;
import com.villageroverhaul.traderework.farmer.FarmerBlocks;
import com.villageroverhaul.traderework.farmer.ScarecrowBlock;
import com.villageroverhaul.traderework.farmer.FarmerItems;
import com.villageroverhaul.traderework.fisherman.BiomeFishModifier;
import com.villageroverhaul.traderework.fisherman.FishermanBlocks;
import com.villageroverhaul.traderework.fisherman.FishermanEntities;
import com.villageroverhaul.traderework.fisherman.FishermanItems;
import com.villageroverhaul.traderework.passive.PassiveCounters;
import com.villageroverhaul.traderework.librarian.LibrarianBlockEntities;
import com.villageroverhaul.traderework.librarian.LibrarianBlocks;
import com.villageroverhaul.traderework.librarian.LibrarianEntities;
import com.villageroverhaul.traderework.librarian.LibrarianItems;
import com.villageroverhaul.traderework.librarian.LoreScrollItem;
import com.villageroverhaul.traderework.mason.MasonBlockEntities;
import com.villageroverhaul.traderework.mason.MasonBlocks;
import com.villageroverhaul.traderework.mason.MasonItems;
import com.villageroverhaul.traderework.mason.VillagerStatueHappiness;
import com.villageroverhaul.traderework.quest.QuestState;
import com.villageroverhaul.traderework.runesmith.BonusDurability;
import com.villageroverhaul.traderework.runesmith.RunesmithBlockEntities;
import com.villageroverhaul.traderework.runesmith.RunesmithBlocks;
import com.villageroverhaul.traderework.runesmith.RunesmithItems;
import com.villageroverhaul.traderework.goldsmith.GoldsmithBlockEntities;
import com.villageroverhaul.traderework.goldsmith.GoldsmithBlocks;
import com.villageroverhaul.traderework.goldsmith.GoldsmithParticles;
import com.villageroverhaul.traderework.trade.ExplorerMaps;
import com.villageroverhaul.traderework.veteran.MobWeapons;
import com.villageroverhaul.traderework.veteran.VeteranBlockEntities;
import com.villageroverhaul.traderework.veteran.VeteranBlocks;
import com.villageroverhaul.traderework.veteran.VeteranItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Villager Overhaul: Trade Rework (split off the core 2026-09-28) - the reworked professions with their own
 * trades, quests, masteries and passives. Registers its content, then plugs the professions into the
 * core (TradeReworkSections). Loads after the core (neoforge.mods.toml).
 */
@Mod(TradeReworkMod.MODID)
public class TradeReworkMod {

    public static final String MODID = "vo_trade_rework";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TradeReworkMod(IEventBus modEventBus, ModContainer modContainer) {
        ExplorerMaps.ATTACHMENT_TYPES.register(modEventBus);
        QuestState.ATTACHMENT_TYPES.register(modEventBus);
        PassiveCounters.ATTACHMENT_TYPES.register(modEventBus);
        LoreScrollItem.DATA_COMPONENTS.register(modEventBus);
        MobWeapons.DATA_COMPONENTS.register(modEventBus);
        BonusDurability.DATA_COMPONENTS.register(modEventBus);
        TradeReworkMenus.MENU_TYPES.register(modEventBus);
        LibrarianBlocks.BLOCKS.register(modEventBus);
        LibrarianBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        LibrarianItems.ITEMS.register(modEventBus);
        LibrarianEntities.ENTITY_TYPES.register(modEventBus);
        MasonBlocks.BLOCKS.register(modEventBus);
        MasonBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        MasonItems.ITEMS.register(modEventBus);
        VillagerStatueHappiness.POI_TYPES.register(modEventBus);
        RunesmithBlocks.BLOCKS.register(modEventBus);
        RunesmithBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        RunesmithItems.ITEMS.register(modEventBus);
        VeteranBlocks.BLOCKS.register(modEventBus);
        VeteranBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        VeteranItems.ITEMS.register(modEventBus);
        CartographerBlocks.BLOCKS.register(modEventBus);
        CartographerItems.ITEMS.register(modEventBus);
        GoldsmithBlocks.BLOCKS.register(modEventBus);
        GoldsmithBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        GoldsmithBlocks.ITEMS.register(modEventBus);
        GoldsmithParticles.PARTICLE_TYPES.register(modEventBus);
        FarmerBlocks.BLOCKS.register(modEventBus);
        FarmerBlocks.ITEMS.register(modEventBus);
        ScarecrowBlock.POI_TYPES.register(modEventBus);
        FarmerItems.ITEMS.register(modEventBus);
        FarmerItems.MOB_EFFECTS.register(modEventBus);
        FishermanBlocks.BLOCKS.register(modEventBus);
        FishermanBlocks.ITEMS.register(modEventBus);
        FishermanItems.ITEMS.register(modEventBus);
        FishermanEntities.ENTITY_TYPES.register(modEventBus);
        BasketBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        BiomeFishModifier.LOOT_MODIFIERS.register(modEventBus);

        TradeReworkSections.register();
    }
}
