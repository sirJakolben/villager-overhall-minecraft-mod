package com.smartvillager.professions.boat;

import com.smartvillager.professions.SmartVillagerProfessionsMod;
import com.smartvillager.professions.fisherman.FishermanItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.boat.AbstractChestBoat;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Optional;

/**
 * What the player does with a boat's parts (Obsidian Fisherman.md, 2026-10-12). Shift + right click with a part in the hand builds it into the slot nearest
 * the spot that was clicked (a cannon only into the front slot, the mast into any slot, as its decoration); Shift + right click with nothing to build opens the
 * chests. Without Shift: shears take the mast's banner off, a banner in the hand puts one on a bare mast. Only these uses are taken over; everything
 * else on a boat (boarding, a Vanilla chest boat's own chest) stays Vanilla. Plus the tooltip of a mast and the creative tab entries.
 */
@EventBusSubscriber(modid = SmartVillagerProfessionsMod.MODID)
public final class BoatEvents {

    private BoatEvents() {
    }

    /** Building in: the specific interaction knows where on the boat the player clicked. */
    @SubscribeEvent
    static void onInteractAt(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getTarget() instanceof AbstractBoat boat) || !event.getEntity().isSecondaryUseActive()) {
            return;
        }
        Player player = event.getEntity();
        ItemStack held = player.getItemInHand(event.getHand());
        Optional<BoatPartType> part = BoatParts.ofItem(held);
        if (part.isEmpty()) {
            return;
        }
        BoatPartType type = part.get();
        BoatState state = BoatUpgrades.state(boat);
        Vec3 local = event.getLocalPos();
        double forward = BoatSlots.forwardOf(boat, local.x, local.z);
        int slot = switch (type.kind()) {
            case DECO -> BoatSlots.nearest(boat, forward, s -> true);
            case SLOT -> type == BoatParts.CANNON ? 0 : BoatSlots.nearest(boat, forward, state::isFree);
        };
        if (!BoatSlots.canInstall(boat, state, type, slot)) {
            if (!boat.level().isClientSide()) {
                player.sendOverlayMessage(Component.translatable("message.smartvillager_professions.boat_part_not_possible"));
            }
            swallow(event);
            return;
        }
        if (!boat.level().isClientSide()) {
            if (type.kind() == BoatPartType.Kind.DECO) {
                BoatUpgrades.setState(boat, state.withDeco(Optional.of(new BoatState.Deco(type.id(), slot, held.copyWithCount(1)))));
            } else {
                BoatUpgrades.setState(boat, state.withPart(slot, type.id()));
                BoatSlots.trimPassengers(boat);
            }
            held.consume(1, player);
            boat.level().playSound(null, boat.getX(), boat.getY(), boat.getZ(), SoundEvents.WOOD_PLACE, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        swallow(event);
    }

    @SubscribeEvent
    static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof AbstractBoat boat)) {
            return;
        }
        Player player = event.getEntity();
        InteractionHand hand = event.getHand();
        ItemStack held = player.getItemInHand(hand);
        BoatState state = BoatUpgrades.state(boat);

        if (player.isSecondaryUseActive()) {
            if (state.chests() > 0 && !(boat instanceof AbstractChestBoat)) {
                if (player instanceof ServerPlayer serverPlayer) {
                    BoatInventory.open(serverPlayer, boat);
                }
                swallow(event);
            }
            return;
        }
        if (!state.mast()) {
            return;
        }

        if (held.is(Items.SHEARS) && state.hasBanner()) {
            if (!boat.level().isClientSide()) {
                ItemStack banner = state.banner().copy();
                BoatUpgrades.setState(boat, state.withBanner(ItemStack.EMPTY));
                held.hurtAndBreak(1, player, hand);
                if (!player.getInventory().add(banner)) {
                    player.drop(banner, false);
                }
                boat.level().playSound(null, boat.getX(), boat.getY(), boat.getZ(), SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            swallow(event);
        } else if (held.is(ItemTags.BANNERS) && !state.hasBanner()) {
            if (!boat.level().isClientSide()) {
                BoatUpgrades.setState(boat, state.withBanner(held.copyWithCount(1)));
                held.consume(1, player);
                boat.level().playSound(null, boat.getX(), boat.getY(), boat.getZ(), SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            swallow(event);
        }
    }

    private static void swallow(PlayerInteractEvent.EntityInteractSpecific event) {
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static void swallow(PlayerInteractEvent.EntityInteract event) {
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.is(FishermanItems.BOAT_MAST.get())) {
            BoatUpgrades.Mast mast = stack.get(BoatUpgrades.MAST.get());
            event.getToolTip().add(mast == null || mast.banner().isEmpty()
                    ? Component.translatable("item.smartvillager_professions.boat_mast.bare").withStyle(ChatFormatting.GRAY)
                    : Component.translatable("item.smartvillager_professions.boat_mast.banner", mast.banner().getHoverName()).withStyle(ChatFormatting.GRAY));
        }
    }

    @SubscribeEvent
    static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            ItemStack mast = new ItemStack(FishermanItems.BOAT_MAST.get());
            mast.set(BoatUpgrades.MAST.get(), new BoatUpgrades.Mast(new ItemStack(Items.WHITE_BANNER)));
            event.accept(mast);
        }
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(FishermanItems.CANNONBALL.get());
        }
    }
}
