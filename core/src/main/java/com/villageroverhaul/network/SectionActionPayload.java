package com.villageroverhaul.network;

import com.villageroverhaul.VillagerOverhaulMod;
import com.villageroverhaul.menu.VillagerMenu;
import com.villageroverhaul.section.Sections;
import com.villageroverhaul.section.VillagerSections;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client asks the server to run a row action of a section (a button next to a row, e.g. a quest reroll) - one
 * payload for every section, so an extension needs no packets of its own. slot is the row's slot (api/SectionOffer),
 * action is up to the section's logic (SectionLogic.onAction). Only while the player has the villager's menu open,
 * and only for a section that villager has.
 */
public record SectionActionPayload(Identifier section, int slot, int action) implements CustomPacketPayload {

    public static final Type<SectionActionPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(VillagerOverhaulMod.MODID, "section_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SectionActionPayload> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, SectionActionPayload::section,
            ByteBufCodecs.VAR_INT, SectionActionPayload::slot,
            ByteBufCodecs.VAR_INT, SectionActionPayload::action,
            SectionActionPayload::new
    );

    @Override
    public Type<SectionActionPayload> type() {
        return TYPE;
    }

    public static void handle(SectionActionPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof VillagerMenu menu) {
            Sections.get(payload.section())
                    .filter(VillagerSections.of(menu.villager())::contains)
                    .ifPresent(section -> section.logic().onAction(menu.villager(), player, section, payload.slot(), payload.action()));
        }
    }
}
