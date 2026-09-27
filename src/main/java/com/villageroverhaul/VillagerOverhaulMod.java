package com.villageroverhaul;

import com.mojang.logging.LogUtils;
import com.villageroverhaul.client.ui.ModMenuTypes;
import com.villageroverhaul.core.ModAttachments;
import com.villageroverhaul.librarian.LibrarianEntities;
import com.villageroverhaul.cartographer.CartographerBlocks;
import com.villageroverhaul.cartographer.CartographerItems;
import com.villageroverhaul.claim.ClaimBlocks;
import com.villageroverhaul.librarian.LibrarianBlockEntities;
import com.villageroverhaul.librarian.LibrarianBlocks;
import com.villageroverhaul.librarian.LibrarianItems;
import com.villageroverhaul.librarian.LoreScrollItem;
import com.villageroverhaul.mason.MasonBlockEntities;
import com.villageroverhaul.mason.MasonBlocks;
import com.villageroverhaul.mason.MasonItems;
import com.villageroverhaul.runesmith.BonusDurability;
import com.villageroverhaul.runesmith.RunesmithBlockEntities;
import com.villageroverhaul.runesmith.RunesmithBlocks;
import com.villageroverhaul.runesmith.RunesmithItems;
import com.villageroverhaul.salvager.SalvagerBlockEntities;
import com.villageroverhaul.salvager.SalvagerBlocks;
import com.villageroverhaul.trade.ExplorerMaps;
import com.villageroverhaul.trade.MissingTrade;
import com.villageroverhaul.trade.RequiredEnchantmentCost;
import com.villageroverhaul.veteran.MobWeapons;
import com.villageroverhaul.veteran.VeteranBlockEntities;
import com.villageroverhaul.veteran.VeteranBlocks;
import com.villageroverhaul.veteran.VeteranItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(VillagerOverhaulMod.MODID)
public class VillagerOverhaulMod {

    public static final String MODID = "villageroverhaul";
    public static final Logger LOGGER = LogUtils.getLogger();

    public VillagerOverhaulMod(IEventBus modEventBus, ModContainer modContainer) {
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ExplorerMaps.ATTACHMENT_TYPES.register(modEventBus);
        RequiredEnchantmentCost.DATA_COMPONENTS.register(modEventBus);
        LoreScrollItem.DATA_COMPONENTS.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        LibrarianBlocks.BLOCKS.register(modEventBus);
        LibrarianBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        LibrarianItems.ITEMS.register(modEventBus);
        MasonBlocks.BLOCKS.register(modEventBus);
        MasonBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        MasonItems.ITEMS.register(modEventBus);
        MobWeapons.DATA_COMPONENTS.register(modEventBus);
        BonusDurability.DATA_COMPONENTS.register(modEventBus);
        RunesmithBlocks.BLOCKS.register(modEventBus);
        RunesmithBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        RunesmithItems.ITEMS.register(modEventBus);
        VeteranBlocks.BLOCKS.register(modEventBus);
        VeteranBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        VeteranItems.ITEMS.register(modEventBus);
        CartographerBlocks.BLOCKS.register(modEventBus);
        SalvagerBlocks.BLOCKS.register(modEventBus);
        SalvagerBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        SalvagerBlocks.ITEMS.register(modEventBus);
        MissingTrade.ITEMS.register(modEventBus);
        CartographerItems.ITEMS.register(modEventBus);
        ClaimBlocks.BLOCKS.register(modEventBus);
        ClaimBlocks.ITEMS.register(modEventBus);
        LibrarianEntities.ENTITY_TYPES.register(modEventBus);
    }
}
