package com.smartvillager.progression;

import com.mojang.logging.LogUtils;
import com.smartvillager.progression.interaction.TradingBlocks;
import com.smartvillager.progression.menu.ModMenuTypes;
import com.smartvillager.progression.section.CoreSections;
import com.smartvillager.progression.state.ModAttachments;
import com.smartvillager.progression.trade.MissingTrade;
import com.smartvillager.progression.trade.RequiredEnchantmentCost;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(SmartVillagerProgressionMod.MODID)
public class SmartVillagerProgressionMod {

    public static final String MODID = "smartvillager_progression";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SmartVillagerProgressionMod(IEventBus modEventBus, ModContainer modContainer) {
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        RequiredEnchantmentCost.DATA_COMPONENTS.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        MissingTrade.ITEMS.register(modEventBus);
        TradingBlocks.BLOCKS.register(modEventBus);
        TradingBlocks.ITEMS.register(modEventBus);

        CoreSections.register();
    }
}
