package com.villageroverhaul.state;

import com.villageroverhaul.VillagerOverhaulMod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;

import java.util.Map;

/**
 * Keeps a villager's progress through a zombie conversion and back (2026-09-28): Vanilla turns a villager
 * into a new ZombieVillager entity and, once cured, into a new Villager again - copying its profession,
 * offers and gossip, but none of our state. This carries VillagerState along both steps (bitten → zombie,
 * cured → villager), right where Vanilla copies its own data (LivingConversionEvent.Post).
 *
 * Workplaces are not carried: dying released them (StationClaims.onDeath) and another villager may own them
 * by the time it is cured, so it claims its workplaces anew. Its Trading Block assignment stays - that is no
 * reservation, and StationClaims drops it if it no longer fits.
 *
 * Done here rather than with the attachment's copyOnDeath, which would carry the stale workplaces along.
 * Other conversions (lightning → witch) don't carry it.
 */
@EventBusSubscriber(modid = VillagerOverhaulMod.MODID)
public final class VillagerConversion {

    private VillagerConversion() {
    }

    @SubscribeEvent
    static void onConvert(LivingConversionEvent.Post event) {
        LivingEntity from = event.getEntity();
        LivingEntity to = event.getOutcome();
        if (from.level().isClientSide() || !isVillagerLike(from) || !isVillagerLike(to) || !from.hasData(ModAttachments.VILLAGER_STATE)) {
            return;
        }
        VillagerState state = from.getData(ModAttachments.VILLAGER_STATE);
        VillagerState carried = state.withStations(new Stations(Map.of(), false, state.stations().tradingBlock()));
        if (to instanceof Villager villager) {
            // Through the access, so the cured villager's profession locks and its offers are rebuilt.
            VillagerStateAccess.of(villager).setState(carried);
        } else {
            to.setData(ModAttachments.VILLAGER_STATE, carried);
        }
    }

    private static boolean isVillagerLike(LivingEntity entity) {
        return entity instanceof Villager || entity instanceof ZombieVillager;
    }
}
