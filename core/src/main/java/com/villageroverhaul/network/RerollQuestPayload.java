package com.villageroverhaul.network;

import com.villageroverhaul.VillagerOverhaulMod;
import com.villageroverhaul.client.ui.VillagerMenu;
import com.villageroverhaul.quest.QuestActions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client asks the server to reroll one quest slot's current offer, subject to its per-slot cooldown. */
public record RerollQuestPayload(int slot) implements CustomPacketPayload {

    public static final Type<RerollQuestPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(VillagerOverhaulMod.MODID, "reroll_quest"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RerollQuestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RerollQuestPayload::slot,
            RerollQuestPayload::new
    );

    @Override
    public Type<RerollQuestPayload> type() {
        return TYPE;
    }

    public static void handle(RerollQuestPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof VillagerMenu menu) {
            QuestActions.reroll(menu.villager(), payload.slot());
        }
    }
}
