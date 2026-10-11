package com.smartvillager.professions.farmer;

import com.smartvillager.professions.SmartVillagerProfessionsMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/**
 * The invisible timer behind the candied apple (farmer/FarmerItems): when it runs out, the entity gets Speed for SPEED_TICKS -
 * the "then Speed" after the apple's Regeneration. It does nothing while it runs, shows no icon and no particles; a milk
 * bucket removes it like any effect (and with it the Speed to come).
 */
public class SugarRushEffect extends MobEffect {

    /** Speed after the Regeneration, in ticks (Obsidian 02_Tweak-Werte). */
    public static final int SPEED_TICKS = 1200;

    protected SugarRushEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xF2A0C2);
    }

    @EventBusSubscriber(modid = SmartVillagerProfessionsMod.MODID)
    static final class Events {

        private Events() {
        }

        @SubscribeEvent
        static void onExpired(MobEffectEvent.Expired event) {
            MobEffectInstance expired = event.getEffectInstance();
            LivingEntity entity = event.getEntity();
            if (!entity.level().isClientSide() && expired != null && expired.getEffect().value() instanceof SugarRushEffect) {
                entity.addEffect(new MobEffectInstance(MobEffects.SPEED, SPEED_TICKS, 0));
            }
        }
    }
}
