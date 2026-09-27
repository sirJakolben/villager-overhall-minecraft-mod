package com.villageroverhaul.network;

import com.villageroverhaul.VillagerOverhaulMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = VillagerOverhaulMod.MODID)
public final class ModPayloads {

    private ModPayloads() {
    }

    @SubscribeEvent
    static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(SelectOfferPayload.TYPE, SelectOfferPayload.STREAM_CODEC, SelectOfferPayload::handle);
        registrar.playToClient(VillagerOffersPayload.TYPE, VillagerOffersPayload.STREAM_CODEC, VillagerOffersPayload::handle);
        registrar.playToServer(RerollQuestPayload.TYPE, RerollQuestPayload.STREAM_CODEC, RerollQuestPayload::handle);
        registrar.playToServer(InvestUpgradePointPayload.TYPE, InvestUpgradePointPayload.STREAM_CODEC, InvestUpgradePointPayload::handle);
        registrar.playToServer(CloneOfferItemPayload.TYPE, CloneOfferItemPayload.STREAM_CODEC, CloneOfferItemPayload::handle);
    }
}
