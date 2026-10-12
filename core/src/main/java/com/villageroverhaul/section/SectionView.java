package com.villageroverhaul.section;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * What the trade screen needs to know about one open section, computed on the server (only it knows the Vanilla
 * catalog and the section logic) and sent with the offers (network/VillagerOffersPayload), in section order.
 * rows: how many offers of the offer list belong to it (a BADGE section has none); nextUpgradeCost: the price of
 * its next rank, -1 when maxed; meterPoints: the size of its meter, 0 when no meter is shown (hidden, or its
 * station isn't owned).
 */
public record SectionView(Identifier id, int rows, int nextUpgradeCost, int meterPoints) {

    public static final StreamCodec<ByteBuf, SectionView> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, SectionView::id,
            ByteBufCodecs.VAR_INT, SectionView::rows,
            ByteBufCodecs.VAR_INT, SectionView::nextUpgradeCost,
            ByteBufCodecs.VAR_INT, SectionView::meterPoints,
            SectionView::new
    );

    public boolean showsMeter() {
        return meterPoints > 0;
    }
}
