package com.smartgolem.client;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The parts of the two-block climb (assets/Block References/iron_golem/iron_golem.bbmodel, Blockbench "Modded Entity"): two golems, "Iron golem" (standing)
 * and "Iron_golem2" (the pose that hangs on the ledge), one hidden while the other shows - ClimbGolemAnimations swaps them with scale keyframes.
 * GENERATED from the bbmodel by a script - do not edit by hand; export the model again instead. Every part is named climb_<group name>.
 */
final class ClimbGolemParts {

    private ClimbGolemParts() {
    }

    static void addTo(PartDefinition root) {
        PartDefinition climb = root.addOrReplaceChild("climb", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition climb_iron_golem = climb.addOrReplaceChild("climb_iron_golem", CubeListBuilder.create(),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition climb_upper_body = climb_iron_golem.addOrReplaceChild("climb_upper_body", CubeListBuilder.create()
                .texOffs(0, 40).addBox(-9.0F, -2.0F, -6.0F, 18.0F, 12.0F, 11.0F),
                PartPose.offset(0.0F, -31.0F, 0.0F));
        PartDefinition climb_head = climb_upper_body.addOrReplaceChild("climb_head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -12.0F, -5.5F, 8.0F, 10.0F, 8.0F)
                .texOffs(24, 0).addBox(-1.0F, -5.0F, -7.5F, 2.0F, 4.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, -2.0F));
        PartDefinition climb_left_arm = climb_upper_body.addOrReplaceChild("climb_left_arm", CubeListBuilder.create()
                .texOffs(60, 58).addBox(-3.0F, -2.5F, -3.0F, 4.0F, 30.0F, 6.0F),
                PartPose.offset(12.0F, 0.0F, 0.0F));
        PartDefinition climb_right_arm = climb_upper_body.addOrReplaceChild("climb_right_arm", CubeListBuilder.create()
                .texOffs(60, 21).addBox(-2.0F, -2.5F, -3.0F, 4.0F, 30.0F, 6.0F),
                PartPose.offset(-11.0F, 0.0F, 0.0F));
        PartDefinition climb_lower_body = climb_iron_golem.addOrReplaceChild("climb_lower_body", CubeListBuilder.create()
                .texOffs(0, 70).addBox(-4.5F, 10.0F, -3.0F, 9.0F, 5.0F, 6.0F, new CubeDeformation(0.5F)),
                PartPose.offset(0.0F, -31.0F, 0.0F));
        PartDefinition climb_right_leg = climb_lower_body.addOrReplaceChild("climb_right_leg", CubeListBuilder.create()
                .texOffs(37, 0).addBox(-3.5F, -3.0F, -3.0F, 6.0F, 16.0F, 5.0F),
                PartPose.offset(-4.0F, 18.0F, 0.0F));
        PartDefinition climb_left_leg = climb_lower_body.addOrReplaceChild("climb_left_leg", CubeListBuilder.create()
                .texOffs(60, 0).mirror(true).addBox(-3.5F, -3.0F, -3.0F, 6.0F, 16.0F, 5.0F).mirror(false),
                PartPose.offset(5.0F, 18.0F, 0.0F));
        PartDefinition climb_iron_golem2 = climb.addOrReplaceChild("climb_iron_golem2", CubeListBuilder.create(),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition climb_arms = climb_iron_golem2.addOrReplaceChild("climb_arms", CubeListBuilder.create(),
                PartPose.offset(0.0F, -32.0F, -26.0F));
        climb_arms.addOrReplaceChild("climb_arms_left_arm",
                CubeListBuilder.create().texOffs(60, 58).addBox(9.0F, -29.51334F, -2.47137F, 4.0F, 30.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, -1.0F, -1.58825F, 0.0F, 0.0F));
        climb_arms.addOrReplaceChild("climb_arms_right_arm",
                CubeListBuilder.create().texOffs(60, 21).addBox(-13.0F, -29.51334F, -2.47137F, 4.0F, 30.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, -1.0F, -1.58825F, 0.0F, 0.0F));
        PartDefinition climb_upper_body2 = climb_arms.addOrReplaceChild("climb_upper_body2", CubeListBuilder.create()
                .texOffs(0, 40).addBox(-9.0F, -2.0F, -6.0F, 18.0F, 12.0F, 11.0F),
                PartPose.offset(0.0F, 1.0F, 26.0F));
        PartDefinition climb_head2 = climb_upper_body2.addOrReplaceChild("climb_head2", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -8.0F, -5.5F, 8.0F, 10.0F, 8.0F)
                .texOffs(24, 0).addBox(-1.0F, -1.0F, -7.5F, 2.0F, 4.0F, 2.0F),
                PartPose.offset(0.0F, -4.0F, -2.0F));
        PartDefinition climb_lower_body2 = climb_upper_body2.addOrReplaceChild("climb_lower_body2", CubeListBuilder.create()
                .texOffs(0, 70).addBox(-4.5F, -2.0F, -3.0F, 9.0F, 5.0F, 6.0F, new CubeDeformation(0.5F)),
                PartPose.offset(0.0F, 12.0F, 0.0F));
        PartDefinition climb_legs = climb_lower_body2.addOrReplaceChild("climb_legs", CubeListBuilder.create()
                .texOffs(37, 0).addBox(-3.5F, -3.0F, -3.0F, 6.0F, 16.0F, 5.0F)
                .texOffs(60, 0).mirror(true).addBox(5.5F, -3.0F, -3.0F, 6.0F, 16.0F, 5.0F).mirror(false),
                PartPose.offset(-4.0F, 6.0F, 0.0F));
    }
}