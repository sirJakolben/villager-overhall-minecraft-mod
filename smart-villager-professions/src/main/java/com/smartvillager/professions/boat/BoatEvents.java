package com.smartvillager.professions.boat;

import com.smartvillager.professions.SmartVillagerProfessionsMod;
import com.smartvillager.professions.fisherman.FishermanItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * The mast's sail is a banner (Obsidian Fisherman.md, 2026-10-12): shears take it off, a banner in the hand puts a new one on a bare mast and
 * its pattern shows on the sail. Only those two uses are taken over; everything else on a boat (boarding) stays Vanilla. Plus the tooltip of an
 * upgraded boat item and the creative tab entries.
 */
@EventBusSubscriber(modid = SmartVillagerProfessionsMod.MODID)
public final class BoatEvents {

    private BoatEvents() {
    }

    @SubscribeEvent
    static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof AbstractBoat boat)) {
            return;
        }
        BoatState state = BoatUpgrades.state(boat);
        if (!state.mast()) {
            return;
        }
        Player player = event.getEntity();
        InteractionHand hand = event.getHand();
        ItemStack held = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
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
            consume(event);
        } else if (held.is(ItemTags.BANNERS) && !state.hasBanner()) {
            if (!boat.level().isClientSide()) {
                BoatUpgrades.setState(boat, state.withBanner(held.copyWithCount(1)));
                held.consume(1, player);
                boat.level().playSound(null, boat.getX(), boat.getY(), boat.getZ(), SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            consume(event);
        }
    }

    private static void consume(PlayerInteractEvent.EntityInteract event) {
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.has(BoatUpgrades.CANNON.get())) {
            event.getToolTip().add(Component.translatable("item.smartvillager_professions.boat_cannon").withStyle(ChatFormatting.GRAY));
        }
        BoatUpgrades.Mast mast = stack.get(BoatUpgrades.MAST.get());
        if (mast != null) {
            event.getToolTip().add(mast.banner().isEmpty()
                    ? Component.translatable("item.smartvillager_professions.boat_mast").withStyle(ChatFormatting.GRAY)
                    : Component.translatable("item.smartvillager_professions.boat_mast_banner", mast.banner().getHoverName()).withStyle(ChatFormatting.GRAY));
        }
    }

    @SubscribeEvent
    static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            ItemStack cannon = new ItemStack(Items.OAK_BOAT);
            cannon.set(BoatUpgrades.CANNON.get(), Unit.INSTANCE);
            ItemStack mast = new ItemStack(Items.OAK_BOAT);
            mast.set(BoatUpgrades.MAST.get(), new BoatUpgrades.Mast(new ItemStack(Items.WHITE_BANNER)));
            ItemStack both = cannon.copy();
            both.set(BoatUpgrades.MAST.get(), new BoatUpgrades.Mast(new ItemStack(Items.WHITE_BANNER)));
            event.accept(cannon);
            event.accept(mast);
            event.accept(both);
        }
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(FishermanItems.CANNONBALL.get());
        }
    }
}
