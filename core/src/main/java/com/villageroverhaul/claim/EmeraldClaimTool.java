package com.villageroverhaul.claim;

import com.villageroverhaul.VillagerOverhaulMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Block B: the emerald is the claim tool (decided 2026-09-24). Right-clicking a villager with an
 * emerald never opens trading - it selects the villager instead; the next right-click with an emerald
 * on a workplace or Trading Block (within CLAIM_WINDOW_TICKS) assigns it there, see ManualClaims.
 * Trading works as before with anything else in hand. The emerald is not used up.
 *
 * Both hooks are NeoForge's own interaction events - no mixin. Only the emerald case is cancelled, so
 * every other right-click on villagers and blocks stays Vanilla. Selections live on the server only.
 */
@EventBusSubscriber(modid = VillagerOverhaulMod.MODID)
public final class EmeraldClaimTool {

    /** How long a selected villager waits for its workplace click (30 s). */
    private static final int CLAIM_WINDOW_TICKS = 600;

    private record Selection(UUID villager, long expiresAt) {
    }

    private static final Map<UUID, Selection> SELECTIONS = new HashMap<>();

    private EmeraldClaimTool() {
    }

    @SubscribeEvent
    static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Villager villager) || !event.getItemStack().is(Items.EMERALD)) {
            return;
        }
        // Both sides, so the client doesn't open the trade screen in the meantime either.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level) {
            SELECTIONS.put(player.getUUID(), new Selection(villager.getUUID(), level.getGameTime() + CLAIM_WINDOW_TICKS));
            level.broadcastEntityEvent(villager, (byte) 14); // Vanilla's green star particles instead of a message
        }
    }

    @SubscribeEvent
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)
                || !event.getItemStack().is(Items.EMERALD)) {
            return;
        }
        Selection selection = SELECTIONS.get(player.getUUID());
        if (selection == null || level.getGameTime() > selection.expiresAt()) {
            SELECTIONS.remove(player.getUUID());
            return; // no villager selected - the click stays a normal click
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        SELECTIONS.remove(player.getUUID());

        Entity entity = level.getEntity(selection.villager());
        if (!(entity instanceof Villager villager) || !villager.isAlive()) {
            player.sendOverlayMessage(Component.translatable("message.villageroverhaul.claim.gone"));
            return;
        }
        ManualClaims.assign(level, villager, event.getPos()).ifPresent(player::sendOverlayMessage);
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SELECTIONS.remove(event.getEntity().getUUID());
    }
}
