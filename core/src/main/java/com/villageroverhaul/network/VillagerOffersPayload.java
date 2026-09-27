package com.villageroverhaul.network;

import com.villageroverhaul.VillagerOverhaulMod;
import com.villageroverhaul.client.ui.VillagerMenu;
import com.villageroverhaul.fallback.RankCaps;
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
 * ClientboundMerchantOffersPacket, whose client handler only accepts a MerchantMenu. Also carries the
 * section sizes (quests first, then basic trades, the rest master trades) - see VillagerOffers - and,
 * per quest row, its quest slot (what the row's reroll button rerolls; hidden slots leave gaps), and the rank caps
 * of a fallback profession (fallback/RankCaps - only the server can roll its catalog).
 */
public record VillagerOffersPayload(int containerId, MerchantOffers offers, List<Integer> questSlots, int basicCount, RankCaps rankCaps) implements CustomPacketPayload {

    public static final Type<VillagerOffersPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(VillagerOverhaulMod.MODID, "villager_offers"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VillagerOffersPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VillagerOffersPayload::containerId,
            MerchantOffers.STREAM_CODEC, VillagerOffersPayload::offers,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), VillagerOffersPayload::questSlots,
            ByteBufCodecs.VAR_INT, VillagerOffersPayload::basicCount,
            RankCaps.STREAM_CODEC, VillagerOffersPayload::rankCaps,
            VillagerOffersPayload::new
    );

    @Override
    public Type<VillagerOffersPayload> type() {
        return TYPE;
    }

    public static void handle(VillagerOffersPayload payload, IPayloadContext context) {
        if (context.player().containerMenu instanceof VillagerMenu menu && menu.containerId == payload.containerId()) {
            menu.setOffers(payload.offers(), payload.questSlots(), payload.basicCount(), payload.rankCaps());
        }
    }
}
