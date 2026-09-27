package com.villageroverhaul.traderework.network;

import com.villageroverhaul.traderework.TradeReworkMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = TradeReworkMod.MODID)
public final class TradeReworkPayloads {

    private TradeReworkPayloads() {
    }

    @SubscribeEvent
    static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(OpenLoreScrollPayload.TYPE, OpenLoreScrollPayload.STREAM_CODEC, OpenLoreScrollPayload::handle);
    }
}
