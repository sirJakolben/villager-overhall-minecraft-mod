package com.smartvillager.base.world;

import com.smartvillager.base.SmartVillagerMod;
import com.smartvillager.base.mixin.ZombieVillagerInvoker;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Weakness starts the cure: a zombie villager with the weakness effect (splash or lingering potion, tipped arrow, command) begins to be cured - no golden
 * apple, no golden food. Vanilla wants the weakness effect and then a golden apple; here the effect alone is enough. The cure itself is Vanilla's: the same
 * 3 to 5 minutes, the strength effect and shaking, the conversion (data, trades, gossip, reputation, advancement for whoever threw the potion), and
 * starting it uses up the weakness, like Vanilla's apple does. Vanilla's golden apple click stays as it is.
 * Done in NeoForge's events, no mixin on the effect: the effect event only notes who threw the potion (it comes before the effect is on the mob), the
 * zombie villager's own tick starts the cure as soon as it has the effect - one effect lookup per zombie villager and tick.
 */
@EventBusSubscriber(modid = SmartVillagerMod.MODID)
public final class ZombieVillagerCure {

    /** Vanilla's cure time: 3600 + random(2401) ticks. */
    private static final int MIN_TICKS = 3600;
    private static final int EXTRA_TICKS = 2401;

    /** Who gave the weakness - the player who gets the credit for the cure. Server thread only. */
    private static final Map<ZombieVillager, UUID> THROWERS = new WeakHashMap<>();

    private ZombieVillagerCure() {
    }

    @SubscribeEvent
    static void onEffectAdded(MobEffectEvent.Added event) {
        if (event.getEntity() instanceof ZombieVillager zombieVillager && !zombieVillager.level().isClientSide() && event.getEffectInstance().is(MobEffects.WEAKNESS)
                && event.getEffectSource() instanceof Player player) {
            THROWERS.put(zombieVillager, player.getUUID());
        }
    }

    @SubscribeEvent
    static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ZombieVillager zombieVillager) || zombieVillager.level().isClientSide() || zombieVillager.isConverting()
                || !zombieVillager.hasEffect(MobEffects.WEAKNESS)) {
            return;
        }
        ((ZombieVillagerInvoker) zombieVillager).smartvillager$startConverting(THROWERS.remove(zombieVillager), MIN_TICKS + zombieVillager.getRandom().nextInt(EXTRA_TICKS));
    }
}
