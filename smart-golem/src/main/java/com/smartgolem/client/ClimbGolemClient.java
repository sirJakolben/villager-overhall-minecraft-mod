package com.smartgolem.client;

import com.smartgolem.SmartGolemMod;
import com.smartgolem.ai.GolemClimbs;
import com.smartgolem.climb.GolemClimb;
import net.minecraft.client.model.animal.golem.IronGolemModel;
import net.minecraft.client.renderer.entity.IronGolemRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

/**
 * The climb animation on the client - all through NeoForge's rendering events, Vanilla's golem renderer is not replaced:
 * - the model's layer definition;
 * - the climb layer (ClimbGolemLayer) added to the Vanilla golem renderer;
 * - the render state of a climbing golem gets the time into the animation and is made invisible, so the Vanilla model is not drawn - only the layer.
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = SmartGolemMod.MODID)
public final class ClimbGolemClient {

    /** The model glides from where the golem stood to where the climb begins in this many ticks. */
    private static final float GLIDE_TICKS = 6.0F;

    private ClimbGolemClient() {
    }

    @SubscribeEvent
    static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ClimbGolemModel.LAYER, ClimbGolemModel::createBodyLayer);
    }

    @SuppressWarnings("unchecked")
    @SubscribeEvent
    static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        // Only on Vanilla's golem renderer (or a subclass of it): its render state is the one the modifier below fills.
        if (event.getRenderer(EntityType.IRON_GOLEM) instanceof IronGolemRenderer renderer) {
            ClimbGolemModel model = new ClimbGolemModel(event.getEntityModels().bakeLayer(ClimbGolemModel.LAYER));
            ((LivingEntityRenderer<IronGolem, IronGolemRenderState, IronGolemModel>) renderer).addLayer(new ClimbGolemLayer(renderer, model));
        }
    }

    @SubscribeEvent
    static void onRegisterRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(IronGolemRenderer.class, (IronGolem golem, IronGolemRenderState state) -> {
            GolemClimb climb = golem.getData(GolemClimb.ATTACHMENT);
            if (!climb.active()) {
                return;
            }
            // Past the end the last pose stays until the server has put the golem on top (a tick or two).
            float ticks = Math.min(Math.max(golem.level().getGameTime() + state.partialTick - climb.startTick(), 0.0F), GolemClimbs.CLIMB_TICKS);
            state.setRenderData(ClimbGolemModel.CLIMB_MILLIS, ticks * 50.0F);
            // The entity is on top of the wall the whole time; the model is drawn where the climb begins (gliding there from where the golem stood).
            float glide = Math.min(ticks / GLIDE_TICKS, 1.0F);
            glide = glide * glide * (3.0F - 2.0F * glide);
            Vec3 drawn = climb.from().lerp(climb.start(), glide);
            state.setRenderData(ClimbGolemModel.CLIMB_OFFSET, drawn.subtract(golem.getPosition(state.partialTick)));
            state.isInvisible = true;
            state.isInvisibleToPlayer = true;
        });
    }
}
