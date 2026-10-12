package com.smartvillager.professions.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import org.joml.Vector3fc;

import java.util.function.Consumer;

/** The boat chest item (items/boat_chest.json, "type": "smartvillager_professions:boat_chest"): the same small chest model as in the boat (BoatChestModel), like Vanilla's chest item. */
public class BoatChestSpecialRenderer implements NoDataSpecialModelRenderer {

    private final ModelPart chest = BoatChestModel.bake();
    private final SpriteGetter sprites;

    public BoatChestSpecialRenderer(SpriteGetter sprites) {
        this.sprites = sprites;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        collector.submitModelPart(chest, poseStack, BoatChestModel.SPRITE.renderType(RenderTypes::entityCutout), lightCoords, overlayCoords, sprites.get(BoatChestModel.SPRITE));
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        chest.getExtentsForGui(new PoseStack(), output);
    }

    public record Unbaked() implements NoDataSpecialModelRenderer.Unbaked {

        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public BoatChestSpecialRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new BoatChestSpecialRenderer(context.sprites());
        }
    }
}
