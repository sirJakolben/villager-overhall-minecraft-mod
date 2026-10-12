package com.smartgolem.client;

import com.smartgolem.SmartGolemMod;
import com.smartgolem.climb.ClimbPose;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.phys.Vec3;

/**
 * The Iron Golem's model while it climbs: the Vanilla golem's cubes and texture - the body only split in its upper and lower part, because the animation
 * bends the hip - posed every frame from the tables of ClimbPose, which are the animation of the Blockbench project (assets/Block References/iron_golem,
 * baked with ./gradlew :smart-golem:bakeClimb). One skeleton, no swap of models. Drawn by ClimbGolemLayer over the Vanilla golem, which is hidden for the
 * time - the Vanilla renderer and model stay as they are.
 */
public class ClimbGolemModel extends EntityModel<IronGolemRenderState> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(SmartGolemMod.MODID, "iron_golem_climb"), "main");

    /** Render data on the golem's render state: milliseconds into the climb animation (absent: no climb). */
    public static final ContextKey<Float> CLIMB_MILLIS = new ContextKey<>(Identifier.fromNamespaceAndPath(SmartGolemMod.MODID, "climb_millis"));

    /** Render data: where the golem really is (worked out from the time) minus where it is drawn (smoothed from the packets), in blocks - small. */
    public static final ContextKey<Vec3> CLIMB_OFFSET = new ContextKey<>(Identifier.fromNamespaceAndPath(SmartGolemMod.MODID, "climb_offset"));

    private final ModelPart upperBody;
    private final ModelPart head;
    private final ModelPart lowerBody;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public ClimbGolemModel(ModelPart root) {
        super(root);
        this.upperBody = root.getChild("upper_body");
        this.head = root.getChild("head");
        this.lowerBody = root.getChild("lower_body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    /** Vanilla's cubes, each part at its pivot in the standing pose (the tables replace these poses). */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("upper_body", CubeListBuilder.create().texOffs(0, 40).addBox(-9.0F, -2.0F, -6.0F, 18.0F, 12.0F, 11.0F),
                PartPose.offset(0.0F, -7.0F, 0.0F));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -12.0F, -5.5F, 8.0F, 10.0F, 8.0F)
                .texOffs(24, 0).addBox(-1.0F, -5.0F, -7.5F, 2.0F, 4.0F, 2.0F), PartPose.offset(0.0F, -7.0F, -2.0F));
        root.addOrReplaceChild("lower_body", CubeListBuilder.create().texOffs(0, 70).addBox(-4.5F, 10.0F, -3.0F, 9.0F, 5.0F, 6.0F, new CubeDeformation(0.5F)),
                PartPose.offset(0.0F, -7.0F, 0.0F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(60, 21).addBox(-2.0F, -2.5F, -3.0F, 4.0F, 30.0F, 6.0F),
                PartPose.offset(-11.0F, -7.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(60, 58).addBox(-3.0F, -2.5F, -3.0F, 4.0F, 30.0F, 6.0F),
                PartPose.offset(12.0F, -7.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(37, 0).addBox(-3.5F, -3.0F, -3.0F, 6.0F, 16.0F, 5.0F),
                PartPose.offset(-4.0F, 11.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(60, 0).mirror().addBox(-3.5F, -3.0F, -3.0F, 6.0F, 16.0F, 5.0F),
                PartPose.offset(5.0F, 11.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(IronGolemRenderState state) {
        super.setupAnim(state);
        Float millis = state.getRenderData(CLIMB_MILLIS);
        if (millis == null) {
            return;
        }
        float position = Math.min(Math.max(millis / 1000.0F, 0.0F), ClimbPose.MOVE_END_SECONDS) * ClimbPose.ROWS_PER_SECOND;
        int row = Math.min((int) position, ClimbPose.ROWS - 2);
        float alpha = position - row;
        pose(upperBody, ClimbPose.UPPER_BODY, row, alpha);
        pose(head, ClimbPose.HEAD, row, alpha);
        pose(lowerBody, ClimbPose.LOWER_BODY, row, alpha);
        pose(rightArm, ClimbPose.RIGHT_ARM, row, alpha);
        pose(leftArm, ClimbPose.LEFT_ARM, row, alpha);
        pose(rightLeg, ClimbPose.RIGHT_LEG, row, alpha);
        pose(leftLeg, ClimbPose.LEFT_LEG, row, alpha);
    }

    /** The part's pose between two rows of its table (x, y, z, xRot, yRot, zRot per row). */
    private static void pose(ModelPart part, float[] table, int row, float alpha) {
        int a = row * 6;
        int b = a + 6;
        part.setPos(lerp(table, a, b, alpha), lerp(table, a + 1, b + 1, alpha), lerp(table, a + 2, b + 2, alpha));
        part.setRotation(lerp(table, a + 3, b + 3, alpha), lerp(table, a + 4, b + 4, alpha), lerp(table, a + 5, b + 5, alpha));
    }

    private static float lerp(float[] table, int a, int b, float alpha) {
        return table[a] + (table[b] - table[a]) * alpha;
    }
}
