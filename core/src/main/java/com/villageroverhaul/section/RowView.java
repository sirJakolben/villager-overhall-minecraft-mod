package com.villageroverhaul.section;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What the trade screen needs to know about one row beyond its MerchantOffer, sent with the offers
 * (network/VillagerOffersPayload), one per offer in list order. slot: the slot its section's logic gave it
 * (api/SectionOffer - e.g. which quest slot a reroll button rerolls). baseResultCount: the unscaled Base yield -
 * when the result gives more, the screen shows it struck through next to the real count (MerchantOffer only
 * knows a base price, not a base result).
 */
public record RowView(int slot, int baseResultCount) {

    public static final StreamCodec<ByteBuf, RowView> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RowView::slot,
            ByteBufCodecs.VAR_INT, RowView::baseResultCount,
            RowView::new
    );
}
