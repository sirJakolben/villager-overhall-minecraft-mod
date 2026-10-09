package com.villageroverhaul.traderework.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.villageroverhaul.traderework.fisherman.VillagerBobber;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.FishingHookRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a villager's bobber (fisherman/VillagerBobber) like Vanilla draws a player's: the same texture and the same string, here from the villager's
 * hand. Vanilla's FishingHookRenderer only draws bobbers owned by a player, so this copies its drawing for a villager owner.
 */
public class VillagerBobberRenderer extends EntityRenderer<VillagerBobber, FishingHookRenderState> {

    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/fishing/fishing_hook.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityCutoutCull(TEXTURE);

    public VillagerBobberRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public FishingHookRenderState createRenderState() {
        return new FishingHookRenderState();
    }

    @Override
    public void extractRenderState(VillagerBobber bobber, FishingHookRenderState state, float partialTicks) {
        super.extractRenderState(bobber, state, partialTicks);
        if (bobber.getOwner() instanceof Villager villager) {
            double yaw = Math.toRadians(Mth.lerp(partialTicks, villager.yBodyRotO, villager.yBodyRot));
            Vec3 hand = villager.getPosition(partialTicks).add(-Math.sin(yaw) * 0.5, villager.getBbHeight() * 0.6, Math.cos(yaw) * 0.5);
            state.lineOriginOffset = hand.subtract(bobber.getPosition(partialTicks).add(0.0, 0.25, 0.0));
        } else {
            state.lineOriginOffset = Vec3.ZERO;
        }
    }

    @Override
    public void submit(FishingHookRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(camera.orientation);
        collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> {
            vertex(buffer, pose, state.lightCoords, 0.0F, 0, 0, 1);
            vertex(buffer, pose, state.lightCoords, 1.0F, 0, 1, 1);
            vertex(buffer, pose, state.lightCoords, 1.0F, 1, 1, 0);
            vertex(buffer, pose, state.lightCoords, 0.0F, 1, 0, 0);
        });
        poseStack.popPose();
        float xa = (float) state.lineOriginOffset.x;
        float ya = (float) state.lineOriginOffset.y;
        float za = (float) state.lineOriginOffset.z;
        float width = Minecraft.getInstance().gameRenderer.getGameRenderState().windowRenderState.appropriateLineWidth;
        collector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, buffer) -> {
            for (int i = 0; i < 16; i++) {
                float a0 = i / 16.0F;
                float a1 = (i + 1) / 16.0F;
                stringVertex(xa, ya, za, buffer, pose, a0, a1, width);
                stringVertex(xa, ya, za, buffer, pose, a1, a0, width);
            }
        });
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    private static void vertex(VertexConsumer builder, PoseStack.Pose pose, int lightCoords, float x, int y, int u, int v) {
        builder.addVertex(pose, x - 0.5F, y - 0.5F, 0.0F)
                .setColor(-1)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(lightCoords)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static void stringVertex(float xa, float ya, float za, VertexConsumer buffer, PoseStack.Pose pose, float aa, float nexta, float width) {
        float x = xa * aa;
        float y = ya * (aa * aa + aa) * 0.5F + 0.25F;
        float z = za * aa;
        float nx = xa * nexta - x;
        float ny = ya * (nexta * nexta + nexta) * 0.5F + 0.25F - y;
        float nz = za * nexta - z;
        float length = Mth.sqrt(nx * nx + ny * ny + nz * nz);
        nx /= length;
        ny /= length;
        nz /= length;
        buffer.addVertex(pose, x, y, z).setColor(-16777216).setNormal(pose, nx, ny, nz).setLineWidth(width);
    }

    @Override
    protected boolean affectedByCulling(VillagerBobber bobber) {
        return false;
    }
}
