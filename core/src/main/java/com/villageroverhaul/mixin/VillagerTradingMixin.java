package com.villageroverhaul.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.villageroverhaul.menu.VillagerMenu;
import com.villageroverhaul.section.VillagerSections;
import com.villageroverhaul.state.VillagerStateAccess;
import com.villageroverhaul.trade.VillagerOffers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Swaps Vanilla trading for ours at the latest possible points, so everything around them stays
 * Vanilla: the mobInteract guards and baby head shake, the "no offers" refusal, TALKED_TO_VILLAGER,
 * updateSpecialPrices (Hero of the Village), setTradingPlayer (villager stops and looks at the player,
 * holds offer items, rejects a second player), stopTrading's discount reset, the restock timers and
 * workstation visit, trade sounds, XP orbs, advancements and TradeWithVillagerEvent.
 *
 * Four narrow interventions:
 * - the offers isEmpty check in mobInteract: the "no offers" refusal only when no section is open either - a
 *   villager whose only open section shows no rows (e.g. a passive with just its meter) still opens the menu.
 * - openTradingScreen inside startTrading: only which menu opens changes.
 * - updateTrades: only where offers come from changes (our data instead of Vanilla trade sets) - its
 *   whole body is trade generation, so a HEAD cancel replaces nothing else.
 * - MerchantOffer.resetUses inside restock/catchUpDemand: only the uses reset is skipped, since our
 *   restock is once per morning and productivity-scaled (work/RestockService), not Vanilla's
 *   twice-a-day full refill.
 */
@Mixin(Villager.class)
public abstract class VillagerTradingMixin {

    @WrapOperation(
            method = "mobInteract",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/trading/MerchantOffers;isEmpty()Z")
    )
    private boolean villageroverhaul$refuseOnlyWithoutSections(MerchantOffers offers, Operation<Boolean> original) {
        Villager villager = (Villager) (Object) this;
        return original.call(offers) && VillagerSections.open(villager, VillagerStateAccess.of(villager).getState()).isEmpty();
    }

    @WrapOperation(
            method = "startTrading",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/villager/Villager;openTradingScreen(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/network/chat/Component;I)V")
    )
    private void villageroverhaul$openVillagerMenu(Villager villager, Player player, Component title, int level, Operation<Void> original) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        VillagerSections.refresh(villager);
        // Stopping the last trade reset every specialPriceDiff to 0, rank discount included - rebuild
        // the list (rank discount baked in) and re-run Vanilla's Hero of the Village on top of it.
        VillagerOffers.refresh(villager);
        serverPlayer.openMenu(
                new SimpleMenuProvider((windowId, inv, p) -> new VillagerMenu(windowId, inv, villager), title),
                buf -> buf.writeVarInt(villager.getId())
        );
        if (serverPlayer.containerMenu instanceof VillagerMenu menu && menu.villager() == villager) {
            VillagerOffers.sendTo(serverPlayer, menu, villager);
        }
    }

    @Inject(method = "updateTrades", at = @At("HEAD"), cancellable = true)
    private void villageroverhaul$fillOwnTrades(ServerLevel level, CallbackInfo ci) {
        Villager villager = (Villager) (Object) this;
        VillagerOffers.fill(villager, villager.getOffers());
        ci.cancel();
    }

    @WrapOperation(
            method = {"restock", "catchUpDemand"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/trading/MerchantOffer;resetUses()V")
    )
    private void villageroverhaul$skipVanillaUsesReset(MerchantOffer offer, Operation<Void> original) {
    }
}
