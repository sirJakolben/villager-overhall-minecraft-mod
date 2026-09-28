package com.villageroverhaul.network;

import com.villageroverhaul.VillagerOverhaulMod;
import com.villageroverhaul.menu.VillagerMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client selected an offer row (index into the full offer list) - our counterpart of Vanilla's
 * ServerboundSelectTradePacket, whose server handler only accepts a MerchantMenu. The client has
 * already run the same two calls locally for prediction; the server repeats them authoritatively.
 */
public record SelectOfferPayload(int index) implements CustomPacketPayload {

    public static final Type<SelectOfferPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(VillagerOverhaulMod.MODID, "select_offer"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectOfferPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SelectOfferPayload::index,
            SelectOfferPayload::new
    );

    @Override
    public Type<SelectOfferPayload> type() {
        return TYPE;
    }

    public static void handle(SelectOfferPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof VillagerMenu menu) {
            if (!menu.stillValid(player)) {
                return;
            }
            menu.setSelectionHint(payload.index());
            menu.tryMoveItems(payload.index());
        }
    }
}
