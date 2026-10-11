package com.smartvillager.professions.network;

import com.smartvillager.professions.SmartVillagerProfessionsMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = SmartVillagerProfessionsMod.MODID)
public final class ProfessionsPayloads {

    private ProfessionsPayloads() {
    }

    @SubscribeEvent
    static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(OpenLoreScrollPayload.TYPE, OpenLoreScrollPayload.STREAM_CODEC, OpenLoreScrollPayload::handle);
    }
}
