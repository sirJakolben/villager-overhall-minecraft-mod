package com.villageroverhaul.traderework.client.ui;

import com.villageroverhaul.traderework.TradeReworkMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = TradeReworkMod.MODID)
public final class TradeReworkScreens {

    private TradeReworkScreens() {
    }

    @SubscribeEvent
    static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(TradeReworkMenus.ENCHANTMENT_STATION_MENU.get(), EnchantmentStationScreen::new);
        event.register(TradeReworkMenus.CRUSHING_STATION_MENU.get(), ThreeInThreeOutScreen::new);
        event.register(TradeReworkMenus.REPAIR_STATION_MENU.get(), ThreeInThreeOutScreen::new);
        event.register(TradeReworkMenus.SMELTING_STATION_MENU.get(), ThreeInThreeOutScreen::new);
    }
}
