package com.smartvillager.professions.client.ui;

import com.smartvillager.progression.client.ui.SectionClientLogic;
import com.smartvillager.professions.SmartVillagerProfessionsMod;
import com.smartvillager.professions.ProfessionsSections;
import com.smartvillager.professions.client.render.MoltenBubbleParticle;
import com.smartvillager.professions.goldsmith.GoldsmithParticles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/** Client-only registrations: the station screens, the molten bubble particle and how the quest rows look in the trade screen. */
@EventBusSubscriber(value = Dist.CLIENT, modid = SmartVillagerProfessionsMod.MODID)
public final class ProfessionsClient {

    private ProfessionsClient() {
    }

    @SubscribeEvent
    static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ProfessionsMenus.ENCHANTMENT_STATION_MENU.get(), EnchantmentStationScreen::new);
        event.register(ProfessionsMenus.CRUSHING_STATION_MENU.get(), ThreeInThreeOutScreen::new);
        event.register(ProfessionsMenus.REPAIR_STATION_MENU.get(), ThreeInThreeOutScreen::new);
        event.register(ProfessionsMenus.SALVAGE_STATION_MENU.get(), ThreeInThreeOutScreen::new);
        event.register(ProfessionsMenus.ADVENTURE_STATION_MENU.get(), ThreeInThreeOutScreen::new);
        event.register(ProfessionsMenus.CHALLENGE_STATION_MENU.get(), ThreeInThreeOutScreen::new);
    }

    @SubscribeEvent
    static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(GoldsmithParticles.MOLTEN_BUBBLE.get(), MoltenBubbleParticle::provider);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        SectionClientLogic.register(ProfessionsSections.QUESTS.id(), new QuestClientLogic());
    }
}
