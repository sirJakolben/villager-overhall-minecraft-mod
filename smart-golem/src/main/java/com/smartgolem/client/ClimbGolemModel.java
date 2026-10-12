package com.smartgolem.client;

import com.smartgolem.SmartGolemMod;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;

/**
 * The model of the Iron Golem's climb (the Blockbench project assets/Block References/iron_golem/iron_golem.bbmodel; the parts come from ClimbGolemParts,
 * the animation from ClimbGolemAnimations): two golems, one standing and one hanging at the ledge, swapped by the animation. Drawn by ClimbGolemLayer
 * over the Vanilla golem, which is hidden for the time - the Vanilla renderer and model stay as they are.
 */
public class ClimbGolemModel extends EntityModel<IronGolemRenderState> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(SmartGolemMod.MODID, "iron_golem_climb"), "main");

    /** Render data on the golem's render state: milliseconds into the climb animation (absent: no climb). */
    public static final ContextKey<Float> CLIMB_MILLIS = new ContextKey<>(Identifier.fromNamespaceAndPath(SmartGolemMod.MODID, "climb_millis"));

    /** Render data: where the model is drawn relative to where the entity is drawn, in blocks (the entity is on top of the wall, the model starts below). */
    public static final ContextKey<net.minecraft.world.phys.Vec3> CLIMB_OFFSET = new ContextKey<>(Identifier.fromNamespaceAndPath(SmartGolemMod.MODID, "climb_offset"));

    private final KeyframeAnimation climb;

    public ClimbGolemModel(ModelPart root) {
        super(root);
        this.climb = ClimbGolemAnimations.CLIMB.bake(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        ClimbGolemParts.addTo(mesh.getRoot());
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(IronGolemRenderState state) {
        super.setupAnim(state);
        Float millis = state.getRenderData(CLIMB_MILLIS);
        if (millis != null) {
            climb.apply((long) (float) millis, 1.0F);
        }
    }
}
