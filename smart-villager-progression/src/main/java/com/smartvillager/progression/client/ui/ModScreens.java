package com.smartvillager.progression.client.ui;

import com.smartvillager.progression.SmartVillagerProgressionMod;
import com.smartvillager.progression.menu.ModMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = SmartVillagerProgressionMod.MODID)
public final class ModScreens {

    private ModScreens() {
    }

    @SubscribeEvent
    static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.VILLAGER_MENU.get(), VillagerScreen::new);
    }
}
