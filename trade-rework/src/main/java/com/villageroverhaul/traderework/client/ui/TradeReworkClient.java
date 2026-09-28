package com.villageroverhaul.traderework.client.ui;

import com.villageroverhaul.client.ui.SectionClientLogic;
import com.villageroverhaul.traderework.TradeReworkMod;
import com.villageroverhaul.traderework.TradeReworkSections;
import com.villageroverhaul.traderework.client.render.MoltenBubbleParticle;
import com.villageroverhaul.traderework.salvager.SalvagerParticles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/** Client-only registrations: the station screens, the molten bubble particle and how the quest rows look in the trade screen. */
@EventBusSubscriber(value = Dist.CLIENT, modid = TradeReworkMod.MODID)
public final class TradeReworkClient {

    private TradeReworkClient() {
    }

    @SubscribeEvent
    static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(TradeReworkMenus.ENCHANTMENT_STATION_MENU.get(), EnchantmentStationScreen::new);
        event.register(TradeReworkMenus.CRUSHING_STATION_MENU.get(), ThreeInThreeOutScreen::new);
        event.register(TradeReworkMenus.REPAIR_STATION_MENU.get(), ThreeInThreeOutScreen::new);
        event.register(TradeReworkMenus.SMELTING_STATION_MENU.get(), ThreeInThreeOutScreen::new);
    }

    @SubscribeEvent
    static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(SalvagerParticles.MOLTEN_BUBBLE.get(), MoltenBubbleParticle::provider);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        SectionClientLogic.register(TradeReworkSections.QUESTS.id(), new QuestClientLogic());
    }
}
