package com.villageroverhaul.network;

import com.villageroverhaul.VillagerOverhaulMod;
import com.villageroverhaul.client.ui.VillagerMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client middle-clicked an item icon of an offer row in creative - the server repeats the clone the
 * client already predicted (VillagerMenu.cloneOfferItem); the carried stack then syncs back as usual.
 */
public record CloneOfferItemPayload(int index, VillagerMenu.OfferItem item) implements CustomPacketPayload {

    public static final Type<CloneOfferItemPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(VillagerOverhaulMod.MODID, "clone_offer_item"));

    private static final StreamCodec<RegistryFriendlyByteBuf, VillagerMenu.OfferItem> OFFER_ITEM_CODEC =
            ByteBufCodecs.VAR_INT.map(
                    ordinal -> VillagerMenu.OfferItem.values()[Math.floorMod(ordinal, VillagerMenu.OfferItem.values().length)],
                    Enum::ordinal
            ).cast();

    public static final StreamCodec<RegistryFriendlyByteBuf, CloneOfferItemPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CloneOfferItemPayload::index,
            OFFER_ITEM_CODEC, CloneOfferItemPayload::item,
            CloneOfferItemPayload::new
    );

    @Override
    public Type<CloneOfferItemPayload> type() {
        return TYPE;
    }

    public static void handle(CloneOfferItemPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof VillagerMenu menu && menu.stillValid(player)) {
            menu.cloneOfferItem(player, payload.index(), payload.item());
        }
    }
}
