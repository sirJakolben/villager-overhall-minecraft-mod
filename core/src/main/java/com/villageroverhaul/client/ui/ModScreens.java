package com.villageroverhaul.client.ui;

import com.villageroverhaul.VillagerOverhaulMod;
import com.villageroverhaul.menu.ModMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = VillagerOverhaulMod.MODID)
public final class ModScreens {

    private ModScreens() {
    }

    @SubscribeEvent
    static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.VILLAGER_MENU.get(), VillagerScreen::new);
    }
}
