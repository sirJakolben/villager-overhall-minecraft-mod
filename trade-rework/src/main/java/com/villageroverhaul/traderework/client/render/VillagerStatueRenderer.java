package com.villageroverhaul.traderework.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.villageroverhaul.traderework.mason.StatueFigure;
import com.villageroverhaul.traderework.mason.VillagerStatueBlock;
import com.villageroverhaul.traderework.mason.VillagerStatueBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws a villager statue (VillagerStatueBlock, lower half) like Vanilla's CopperGolemStatueBlockRenderer:
 * the figure turned with the block's facing, in the same space a living entity is drawn in
 * (LivingEntityRenderer: body turn, flip, 1.501 down), so the villager model fits without any offsets of its own.
 */
public class VillagerStatueRenderer implements BlockEntityRenderer<VillagerStatueBlockEntity, VillagerStatueRenderer.State> {

    private final VillagerStatueFigure figure;

    public VillagerStatueRenderer(BlockEntityRendererProvider.Context context) {
        this.figure = new VillagerStatueFigure(context.entityModelSet());
    }

    public static class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public StatueFigure figure = StatueFigure.NONE;
        public @Nullable ResourceKey<VillagerType> villagerType;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(VillagerStatueBlockEntity statue, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(statue, state, partialTicks, cameraPosition, breakProgress);
        BlockState blockState = statue.getBlockState();
        state.facing = blockState.getValue(VillagerStatueBlock.FACING);
        state.figure = blockState.getValue(VillagerStatueBlock.FIGURE);
        state.villagerType = blockState.getBlock() instanceof VillagerStatueBlock block ? block.villagerType() : null;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.villagerType == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        // An entity whose body faces "facing" - the statue looks at whoever placed it.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.facing.toYRot()));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        figure.submit(poseStack, collector, state.villagerType, state.figure, state.lightCoords, OverlayTexture.NO_OVERLAY, 0, state.breakProgress);
        poseStack.popPose();
    }

    /** The adult reaches into the block above, so the renderer must not be culled at the lower block's top. */
    @Override
    public AABB getRenderBoundingBox(VillagerStatueBlockEntity statue) {
        BlockPos pos = statue.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 2.1, pos.getZ() + 1);
    }
}
