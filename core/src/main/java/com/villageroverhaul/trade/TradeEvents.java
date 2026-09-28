package com.villageroverhaul.trade;

import com.villageroverhaul.VillagerOverhaulMod;
import com.villageroverhaul.progression.ProfessionLock;
import com.villageroverhaul.state.VillagerStateAccess;
import net.minecraft.world.entity.npc.villager.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;

import java.util.Optional;

/**
 * Connects Vanilla's trade flow back to our state through NeoForge's own events - no mixin needed.
 * The first trade also locks the villager's profession the Vanilla way, see ProfessionLock.
 * A completed trade (MerchantResultSlot -> Villager.notifyTrade -> TradeWithVillagerEvent) is booked
 * by the offer's section (SectionLogic.onUse - stock for a trade, rotation for a quest); that state change rebuilds the offer
 * list via VillagerOffers.refresh. On load, the list Vanilla restored from its own "Offers" NBT is
 * replaced with a fresh projection of our state, which is what actually decides uses and prices.
 */
@EventBusSubscriber(modid = VillagerOverhaulMod.MODID)
public final class TradeEvents {

    private TradeEvents() {
    }

    @SubscribeEvent
    static void onTrade(TradeWithVillagerEvent event) {
        if (!(event.getAbstractVillager() instanceof Villager villager)) {
            return;
        }
        // Resolved first: any state change below rebuilds the offer list, and the traded offer object with it.
        Optional<VillagerOffers.Source> source = VillagerOffers.sourceOf(villager, event.getMerchantOffer());
        ProfessionLock.lock(villager);
        // A traded-with villager keeps its job for good, even one that got it from a station (StationClaims).
        VillagerStateAccess access = VillagerStateAccess.of(villager);
        if (access.getState().stations().heldByStation()) {
            access.setState(access.getState().withStations(access.getState().stations().withHeldByStation(false)));
        }
        source.ifPresentOrElse(
                booked -> booked.section().logic().onUse(villager, booked.section(), booked.offer()),
                () -> VillagerOverhaulMod.LOGGER.warn("Trade with {} did not match any current offer - not booked", villager));
    }

    @SubscribeEvent
    static void onJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Villager villager) {
            VillagerOffers.refresh(villager);
        }
    }
}
