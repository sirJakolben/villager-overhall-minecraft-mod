package com.smartvillager.progress;

import com.mojang.logging.LogUtils;
import com.smartvillager.base.api.SmartVillagerApi;
import com.smartvillager.progress.menu.ModMenuTypes;
import com.smartvillager.progress.section.CoreSections;
import com.smartvillager.progress.state.ModAttachments;
import com.smartvillager.progress.station.StationClaimHandler;
import com.smartvillager.progress.station.StationFocus;
import com.smartvillager.progress.trade.MissingTrade;
import com.smartvillager.progress.trade.RequiredEnchantmentCost;
import com.smartvillager.progress.trade.TagCost;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(SmartVillagerProgressMod.MODID)
public class SmartVillagerProgressMod {

    public static final String MODID = "smartvillager_progress";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SmartVillagerProgressMod(IEventBus modEventBus, ModContainer modContainer) {
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        RequiredEnchantmentCost.DATA_COMPONENTS.register(modEventBus);
        TagCost.DATA_COMPONENTS.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        MissingTrade.ITEMS.register(modEventBus);

        CoreSections.register();

        // Smart Villager (the rung below) pauses Vanilla's place-bound behaviors and runs the emerald claim; we tell it about stations.
        SmartVillagerApi.registerStayPutWhileWorking(villager -> StationFocus.awayStation(villager).isPresent());
        SmartVillagerApi.registerClaimHandler(new StationClaimHandler());
    }
}
