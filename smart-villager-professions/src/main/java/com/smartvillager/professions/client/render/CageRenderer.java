package com.smartvillager.professions.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.smartvillager.professions.cage.ZombieVillagerCageBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the prisoner of a Zombie Villager Cage: a display copy of the zombie villager, built once from the saved tag the server sent (like the mob
 * in a spawner) and drawn standing in the middle of the cage, looking at the front. A curing prisoner shakes (the tag carries its conversion).
 * The cage's own bars are the block model.
 */
public class CageRenderer implements BlockEntityRenderer<ZombieVillagerCageBlockEntity, CageRenderer.State> {

    private final EntityRenderDispatcher entityRenderer;

    public CageRenderer(BlockEntityRendererProvider.Context context) {
        this.entityRenderer = context.entityRenderer();
    }

    public static class State extends BlockEntityRenderState {
        public @Nullable EntityRenderState prisoner;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ZombieVillagerCageBlockEntity cage, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(cage, state, partialTicks, cameraPosition, breakProgress);
        state.prisoner = null;
        Level level = cage.getLevel();
        CompoundTag tag = cage.occupantTag();
        if (level == null || tag == null) {
            return;
        }
        Entity display = cage.displayEntity();
        if (display == null) {
            display = EntityType.loadEntityRecursive(EntityType.ZOMBIE_VILLAGER, tag, level, EntitySpawnReason.LOAD, EntityProcessor.NOP);
            if (display == null) {
                return;
            }
            display.setYRot(0.0F);
            display.setYBodyRot(0.0F);
            display.setYHeadRot(0.0F);
            cage.setDisplayEntity(display);
        }
        display.tickCount = (int) level.getGameTime();
        state.prisoner = entityRenderer.extractEntity(display, partialTicks);
        state.prisoner.lightCoords = state.lightCoords;
        state.prisoner.displayFireAnimation = false;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.prisoner == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        entityRenderer.submit(state.prisoner, camera, 0.0, 0.0, 0.0, poseStack, collector);
        poseStack.popPose();
    }

    /** The prisoner is two blocks high, the block entity sits in the lower one. */
    @Override
    public AABB getRenderBoundingBox(ZombieVillagerCageBlockEntity cage) {
        BlockPos pos = cage.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 2.1, pos.getZ() + 1);
    }
}
