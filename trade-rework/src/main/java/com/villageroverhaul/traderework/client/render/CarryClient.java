package com.villageroverhaul.traderework.client.render;

import com.villageroverhaul.traderework.TradeReworkMod;
import com.villageroverhaul.traderework.cage.CarryPose;
import com.villageroverhaul.traderework.cage.Cages;
import net.minecraft.client.renderer.entity.IronGolemRenderer;
import net.minecraft.client.renderer.entity.ZombieVillagerRenderer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

/**
 * The cage and the carrying, on the client - through NeoForge's events only, no Vanilla renderer is replaced:
 * - the cage's block entity renderer (CageRenderer);
 * - a golem with a zombie villager on it holds both arms out in front: Vanilla's attack pose of the golem model, frozen at the point where the arms
 *   are about horizontal (the model reads the "attack ticks" of the render state; a constant keeps the arms still);
 * - the zombie villager on a golem is drawn lying on its back like a sleeping player, head to the golem's right (CarryPose).
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = TradeReworkMod.MODID)
public final class CarryClient {

    /** Vanilla's arm angle in the attack pose is -2 + 1.5 * triangleWave(t, 10); at t = 1.5 it is about -1.4 radians: the arms reach forward. */
    private static final float ARMS_FORWARD_TICKS = 1.5F;

    private CarryClient() {
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Cages.CAGE_ENTITY.get(), CageRenderer::new);
    }

    @SubscribeEvent
    static void onRegisterRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(IronGolemRenderer.class, (IronGolem golem, net.minecraft.client.renderer.entity.state.IronGolemRenderState state) -> {
            for (var passenger : golem.getPassengers()) {
                if (passenger instanceof ZombieVillager) {
                    state.attackTicksRemaining = ARMS_FORWARD_TICKS;
                    return;
                }
            }
        });
        event.registerEntityModifier(ZombieVillagerRenderer.class,
                (ZombieVillager zombie, net.minecraft.client.renderer.entity.state.ZombieVillagerRenderState state) -> {
                    if (zombie.getVehicle() instanceof IronGolem golem) {
                        state.pose = Pose.SLEEPING;
                        state.bedOrientation = CarryPose.headDirection(golem.yBodyRot);
                    }
                });
    }
}
