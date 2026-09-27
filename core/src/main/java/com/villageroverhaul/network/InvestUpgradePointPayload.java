package com.villageroverhaul.network;

import com.villageroverhaul.VillagerOverhaulMod;
import com.villageroverhaul.client.ui.VillagerMenu;
import com.villageroverhaul.core.VillagerStateAccess;
import com.villageroverhaul.progression.ProgressionService;
import com.villageroverhaul.progression.UpgradeGroup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client asks the server to spend one unspent upgrade point into a group - clicking a trade_level_button. */
public record InvestUpgradePointPayload(UpgradeGroup group) implements CustomPacketPayload {

    public static final Type<InvestUpgradePointPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(VillagerOverhaulMod.MODID, "invest_upgrade_point"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InvestUpgradePointPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(ordinal -> UpgradeGroup.values()[ordinal], UpgradeGroup::ordinal),
            InvestUpgradePointPayload::group,
            InvestUpgradePointPayload::new
    );

    @Override
    public Type<InvestUpgradePointPayload> type() {
        return TYPE;
    }

    public static void handle(InvestUpgradePointPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof VillagerMenu menu) {
            ProgressionService.investPoint(menu.villager(), payload.group());
        }
    }
}
