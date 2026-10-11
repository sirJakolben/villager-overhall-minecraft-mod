package com.smartvillager.professions;

import com.mojang.logging.LogUtils;
import com.smartvillager.professions.cage.Cages;
import com.smartvillager.professions.cavemap.CaveMapItems;
import com.smartvillager.professions.cage.test.CageGameTests;
import com.smartvillager.professions.cage.ZombieCaptives;
import com.smartvillager.professions.cartographer.CartographerBlocks;
import com.smartvillager.professions.cartographer.CartographerItems;
import com.smartvillager.professions.basket.BasketBlockEntities;
import com.smartvillager.professions.client.ui.ProfessionsMenus;
import com.smartvillager.professions.farmer.FarmerBlocks;
import com.smartvillager.professions.farmer.ScarecrowBlock;
import com.smartvillager.professions.farmer.FarmerItems;
import com.smartvillager.professions.fisherman.BiomeFishModifier;
import com.smartvillager.professions.fisherman.FishermanBlocks;
import com.smartvillager.professions.fisherman.FishermanEntities;
import com.smartvillager.professions.fisherman.FishermanItems;
import com.smartvillager.professions.passive.PassiveCounters;
import com.smartvillager.professions.librarian.LibrarianBlockEntities;
import com.smartvillager.professions.librarian.LibrarianBlocks;
import com.smartvillager.professions.librarian.LibrarianEntities;
import com.smartvillager.professions.librarian.LibrarianItems;
import com.smartvillager.professions.librarian.LoreScrollItem;
import com.smartvillager.professions.mapping.MappingBlocks;
import com.smartvillager.professions.mapping.test.MapGameTests;
import com.smartvillager.professions.mason.MasonBlockEntities;
import com.smartvillager.professions.mason.MasonBlocks;
import com.smartvillager.professions.mason.MasonItems;
import com.smartvillager.professions.mason.VillagerStatueHappiness;
import com.smartvillager.professions.quest.QuestState;
import com.smartvillager.professions.runesmith.BonusDurability;
import com.smartvillager.professions.runesmith.RunesmithBlockEntities;
import com.smartvillager.professions.runesmith.RunesmithBlocks;
import com.smartvillager.professions.runesmith.RunesmithItems;
import com.smartvillager.professions.goldsmith.GoldsmithBlockEntities;
import com.smartvillager.professions.goldsmith.GoldsmithBlocks;
import com.smartvillager.professions.goldsmith.GoldsmithParticles;
import com.smartvillager.professions.trade.ExplorerMaps;
import com.smartvillager.professions.veteran.MobWeapons;
import com.smartvillager.professions.veteran.VeteranBlockEntities;
import com.smartvillager.professions.veteran.VeteranBlocks;
import com.smartvillager.professions.veteran.VeteranItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Smart Villager Professions (split off the core 2026-09-28) - the reworked professions with their own
 * trades, quests, masteries and passives. Registers its content, then plugs the professions into the
 * core (ProfessionsSections). Loads after the core (neoforge.mods.toml).
 */
@Mod(SmartVillagerProfessionsMod.MODID)
public class SmartVillagerProfessionsMod {

    public static final String MODID = "smartvillager_professions";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SmartVillagerProfessionsMod(IEventBus modEventBus, ModContainer modContainer) {
        ExplorerMaps.ATTACHMENT_TYPES.register(modEventBus);
        QuestState.ATTACHMENT_TYPES.register(modEventBus);
        PassiveCounters.ATTACHMENT_TYPES.register(modEventBus);
        LoreScrollItem.DATA_COMPONENTS.register(modEventBus);
        MobWeapons.DATA_COMPONENTS.register(modEventBus);
        BonusDurability.DATA_COMPONENTS.register(modEventBus);
        ProfessionsMenus.MENU_TYPES.register(modEventBus);
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
        ZombieCaptives.ATTACHMENT_TYPES.register(modEventBus);
        Cages.register(modEventBus);
        CageGameTests.register(modEventBus);
        CartographerBlocks.BLOCKS.register(modEventBus);
        CartographerItems.ITEMS.register(modEventBus);
        MappingBlocks.register(modEventBus);
        CaveMapItems.register(modEventBus);
        MapGameTests.register(modEventBus);
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

        ProfessionsSections.register();
    }
}
