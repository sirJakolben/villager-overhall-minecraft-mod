package com.villageroverhaul.network;

import com.villageroverhaul.VillagerOverhaulMod;
import com.villageroverhaul.menu.VillagerMenu;
import com.villageroverhaul.section.SectionView;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Server sends the villager's current offer list to the trading player - our counterpart of Vanilla's
 * ClientboundMerchantOffersPacket, whose client handler only accepts a MerchantMenu. Also carries the open
 * sections in order (the offer list holds their rows one section after another, see VillagerOffers) and, per
 * row, the slot its section's logic gave it (api/SectionOffer - e.g. which quest slot a reroll button rerolls).
 */
public record VillagerOffersPayload(int containerId, MerchantOffers offers, List<SectionView> sections, List<Integer> rowSlots)
        implements CustomPacketPayload {

    public static final Type<VillagerOffersPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(VillagerOverhaulMod.MODID, "villager_offers"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VillagerOffersPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VillagerOffersPayload::containerId,
            MerchantOffers.STREAM_CODEC, VillagerOffersPayload::offers,
            SectionView.STREAM_CODEC.apply(ByteBufCodecs.list()), VillagerOffersPayload::sections,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), VillagerOffersPayload::rowSlots,
            VillagerOffersPayload::new
    );

    @Override
    public Type<VillagerOffersPayload> type() {
        return TYPE;
    }

    public static void handle(VillagerOffersPayload payload, IPayloadContext context) {
        if (context.player().containerMenu instanceof VillagerMenu menu && menu.containerId == payload.containerId()) {
            menu.setOffers(payload.offers(), payload.sections(), payload.rowSlots());
        }
    }
}
