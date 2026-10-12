package com.villageroverhaul.traderework.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.villageroverhaul.traderework.mason.StatueFigure;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerType;
import org.joml.Vector3fc;

import java.util.function.Consumer;

/**
 * The villager statue items (items/*_villager_statue.json, "type": "vo_trade_rework:villager_statue"), like
 * Vanilla's CopperGolemStatueSpecialRenderer: an unemployed adult of the item's villager type on its plate. The
 * figure of a placed statue is only rolled on placing, so the item can't show it.
 */
public class VillagerStatueSpecialRenderer implements NoDataSpecialModelRenderer {

    private final VillagerStatueFigure figure;
    private final ResourceKey<VillagerType> villagerType;

    public VillagerStatueSpecialRenderer(VillagerStatueFigure figure, ResourceKey<VillagerType> villagerType) {
        this.figure = figure;
        this.villagerType = villagerType;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        figure.submit(poseStack, collector, villagerType, StatueFigure.NONE, 0.0F, lightCoords, overlayCoords, outlineColor, null);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        figure.getExtents(output);
    }

    public record Unbaked(ResourceKey<VillagerType> villagerType) implements NoDataSpecialModelRenderer.Unbaked {

        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceKey.codec(Registries.VILLAGER_TYPE).fieldOf("villager_type").forGetter(Unbaked::villagerType)
        ).apply(instance, Unbaked::new));

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public VillagerStatueSpecialRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new VillagerStatueSpecialRenderer(new VillagerStatueFigure(context.entityModelSet()), villagerType);
        }
    }
}
