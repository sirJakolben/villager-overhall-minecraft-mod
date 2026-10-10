package com.smartgolem.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.animal.golem.IronGolemModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Draws the climb model (ClimbGolemModel) in place of the Vanilla golem, which is invisible for the time (ClimbGolemClient): a layer of the Vanilla
 * renderer, added the NeoForge way, so other mods that change the golem's renderer or models keep working. Only while the render state carries the
 * climb's time.
 */
public class ClimbGolemLayer extends RenderLayer<IronGolemRenderState, IronGolemModel> {

    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/iron_golem/iron_golem.png");

    private final ClimbGolemModel model;

    public ClimbGolemLayer(RenderLayerParent<IronGolemRenderState, IronGolemModel> renderer, ClimbGolemModel model) {
        super(renderer);
        this.model = model;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, IronGolemRenderState state, float yRot, float xRot) {
        if (state.getRenderData(ClimbGolemModel.CLIMB_MILLIS) == null) {
            return;
        }
        model.setupAnim(state);
        Vec3 offset = state.getRenderData(ClimbGolemModel.CLIMB_OFFSET);
        poseStack.pushPose();
        if (offset != null) {
            // The offset is in the world; the stack is turned by the entity (180 - body yaw) and mirrored (-1, -1, 1): the same the other way round.
            Vector3f local = new Vector3f((float) offset.x, (float) offset.y, (float) offset.z).rotateY((float) Math.toRadians(-(180.0F - state.bodyRot)));
            poseStack.translate(-local.x, -local.y, local.z);
        }
        renderColoredCutoutModel(model, TEXTURE, poseStack, submitNodeCollector, lightCoords, state, -1, 0);
        poseStack.popPose();
    }
}
