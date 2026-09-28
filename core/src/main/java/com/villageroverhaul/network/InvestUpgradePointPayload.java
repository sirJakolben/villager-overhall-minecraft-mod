package com.villageroverhaul.network;

import com.villageroverhaul.VillagerOverhaulMod;
import com.villageroverhaul.menu.VillagerMenu;
import com.villageroverhaul.progression.ProgressionService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client asks the server to spend upgrade points on a section's next rank - clicking its rank button. */
public record InvestUpgradePointPayload(Identifier section) implements CustomPacketPayload {

    public static final Type<InvestUpgradePointPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(VillagerOverhaulMod.MODID, "invest_upgrade_point"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InvestUpgradePointPayload> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, InvestUpgradePointPayload::section,
            InvestUpgradePointPayload::new
    );

    @Override
    public Type<InvestUpgradePointPayload> type() {
        return TYPE;
    }

    public static void handle(InvestUpgradePointPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof VillagerMenu menu) {
            ProgressionService.investPoint(menu.villager(), payload.section());
        }
    }
}
