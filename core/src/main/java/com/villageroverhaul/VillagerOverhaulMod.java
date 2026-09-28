package com.villageroverhaul;

import com.mojang.logging.LogUtils;
import com.villageroverhaul.menu.ModMenuTypes;
import com.villageroverhaul.section.CoreSections;
import com.villageroverhaul.state.ModAttachments;
import com.villageroverhaul.station.TradingBlocks;
import com.villageroverhaul.trade.MissingTrade;
import com.villageroverhaul.trade.RequiredEnchantmentCost;
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
        RequiredEnchantmentCost.DATA_COMPONENTS.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        MissingTrade.ITEMS.register(modEventBus);
        TradingBlocks.BLOCKS.register(modEventBus);
        TradingBlocks.ITEMS.register(modEventBus);

        CoreSections.register();
    }
}
