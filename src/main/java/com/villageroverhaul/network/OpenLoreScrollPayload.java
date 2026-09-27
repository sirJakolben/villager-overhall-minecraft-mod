package com.villageroverhaul.network;

import com.villageroverhaul.VillagerOverhaulMod;
import com.villageroverhaul.client.ui.LoreScrollScreens;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server tells the reading player's client to open a lore scroll's book window - sent after the server
 * fixed which fragment the scroll shows (LoreScrollItem.use), so the first read already has it.
 */
public record OpenLoreScrollPayload(String origin, int tier, int fragment) implements CustomPacketPayload {

    public static final Type<OpenLoreScrollPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(VillagerOverhaulMod.MODID, "open_lore_scroll"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenLoreScrollPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, OpenLoreScrollPayload::origin,
            ByteBufCodecs.VAR_INT, OpenLoreScrollPayload::tier,
            ByteBufCodecs.VAR_INT, OpenLoreScrollPayload::fragment,
            OpenLoreScrollPayload::new
    );

    @Override
    public Type<OpenLoreScrollPayload> type() {
        return TYPE;
    }

    /** Client only (registered playToClient) - LoreScrollScreens is never touched on a dedicated server. */
    public static void handle(OpenLoreScrollPayload payload, IPayloadContext context) {
        LoreScrollScreens.open(payload.origin(), payload.tier(), payload.fragment());
    }
}
