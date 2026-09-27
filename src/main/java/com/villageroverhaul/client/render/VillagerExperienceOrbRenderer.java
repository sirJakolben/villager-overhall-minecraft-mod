package com.villageroverhaul.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.villageroverhaul.librarian.VillagerExperienceOrb;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ExperienceOrbRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Vanilla's ExperienceOrbRenderer with the Vanilla orb texture, only the color pulse changed: Vanilla
 * pulses red 0-255 at full green (yellow-green); here red stays near 0 and blue pulses 128-255 at full
 * green, so the orb swings between juicy green and turquoise.
 */
public class VillagerExperienceOrbRenderer extends EntityRenderer<VillagerExperienceOrb, ExperienceOrbRenderState> {

    private static final Identifier EXPERIENCE_ORB_LOCATION = Identifier.withDefaultNamespace("textures/entity/experience/experience_orb.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucentCullItemTarget(EXPERIENCE_ORB_LOCATION);

    public VillagerExperienceOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.15F;
        this.shadowStrength = 0.75F;
    }

    @Override
    protected int getBlockLightLevel(VillagerExperienceOrb entity, BlockPos blockPos) {
        return Mth.clamp(super.getBlockLightLevel(entity, blockPos) + 7, 0, 15);
    }

    @Override
    public void submit(ExperienceOrbRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        int icon = state.icon;
        float u0 = (icon % 4 * 16) / 64.0F;
        float u1 = (icon % 4 * 16 + 16) / 64.0F;
        float v0 = (icon / 4 * 16) / 64.0F;
        float v1 = (icon / 4 * 16 + 16) / 64.0F;
        float rr = state.ageInTicks / 2.0F;
        int rc = (int) ((Mth.sin(rr + (float) (Math.PI * 4.0 / 3.0)) + 1.0F) * 0.1F * 255.0F);
        int gc = 255;
        int bc = (int) (128.0F + (Mth.sin(rr) + 1.0F) * 0.5F * 127.0F);
        poseStack.translate(0.0F, 0.1F, 0.0F);
        poseStack.mulPose(camera.orientation);
        poseStack.scale(0.3F, 0.3F, 0.3F);
        submitNodeCollector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> {
            vertex(buffer, pose, -0.5F, -0.25F, rc, gc, bc, u0, v1, state.lightCoords);
            vertex(buffer, pose, 0.5F, -0.25F, rc, gc, bc, u1, v1, state.lightCoords);
            vertex(buffer, pose, 0.5F, 0.75F, rc, gc, bc, u1, v0, state.lightCoords);
            vertex(buffer, pose, -0.5F, 0.75F, rc, gc, bc, u0, v0, state.lightCoords);
        });
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, int r, int g, int b, float u, float v, int lightCoords) {
        buffer.addVertex(pose, x, y, 0.0F)
                .setColor(r, g, b, 128)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(lightCoords)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public ExperienceOrbRenderState createRenderState() {
        return new ExperienceOrbRenderState();
    }

    @Override
    public void extractRenderState(VillagerExperienceOrb entity, ExperienceOrbRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.icon = entity.getIcon();
    }
}
