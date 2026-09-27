package com.villageroverhaul.traderework.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.villageroverhaul.traderework.veteran.WeaponRackBlock;
import com.villageroverhaul.traderework.veteran.WeaponRackBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the weapon on a weapon rack (WeaponRackBlock), built like Vanilla's ShelfRenderer: the item model
 * in the FIXED context (the one item frames use), turned with the rack's facing, lying flat just in front
 * of the board, larger than in an item frame so the weapon fills the rack.
 */
public class WeaponRackRenderer implements BlockEntityRenderer<WeaponRackBlockEntity, WeaponRackRenderer.State> {

    /** Distance of the weapon from the block middle towards the wall - just in front of the 2 px board. */
    private static final double WALL_OFFSET = 0.34;
    private static final float ITEM_SCALE = 0.75F;

    private final ItemModelResolver itemModelResolver;

    public WeaponRackRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    public static class State extends BlockEntityRenderState {
        public @Nullable ItemStackRenderState weapon;
        public Direction facing = Direction.NORTH;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(WeaponRackBlockEntity rack, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(rack, state, partialTicks, cameraPosition, breakProgress);
        state.facing = rack.getBlockState().getValue(WeaponRackBlock.FACING);
        ItemStack weapon = rack.weapon();
        if (weapon.isEmpty()) {
            state.weapon = null;
            return;
        }
        ItemStackRenderState itemState = new ItemStackRenderState();
        itemModelResolver.updateForTopItem(itemState, weapon, ItemDisplayContext.FIXED, rack.getLevel(), null, (int) rack.getBlockPos().asLong());
        state.weapon = itemState;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.weapon == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        // Same turn as the shelf: afterwards local -z points to the wall, +z to the viewer.
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot()));
        poseStack.translate(0.0, 0.0, -WALL_OFFSET);
        poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
        state.weapon.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
