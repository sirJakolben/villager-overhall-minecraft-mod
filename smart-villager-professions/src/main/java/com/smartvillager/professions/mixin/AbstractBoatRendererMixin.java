package com.smartvillager.professions.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.smartvillager.professions.boat.BoatSlots;
import com.smartvillager.professions.boat.BoatState;
import com.smartvillager.professions.boat.BoatUpgrades;
import com.smartvillager.professions.client.render.BoatRenderStateExt;
import com.smartvillager.professions.client.render.BoatUpgradeRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractBoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cannon, mast and banner sail of an upgraded boat are drawn right after the hull, inside the pose Vanilla's boat renderer built (BoatUpgradeRenderer).
 * Three small hooks: the renderer gets its flag model and sprites when it is built, the entity's upgrades are copied into the render state, and
 * the extra parts are submitted just before Vanilla's own per-type additions (water patch). A plain boat does nothing there.
 */
@Mixin(AbstractBoatRenderer.class)
public abstract class AbstractBoatRendererMixin {

    @Unique
    private BoatUpgradeRenderer smartvillager_professions$upgradeRenderer;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void smartvillager_professions$createUpgradeRenderer(EntityRendererProvider.Context context, Identifier texture, CallbackInfo ci) {
        this.smartvillager_professions$upgradeRenderer = new BoatUpgradeRenderer(context.getModelSet(), context.getSprites(), texture);
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/vehicle/boat/AbstractBoat;Lnet/minecraft/client/renderer/entity/state/BoatRenderState;F)V", at = @At("TAIL"))
    private void smartvillager_professions$extractUpgrades(AbstractBoat entity, BoatRenderState state, float partialTicks, CallbackInfo ci) {
        BoatState upgrades = BoatUpgrades.state(entity);
        ((BoatRenderStateExt) state).smartvillager_professions$setUpgrades(upgrades);
        if (upgrades.filledSlots() > 0 || upgrades.mast()) {
            ((BoatRenderStateExt) state).smartvillager_professions$setOffsets(BoatSlots.offsets(entity));
        }
    }

    @Inject(
            method = "submit",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/AbstractBoatRenderer;submitTypeAdditions(Lnet/minecraft/client/renderer/entity/state/BoatRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V")
    )
    private void smartvillager_professions$submitUpgrades(BoatRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        BoatRenderStateExt ext = (BoatRenderStateExt) state;
        this.smartvillager_professions$upgradeRenderer.submit(ext.smartvillager_professions$upgrades(), ext.smartvillager_professions$offsets(), poseStack, collector, state.lightCoords, state.ageInTicks);
    }
}
